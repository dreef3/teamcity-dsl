package com.github.dreef3.teamcity.util.steps

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep
import com.github.dreef3.teamcity.util.snippets.SHEBANG

open class CheckStackExistsStep(init: CheckStackExistsStep.() -> Unit = {}) : ScriptBuildStep() {
    var stack = ""

    init {
        init()
        param("env.DNS_SERVER", "")
        name = "Check that stack already exists"
        scriptContent = """
        $SHEBANG

        STACK_EXISTS="false"

        if [ -n "${'$'}(rancher stacks --format '{{.Stack.Name}}' | grep "^$stack")" ]; then
            STACK_EXISTS="true"
        fi

        echo "##teamcity[setParameter name='env.STACK_EXISTS' value='${'$'}{STACK_EXISTS}']"
        """.trimIndent()
    }
}

fun BuildSteps.checkStackExists(init: CheckStackExistsStep.() -> Unit = {}) {
    step(CheckStackExistsStep(init))
}
