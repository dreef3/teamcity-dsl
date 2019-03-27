package com.github.dreef3.teamcity.dsl

class ProductionParameters : ProductBuildParameters() {
    init {
        param("env.APP_ENV", "prod")
        param("env.SERVICE_VARIANT", "%env.APP_ENV%")
        param("env.STACK_ENV", "production")
    }
}
