package com.github.dreef3.teamcity.util.triggers

import jetbrains.buildServer.configs.kotlin.v2017_2.triggers.VcsTrigger

object PullRequestTrigger : VcsTrigger({
    triggerRules = """
                +:**
                -:comment=^Merge pull request #.*
            """.trimIndent()
    branchFilter = """
                +:*
                -:<default>
                -:master
            """.trimIndent()
})

fun pullRequestTrigger(rules: String = "+:**"): VcsTrigger = VcsTrigger {
    triggerRules = """
            $rules
            -:comment=^Merge pull request #.*
        """.trimIndent()
    branchFilter = """
            +:*
            -:<default>
            -:master
        """.trimIndent()
}
