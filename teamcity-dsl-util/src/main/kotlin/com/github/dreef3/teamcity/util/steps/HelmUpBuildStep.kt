package com.github.dreef3.teamcity.util.steps

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.snippets.SKIP_STEP_CHECK

open class HelmUpBuildStep(init: HelmUpBuildStep.() -> Unit = {}) : ScriptBuildStep() {

    var imageTag by varOrParam("env.DOCKER_IMAGE_TAG")
    var prefix by varOrParam("env.RELEASE_PREFIX")
    var variant by varOrParam("env.SERVICE_VARIANT")
    var skip = false
    var namespace by varOrParam("k8s.namespace")
    var chartPath by varOrParam("env.HELM_CHART_PATH")
    var helmOpts by varOrParam("env.HELM_OPTS")

    var cluster by varOrParam("env.K8S_CLUSTER")
    var user by varOrParam("k8s.user.login")
    var password by varOrParam("k8s.user.password")

    init {
        init()

        name = "Run helm upgrade"
        scriptContent = """
        $SHEBANG

        ${SKIP_STEP_CHECK(skip)}

        docker run --rm -t \
        -v %teamcity.build.workingDir%:/work \
        -e USER='$user' \
        -e PASSWORD='$password' \
        -e CLUSTER='$cluster' \
        -e NAMESPACE='$namespace' \
        -e CHART_PATH='$chartPath' \
        -e SERVICE_VARIANT='$variant' \
        -e HELM_OPTS='$helmOpts' \
        -e DOCKER_IMAGE_TAG='$imageTag' \
        -e RELEASE_PREFIX='$prefix' \
        saas-crm/helm-up
        """.trimIndent()
    }
}

fun BuildSteps.helmUp(init: HelmUpBuildStep.() -> Unit = {}) {
    step(HelmUpBuildStep(init))
}
