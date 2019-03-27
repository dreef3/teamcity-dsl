package com.github.dreef3.teamcity.dsl.impl.stages

import jetbrains.buildServer.configs.kotlin.v2017_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.PipelineType
import com.github.dreef3.teamcity.dsl.ServiceFeatures
import com.github.dreef3.teamcity.dsl.impl.PipelineBuildType
import com.github.dreef3.teamcity.dsl.impl.StageBuildContext
import com.github.dreef3.teamcity.dsl.impl.StageImpl
import com.github.dreef3.teamcity.dsl.stages.BuildStage
import com.github.dreef3.teamcity.util.extensions.toPascalCase
import com.github.dreef3.teamcity.util.extensions.toUUID
import com.github.dreef3.teamcity.util.features.BuildStatusToStashFeature
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.steps.detectImageTag
import com.github.dreef3.teamcity.util.steps.dockerImage

class BuildStageImpl : StageImpl<BuildStage>() {
    override fun build(def: BuildStage, ctx: StageBuildContext, init: PipelineBuildType.() -> Unit): PipelineBuildType {
        val (service, type, parentId) = ctx
        val vcsRoot = service.vcsRootForPipeline(type)
        val autoDetect = type == PipelineType.PULL_REQUEST

        return PipelineBuildType(ctx, def) {
            val bt = type.toString().toPascalCase()

            injectVars = listOf("env.SERVICE_VARIANT", "env.DOCKER_IMAGE_TAG")

            id = "${parentId}_Build$bt"
            uuid = id.toUUID()
            name = "Build"

            vcs {
                root(vcsRoot)
                cleanCheckout = true
            }

            features {
                feature(BuildStatusToStashFeature)
            }

            params {
                text("env.SKIP_ALL", "", "", "", ParameterDisplay.HIDDEN)
            }

            steps {
                if (autoDetect) {
                    detectImageTag()

                    script {
                        name = "Set variant from image tag"
                        scriptContent = """
                        $SHEBANG
                        echo "##teamcity[setParameter name='env.SERVICE_VARIANT' value='${'$'}DOCKER_IMAGE_TAG']";
                        """.trimIndent()
                    }
                }

                if (service.def.features.contains(ServiceFeatures.DEBUG_DEPLOY)) {
                    return@steps
                }

                this@PipelineBuildType.applyExtensions { order < 0 }

                val image = "${service.config.docker.imagePrefix}/${service.def.name}"

                script {
                    scriptContent = """
                    $SHEBANG

                    if [ -z "${'$'}DOCKER_BUILD_REUSE" ]; then
                        exit 0;
                    fi

                    image=${'$'}DOCKER_REGISTRY/$image:${'$'}DOCKER_IMAGE_TAG

                    docker pull ${'$'}image || exit 0
                    hash=$(docker inspect ${'$'}image | jq -r '.[0].Config.Labels["saas.crm.commitHash"]')

                    if [ "${'$'}hash" = "$(git rev-parse HEAD)" ]; then
                        echo "##teamcity[setParameter name='env.SKIP_ALL' value='true']";
                    fi
                    """.trimIndent()
                }

                dockerImage {
                    dockerFile = def.dockerFile
                    workingDir = def.workingDir
                    imageName = image
                }

                this@PipelineBuildType.applyExtensions { order >= 0 }
            }

            init()
        }
    }
}
