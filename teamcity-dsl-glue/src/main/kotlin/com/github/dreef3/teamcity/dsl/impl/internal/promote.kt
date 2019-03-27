package com.github.dreef3.teamcity.dsl.impl.internal

import jetbrains.buildServer.configs.kotlin.v2017_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.Stage
import com.github.dreef3.teamcity.dsl.impl.PipelineBuildType
import com.github.dreef3.teamcity.dsl.impl.StageBuildContext
import com.github.dreef3.teamcity.dsl.impl.StageImpl
import com.github.dreef3.teamcity.dsl.impl.StageInit
import com.github.dreef3.teamcity.util.extensions.toPascalCase
import com.github.dreef3.teamcity.util.extensions.toUUID
import com.github.dreef3.teamcity.util.snippets.SHEBANG

class PromoteStage: Stage() {
    var from: String = "candidate"
    var to: String = "stable"

    init {
        type = PipelineStageType.PROMOTE
    }
}

class PromoteStageImpl: StageImpl<PromoteStage>() {
    override fun build(def: PromoteStage, ctx: StageBuildContext, init: StageInit): PipelineBuildType =
        PipelineBuildType(ctx, def) {
            val (service, type, parentId) = ctx
            val bt = type.toString().toPascalCase()

            injectVars = listOf("env.SERVICE_VARIANT", "env.DOCKER_IMAGE_TAG")

            id = "${parentId}_Promote$bt"
            uuid = id.toUUID()
            name = "Promote"

            params {
                text("env.IMAGE_NAMES", service.imageName, display = ParameterDisplay.HIDDEN)
            }

            steps {
                this@PipelineBuildType.applyExtensions()

                script {
                    name = "Promote Docker images"
                    scriptContent = """
                $SHEBANG
                TAG=${def.from};
                NEW_TAG=${def.to};
                read -a IMAGES <<< "${'$'}IMAGE_NAMES"

                echo "Promoting ${'$'}TAG to ${'$'}NEW_TAG for images ${'$'}{IMAGES[@]}"

                for name in "${'$'}{IMAGES[@]}"
                do
                    IMAGE=${'$'}DOCKER_REGISTRY/${'$'}name
                    echo ${'$'}IMAGE
                    docker pull ${'$'}IMAGE:${'$'}TAG

                    LAST_PROMOTED_TAG=`curl -H "Authorization: Basic ${'$'}DOCKER_AUTH_TOKEN" https://${'$'}DOCKER_REGISTRY/v2/${'$'}name/tags/list | jq -r .[.tags] | egrep "^[0-9]{12}" | tail -1`

                    if [[ ${'$'}NEW_TAG != "stable" && -n ${'$'}LAST_PROMOTED_TAG ]]; then
                        LAST_BUILDED_HASH=`docker inspect --format='{{index .Config.Labels "saas.crm.commitHash"}}' ${'$'}IMAGE:${'$'}TAG`

                        docker pull ${'$'}IMAGE:${'$'}{LAST_PROMOTED_TAG}

                        LAST_PROMOTED_HASH=`docker inspect --format='{{index .Config.Labels "saas.crm.commitHash"}}' ${'$'}IMAGE:${'$'}{LAST_PROMOTED_TAG}`

                        if [[ -n ${'$'}{LAST_BUILDED_HASH} ]] && [[ "${'$'}{LAST_BUILDED_HASH}" == "${'$'}{LAST_PROMOTED_HASH}" ]]; then
                            echo "Skipping promote for ${'$'}IMAGE:${'$'}TAG"
                            NEW_TAG=${'$'}{LAST_PROMOTED_TAG}
                            break
                        fi
                    fi

                    docker tag ${'$'}IMAGE:${'$'}TAG ${'$'}IMAGE:${'$'}NEW_TAG
                    docker push ${'$'}IMAGE:${'$'}NEW_TAG
                done
                echo "##teamcity[setParameter name='env.DOCKER_IMAGE_TAG' value='${'$'}NEW_TAG']"
                """.trimIndent()
                }
            }

            buildNumberPattern = "%build.counter%"

            init()
        }
}
