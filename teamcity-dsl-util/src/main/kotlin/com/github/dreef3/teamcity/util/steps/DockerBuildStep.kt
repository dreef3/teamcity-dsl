package com.github.dreef3.teamcity.util.steps

import com.github.dreef3.teamcity.util.snippets.SHEBANG
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.util.snippets.SKIP_STEP_CHECK

open class DockerBuildStep(init: DockerBuildStep.() -> Unit = {}) : ScriptBuildStep() {

    var dockerFile = "Dockerfile"
    var dockerRegistry by stringParameter("env.DOCKER_REGISTRY")
    var imageTag = ""
    var imageName = ""
    var args = ""
    var push = true
    var skip = false

    init {
        init()
        name = "Run docker build for $imageName:%env.DOCKER_IMAGE_TAG%"
        scriptContent = """
        $SHEBANG

        ${SKIP_STEP_CHECK(skip)}

        DOCKER_IMAGE_TAG=${'$'}{DOCKER_IMAGE_TAG:-$imageTag}
        DOCKER_IMAGE_NAME=${'$'}{DOCKER_IMAGE_NAME:-$imageName}
        WORKING_DIR="%teamcity.build.workingDir%"

        TAG="${'$'}DOCKER_REGISTRY/${'$'}DOCKER_IMAGE_NAME:${'$'}DOCKER_IMAGE_TAG"

        echo "Docker image tag: ${'$'}TAG"

        REPO_NAME=`basename %vcsroot.url% | sed s'/\.git//'`

        docker build --pull --force-rm --no-cache -t=${'$'}TAG -f $dockerFile \
                     $args --build-arg COMMIT_HASH=%build.vcs.number% \
                     --build-arg REPO_NAME=${'$'}{REPO_NAME} \
                     -- ${'$'}WORKING_DIR

        if [[ "$push" == "true" ]]; then
            docker push ${'$'}TAG
        fi
        """.trimIndent()
    }
}

fun BuildSteps.dockerImage(init: DockerBuildStep.() -> Unit = {}) {
    val step = DockerBuildStep(init)
    step(step)
}

fun BuildSteps.detectImageTag(init: GetPRJiraNumberBuildStep.() -> Unit = {}) {
    jiraNumberForPR(init)
    script {
        name = "Set image tag for Docker build"
        scriptContent = """
        $SHEBANG

        ESCAPED_TAG=""
        if [[ %teamcity.build.branch% == "refs/heads/develop" || %teamcity.build.branch% == "develop" ]]; then
            ESCAPED_TAG="develop"
        else
            ESCAPED_TAG=`echo "${'$'}ISSUE_ID" | sed 's_/_-_' | awk '{print tolower(${'$'}0)}'`;
        fi

        # If ESCAPED TAG is still empty than fail the build
        if [[ -z ${'$'}ESCAPED_TAG ]]; then exit 1; fi

        echo ${'$'}ESCAPED_TAG
        echo "##teamcity[setParameter name='env.DOCKER_IMAGE_TAG' value='${'$'}ESCAPED_TAG']";
        """.trimIndent()
    }
}
