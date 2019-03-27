package com.github.dreef3.teamcity.util.buildTypes

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildType

abstract class PipelinePhaseBuildType : BuildType() {
    abstract fun postInit()
}
