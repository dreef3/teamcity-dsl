package com.github.dreef3.teamcity.dsl.resources

import com.github.dreef3.teamcity.dsl.ServiceResource
import com.github.dreef3.teamcity.dsl.ServiceResources
import com.github.dreef3.teamcity.dsl.TeamCityDslMarker

@TeamCityDslMarker
class Swagger(init: Swagger.() -> Unit = {}, base: Swagger?) : ServiceResource(base = base as ServiceResource?) {
    var dockerFile = "swagger.Dockerfile"
    var imageName = ""
    var args = ""
    var composeFile = ""
    var name = ""

    init {
        if (base != null) {
            dockerFile = base.dockerFile
            imageName = base.imageName
            args = base.args
            composeFile = base.composeFile
            name = base.name
        }

        init()
    }

}

fun ServiceResources.swagger(base: Swagger? = null, init: Swagger.() -> Unit = {}) {
    val swagger = Swagger(init, base)

    if (swagger.name.isEmpty()) {
        swagger.name = "${this.service.name}-swagger"
    }

    if (swagger.imageName.isEmpty()) {
        swagger.imageName = "saas-crm/${swagger.name}"
    }

    resources += swagger
}
