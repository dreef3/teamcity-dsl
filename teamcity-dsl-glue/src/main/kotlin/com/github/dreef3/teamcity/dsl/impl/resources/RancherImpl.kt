package com.github.dreef3.teamcity.dsl.impl.resources

import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import com.github.dreef3.teamcity.dsl.DslConfig
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.StackEnvironment
import com.github.dreef3.teamcity.dsl.impl.ServiceResourceImpl
import com.github.dreef3.teamcity.dsl.impl.PipelineBuildType
import com.github.dreef3.teamcity.dsl.impl.PipelineExtension
import com.github.dreef3.teamcity.dsl.resources.Rancher
import com.github.dreef3.teamcity.util.steps.rancherUp
import com.github.dreef3.teamcity.util.steps.warmUpService
import com.github.dreef3.teamcity.util.vcs.defaultVcsRoots
import com.github.dreef3.teamcity.util.vcs.mainBranch

class RancherImpl : ServiceResourceImpl<Rancher>() {
    override fun pipelineExtensions(resource: Rancher): List<PipelineExtension> {
        val deploy: PipelineBuildType.() -> Unit = {
            val (service, _, _, info) = context

            vcs {
                root(mainBranch(info.rootProject, "Rancher"), """
                +:.=>.
                """.trimIndent())
            }

            steps {
                rancherUp {
                    this@rancherUp.service = service.def.name
                }

                if (resource.healthCheckUrl.isNotEmpty()) {
                    warmUpService {
                        this@warmUpService.service = service.def.name
                        url = resource.healthCheckUrl
                    }
                }

            }
        }

        return listOf(PipelineExtension(PipelineStageType.DEPLOY, deploy))
    }

    override fun parentProjectExtensions(config: DslConfig, resource: Rancher): List<(Project) -> Unit> = listOf({ project ->
        val params = config.params[StackEnvironment.ALL]

        requireNotNull(params)
        requireNotNull(params!!["rancher.repository"]) {
            "Must define 'rancher.repository' parameter: repository with Rancher stack configs"
        }

        val (_, repository) = params["rancher.repository"]!!

        try {
            mainBranch(project, "Rancher")
        } catch (_: IllegalArgumentException) {
            project.defaultVcsRoots(repository, "Rancher")
        }
    })
}
