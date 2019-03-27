package com.github.dreef3.teamcity.dsl.impl

import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import com.github.dreef3.teamcity.dsl.PipelineType
import com.github.dreef3.teamcity.dsl.Stage

data class StageBuildContext(
    val service: ServiceBlueprint,
    val type: PipelineType,
    val parentId: String,
    val info: BuildInfo,
    val project: Project
)

typealias StageInit = PipelineBuildType.() -> Unit

abstract class StageImpl<T : Stage> {
    abstract fun build(def: T, ctx: StageBuildContext, init: StageInit): PipelineBuildType
}
