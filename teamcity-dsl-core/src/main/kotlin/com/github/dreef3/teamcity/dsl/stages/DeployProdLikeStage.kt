package com.github.dreef3.teamcity.dsl.stages

import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.Stage

class DeployProdLikeStage(init: DeployProdLikeStage.() -> Unit = {}) : Stage() {
    init {
        type = PipelineStageType.DEPLOY_PROD_LIKE
        init()
    }
}
