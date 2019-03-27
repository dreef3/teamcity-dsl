package com.github.dreef3.teamcity.dsl.impl.resources

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildType
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.resources.FlywayMigrations
import com.github.dreef3.teamcity.dsl.impl.ServiceResourceImpl
import com.github.dreef3.teamcity.dsl.impl.PipelineExtension
import com.github.dreef3.teamcity.util.steps.addImageToPromoted
import com.github.dreef3.teamcity.util.steps.dockerImage
import com.github.dreef3.teamcity.util.steps.runFlywayMigrations

class FlywayMigrationsImpl : ServiceResourceImpl<FlywayMigrations>() {

    override fun pipelineExtensions(resource: FlywayMigrations): List<PipelineExtension> {
        val buildMigrationsImage: BuildType.() -> Unit = {
            steps {
                dockerImage {
                    imageName = resource.imageName
                    args = resource.args
                    dockerFile = resource.dockerFile
                }
            }
        }

        val runMigrations: BuildType.() -> Unit = {
            steps {
                runFlywayMigrations {
                    dockerImageName = resource.imageName
                    userName = resource.database.user
                    userPassword = resource.database.password
                    databaseHost = resource.database.host
                    databasePort = resource.database.port
                    databaseName = resource.database.name
                    databaseSchema = resource.database.schema
                }
            }
        }

        return listOf(
            PipelineExtension(PipelineStageType.BUILD, buildMigrationsImage),
            PipelineExtension(PipelineStageType.DEPLOY_PROD_LIKE, runMigrations, -1),
            PipelineExtension(PipelineStageType.DEPLOY, runMigrations, -1),
            PipelineExtension(PipelineStageType.PROMOTE, {
                steps {
                    addImageToPromoted(resource.imageName)
                }
            })
        )
    }
}

