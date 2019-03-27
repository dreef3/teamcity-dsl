package com.github.dreef3.teamcity.dsl.impl

import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import com.github.dreef3.teamcity.dsl.DslConfig
import com.github.dreef3.teamcity.dsl.Product
import com.github.dreef3.teamcity.dsl.Service

open class TeamCityDsl(val config: DslConfig,
                       val impl: ProductBlueprint) {
    companion object {
        fun defaultImpl() = DslBlueprint(BundledStages, BundledResources, BundledProductResources)
        fun createDefault(config: DslConfig) = TeamCityDsl(config, ProductBlueprint(defaultImpl(), config))
    }

    fun product(init: Product.() -> Unit): Product = Product(config, init)

    fun service(init: Service.() -> Unit): Service = Service(config, init, Service(config, config.serviceDefaults))

    fun bind(project: Project, services: List<Service> = emptyList(), products: List<Product> = emptyList()) {
        impl.bindProducts(project, products.toList())
        impl.bindServices(project, services)
    }
}
