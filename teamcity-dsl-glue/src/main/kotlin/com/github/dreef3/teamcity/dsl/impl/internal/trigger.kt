package com.github.dreef3.teamcity.dsl.impl.internal

import jetbrains.buildServer.configs.kotlin.v2017_2.*
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.dsl.PipelineType
import com.github.dreef3.teamcity.dsl.Product
import com.github.dreef3.teamcity.dsl.impl.ServiceBlueprint
import com.github.dreef3.teamcity.util.extensions.toPascalCase
import com.github.dreef3.teamcity.util.extensions.toUUID
import com.github.dreef3.teamcity.util.features.BuildStatusToStashFeature
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.templates.withCommonBuildSettings

fun triggerStageExtId(id: String, type: PipelineType) =
    "${id}_Trigger${type.toString().toPascalCase()}"

fun createTriggerStage(build: ServiceBlueprint) =
    { type: PipelineType, vcsRoot: VcsRoot, pipelineTrigger: Triggers.() -> Unit ->
        BuildType {
            withCommonBuildSettings()

            val bt = type.toString().toPascalCase()

            id = triggerStageExtId(build.id, type)
            uuid = id.toUUID()
            name = "Trigger"

            params {
                text("env.SERVICE_VARIANT", "", display = ParameterDisplay.HIDDEN)
            }

            vcs {
                root(vcsRoot)
            }

            features {
                feature(BuildStatusToStashFeature)
            }

            triggers {
                pipelineTrigger()
            }
        }
    }

internal fun createTriggerAllStage(product: Product, project: Project): BuildType {
    val id = "${product.id}TriggerAll".toId(project.id)

    val triggerAll = BuildType {
        withCommonBuildSettings()

        this@BuildType.id = id
        uuid = id.toUUID()
        name = "TriggerAll"
        buildNumberPattern = "%build.counter%"

        params {
            text("env.SERVICE_VARIANT", "", display = ParameterDisplay.HIDDEN)
        }

        steps {
            script {
                scriptContent = """
                $SHEBANG

                echo "Triggering QA rebuild"
                """.trimIndent()
            }
        }
    }

    val suffix = "Trigger${PipelineType.DEVELOP.toString().toPascalCase()}"
    project.subProjects
        .flatMap { it.subProjects }
        .flatMap { it.buildTypes }
        .filter { it.id.endsWith(suffix) }
        .forEach { build ->
            triggerAll.dependencies {
                snapshot(build) {
                    reuseBuilds = ReuseBuilds.NO
                    onDependencyFailure = FailureAction.FAIL_TO_START
                }
            }
        }

    return triggerAll
}
