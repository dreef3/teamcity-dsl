package com.github.dreef3.teamcity.dsl.impl.internal

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2017_2.toId
import com.github.dreef3.teamcity.dsl.Product
import com.github.dreef3.teamcity.util.blueGreen.bgSetOppositeStackName
import com.github.dreef3.teamcity.util.extensions.toUUID
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.steps.ConsulKvAction
import com.github.dreef3.teamcity.util.steps.consulKV
import com.github.dreef3.teamcity.util.templates.withCommonBuildSettings

fun createSwitchProdStage(product: Product, parentId: String) = BuildType {
    id = "SwitchProd".toId(parentId)
    uuid = id.toUUID()
    name = "SwitchProd"

    steps {
        bgSetOppositeStackName {
            key = "saas/${product.id.toLowerCase()}/prod"
        }
        consulKV {
            action = ConsulKvAction.PUT
            key = "saas/${product.id.toLowerCase()}/prod"
            data = "%env.BG_STAGING_STACK%"
        }
        consulKV {
            action = ConsulKvAction.PUT
            key = "saas/${product.id.toLowerCase()}/staging"
            data = "%env.BG_PROD_STACK%"
        }
        script {
            name = "Notify release bot"
            scriptContent = """
            $SHEBANG
            curl -d '{"project":"%teamcity.project.id%"}' -H "Content-Type: application/json"\
            -X POST %release.bot.host%/webhook
            """.trimIndent()
        }
    }

    withCommonBuildSettings()
}
