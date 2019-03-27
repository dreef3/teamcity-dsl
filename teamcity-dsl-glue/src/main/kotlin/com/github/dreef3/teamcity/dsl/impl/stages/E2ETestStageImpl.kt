package com.github.dreef3.teamcity.dsl.impl.stages

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildStep
import jetbrains.buildServer.configs.kotlin.v2017_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.impl.PipelineBuildType
import com.github.dreef3.teamcity.dsl.impl.StageBuildContext
import com.github.dreef3.teamcity.dsl.impl.StageImpl
import com.github.dreef3.teamcity.dsl.impl.StageInit
import com.github.dreef3.teamcity.dsl.stages.E2ETestStage
import com.github.dreef3.teamcity.util.extensions.toPascalCase
import com.github.dreef3.teamcity.util.extensions.toUUID
import com.github.dreef3.teamcity.util.features.BuildStatusToStashFeature
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.steps.installNode
import com.github.dreef3.teamcity.util.steps.installNodeModules
import com.github.dreef3.teamcity.util.steps.npm

val counters = hashMapOf<String, Int>()

class E2ETestStageImpl : StageImpl<E2ETestStage>() {
    override fun build(def: E2ETestStage, ctx: StageBuildContext, init: StageInit): PipelineBuildType {
        val (service, type, parentId) = ctx
        val vcsRoot = service.vcsRootForPipeline(type)

        return PipelineBuildType(ctx, def) {
            val bt = type.toString().toPascalCase()
            val counter = counters.compute(parentId) { _, i -> (i ?: 0) + 1 }

            injectVars = listOf("env.SERVICE_VARIANT")

            id = "${parentId}_E2E$counter$bt"
            uuid = id.toUUID()
            name = "E2E$counter"

            params {
                text("env.E2E_BASE_URL", def.url, display = ParameterDisplay.HIDDEN)
                text("env.npm_config_api", "%env.APP_ENV%", display = ParameterDisplay.HIDDEN)
            }

            vcs {
                root(vcsRoot)
            }

            failureConditions {
                executionTimeoutMin = 120
            }

            features {
                feature(BuildStatusToStashFeature)
            }

            artifactRules = """
                        tmp/testResults.tar.gz
                        tmp/allureResults-e2e.tar.gz
                    """.trimIndent()

            steps {
                script {
                    name = "Set E2E tests browser URL"
                    scriptContent = """
                    $SHEBANG
                    E2E_BASE_URL=`echo ${'$'}E2E_BASE_URL | sed "s/__variant__/${'$'}{SERVICE_VARIANT}/g" | sed "s/__env__/${'$'}{APP_ENV}/g"`
                    echo ${'$'}E2E_BASE_URL
                    echo "##teamcity[setParameter name='env.E2E_BASE_URL' value='${'$'}{E2E_BASE_URL}']"
                    """.trimIndent()
                }

                installNode()
                installNodeModules {
                    workingDir = def.path
                }
                npm {
                    workingDir = def.path
                    commands = "run e2e:ci"
                }
                npm {
                    workingDir = def.path
                    commands = """
                            run allure:update
                            run allure:e2e
                            """.trimIndent()
                    executionMode = BuildStep.ExecutionMode.ALWAYS
                }
                npm {
                    workingDir = def.path
                    commands = "run e2e:artifact"
                    executionMode = BuildStep.ExecutionMode.ALWAYS
                }
                step {
                    name = "Allure Report"
                    this.type = "allureReportGeneratorRunner"
                    executionMode = BuildStep.ExecutionMode.ALWAYS
                    param("allure.result.directory", "${def.path}/tmp/allure/e2e/")
                    param("allure.report.path.prefix", "allure-report/")
                }
            }

            init()

        }
    }
}
