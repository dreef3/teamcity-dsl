package com.github.dreef3.teamcity.dsl.stages

import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.Stage

class DeployStage(init: DeployStage.() -> Unit = {}) : Stage() {
    init {
        type = PipelineStageType.DEPLOY
        init()
    }
}
