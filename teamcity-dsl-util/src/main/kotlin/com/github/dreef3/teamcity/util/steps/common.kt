package com.github.dreef3.teamcity.util.steps

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildStep
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildTypeSettings

fun BuildSteps.copyFrom(source: BuildTypeSettings, predicate: (BuildStep) -> Boolean = { true }) {
    source.steps.items.filter(predicate).forEach { s -> step(s) }
}
