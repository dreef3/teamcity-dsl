package com.github.dreef3.teamcity.util.steps

import jetbrains.buildServer.configs.kotlin.v2017_2.Parametrized
import kotlin.reflect.KProperty

class VarOrParamDelegate(val name: String) {
    var value: String? = null

    operator fun getValue(thisRef: Parametrized, property: KProperty<*>): String? {
        return value ?: thisRef.params.find { it.name == name }?.value ?: "%$name%"
    }
    operator fun setValue(thisRef: Parametrized, property: KProperty<*>, value: String?) {
        thisRef.param(name, value ?: "")
        this.value = value
    }
}

class VarOrParam(val value: String) {
    operator fun provideDelegate(thisRef: Parametrized, prop: KProperty<*>) = VarOrParamDelegate(value)
}

fun varOrParam(s: String) = VarOrParam(s)
