package com.github.dreef3.teamcity.util.steps

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep
import com.github.dreef3.teamcity.util.snippets.SHEBANG

class HelmDeleteBuildStep(init: HelmDeleteBuildStep.() -> Unit): ScriptBuildStep() {
    var release = ""
    var namespace = "%k8s.namespace%"
    var purge = true

    init {
        init()

        param("k8s.namespace", "")

        val dockerOpts = "-e CLUSTER=%env.K8S_CLUSTER%" +
            " -e USER=%k8s.user.login%" +
            " -e PASSWORD='%k8s.user.password%'"

        name = "Delete Helm release"

        scriptContent = """
        $SHEBANG

        docker run --rm $dockerOpts -t \
        k8s/client:latest \
        helm delete ${if (purge) "--purge" else ""} \
        --tiller-namespace $namespace \
        $release
        """.trimIndent()
    }
}

fun BuildSteps.helmDelete(init: HelmDeleteBuildStep.() -> Unit) {
    step(HelmDeleteBuildStep(init))
}
