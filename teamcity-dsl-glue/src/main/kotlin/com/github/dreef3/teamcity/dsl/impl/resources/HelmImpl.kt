package com.github.dreef3.teamcity.dsl.impl.resources

import jetbrains.buildServer.configs.kotlin.v2017_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2017_2.toId
import com.github.dreef3.teamcity.dsl.DslConfig
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.StackEnvironment
import com.github.dreef3.teamcity.dsl.impl.PipelineBuildType
import com.github.dreef3.teamcity.dsl.impl.PipelineExtension
import com.github.dreef3.teamcity.dsl.impl.ServiceResourceImpl
import com.github.dreef3.teamcity.dsl.impl.internal.toBuildParams
import com.github.dreef3.teamcity.dsl.resources.Helm
import com.github.dreef3.teamcity.util.extensions.toUUID
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.steps.deployConfigMap
import com.github.dreef3.teamcity.util.steps.helmDelete
import com.github.dreef3.teamcity.util.steps.helmUp
import com.github.dreef3.teamcity.util.vcs.mainBranch
import com.github.dreef3.teamcity.util.vcs.mainBranchVcsRoot

class HelmImpl : ServiceResourceImpl<Helm>() {
    companion object {
        private const val chartsDir = "deployment/charts"
        private const val utilityRepoName = "saascrm/zeliboba-charts"
    }

    override fun pipelineExtensions(resource: Helm): List<PipelineExtension> {
        val deploy: PipelineBuildType.() -> Unit = {
            val self = this
            val (service, _, _, info, project) = context

            vcs {
                root(mainBranch(info.rootProject, "K8sCharts"), """
                +:.=>$chartsDir
                """.trimIndent())
            }

            steps {
                val buildParams = project.params.params + self.params.params
                val serviceName = service.def.name

                deployConfigMap(buildParams, serviceName, resource.namespace)

                helmUp {
                    prefix = serviceName
                    chartPath = "$chartsDir/${resource.chartPath}"
                    namespace = resource.namespace
                    helmOpts = if (resource.vars.isEmpty()) ""
                    else "--set " + resource.vars
                        .map { (k, v) -> "$k=$v" }
                        .joinToString(",")
                }
            }
        }

        val deployDependencies: PipelineBuildType.() -> Unit = {
            val serviceName = context.service.def.name

            steps {
                script {
                    name = "Check if release already exists"
                    scriptContent = """
                    $SHEBANG

                    release=${'$'}(docker run --rm -t \
                    -e USER='%k8s.user.login%' \
                    -e PASSWORD='%k8s.user.password%' \
                    -e CLUSTER='%env.K8S_CLUSTER%' \
                    k8s/client:latest \
                    helm --tiller-namespace=%k8s.namespace% \
                    status -o json \
                    $serviceName-%env.SERVICE_VARIANT% | jq -r '.name') || $(echo "")

                    if [ -z "${'$'}release" ]; then
                        echo "No existing release found, will not skip"
                        exit 0;
                    fi

                    scaled_down=${'$'}(docker run --rm -t \
                    -e USER='%k8s.user.login%' \
                    -e PASSWORD='%k8s.user.password%' \
                    -e CLUSTER='%env.K8S_CLUSTER%' \
                    k8s/client:latest \
                    kubectl -n %k8s.namespace% get deployments -o name \
                    --selector="com.github.dreef3.teamcity/cleanup=true,app.kubernetes.io/instance=${'$'}release") || ${'$'}(echo "")

                    if [ -n "${'$'}scaled_down" ]; then
                        echo "Found cleaned up deployments, will not skip"
                        exit 0;
                    fi

                    echo "##teamcity[setParameter name='env.SKIP_ALL' value='true']";
                    """.trimIndent()
                }
            }
        }

        val deployments = resource.pipelines
            .map { PipelineExtension(PipelineStageType.DEPLOY, deploy, pipelineType = it) }
            .toTypedArray()

        return listOf(
            *deployments,
            PipelineExtension(PipelineStageType.DEPLOY_DEPENDENCIES, deployDependencies, -1000)
        )
    }

