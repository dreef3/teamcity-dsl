package com.github.dreef3.teamcity.dsl

open class Stage(init: Stage.() -> Unit = {}, base: Stage? = null) {
    open val runOnDependencies = false

    var pipeline: PipelineType? = null
    var type: PipelineStageType = PipelineStageType.CUSTOM

    init {
        if (base != null) {
            pipeline = base.pipeline
            type = base.type
        }

        init()
    }
}
