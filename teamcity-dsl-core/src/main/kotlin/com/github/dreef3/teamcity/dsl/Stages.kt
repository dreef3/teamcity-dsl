package com.github.dreef3.teamcity.dsl

import com.github.dreef3.teamcity.dsl.stages.*

open class Stages(init: Stages.() -> Unit = {}, base: Stages? = null) {

    val items = arrayListOf<Stage>()

    init {
        copyFrom(base)
        init()
    }

    fun reset() {
        items.clear()
    }

    fun reset(vararg type: PipelineStageType) {
        val types = type.toSet()
        items.removeIf { types.contains(it.type) }
    }

    fun build(init: BuildStage.() -> Unit = {}) {
        stage(BuildStage(init))
    }

    fun deploy() {
        stage(DeployStage())
        stage(DependenciesDeployStage())
    }

    fun api(init: ApiTestStage.() -> Unit) {
        stage(ApiTestStage(init))
    }

    fun e2e(init: E2ETestStage.() -> Unit) {
        stage(E2ETestStage(init))
    }

    fun stage(base: Stage) {
        items += base
    }

    fun copyFrom(base: Stages?) {
        if (base != null) {
            items.addAll(base.items)
        }
    }
}
