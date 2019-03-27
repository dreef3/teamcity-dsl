package com.github.dreef3.teamcity.dsl.impl

import jetbrains.buildServer.configs.kotlin.v2017_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.Stage
import com.github.dreef3.teamcity.dsl.impl.internal.toBuildParams
import com.github.dreef3.teamcity.util.buildTypes.PipelinePhaseBuildType
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.templates.withCommonBuildSettings

class PipelineBuildType(val context: StageBuildContext,
                        def: Stage,
                        init: PipelineBuildType.() -> Unit = {}) : PipelinePhaseBuildType() {
    var injectVars: List<String> = emptyList()
    var label: String = ""
    val stageType: PipelineStageType = def.type

    init {
        withCommonBuildSettings()

        params {
            text("env.DOCKER_IMAGE_TAG", "", display = ParameterDisplay.HIDDEN)
            text("env.SERVICE_VARIANT", "", display = ParameterDisplay.HIDDEN)
        }

        params(toBuildParams(context.info.product.pipelineParams[context.type]))
        params(toBuildParams(context.service.def.pipelineParams[context.type]))

        init()
    }

    override fun postInit() {
        artifactRules += "\ninject_file.sh"

        dependencies.items.forEach {
            it.artifacts {
                artifactRules += "\ninject_file.sh"
            }
        }

        if (!injectVars.isNotEmpty()) return

        injectVars.forEach {
            if (!params.hasParam(it)) {
                params.text(it, "", display = ParameterDisplay.HIDDEN)
            }
        }

        addInjectStep()
    }

    internal fun applyExtensions(filter: PipelineExtension.() -> Boolean = { true }) {
        context.service.applyExtensions(this, stageType, context.type, filter)
    }

    private fun addInjectStep() {
        val vars = injectVars.joinToString("\n") {
            "echo \\\"##teamcity[setParameter name='$it' value='%$it%']\\\""
        }

        steps.items.add(0, ScriptBuildStep {
            name = "Read parameters from inject_file"
            scriptContent = """
                $SHEBANG
                echo "injectVars = ${this@PipelineBuildType.injectVars}"

                if [ ! -f inject_file.sh ]; then
                    touch inject_file.sh
                fi

                cat inject_file.sh

                bash -x inject_file.sh
                """.trimIndent()
        })

        steps.items.add(steps.items.size, ScriptBuildStep {
            name = "Save parameters to inject_file"
            scriptContent = """
                $SHEBANG
                echo "$vars" > inject_file.sh
                """.trimIndent()
        })
    }

}
