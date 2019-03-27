package com.github.dreef3.teamcity.dsl.resources

import com.github.dreef3.teamcity.dsl.*

@TeamCityDslMarker
class Helm(init: Helm.() -> Unit) : ServiceResource() {
    var chartPath = ""
    var namespace = "%k8s.namespace%"
    var vars = hashMapOf<String, String>()
    var pipelines = listOf(PipelineType.ANY)

    init {
        init()
    }
}

fun ServiceResources.helm(init: Helm.() -> Unit = {}) {
    resources += Helm(init)
}


class HelmProductConfig: ProductBuildParameters() {
    var namespace
        get() = this["k8s.namespace"]?.second
        set(value) = this.param("k8s.namespace", value ?: "")

}

fun ProductBuildParameters.helm(init: HelmProductConfig.() -> Unit) {
    putAll(HelmProductConfig().apply(init))
}
