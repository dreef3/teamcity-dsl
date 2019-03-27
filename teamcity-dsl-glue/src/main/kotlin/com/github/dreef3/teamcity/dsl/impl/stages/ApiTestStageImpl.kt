package com.github.dreef3.teamcity.dsl.impl.stages

import jetbrains.buildServer.configs.kotlin.v2017_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.impl.PipelineBuildType
import com.github.dreef3.teamcity.dsl.impl.StageBuildContext
import com.github.dreef3.teamcity.dsl.impl.StageImpl
import com.github.dreef3.teamcity.dsl.impl.StageInit
import com.github.dreef3.teamcity.dsl.stages.ApiTestStage
import com.github.dreef3.teamcity.util.extensions.toPascalCase
import com.github.dreef3.teamcity.util.extensions.toUUID
import com.github.dreef3.teamcity.util.features.BuildStatusToStashFeature
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.steps.installNode
import com.github.dreef3.teamcity.util.steps.installNodeModules
import com.github.dreef3.teamcity.util.steps.npm

class ApiTestStageImpl : StageImpl<ApiTestStage>() {
    override fun build(def: ApiTestStage, ctx: StageBuildContext, init: StageInit): PipelineBuildType {
        val (service, type, parentId) = ctx
        val vcsRoot = service.vcsRootForPipeline(type)

        return PipelineBuildType(ctx, def) {
            val bt = type.toString().toPascalCase()

            id = "${parentId}_ApiTest$bt"
            uuid = id.toUUID()
            name = "ApiTest"

            params {
                text("env.API_BASE_URL", def.url, display = ParameterDisplay.HIDDEN)
            }

            vcs {
                root(vcsRoot)
            }

            features {
                feature(BuildStatusToStashFeature)
            }

            steps {
                // TODO Move to apiSetBaseUrl once we all migrate to new DSL
                script {
                    name = "Set URL of service under test"
                    scriptContent = """
                    $SHEBANG

                    API_BASE_URL=`echo ${'$'}API_BASE_URL | sed "s/__variant__/${'$'}{SERVICE_VARIANT}/g" | sed "s/__env__/${'$'}{APP_ENV}/g"`
                    echo ${'$'}API_BASE_URL
                    echo "##teamcity[setParameter name='env.API_BASE_URL' value='${'$'}{API_BASE_URL}']"
                    """.trimIndent()
                }
                installNode()
                installNodeModules {
                    workingDir = def.path
                }
                npm {
                    name = "Run API tests"
                    workingDir = def.path
                    commands = "run test:api:ci"
                }
            }

            init()
        }
    }
}
