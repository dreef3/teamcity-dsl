package com.github.dreef3.teamcity.dsl.resources

import com.github.dreef3.teamcity.dsl.ProductBuildParameters
import com.github.dreef3.teamcity.dsl.ServiceResource
import com.github.dreef3.teamcity.dsl.ServiceResources
import com.github.dreef3.teamcity.dsl.TeamCityDslMarker

@TeamCityDslMarker
class Rancher(init: Rancher.() -> Unit = {}) : ServiceResource() {
    var healthCheckUrl = ""

    init {
        init()
    }
}

fun ServiceResources.rancher(init: Rancher.() -> Unit = {}) {
    resources += Rancher(init)
}

class RancherProductConfig: ProductBuildParameters() {
    var composeFile
        get() = this["env.DOCKER_COMPOSE_FILE"]?.second
        set(value) = this.param("env.DOCKER_COMPOSE_FILE", value ?: "")

    var stack
        get() = this["env.RANCHER_STACK"]?.second
        set(value) = this.param("env.RANCHER_STACK", value ?: "")

    var scale
        get() = this["env.SERVICE_SCALE"]?.second
        set(value) = this.param("env.SERVICE_SCALE", value ?: "")

    init {
        param("env.SERVICE_SCALE", "1")
    }
}

fun ProductBuildParameters.rancher(init: RancherProductConfig.() -> Unit) {
    putAll(RancherProductConfig().apply(init))
}
