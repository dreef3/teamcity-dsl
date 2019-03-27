package com.github.dreef3.teamcity.dsl.impl.stages

import jetbrains.buildServer.configs.kotlin.v2017_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2017_2.toId
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.PipelineType
import com.github.dreef3.teamcity.dsl.StackEnvironment
import com.github.dreef3.teamcity.dsl.impl.*
import com.github.dreef3.teamcity.dsl.impl.internal.toBuildParams
import com.github.dreef3.teamcity.dsl.stages.DependenciesDeployStage
import com.github.dreef3.teamcity.util.extensions.toPascalCase
import com.github.dreef3.teamcity.util.extensions.toUUID
import com.github.dreef3.teamcity.util.snippets.SHEBANG

class DependenciesDeployStageImpl : StageImpl<DependenciesDeployStage>() {
    override fun build(def: DependenciesDeployStage, ctx: StageBuildContext, init: StageInit): PipelineBuildType {
        val (service, _, parentId, info) = ctx

        return PipelineBuildType(ctx, def) {
            buildNumberPattern = "%build.counter%"
            injectVars = listOf("env.SERVICE_VARIANT", "env.DOCKER_IMAGE_TAG")

            id = "DeployFeature_${service.def.name.toPascalCase()}".toId(parentId)
            uuid = id.toUUID()
            name = "Deploy Dependency [${service.id}]"

            params(toBuildParams(info.product.pipelineParams[PipelineType.PULL_REQUEST]))
            params(toBuildParams(service.def.params[StackEnvironment.ALL]))
            params(toBuildParams(service.def.params[StackEnvironment.TEST]))
            params(toBuildParams(service.def.pipelineParams[PipelineType.PULL_REQUEST]))

            params {
                text("env.SERVICE_VARIANT", "", display = ParameterDisplay.HIDDEN)
                text("env.STACK_EXISTS", "", display = ParameterDisplay.HIDDEN)
            }

            steps {
                script {
                    name = "Save actual image tag for later"
                    scriptContent = """
                            $SHEBANG
                            echo "##teamcity[setParameter name='env.ACTUAL_DOCKER_IMAGE_TAG' value='${'$'}DOCKER_IMAGE_TAG']";
                            echo "##teamcity[setParameter name='env.DOCKER_IMAGE_TAG' value='stable']";
                            """.trimIndent()
                }
            }

            service.applyExtensions(this, PipelineStageType.DEPLOY_DEPENDENCIES, PipelineType.PULL_REQUEST) { true }
            service.applyExtensions(this, PipelineStageType.DEPLOY, PipelineType.PULL_REQUEST) { true }

            steps {
                script {
                    name = "Restore actual image tag"
                    scriptContent = """
                    $SHEBANG
                    echo "##teamcity[setParameter name='env.DOCKER_IMAGE_TAG' value='${'$'}ACTUAL_DOCKER_IMAGE_TAG']";
                    """.trimIndent()
                }
            }

            init()
        }
    }
}
