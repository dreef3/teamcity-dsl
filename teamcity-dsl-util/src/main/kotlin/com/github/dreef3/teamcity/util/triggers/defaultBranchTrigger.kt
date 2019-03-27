package com.github.dreef3.teamcity.util.triggers

import jetbrains.buildServer.configs.kotlin.v2017_2.Triggers
import jetbrains.buildServer.configs.kotlin.v2017_2.triggers.vcs

fun Triggers.defaultBranchTrigger(branch: String = "develop", rules: String = "+:**") {
    vcs {
        triggerRules = rules
        branchFilter = "+:$branch"
    }
}
