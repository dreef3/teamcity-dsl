package com.github.dreef3.teamcity.dsl.impl.resources

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildType
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.resources.Swagger
import com.github.dreef3.teamcity.dsl.impl.ServiceResourceImpl
import com.github.dreef3.teamcity.dsl.impl.PipelineExtension
import com.github.dreef3.teamcity.util.steps.addImageToPromoted
import com.github.dreef3.teamcity.util.steps.dockerImage
import com.github.dreef3.teamcity.util.steps.rancherUp

class SwaggerImpl: ServiceResourceImpl<Swagger>() {
    override fun pipelineExtensions(resource: Swagger): List<PipelineExtension> {

        val buildSwaggerImage: BuildType.() -> Unit = {
            steps {
                dockerImage {
                    imageName = resource.imageName
                    args = resource.args
                    dockerFile = resource.dockerFile
                }
            }
        }

        val deploySwagger: BuildType.() -> Unit = {
            steps {
                rancherUp {
                    name = resource.name
                    service = resource.name
                    composeFile = resource.composeFile
                }
            }
        }

        return listOf(
            PipelineExtension(PipelineStageType.BUILD, buildSwaggerImage),
            PipelineExtension(PipelineStageType.DEPLOY, deploySwagger),
            PipelineExtension(PipelineStageType.PROMOTE, {
                steps { addImageToPromoted(resource.imageName) }
            })
        )
    }
}

