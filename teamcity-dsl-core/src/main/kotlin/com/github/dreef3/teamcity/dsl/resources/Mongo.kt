package com.github.dreef3.teamcity.dsl.resources

import com.github.dreef3.teamcity.dsl.ServiceResource
import com.github.dreef3.teamcity.dsl.TeamCityDslMarker

@TeamCityDslMarker
class Mongo(init: Mongo.() -> Unit = {}, base: Mongo? = null) : ServiceResource(base = base as ServiceResource?) {
    var name = "%env.MONGO_DB_NAME%"
    var user = "%env.MONGO_DB_USER%"
    var password = "%env.MONGO_DB_PASSWORD%"
    var host = "%env.MONGO_DB_HOST%"
    var port = "%env.MONGO_DB_PORT%"

    init {
        if (base != null) {
            name = base.name
            user = base.user
            password = base.password
            host = base.host
            port = base.port
        }

        init()

    }
}
