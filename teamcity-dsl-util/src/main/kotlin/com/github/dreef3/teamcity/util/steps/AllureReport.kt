package com.github.dreef3.teamcity.util.steps

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildStep
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps

fun BuildSteps.allureReport(logsPath: String = "testResults") {
    step {
        name = "Allure Report"
        this.type = "allureReportGeneratorRunner"
        executionMode = BuildStep.ExecutionMode.ALWAYS

        param("allure.result.directory", logsPath)
        param("allure.report.path.prefix", "allure-report/")
    }
}
