package com.github.dreef3.teamcity.dsl.resources

import com.github.dreef3.teamcity.dsl.ProductResource
import com.github.dreef3.teamcity.dsl.ProductResources

class BlueGreenDeploy : ProductResource() {
    var host = ""
    var stagingHost = ""
    var clientVersion = "latest"
}

fun ProductResources.blueGreen(init: BlueGreenDeploy.() -> Unit) {
    val bg = BlueGreenDeploy().apply(init)

    items += bg
}
