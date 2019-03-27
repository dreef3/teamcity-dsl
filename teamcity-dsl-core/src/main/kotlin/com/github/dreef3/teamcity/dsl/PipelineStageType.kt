package com.github.dreef3.teamcity.dsl

enum class PipelineStageType {
    CUSTOM,
    BUILD,
    DEPLOY_DEPENDENCIES,
    DEPLOY_PROD_LIKE,
    DEPLOY,
    TEST,
    PROMOTE,
}
