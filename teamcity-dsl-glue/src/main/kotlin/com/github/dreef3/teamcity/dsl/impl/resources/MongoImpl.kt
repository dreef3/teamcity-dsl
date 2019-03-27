package com.github.dreef3.teamcity.dsl.impl.resources

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildType
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.resources.Mongo
import com.github.dreef3.teamcity.dsl.impl.ServiceResourceImpl
import com.github.dreef3.teamcity.dsl.impl.PipelineExtension
import com.github.dreef3.teamcity.util.steps.createMongoFeatureDB

class MongoImpl: ServiceResourceImpl<Mongo>() {
    override fun pipelineExtensions(resource: Mongo): List<PipelineExtension> {
        val deployDependencies: BuildType.() -> Unit = {
            steps {
                createMongoFeatureDB {
                    dbName = resource.name
                }
            }
        }

        return listOf(
            PipelineExtension(PipelineStageType.DEPLOY_DEPENDENCIES, deployDependencies)
        )
    }
}

