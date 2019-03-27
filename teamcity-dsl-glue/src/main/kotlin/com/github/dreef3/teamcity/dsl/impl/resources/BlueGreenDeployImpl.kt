package com.github.dreef3.teamcity.dsl.impl.resources

import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2017_2.toId
import com.github.dreef3.teamcity.dsl.*
import com.github.dreef3.teamcity.dsl.impl.PipelineExtension
import com.github.dreef3.teamcity.dsl.impl.ProductBlueprint
import com.github.dreef3.teamcity.dsl.impl.ProductResourceImpl
import com.github.dreef3.teamcity.dsl.impl.ServiceResourceImpl
import com.github.dreef3.teamcity.dsl.resources.BlueGreenDeploy
import com.github.dreef3.teamcity.util.extensions.toUUID
import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.templates.withCommonBuildSettings
import java.lang.IllegalArgumentException

class BlueGreenDeployImpl : ProductResourceImpl<BlueGreenDeploy>() {
    override fun serviceExtensions(resource: BlueGreenDeploy, product: Product): Service.() -> Unit = {
        resources.resources += ServiceBlueGreenDeploy(resource)
    }

    override fun productProjectExtensions(resource: BlueGreenDeploy, product: Product): Project.() -> Unit = {
        val project = this.subProjects.find { it.id == ProductBlueprint.productionProjectId(this) }
            ?: throw IllegalArgumentException("No production subproject found!")

        project.buildType {
            id = "Switch".toId(project.id)
            uuid = id.toUUID()
            name = "Switch"

            withCommonBuildSettings()

            steps {
                script {
                    scriptContent = """
                    $SHEBANG
                    docker pull saas-crm/blue-green:${resource.clientVersion}
                    docker run --rm -t \
                        -e CLUSTER=%env.K8S_CLUSTER% \
                        -e USER=%k8s.user.login% \
                        -e PASSWORD='%k8s.user.password%' \
                        -e NAMESPACE='%k8s.namespace%' \
                        -e PRODUCT='${product.id}' \
                        -e HOST=${resource.host} \
                        -e STAGING_HOST=${resource.stagingHost} \
                        saas-crm/blue-green:${resource.clientVersion} \
                        switch
                    """.trimIndent()
                }
            }
        }
    }
}

class ServiceBlueGreenDeploy(internal val parent: BlueGreenDeploy) : ServiceResource()

class ServiceBlueGreenDeployImpl: ServiceResourceImpl<ServiceBlueGreenDeploy>() {
    override fun pipelineExtensions(resource: ServiceBlueGreenDeploy): List<PipelineExtension> {
        val deploy = PipelineExtension(PipelineStageType.DEPLOY, order = -100, pipelineType = PipelineType.PRODUCTION, fn = {
            val productId = this.context.info.product.id

            steps {
                script {
                    scriptContent = """
                    $SHEBANG
                    docker pull saas-crm/blue-green:${resource.parent.clientVersion}
                    current=$(docker run --rm -t \
                        -e CLUSTER=%env.K8S_CLUSTER% \
                        -e USER=%k8s.user.login% \
                        -e PASSWORD='%k8s.user.password%' \
                        -e NAMESPACE='%k8s.namespace%' \
                        -e PRODUCT='$productId' \
                        saas-crm/blue-green:${resource.parent.clientVersion} \
                        current)

                    echo "##teamcity[setParameter name='env.SERVICE_VARIANT' value='${'$'}current']"
                    """.trimIndent()
                }
            }
        })

        return listOf(deploy)
    }
}
