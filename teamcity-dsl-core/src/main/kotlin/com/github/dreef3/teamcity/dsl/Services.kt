package com.github.dreef3.teamcity.dsl

@TeamCityDslMarker
open class Services(val product: Product, val defaultService: () -> Service) {
    var items: List<Service> = emptyList()

    fun backend(base: Service? = defaultService(), init: Service.() -> Unit = {}): Service {
        val service = Service(product.config, init, base)

        items += service

        return service
    }

    fun frontend(base: Service? = defaultService(),
                 init: Service.() -> Unit = {}): Service = backend(base, init)
}
