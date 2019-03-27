package com.github.dreef3.teamcity.dsl.impl

import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import com.github.dreef3.teamcity.dsl.*

abstract class ServiceResourceImpl<T: ServiceResource> {
    abstract fun pipelineExtensions(resource: T): List<PipelineExtension>

    open fun serviceExtensions(resource: T): List<ServiceExtension> = emptyList()

    open fun parentProjectExtensions(config: DslConfig, resource: T): List<(Project) -> Unit> = emptyList()

    open fun serviceProjectExtensions(config: DslConfig, service: Service, resource: T): Project.() -> Unit = {}
}

abstract class ProductResourceImpl<T: ProductResource> {
    open fun productProjectExtensions(resource: T, product: Product): Project.() -> Unit = {}

    open fun serviceExtensions(resource: T, product: Product): Service.() -> Unit = {}

    open fun productExtensions(resource: T): Product.() -> Unit = {}
}
