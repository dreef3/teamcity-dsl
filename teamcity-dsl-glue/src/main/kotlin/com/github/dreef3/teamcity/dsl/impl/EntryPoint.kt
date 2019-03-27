package com.github.dreef3.teamcity.dsl.impl

import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import com.github.dreef3.dsl.Manifest
import com.github.dreef3.teamcity.dsl.*

fun Manifest.toProject(service: Service, project: Project): Project {
    val productId = this.productId
    val blueprint = DslBlueprint(BundledStages, BundledResources, BundledProductResources)
    val info = BuildInfo(Product(service.config, {id = productId}), emptyList(), project)
    return ServiceBlueprint(blueprint, service.config, service, project).toProject(info)
}
