package com.github.dreef3.teamcity.util.steps

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildStep
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps

open class NpmBuildStep(init: NpmBuildStep.() -> Unit = {}) : BuildStep() {
    var workingDir by stringParameter("teamcity.build.workingDir")

    var commands by stringParameter("npm_commands")

    init {
        type = "jonnyzzz.npm"
        init()
        name = "npm $commands"
    }
}

fun BuildSteps.npm(init: NpmBuildStep.() -> Unit = {}) {
    step(NpmBuildStep(init))
}

fun BuildSteps.installNode() {
    step {
        name = "Install Node.js"
        type = "jonnyzzz.nvm"
        param("version", "10.15.0")
    }
    npm {
        commands = "install -g npm"
    }
}

fun BuildSteps.installNodeModules(init: NpmBuildStep.() -> Unit = {}) {
    npm {
        commands = "ci"
        init()
    }
}
