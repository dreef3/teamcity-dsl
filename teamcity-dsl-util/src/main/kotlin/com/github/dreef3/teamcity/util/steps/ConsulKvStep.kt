package com.github.dreef3.teamcity.util.steps

import com.github.dreef3.teamcity.util.snippets.SHEBANG
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep

enum class ConsulKvAction {
    GET,
    PUT,
}

fun consulCommand(action: ConsulKvAction, key: String, data: String = "",
                  server: String = "%env.SSH_PROXY_HOST%"): String {
    val command = when (action) {
        ConsulKvAction.GET -> "curl 0.0.0.0:8500/v1/kv/$key?raw=true"
        ConsulKvAction.PUT -> "curl -XPUT 0.0.0.0:8500/v1/kv/$key --data '$data'"
    }

    return """
    CONSUL_SERVER=$server

    CMD_OUT=${'$'}(ssh -i ${'$'}{SSH_KEY} ${'$'}{SSH_USER}@${'$'}{CONSUL_SERVER} $command)
    """.trimIndent()
}

open class ConsulKvStep(init: ConsulKvStep.() -> Unit = {}) : ScriptBuildStep() {

    var server = "%env.SSH_PROXY_HOST%"
    var key = ""
    var data = ""
    var action: ConsulKvAction = ConsulKvAction.GET

    init {
        init()
        val postCommand = when (action) {
            ConsulKvAction.GET -> """
            KEY=${'$'}(echo $key | sed 's/[//]/_/g' | sed 's/-//g' | awk '{print toupper($0)}')

            echo "##teamcity[setParameter name='env.CONSUL_${'$'}KEY' value='${'$'}CMD_OUT']"
            echo "##teamcity[setParameter name='env.CONSUL_KEY_VALUE' value='${'$'}CMD_OUT']"
            """.trimIndent()
            ConsulKvAction.PUT -> ""
        }
        scriptContent = """
        $SHEBANG
        ${consulCommand(action, key, data, server)}

        $postCommand
        """.trimIndent()

        name = "Consul: $action $key $data"
    }
}

fun BuildSteps.consulKV(init: ConsulKvStep.() -> Unit = {}) {
    step(ConsulKvStep(init))
}
