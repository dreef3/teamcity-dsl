package com.github.dreef3.teamcity.util.steps

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.Parameter
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.util.snippets.SHEBANG

fun BuildSteps.deployConfigMap(buildParams: List<Parameter>, prefix: String, ns: String) {
    val params = buildParams
        .map { Pair(it.name, it.value) }.toMap()
        .filter { (k, _) -> k.startsWith("env.") }
        .map { (k, v) -> "${k.replace("env.", "")}: '%$k%'" }

    val configMapName = "$prefix-${'$'}{SERVICE_VARIANT}"
    val configMap = """
apiVersion: v1
kind: ConfigMap
metadata:
  name: '$configMapName'
data:
  ${params.joinToString("\n  ")}
"""

    script {
        name = "Generate ConfigMap with build parameters"
        scriptContent =
        """
        $SHEBANG
        echo "$configMap" > $configMapName.yml
        """.trimIndent()
    }

    kubectlApplyStep {
        name = "Deploy ConfigMap"
        configDir = ""
        configFile = "$configMapName.yml"
        namespace = ns
    }
}
