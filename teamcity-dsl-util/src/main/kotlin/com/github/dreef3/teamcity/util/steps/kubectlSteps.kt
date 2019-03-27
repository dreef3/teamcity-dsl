package com.github.dreef3.teamcity.util.steps

import com.github.dreef3.teamcity.util.snippets.SKIP_STEP_CHECK
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep
import com.github.dreef3.teamcity.util.snippets.SHEBANG

open class KubectlBuildStep(init: KubectlBuildStep.() -> Unit = {}) : ScriptBuildStep() {

    var configDir = "configs"
    var configFile = ""
    var namespace = ""
    var skip = false

    init {
        init()

        name = "Deploy k8s resource $configFile into $namespace"

        scriptContent = """
        $SHEBANG
        ${SKIP_STEP_CHECK(skip)}

        docker run --rm -e CLUSTER=%env.K8S_CLUSTER% \
                        -e USER=%k8s.user.login% \
                        -e PASSWORD='%k8s.user.password%'\
                        -v %teamcity.build.workingDir%/$configDir:/tmp/kubectl \
                        k8s/client \
                        kubectl --namespace=$namespace apply -f /tmp/kubectl/$configFile

        """.trimIndent()
    }

}

fun BuildSteps.kubectlApplyStep(init: KubectlBuildStep.() -> Unit = {}) {
    step(KubectlBuildStep(init))
}


open class EnvsubstConfigStep(init: EnvsubstConfigStep.() -> Unit = {}) : ScriptBuildStep() {
    var inFile = ""
    var outFile = "out.yml"
    var outDir = "./"

    init {
        init()

        name = "Populate ConfigMap from environment variables"

        scriptContent = """
        $SHEBANG

        mkdir -p $outDir

        envsubst < $inFile > $outDir/$outFile

        """.trimIndent()
    }

}

fun BuildSteps.envsubstConfigStep(init: EnvsubstConfigStep.() -> Unit = {}) {
    step(EnvsubstConfigStep(init))
}
