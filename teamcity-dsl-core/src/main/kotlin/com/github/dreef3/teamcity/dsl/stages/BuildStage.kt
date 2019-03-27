package com.github.dreef3.teamcity.dsl.stages

import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.Stage

class BuildStage(init: BuildStage.() -> Unit = {}) : Stage() {
    var dockerFile: String = "Dockerfile"
    var workingDir: String = "."

    init {
        type = PipelineStageType.BUILD
        init()
    }
}
