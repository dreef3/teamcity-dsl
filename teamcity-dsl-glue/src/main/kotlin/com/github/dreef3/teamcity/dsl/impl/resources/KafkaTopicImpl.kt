package com.github.dreef3.teamcity.dsl.impl.resources

import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.resources.KafkaTopic
import com.github.dreef3.teamcity.dsl.impl.ServiceResourceImpl
import com.github.dreef3.teamcity.dsl.impl.PipelineExtension
import com.github.dreef3.teamcity.util.steps.kafkaCreateTopic

class KafkaTopicImpl : ServiceResourceImpl<KafkaTopic>() {
    override fun pipelineExtensions(resource: KafkaTopic): List<PipelineExtension> = listOf(
        PipelineExtension(PipelineStageType.DEPLOY_DEPENDENCIES, {
            steps {
                kafkaCreateTopic(resource.server, resource.name)
            }
        }, -1)
    )

}

