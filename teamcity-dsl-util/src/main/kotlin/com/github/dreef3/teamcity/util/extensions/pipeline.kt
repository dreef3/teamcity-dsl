package com.github.dreef3.teamcity.util.extensions

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2017_2.FailureAction
import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import jetbrains.buildServer.configs.kotlin.v2017_2.ReuseBuilds
import com.github.dreef3.teamcity.util.buildTypes.PipelinePhaseBuildType

open class Pipeline(val project: Project, init: Pipeline.() -> Unit) {
    val phases = arrayListOf<List<Phase>>()

    var reuseBuilds: ReuseBuilds = ReuseBuilds.NO

    fun phase(vararg build: (Project) -> BuildType) {
        val pipeline = this
        val newPhases = build.map { buildFn ->
            val phase = Phase(buildFn(project))

            phase.buildType.name = "[${phases.size + 1}] ${phase.buildType.name}"

            phases.lastOrNull()?.let { prevPhases ->
                for (prev in prevPhases) {
                    phase.params += prev.params
                    phase.onDependency = prev.onDependency
                }
            }

            phase.params
                .filter { name -> phase.buildType.params.params.find { param -> param.name == name } == null }
                .forEach { name ->
                    phase.buildType.params.param(name, "")
                }

            phases.lastOrNull()?.let { prevPhases ->
                for (prev in prevPhases) {
                    prev.params.forEach { param ->
                        phase.buildType.params.param(param, "%dep.${prev.buildType.id}.$param%")
                    }
                    phase.buildType.dependencies {
                        dependency(prev.buildType) {
                            snapshot {
                                reuseBuilds = pipeline.reuseBuilds
                                onDependencyFailure = prev.onDependency
                            }
                        }
                    }
                }
            }

            if (phase.buildType is PipelinePhaseBuildType) {
                phase.buildType.postInit()
            }

            phase
        }

        if (newPhases.isNotEmpty()) {
            phases.add(newPhases)
        }
    }

    init {
        init()
    }
}

class Phase(val buildType: BuildType) {
    var params: Array<String> = emptyArray()
    var onDependency: FailureAction = FailureAction.FAIL_TO_START
}

fun Project.pipeline(init: Pipeline.() -> Unit) {
    val pipeline = Pipeline(this, init)

    pipeline.phases.forEach { phaseGroup ->
        phaseGroup.forEach { phase ->
            this.buildType(phase.buildType)
        }
    }
}
