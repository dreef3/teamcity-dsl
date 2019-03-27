package com.github.dreef3.teamcity.dsl.impl

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import com.github.dreef3.teamcity.dsl.impl.stages.BuildStageImpl
import com.github.dreef3.teamcity.dsl.stages.BuildStage

class StageImplTest {
    companion object {
        @JvmStatic
        fun stages() = mapOf(
            BuildStage() to BuildStageImpl()
        )
    }

    @ParameterizedTest
    @MethodSource("stages")
    internal fun `name starts with pipeline type`() {
    }
}
