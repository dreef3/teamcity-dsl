package com.github.dreef3.teamcity.dsl.impl.resources

import jetbrains.buildServer.configs.kotlin.v2017_2.ParameterDisplay
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.resources.Postgres
import com.github.dreef3.teamcity.dsl.impl.ServiceResourceImpl
import com.github.dreef3.teamcity.dsl.impl.PipelineBuildType
import com.github.dreef3.teamcity.dsl.impl.PipelineExtension
import com.github.dreef3.teamcity.util.steps.createPostgresFeatureDB

class PostgresImpl: ServiceResourceImpl<Postgres>() {
    override fun pipelineExtensions(resource: Postgres): List<PipelineExtension> {

        val deployDependencies: PipelineBuildType.() -> Unit = {
            injectVars += listOf("env.DB_NAME")

            steps {
                createPostgresFeatureDB {
                    dbName = resource.name
                    dbTemplate = resource.dbTemplate
                }
            }
        }

        return listOf(
            PipelineExtension(PipelineStageType.DEPLOY_DEPENDENCIES, deployDependencies)
        )
    }
}
