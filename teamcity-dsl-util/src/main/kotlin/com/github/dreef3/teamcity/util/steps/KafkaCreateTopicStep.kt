package com.github.dreef3.teamcity.util.steps

import com.github.dreef3.teamcity.util.snippets.SHEBANG
import com.github.dreef3.teamcity.util.snippets.SKIP_STEP_CHECK
import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script

fun BuildSteps.kafkaCreateTopic(zookeeperHost: String, topic: String, skip: Boolean = true) {
    script {
        name = "Create kafka topic"
        scriptContent = """
        $SHEBANG
        ${SKIP_STEP_CHECK(skip)}

        docker run --rm --network host confluentinc/cp-kafka:4.1.1 \
            kafka-topics \
            --create \
            --if-not-exists \
            --zookeeper $zookeeperHost \
            --replication-factor 1 \
            --partitions 1 \
            --config retention.ms=7200000 \
            --topic $topic
        """.trimIndent()
    }
}