    override fun parentProjectExtensions(config: DslConfig, resource: Helm): List<(Project) -> Unit> = listOf({ project ->
        val params = config.params[StackEnvironment.ALL]

        requireNotNull(params)
        requireNotNull(params!!["k8s.chart.repository.url"]) {
            "Must define 'k8s.chart.repository.url' parameter"
        }
        requireNotNull(params["k8s.chart.repository.branch"]) {
            "Must define 'k8s.chart.repository.branch' parameter"
        }

        val (_, repository) = params["k8s.chart.repository.url"]!!
        val (_, branch) = params["k8s.chart.repository.branch"]!!

        mainBranchVcsRoot(project, repository, "K8sCharts", branch)

        var utilityRepo = "K8sCharts"

        if (repository != utilityRepoName) {
            utilityRepo = "K8sChartsUtility"
            mainBranchVcsRoot(project, repository, utilityRepo, branch)
        }

        project.subProject {
            id = "k8s".toId(project.id)
            uuid = id.toUUID()
            parentId = project.id
            name = "Kubernetes Utilities"

            params(toBuildParams(config.params[StackEnvironment.ALL]))
            params(toBuildParams(config.params[StackEnvironment.TEST]))

            buildType {
                id = "InstallCleanup".toId(this@subProject.id)
                uuid = id.toUUID()
                name = "Install cleanup job"

                params {
                    text("k8s.namespace", "", "Namespace", "", ParameterDisplay.PROMPT)
                    text("helm.chart.values", "", "Values", "", ParameterDisplay.PROMPT)

                    text("env.NAMESPACE", "%k8s.namespace%", display = ParameterDisplay.HIDDEN)
                    text("env.CLUSTER", "%env.K8S_CLUSTER%", display = ParameterDisplay.HIDDEN)
                    text("env.USER", "%k8s.user.login%", display = ParameterDisplay.HIDDEN)
                    text("env.PASSWORD", "%k8s.user.password%", display = ParameterDisplay.HIDDEN)
                    text("env.SERVICE_VARIANT", "features", display = ParameterDisplay.HIDDEN)
                    text("env.DOCKER_IMAGE_TAG", "latest", display = ParameterDisplay.HIDDEN)
                }

                vcs {
                    mainBranch(project, utilityRepo)
                }

                steps {
                    deployConfigMap(this@buildType.params.params, "cleanup", "%k8s.namespace%")

                    helmUp {
                        prefix = "cleanup"
                        chartPath = "zeliboba/charts/cleanup"
                        namespace = "%k8s.namespace%"
                        helmOpts = "--set %helm.chart.values%"
                    }
                }
            }

            buildType {
                id = "InstallRelease".toId(this@subProject.id)
                uuid = id.toUUID()
                name = "Install Helm release"

                vcs {
                    mainBranch(project, "K8sCharts")
                }

                params {
                    text("helm.chart.path", "", "Path to chart", "", ParameterDisplay.PROMPT)
                    text("helm.namespace", "", "Namespace", "", ParameterDisplay.PROMPT)
                    text("helm.chart.values", "", "Values", "", ParameterDisplay.PROMPT)
                }

                steps {
                    helmUp {
                        chartPath = "$prefix/%helm.chart.path%"
                        namespace = "%helm.namespace%"
                        helmOpts = "--set %helm.chart.values%"
                    }
                }
            }

            buildType {
                id = "DeleteRelease".toId(this@subProject.id)
                uuid = id.toUUID()
                name = "Delete Helm release"

                vcs {
                    mainBranch(project, "K8sCharts")
                }

                params {
                    text("helm.release", "", "Release name", "", ParameterDisplay.PROMPT)
                    text("helm.namespace", "", "Namespace", "", ParameterDisplay.PROMPT)
                }

                steps {
                    helmDelete {
                        release = "%helm.release%"
                        namespace = "%helm.namespace%"
                    }
                }
            }
        }
    })
}
