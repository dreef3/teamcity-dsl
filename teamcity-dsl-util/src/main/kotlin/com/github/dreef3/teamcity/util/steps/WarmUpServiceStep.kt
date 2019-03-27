package com.github.dreef3.teamcity.util.steps

import com.github.dreef3.teamcity.util.snippets.SHEBANG
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep

open class WarmUpServiceStep(init: WarmUpServiceStep.() -> Unit = {}) : ScriptBuildStep() {
    companion object {
        const val TYPE = "warmUp"
    }

    var url = ""
    var service = ""
    var code = "200"

    init {
        init()

        name = TYPE
        scriptContent = """
        $SHEBANG

        if  [ -z SSH_USER ] || [ -z SSH_PROXY_HOST ] || [ -z SSH_KEY ]; then
            echo "Invalid SSH proxy parameters!";
            exit 1;
        fi

        if [ -z "$service" ] || [ -z "$url" ] || [ -z "${'$'}SERVICE_VARIANT" ]; then
            echo "Not all required parameters are set!";
            exit 1;
        fi

        host=$service-${'$'}SERVICE_VARIANT.saascrm.internal
        url=http://${'$'}host$url

        timeout=2
        prev_timeout=1
        retries=10

        command="/bin/bash -c \"curl --insecure -s -o /dev/null -w '%{http_code}' --resolve ${'$'}host:80:127.0.0.1 ${'$'}url\""

        for i in ${'$'}(seq ${'$'}retries); do
            code=${'$'}(ssh -i ${'$'}{SSH_KEY} ${'$'}{SSH_USER}@${'$'}{SSH_PROXY_HOST} ${'$'}command)
            if [[ "${'$'}code" != "$code" ]]; then
                echo "No response from $service, waiting for ${'$'}{timeout} seconds. Retries left: ${'$'}((retries-i))"

                sleep ${'$'}timeout
                old_timeout=${'$'}timeout
                timeout=${'$'}((timeout+prev_timeout))
                prev_timeout=${'$'}old_timeout
            else
                exit 0
            fi
        done

        exit 1
        """.trimIndent()
    }
}

fun BuildSteps.warmUpService(init: WarmUpServiceStep.() -> Unit = {}) {
    step(WarmUpServiceStep(init))
}
