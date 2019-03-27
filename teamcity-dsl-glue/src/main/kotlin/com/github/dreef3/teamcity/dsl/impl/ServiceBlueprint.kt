package com.github.dreef3.teamcity.dsl.impl

import jetbrains.buildServer.configs.kotlin.v2017_2.*
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.dsl.*
import com.github.dreef3.teamcity.dsl.impl.internal.PromoteStage
import com.github.dreef3.teamcity.dsl.impl.internal.PromoteStageImpl
import com.github.dreef3.teamcity.dsl.impl.internal.createTriggerStage
import com.github.dreef3.teamcity.dsl.impl.internal.toBuildParams
import com.github.dreef3.teamcity.util.extensions.Pipeline
import com.github.dreef3.teamcity.util.extensions.pipeline
import com.github.dreef3.teamcity.util.extensions.toPascalCase
import com.github.dreef3.teamcity.util.extensions.toUUID
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.triggers.defaultBranchTrigger
import com.github.dreef3.teamcity.util.triggers.pullRequestTrigger
import com.github.dreef3.teamcity.util.vcs.defaultVcsRoots
import com.github.dreef3.teamcity.util.vcs.mainBranch
import com.github.dreef3.teamcity.util.vcs.pullRequest

@TeamCityDslMarker
open class ServiceBlueprint(internal val impl: DslBlueprint,
                            internal val config: DslConfig,
                            internal val def: Service,
                            private val parentProject: Project) {

    val parentId = parentProject.id
    internal val id: String = def.name.toPascalCase().toId(parentId)
    internal val imageName: String
        get() = "${config.docker.imagePrefix}/${def.name}"

    protected var generatedProject: Project? = null

    private val triggerRule: String
        get() = if (def.path.isEmpty() || def.path == ".") "+:**"
        else "+:${def.path}/**".replace("//", "/")

    val deployProdExtId: String
        get() = "DeployProd".toId(PipelineType.PRODUCTION.name.toId(id))

    init {
        def.resources.resources
            .flatMap { listOf(it) + it.dependencies }
            .map { Pair(it, impl.resources[it::class.java] as ServiceResourceImpl<ServiceResource>) }
            .flatMap { (def, impl) -> impl.serviceExtensions(def) }
            .forEach { def.apply(it) }
    }

    fun toProject(info: BuildInfo): Project {
        val build = this

        data class PipelineDef(val pipeline: (BuildInfo) -> Pipeline.() -> Unit,
                               val pipelineType: PipelineType,
                               val stack: StackEnvironment,
                               val condition: () -> Boolean)

        val pipelines = listOf(
            PipelineDef(this::pullRequestPipeline, PipelineType.PULL_REQUEST, StackEnvironment.TEST) {
                def.features.contains(ServiceFeatures.PULL_REQUESTS)
            },
            PipelineDef(this::developPipeline, PipelineType.DEVELOP, StackEnvironment.TEST) { true },
            PipelineDef(this::productionPipeline, PipelineType.PRODUCTION, StackEnvironment.PRODUCTION) {
                def.features.contains(ServiceFeatures.PRODUCTION)
            }
        )
        val projectExtensions = def.resources.resources.map { resource ->
            (impl.resources[resource::class.java] as ServiceResourceImpl<ServiceResource>)
                .serviceProjectExtensions(config, this.def, resource)
        }

        val result = Project {
            id = build.id
            uuid = id.toUUID()
            parentId = build.parentId
            name = build.def.name

            parentProject.defaultVcsRoots(build.def.repository, build.def.name, build.def.mainBranch)

            pipelines
                .filter { (_, _, _, condition) -> condition() }
                .forEach { (fn, type, stack) ->
                    subProject {
                        id = type.name.toUpperCase().toId(this@Project.id)
                        uuid = id.toUUID()
                        parentId = this@Project.id
                        name = type.name.split("_").joinToString(" ").toPascalCase()

                        params(toBuildParams(def.params[StackEnvironment.ALL]))
                        params(toBuildParams(info.product.params[stack]))
                        params(toBuildParams(def.params[stack]))

                        pipeline(fn(info))
                    }
                }

            projectExtensions.forEach { this@Project.apply(it) }
        }

        generatedProject = result

        return result
    }


    fun vcsRootForPipeline(type: PipelineType): VcsRoot {
        return when (type) {
            PipelineType.PULL_REQUEST -> pullRequest(parentProject, def.name)
            PipelineType.DEVELOP -> mainBranch(parentProject, def.name)
            PipelineType.PROD_LIKE -> mainBranch(parentProject, def.name)
            PipelineType.PRODUCTION -> mainBranch(parentProject, def.name)
            PipelineType.ANY -> throw RuntimeException("Incorrect pipeline type")
        }
    }

    fun developPipeline(info: BuildInfo): Pipeline.() -> Unit {
        val type = PipelineType.DEVELOP
        val vars = listOf("env.DOCKER_IMAGE_TAG", "env.APP_ENV", "env.SERVICE_VARIANT")

        return {
            reuseBuilds = ReuseBuilds.SUCCESSFUL

            phase(*buildStages(info, type) {
                injectVars = vars
                params {
                    text("env.DOCKER_IMAGE_TAG", "candidate", display = ParameterDisplay.HIDDEN)
                    text("env.APP_ENV", config.candidate, display = ParameterDisplay.HIDDEN)
                    text("env.SERVICE_VARIANT", config.candidate, display = ParameterDisplay.HIDDEN)
                }
            })
            phase(*deployStages(info, type) {
                injectVars = vars
                label = "Candidate"
            })

            phase(*e2eStages(info, type) {
                injectVars = vars
            })

            phase(promoteStage(PromoteStage(), type, info))

            phase(*deployStages(info, type) {
                injectVars = emptyList()
                label = "Stable"

                params {
                    text("env.DOCKER_IMAGE_TAG", "stable", display = ParameterDisplay.HIDDEN)
                    text("env.APP_ENV", config.stable, display = ParameterDisplay.HIDDEN)
                    text("env.SERVICE_VARIANT", config.stable, display = ParameterDisplay.HIDDEN)
                }
            })

            phase(triggerStage(type) {
                if (!isDebug(info)) {
                    defaultBranchTrigger(def.mainBranch, triggerRule)
                }
            })
        }
    }

    private fun promoteStage(def: PromoteStage, type: PipelineType, info: BuildInfo,
                             init: PipelineBuildType.() -> Unit = {}): (Project) -> BuildType = { project ->
        PromoteStageImpl().build(def, StageBuildContext(this, type, this.id, info, project), init)
    }

    private fun buildStages(info: BuildInfo,
                            type: PipelineType,
                            init: PipelineBuildType.() -> Unit = {}): Array<(Project) -> PipelineBuildType> =
        stagesForType(type, listOf(PipelineStageType.BUILD), def, init, this.id, info).toTypedArray()

    private fun deployStages(info: BuildInfo,
                             type: PipelineType,
                             init: PipelineBuildType.() -> Unit = {}): Array<(Project) -> PipelineBuildType> =
        stagesForType(type, listOf(PipelineStageType.DEPLOY), def, init, this.id, info).toTypedArray()

    private fun deployProdLikeStages(info: BuildInfo,
                                     type: PipelineType,
                                     init: PipelineBuildType.() -> Unit = {}): Array<(Project) -> PipelineBuildType> =
        stagesForType(type, listOf(PipelineStageType.DEPLOY_PROD_LIKE), def, init, this.id, info).toTypedArray()

    private fun dependencyDeployStages(info: BuildInfo): Array<(Project) -> PipelineBuildType> {
        val isAll = { svc: ServiceBlueprint -> svc.def.features.contains(ServiceFeatures.PULL_REQUESTS_ALL_SERVICES) }
        val services = if (isAll(this))
            info.services.filter { isAll(it) }
        else listOf(this)

        return services
            .flatMap {
                it.stagesForType(
                    PipelineType.PULL_REQUEST,
                    listOf(PipelineStageType.DEPLOY_DEPENDENCIES),
                    it.def, {}, this@ServiceBlueprint.id, info)
            }
            .toTypedArray()
    }

    private fun e2eStages(info: BuildInfo,
                          type: PipelineType,
                          init: PipelineBuildType.() -> Unit = {}): Array<(Project) -> BuildType> {
        val dependents = def.frontends
            .map { frontend -> info.services.first { it.def == frontend } }
            .flatMap { frontend ->
                frontend.stagesForType(type, listOf(PipelineStageType.TEST),
                    frontend.def, init, this@ServiceBlueprint.id, info) { (def) -> def.runOnDependencies }
            }
            .toList()

        val e2eTests = dependents + stagesForType(type, listOf(PipelineStageType.TEST),
            def, init, this.id, info)

        return e2eTests.toTypedArray()
    }

    fun pullRequestPipeline(info: BuildInfo): Pipeline.() -> Unit {
        val type = PipelineType.PULL_REQUEST
        val vars = listOf("env.DOCKER_IMAGE_TAG", "env.SERVICE_VARIANT")

        return {
            reuseBuilds = ReuseBuilds.SUCCESSFUL

            phase(*buildStages(info, type))

            phase(*dependencyDeployStages(info))

            phase(*deployStages(info, type) {
                injectVars = vars
                label = type.toString().toPascalCase()
            })

            phase(*e2eStages(info, type) {
                injectVars = vars
            })

            phase(triggerStage(type) {
                if (!isDebug(info)) {
                    trigger(pullRequestTrigger(triggerRule))
                }
            })
        }
    }

    internal fun stagesForType(pipeline: PipelineType,
                               types: List<PipelineStageType>,
                               def: Service,
                               init: PipelineBuildType.() -> Unit,
                               parentId: String = this.id,
                               info: BuildInfo,
                               filter: (Pair<Stage, StageImpl<out Stage>>) -> Boolean = { true }): List<(Project) -> PipelineBuildType> {
        return def.stages.items.asSequence()
            .filter {
                (types.contains(it.type))
                    && (it.pipeline == pipeline || it.pipeline == null)
            }
            .map {
                val stageImpl = impl.stages[it.javaClass] ?: throw IllegalArgumentException("Stage not found")

                Pair(it, stageImpl)
            }
            .filter(filter)
            .map { (def, stageImpl) ->
                { project: Project ->
                    (stageImpl as StageImpl<Stage>).build(def,
                        StageBuildContext(this, pipeline, parentId, info, project), init)
                }
            }.toList()
    }

    private fun getExtensionsForStep(resources: List<ServiceResource>,
                                     type: PipelineStageType,
                                     pipelineType: PipelineType,
                                     filter: PipelineExtension.() -> Boolean): List<PipelineExtension> {

        fun extractExtensions(res: ServiceResource): List<PipelineExtension> {
            val resourceImpl = impl.resources[res.javaClass] as ServiceResourceImpl<ServiceResource>

            requireNotNull(resourceImpl)

            return resourceImpl.pipelineExtensions(res)
                .asSequence()
                .filter { it.type == type && filter(it) }
                .filter { setOf(PipelineType.ANY, pipelineType).contains(it.pipelineType) }
                .toList()
        }

        return resources
            .flatMap { resource ->
                this@ServiceBlueprint
                    .getExtensionsForStep(resource.dependencies, type, pipelineType, filter) + extractExtensions(resource)
            }
            .sortedBy { it.order }
    }


    fun applyExtensions(stage: PipelineBuildType,
                        type: PipelineStageType,
                        pipelineType: PipelineType,
                        filter: PipelineExtension.() -> Boolean) {
        val extensions = getExtensionsForStep(def.resources.resources, type, pipelineType, filter).map { it.fn }

        for (extension in extensions) {
            stage.extension()
        }
    }

    private fun triggerStage(type: PipelineType, trigger: Triggers.() -> Unit): (Project) -> BuildType {
        return { project ->
            createTriggerStage(this)(type, vcsRootForPipeline(type), trigger)
        }
    }

    fun productionPipeline(info: BuildInfo): Pipeline.() -> Unit {
        val type = PipelineType.PRODUCTION

        return {
            phase(promoteStage(PromoteStage().apply {
                from = "stable"
                to = "`date +%%Y%%m%%d%%H%%M`"
            }, type, info) {
                vcs {
                    root(vcsRootForPipeline(type))
                }
                steps {
                    script {
                        name = "Push release tag"
                        scriptContent = """
                            $SHEBANG
                            set -e
                            set +x

                            git tag `date +%%Y%%m%%d%%H%%M`
                            git push --tags
                            """.trimIndent()
                    }
                }
            })

            val deployProdStages = deployStages(info, type) {
                injectVars = listOf("env.DOCKER_IMAGE_TAG", "env.ACTUAL_STACK_NAME")
                label = "Staging"
                name = "[${type.name.toPascalCase()}] DeployStaging"
            }.map { fn ->
                ({ project: Project ->
                    val result = fn(project)

                    result.params {
                        text("env.APP_ENV", "prod", display = ParameterDisplay.HIDDEN)
                    }

                    // FIXME It's a hack to propagate PROD project id instead of service project id
                    result.id = "${project.id}_Deploy${result.label}"
                    result.uuid = project.id.toUUID()
                    result.name = "[${type.toString().toPascalCase()}] Deploy${result.label}"

                    result
                })
            }.toTypedArray()

            phase(*deployProdStages)

            // TODO Smoke, Switch
        }
    }

    private fun isDebug(info: BuildInfo) = def.features.contains(ServiceFeatures.DEBUG)
}
