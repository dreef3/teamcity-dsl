package com.github.dreef3.teamcity.dsl.impl.stages

import jetbrains.buildServer.configs.kotlin.v2017_2.*
import jetbrains.buildServer.configs.kotlin.v2017_2.triggers.schedule
import com.github.dreef3.teamcity.dsl.Product
import com.github.dreef3.teamcity.dsl.ProductFeatures
import com.github.dreef3.teamcity.dsl.ServiceFeatures
import com.github.dreef3.teamcity.dsl.impl.ServiceBlueprint
import com.github.dreef3.teamcity.util.extensions.toUUID
import com.github.dreef3.teamcity.util.templates.withCommonBuildSettings

fun createDeployAllStage(project: Project, product: Product, services: List<ServiceBlueprint>) = BuildType {
    id = "DeployProd".toId(project.id)
    uuid = id.toUUID()
    name = "DeployProd"

    withCommonBuildSettings()

    buildNumberPattern = "%build.counter%"

    params {
        text("env.ACTUAL_STACK_NAME", "", display = ParameterDisplay.HIDDEN)
        text("env.DOCKER_IMAGE_TAG", "", display = ParameterDisplay.HIDDEN)
    }

    if (!product.features.contains(ProductFeatures.DEBUG)) {
        triggers {
            schedule {
                schedulingPolicy = cron {
                    seconds = "0"
                    minutes = "0"
                    hours = "3"
                    dayOfMonth = "*"
                    month = "*"
                    dayOfWeek = "2-6"
                    year = "*"
                }
                triggerBuild = always()
                withPendingChangesOnly = false
            }
        }
    }

    dependencies {
        services
            .filter { it.def.features.contains(ServiceFeatures.PRODUCTION) }
            .forEach { service ->
                snapshot(service.deployProdExtId) {
                    reuseBuilds = ReuseBuilds.NO
                    onDependencyFailure = FailureAction.FAIL_TO_START
                }
            }
    }
}
