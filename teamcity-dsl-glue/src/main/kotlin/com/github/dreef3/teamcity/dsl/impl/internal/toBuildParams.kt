package com.github.dreef3.teamcity.dsl.impl.internal

import jetbrains.buildServer.configs.kotlin.v2017_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2017_2.ParametrizedWithType
import com.github.dreef3.teamcity.dsl.ParamType
import com.github.dreef3.teamcity.dsl.ProductBuildParameters

fun toBuildParams(params: Map<String, Pair<ParamType, String>>?): ParametrizedWithType.() -> Unit = {
    if (params == null) {
        throw IllegalArgumentException("ProductBuildParameters are not set")
    }

    val result = this

    params
        .filter { (k, v) -> v.first == ParamType.PLAIN }
        .forEach { (k, v) -> result.text(k, v.second, display = ParameterDisplay.HIDDEN) }
    params
        .filter { (k, v) -> v.first == ParamType.PASSWORD }
        .forEach { (k, v) -> result.password(k, v.second, display = ParameterDisplay.HIDDEN) }
}
