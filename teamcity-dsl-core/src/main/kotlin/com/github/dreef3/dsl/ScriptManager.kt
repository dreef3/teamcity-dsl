package com.github.dreef3.dsl

import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import javax.script.ScriptEngineManager

object ScriptManager: ScriptEngineManager() {
    init {
        if (getEngineByExtension("kts") == null) {
            registerEngineExtension("kts", KotlinJsr223JvmLocalScriptEngineFactory())
        }
    }
}
