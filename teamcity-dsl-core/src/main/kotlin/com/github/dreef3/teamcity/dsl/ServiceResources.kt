package com.github.dreef3.teamcity.dsl

@TeamCityDslMarker
open class ServiceResources(val service: Service,
                            init: ServiceResources.() -> Unit = {}, base: ServiceResources? = null) {
    var resources: List<ServiceResource> = emptyList()

    init {
        copyFrom(base)
        init()
    }

    fun copyFrom(base: ServiceResources?) {
        if (base != null) {
            resources = base.resources
        }
    }
}

@TeamCityDslMarker
class ProductResources {
    var items: List<ProductResource> = emptyList()

    fun resource(resource: ProductResource) {
        items += resource
    }
}
