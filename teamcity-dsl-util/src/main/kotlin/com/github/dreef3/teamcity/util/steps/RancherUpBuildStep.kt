package com.github.dreef3.teamcity.util.steps

import com.github.dreef3.teamcity.util.snippets.SKIP_STEP_CHECK
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep
import com.github.dreef3.teamcity.util.snippets.SHEBANG

val MAX_RETRIES = 10

open class RancherUpBuildStep(init: RancherUpBuildStep.() -> Unit = {}) : ScriptBuildStep() {
    var imageTag = ""
    var service = ""

    var dnsServer = ""
    var composeFile = ""
    var rancherFile = ""
    var stack = ""
    var variant = "${'$'}DOCKER_IMAGE_TAG"
    var skip = false

    init {
        init()

        param("env.DNS_SERVER", "")

        name = "Deploy stack to Rancher"

        scriptContent = """
        $SHEBANG

        ${SKIP_STEP_CHECK(skip)}

        export DOCKER_IMAGE_TAG=${'$'}{DOCKER_IMAGE_TAG:-$imageTag}
        export SERVICE_VARIANT=${'$'}{SERVICE_VARIANT:-$variant}
        export DNS_SERVER=${'$'}{DNS_SERVER:-$dnsServer}
        export RANCHER_STACK=${'$'}{RANCHER_STACK:-$stack}
        export DOCKER_COMPOSE_FILE=${'$'}{DOCKER_COMPOSE_FILE:-$composeFile}
        export RANCHER_COMPOSE_FILE=${'$'}{RANCHER_COMPOSE_FILE:-$rancherFile}
        export STACK="${'$'}{RANCHER_STACK}-${'$'}{SERVICE_VARIANT}"

        echo "Docker image tag: ${'$'}DOCKER_IMAGE_TAG"
        echo "Rancher stack: ${'$'}STACK"
        echo "DNS server: ${'$'}DNS_SERVER"
        echo "Variant: ${'$'}SERVICE_VARIANT"
        echo "Docker-compose file: ${'$'}DOCKER_COMPOSE_FILE"
        echo "Rancher compose file: ${'$'}RANCHER_COMPOSE_FILE"

        if  [ -z "${'$'}SERVICE_VARIANT" ] || \
            [ -z "${'$'}DNS_SERVER" ] || \
            [ -z "${'$'}RANCHER_STACK" ] || \
            [ -z "${'$'}DOCKER_COMPOSE_FILE" ] || \
            [ -z "${'$'}DOCKER_IMAGE_TAG" ]; then
            echo "Not all required parameters are set!"
            exit 1;
        fi

        export RANCHER_COMPOSE_ARG="";

        if [ -n "${'$'}RANCHER_COMPOSE_FILE" ]; then
            RANCHER_COMPOSE_ARG="--rancher-file ${'$'}RANCHER_COMPOSE_FILE";
        fi

        retries=0
        until [ ${'$'}retries -ge $MAX_RETRIES ]
        do
            rancher up -d -f "${'$'}{DOCKER_COMPOSE_FILE}" \
                ${'$'}RANCHER_COMPOSE_ARG \
                --upgrade --pull --force-upgrade --confirm-upgrade \
                --interval 30000 --batch-size 1 \
                --stack "${'$'}{STACK}" \
                $service && break || sleep 5

            retries=$((retries+1))
        done

        echo "##teamcity[setParameter name='env.SERVICE_VARIANT' value='${'$'}{SERVICE_VARIANT}']"
        echo "##teamcity[setParameter name='env.ACTUAL_STACK_NAME' value='${'$'}{STACK}']"
        """.trimIndent()
    }
}

fun BuildSteps.rancherUp(init: RancherUpBuildStep.() -> Unit = {}) {
    step(RancherUpBuildStep(init))
}
