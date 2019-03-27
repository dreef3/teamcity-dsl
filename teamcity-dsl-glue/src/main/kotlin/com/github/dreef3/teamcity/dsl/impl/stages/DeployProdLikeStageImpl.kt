package com.github.dreef3.teamcity.dsl.impl.stages

import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.ProductionParameters
import com.github.dreef3.teamcity.dsl.StackEnvironment
import com.github.dreef3.teamcity.dsl.impl.PipelineBuildType
import com.github.dreef3.teamcity.dsl.impl.StageBuildContext
import com.github.dreef3.teamcity.dsl.impl.StageImpl
import com.github.dreef3.teamcity.dsl.impl.StageInit
import com.github.dreef3.teamcity.dsl.stages.DeployProdLikeStage
import com.github.dreef3.teamcity.util.extensions.toPascalCase
import com.github.dreef3.teamcity.util.extensions.toUUID
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.steps.consulKV
import com.github.dreef3.teamcity.util.steps.rancherUp
import com.github.dreef3.teamcity.util.steps.warmUpService
import com.github.dreef3.teamcity.util.triggers.defaultBranchTrigger

class DeployProdLikeStageImpl : StageImpl<DeployProdLikeStage>() {
    override fun build(def: DeployProdLikeStage, ctx: StageBuildContext, init: StageInit): PipelineBuildType {
        val (service, type, parentId, info) = ctx
        return PipelineBuildType(ctx, def) {
            val bt = type.toString().toPascalCase()

            injectVars = listOf()

            init()

            val stackComposeFile = info.product.params[StackEnvironment.ALL]?.get("env.DOCKER_COMPOSE_FILE")!!.second
            val stackName = info.product.params[StackEnvironment.ALL]?.get("env.RANCHER_STACK")!!.second + "_prodlike"

            require(stackComposeFile.isNotEmpty())
            require(stackName.isNotEmpty())

            triggers {
                defaultBranchTrigger()
            }

            steps {
                consulKV {
                    server = ProductionParameters()["env.SSH_PROXY_HOST"]!!.second
                    key = "saas/${info.product.id.toLowerCase()}/prod"
                }
                script {
                    name = "Get image tag of current production version"
                    scriptContent = """
                    $SHEBANG
                    RANCHER_ACCESS_KEY=${ProductionParameters()["env.RANCHER_ACCESS_KEY"]!!.second}
                    RANCHER_SECRET_KEY=${ProductionParameters()["env.RANCHER_SECRET_KEY"]!!.second}
                    RANCHER_URL=${ProductionParameters()["env.RANCHER_URL"]!!.second}
                    DOCKER_IMAGE=${'$'}(rancher inspect --format="{{.imageUuid}}" `rancher ps -c | egrep ${"$"}{CONSUL_KEY_VALUE}-${service.def.name}-\([0-9]+\) | awk '{print ${'$'}1}'`)
                    IMAGE_TAG=$(echo ${"$"}{DOCKER_IMAGE} | sed 's/:/ /g' | awk '{print ${'$'}4}')
                    echo "##teamcity[setParameter name='env.DOCKER_IMAGE_TAG' value='${'$'}IMAGE_TAG']"
                    """.trimIndent()
                }

                this@PipelineBuildType.applyExtensions { order < 0 }
                rancherUp {
                    composeFile = stackComposeFile
                    stack = stackName
                    this@rancherUp.service = service.def.name
                }
//
//                if (service.def.healthCheckUrl.isNotEmpty()) {
//                    warmUpService {
//                        this@warmUpService.prefix = prefix.def.name
//                        url = prefix.def.healthCheckUrl
//                    }
//                }

                this@PipelineBuildType.applyExtensions { order >= 0 }
            }

            id = "${parentId}_Deploy$label"
            uuid = id.toUUID()
            name = "Deploy$label"
        }
    }
}
