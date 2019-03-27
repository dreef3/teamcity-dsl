package com.github.dreef3.teamcity.dsl.resources

import com.github.dreef3.teamcity.dsl.ProductBuildParameters
import com.github.dreef3.teamcity.dsl.ServiceResource
import com.github.dreef3.teamcity.dsl.TeamCityDslMarker

@TeamCityDslMarker
class Postgres(init: Postgres.() -> Unit = {}, base: Postgres? = null) : ServiceResource(base = base as ServiceResource?) {
    var name = "%env.DB_NAME%"
    var user = "%env.DB_USER%"
    var password = "%env.DB_PASSWORD%"
    var host = "%env.DB_HOST%"
    var port = "%env.DB_PORT%"
    var schema = "%env.DB_SCHEMA%"

    // https://www.postgresql.org/docs/9.5/static/manage-ag-templatedbs.html
    var dbTemplate = "template1"

    init {
        init()
    }
}

class PostgresProductConfig: ProductBuildParameters() {
    var name
        get() = this["env.DB_NAME"]?.second
        set(value) = this.param("env.DB_NAME", value ?: "")
    var user
        get() = this["env.DB_USER"]?.second
        set(value) = this.param("env.DB_USER", value ?: "")
    var password
        get() = this["env.DB_PASSWORD"]?.second
        set(value) = this.password("env.DB_PASSWORD", value ?: "")
    var host
        get() = this["env.DB_HOST"]?.second
        set(value) = this.param("env.DB_HOST", value ?: "")
    var port
        get() = this["env.DB_PORT"]?.second
        set(value) = this.param("env.DB_PORT", value ?: "")
    var schema
        get() = this["env.DB_SCHEMA"]?.second
        set(value) = this.param("env.DB_SCHEMA", value ?: "")

    init {
        param("env.DB_PORT", "5432")
    }
}

fun ProductBuildParameters.postgres(init: PostgresProductConfig.() -> Unit) {
    putAll(PostgresProductConfig().apply(init))
}
