package com.github.dreef3.teamcity.dsl.impl.resources

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.resources.DjangoMigrations
import com.github.dreef3.teamcity.dsl.impl.ServiceResourceImpl
import com.github.dreef3.teamcity.dsl.impl.PipelineExtension
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.snippets.SKIP_STEP_CHECK

class DjangoMigrationsImpl : ServiceResourceImpl<DjangoMigrations>() {
    override fun pipelineExtensions(resource: DjangoMigrations): List<PipelineExtension> {
        val runMigrations: BuildType.() -> Unit = {
            steps {
                script {
                    name = "Run Django migrations using ${resource.imageName}"

                    scriptContent = """
                    $SHEBANG

                    ${SKIP_STEP_CHECK(false)}

                    DOCKER_IMAGE_NAME=${'$'}{DOCKER_IMAGE_NAME:-${resource.imageName}}
                    TAG="${'$'}DOCKER_REGISTRY/${'$'}DOCKER_IMAGE_NAME:${'$'}DOCKER_IMAGE_TAG"
                    echo "Docker image tag: ${'$'}TAG"

                    docker pull ${'$'}TAG
                    docker run --network host \
                    -e DB_HOST=${resource.database.host} \
                    -e DB_PORT=${resource.database.port} \
                    -e DB_NAME=${resource.database.name} \
                    -e DB_USER=${resource.database.user} \
                    -e DB_PASSWORD=${resource.database.password} \
                    ${'$'}TAG \
                    migrate
                    """.trimIndent()
                }
            }
        }

        return listOf(
            PipelineExtension(PipelineStageType.DEPLOY, runMigrations, -1)
        )
    }

}

