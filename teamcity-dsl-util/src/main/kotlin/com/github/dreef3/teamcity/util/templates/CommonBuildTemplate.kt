package com.github.dreef3.teamcity.util.templates

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildTypeSettings
import jetbrains.buildServer.configs.kotlin.v2017_2.ParameterDisplay

fun BuildTypeSettings.withCommonBuildSettings() {
    params {
        text("env.ISSUE_ID", "", display = ParameterDisplay.HIDDEN)
    }

    failureConditions {
        executionTimeoutMin = 30
    }

    requirements {
        equals("env.docker_agent", "true")
        equals("teamcity.vault.supported", "true")
    }
}
