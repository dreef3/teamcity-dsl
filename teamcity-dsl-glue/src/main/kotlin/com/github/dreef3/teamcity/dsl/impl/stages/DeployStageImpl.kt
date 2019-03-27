package com.github.dreef3.teamcity.dsl.impl.stages

import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.impl.PipelineBuildType
import com.github.dreef3.teamcity.dsl.impl.StageBuildContext
import com.github.dreef3.teamcity.dsl.impl.StageImpl
import com.github.dreef3.teamcity.dsl.impl.StageInit
import com.github.dreef3.teamcity.dsl.impl.internal.toBuildParams
import com.github.dreef3.teamcity.dsl.stages.DeployStage
import com.github.dreef3.teamcity.util.extensions.toPascalCase
import com.github.dreef3.teamcity.util.extensions.toUUID

class DeployStageImpl : StageImpl<DeployStage>() {
    override fun build(def: DeployStage, ctx: StageBuildContext, init: StageInit): PipelineBuildType {
        val (_, type, parentId, info) = ctx

        return PipelineBuildType(ctx, def) {
            val bt = type.toString().toPascalCase()

            buildNumberPattern = "%build.counter%"
            injectVars = listOf("env.DOCKER_IMAGE_TAG", "env.SERVICE_VARIANT")

            params(toBuildParams(info.product.pipelineParams[type]))

            init()

            this@PipelineBuildType.applyExtensions { order < 0 }
            this@PipelineBuildType.applyExtensions { order >= 0 }

            id = "${parentId}_Deploy$label"
            uuid = id.toUUID()
            name = "Deploy$label"
        }
    }
}
