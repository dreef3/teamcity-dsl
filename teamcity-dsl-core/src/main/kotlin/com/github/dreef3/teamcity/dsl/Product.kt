package com.github.dreef3.teamcity.dsl

@TeamCityDslMarker
open class Product(val config: DslConfig, init: Product.() -> Unit = {}, base: Product? = null): WithParams() {
    var id = ""
    var services: Services = Services(this) { Service(config).apply(config.serviceDefaults) }
    var features = emptyList<ProductFeatures>()
    var resources = ProductResources()

    init {
        if (base != null) {
            id = base.id
            services = base.services
            params = base.params
            pipelineParams = base.pipelineParams
            features = base.features
        }

        init()

        require(id.isNotEmpty()) { "id cannot be empty" }
    }

    fun services(init: Services.() -> Unit = {}) {
        services.init()
    }

    fun resources(init: ProductResources.() -> Unit = {}) {
        resources.init()
    }
}
