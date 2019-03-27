package com.github.dreef3.teamcity.dsl.impl

import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.PipelineType

open class PipelineExtension(var type: PipelineStageType,
                             var fn: PipelineBuildType.() -> Unit,
                             var order: Int = 0,
                             var pipelineType: PipelineType = PipelineType.ANY)

