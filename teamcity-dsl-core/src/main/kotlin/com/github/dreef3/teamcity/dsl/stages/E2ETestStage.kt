package com.github.dreef3.teamcity.dsl.stages

import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.Stage

class E2ETestStage(init: E2ETestStage.() -> Unit = {}) : Stage() {
    override val runOnDependencies = true

    var path: String = "e2e"
    var url = ""

    init {
        type = PipelineStageType.TEST
        init()
    }
}
