package com.github.dreef3.teamcity.dsl.stages

import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.Stage

class ApiTestStage(init: ApiTestStage.() -> Unit = {}) : Stage() {
    var path: String = "e2e"
    var url = ""

    init {
        type = PipelineStageType.TEST

        init()
    }

}
