package com.github.dreef3.teamcity.util.steps

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.util.snippets.SHEBANG

fun BuildSteps.addImageToPromoted(name: String) {
    script {
        this@script.name = "Add image $name to promotion list"
        scriptContent = """
        $SHEBANG
        IMAGE_NAMES="${'$'}IMAGE_NAMES $name"
        echo "##teamcity[setParameter name='env.IMAGE_NAMES' value='${'$'}IMAGE_NAMES']"
        """.trimIndent()
    }
}
