package com.github.dreef3.teamcity.dsl.stages

import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.Stage

class DependenciesDeployStage(init: DependenciesDeployStage.() -> Unit = {}) : Stage() {
    init {
        type = PipelineStageType.DEPLOY_DEPENDENCIES
        init()
    }
}
