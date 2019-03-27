package com.github.dreef3.teamcity.dsl

@TeamCityDslMarker
abstract class ServiceResource(init: ServiceResource.() -> Unit = {}, base: ServiceResource? = null) {
    var dependencies: List<ServiceResource> = emptyList()

    init {
        if (base != null) {
            dependencies = base.dependencies
        }

        init()
    }
}

@TeamCityDslMarker
abstract class ProductResource {
    var dependencies: List<ProductResource> = emptyList()
}
