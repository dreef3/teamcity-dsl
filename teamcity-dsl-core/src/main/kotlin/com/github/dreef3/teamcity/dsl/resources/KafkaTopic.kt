package com.github.dreef3.teamcity.dsl.resources

import com.github.dreef3.teamcity.dsl.ProductBuildParameters
import com.github.dreef3.teamcity.dsl.ServiceResource
import com.github.dreef3.teamcity.dsl.ServiceResources
import com.github.dreef3.teamcity.dsl.TeamCityDslMarker

@TeamCityDslMarker
class KafkaTopic(init: KafkaTopic.() -> Unit = {}, base: KafkaTopic?) : ServiceResource(base = base as ServiceResource?) {
    var name = ""
    var server = "%env.KAFKA_BOOTSTRAP_SERVERS%:2181"

    init {
        init()
    }
}

fun ServiceResources.kafkaTopic(base: KafkaTopic? = null, init: KafkaTopic.() -> Unit) {
    val topic = KafkaTopic(init, base)

    resources += topic
}

class KafkaProductConfig: ProductBuildParameters() {
    var bootstrapServers
        get() = this["env.KAFKA_BOOTSTRAP_SERVERS"]?.second?.split(",")
        set(value) = this.param("env.KAFKA_BOOTSTRAP_SERVERS",
            (value ?: emptyList()).joinToString(","))
}

fun ProductBuildParameters.kafka(init: KafkaProductConfig.() -> Unit) {
    putAll(KafkaProductConfig().apply(init))
}
