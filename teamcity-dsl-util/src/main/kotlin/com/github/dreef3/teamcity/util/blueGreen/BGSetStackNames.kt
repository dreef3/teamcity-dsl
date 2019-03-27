package com.github.dreef3.teamcity.util.blueGreen

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.steps.ConsulKvAction
import com.github.dreef3.teamcity.util.steps.consulCommand
import com.github.dreef3.teamcity.util.steps.consulKV

open class BGSetStackNames(init: BGSetStackNames.() -> Unit = {}) {
    fun inject(steps: BuildSteps) {
        val stackKey = this.key
        steps.consulKV {
            action = ConsulKvAction.GET
            key = stackKey
        }
        steps.script {
            name = "Set blue-green deployment stack names"
            scriptContent = """
            $SHEBANG

            KEY=${'$'}(echo $stackKey | sed 's/[//]/_/g' | sed 's/-//g' | awk '{print toupper(${'$'}0)}')

            stack_name_env_var=CONSUL_${'$'}{KEY}

            if [ -z "${'$'}{!stack_name_env_var}" ]; then
                echo "Failed to read current value for prod stack. \
                    Will set $stackKey into 'green'"

                ${consulCommand(ConsulKvAction.PUT, stackKey, "green")}

                declare "${'$'}stack_name_env_var"="green"
            fi

            BG_PROD_STACK=""
            BG_STAGING_STACK=""

            if [ ${'$'}{!stack_name_env_var} != "blue" ] && [ ${'$'}{!stack_name_env_var} != "green" ]; then
                echo "Invalid stack name value: ${'$'}{!stack_name_env_var}"
                exit 1
            fi

            BG_PROD_STACK=${'$'}{!stack_name_env_var}
            if [[ ${'$'}BG_PROD_STACK == "green" ]]; then
                BG_STAGING_STACK="blue"
            else
                BG_STAGING_STACK="green"
            fi

            echo "##teamcity[setParameter name='env.BG_PROD_STACK' value='${'$'}BG_PROD_STACK']"
            echo "##teamcity[setParameter name='env.BG_STAGING_STACK' value='${'$'}BG_STAGING_STACK']"
            echo "##teamcity[setParameter name='env.SERVICE_VARIANT' value='${'$'}BG_STAGING_STACK']"
            """.trimIndent()
        }
    }

    var key = ""

    init {
        init()
        require(key.isNotEmpty())
    }
}

fun BuildSteps.bgSetOppositeStackName(init: BGSetStackNames.() -> Unit = {}) {
    BGSetStackNames(init).inject(this)
}
