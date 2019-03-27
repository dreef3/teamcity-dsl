package com.github.dreef3.teamcity.dsl

open class ProductBuildParameters(base: ProductBuildParameters? = null) : HashMap<String, Pair<ParamType, String>>(base ?: emptyMap()) {
    fun param(key: String, value: String) {
        this[key] = Pair(ParamType.PLAIN, value)
    }

    fun password(key: String, value: String) {
        this[key] = Pair(ParamType.PASSWORD, value)
    }

    fun env(key: String, value: String) {
        this["env.$key"] = Pair(ParamType.PLAIN, value)
    }
}

fun vault(path: String, key: String) = "%vault:$path!/$key%"
