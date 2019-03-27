package com.github.dreef3.teamcity.dsl

@TeamCityDslMarker
open class Service(val config: DslConfig,
                   init: Service.() -> Unit = {}, base: Service? = null): WithParams() {
    var name = ""
    var repository = ""
    var mainBranch = "develop"
    var path = "."
    var features = listOf(ServiceFeatures.PRODUCTION, ServiceFeatures.PULL_REQUESTS)

    var resources: ServiceResources = ServiceResources(this)
    var stages: Stages = Stages()
    var frontends: List<Service> = emptyList()

    init {
        if (base != null) {
            name = base.name
            repository = base.repository
            resources.copyFrom(base.resources)
            stages.copyFrom(base.stages)
        }

        init()
    }

    fun resources(base: ServiceResources? = null, init: ServiceResources.() -> Unit) {
        resources.copyFrom(base)
        resources.init()
    }

    fun stages(base: Stages? = null, init: Stages.() -> Unit = {}) {
        stages.copyFrom(base)
        stages.init()
    }

    fun frontend(service: Service) {
        frontends += service
    }
}
