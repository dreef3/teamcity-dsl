package com.github.dreef3.teamcity.dsl

class StashConfig {
    var login = "saas_release_bot"
    // FIXME Commit publish feature requires password field which doesn't work with Vault plugin
    // var password = vault("saascrm/kv/ad", "saas_release_bot")
    var password = "zxxdf770745edb5f1009e5ad49dcfb98cb7775d03cbe80d301b"
}

class JIRAConfig {
    var login = "saas_release_bot"
    var password = vault("saascrm/kv/ad", "saas_release_bot")
}

class DockerConfig {
    var imagePrefix = ""
    var token = ""
    var registry = ""
}

class DslConfig(base: DslConfig? = null, init: DslConfig.() ->Unit = {}): WithParams() {
    var candidate: String
        get() = params[StackEnvironment.ALL]!!["env.CANDIDATE_ENV"]!!.second
        set(value) = params[StackEnvironment.ALL]!!.param("env.CANDIDATE_ENV", value)
    var stable: String
        get() = params[StackEnvironment.ALL]!!["env.APP_ENV"]!!.second
        set(value) = params[StackEnvironment.ALL]!!.param("env.APP_ENV", value)

    var docker = DockerConfig()
    var stash = StashConfig()
    var jira = JIRAConfig()

    fun docker(init: DockerConfig.() -> Unit) {
        docker.apply(init)
    }

    fun stash(init: StashConfig.() -> Unit) {
        stash.apply(init)
    }

    var serviceDefaults: Service.() -> Unit = {
        stages {
            build()
            deploy()
        }
    }

    internal fun applyParams(from: WithParams) {
        params.forEach { env, p -> from.params[env]?.let { p.putAll(it) } }
    }

    init {
        candidate = "qa2"
        stable = "qa3"

        if (base != null) {
            candidate = base.candidate
            stable = base.stable
            docker = base.docker
            params = hashMapOf(
                StackEnvironment.ALL to ProductBuildParameters(base.params[StackEnvironment.ALL]),
                StackEnvironment.TEST to ProductBuildParameters(base.params[StackEnvironment.TEST]),
                StackEnvironment.PRODUCTION to ProductBuildParameters(base.params[StackEnvironment.PRODUCTION])
            )
            pipelineParams = hashMapOf(
                PipelineType.DEVELOP to ProductBuildParameters(base.pipelineParams[PipelineType.DEVELOP]),
                PipelineType.PROD_LIKE to ProductBuildParameters(base.pipelineParams[PipelineType.PROD_LIKE]),
                PipelineType.PULL_REQUEST to ProductBuildParameters(base.pipelineParams[PipelineType.PULL_REQUEST]),
                PipelineType.PRODUCTION to ProductBuildParameters(base.pipelineParams[PipelineType.PRODUCTION])
            )
        }

        init()

        params[StackEnvironment.ALL]?.param("env.DOCKER_AUTH_TOKEN", docker.token)
        params[StackEnvironment.ALL]?.param("env.DOCKER_REGISTRY", docker.registry)
        params[StackEnvironment.ALL]?.param("env.BITBUCKET_STASH_LOGIN", stash.login)
        params[StackEnvironment.ALL]?.password("env.BITBUCKET_STASH_PASSWORD", stash.password)
        params[StackEnvironment.ALL]?.param("env.JIRA_USERNAME", jira.login)
        params[StackEnvironment.ALL]?.param("env.JIRA_PASSWORD", jira.password)
    }
}

open class WithParams {
    var params = hashMapOf(
        StackEnvironment.ALL to ProductBuildParameters(),
        StackEnvironment.TEST to ProductBuildParameters(),
        StackEnvironment.PRODUCTION to ProductBuildParameters()
    )
    var pipelineParams: MutableMap<PipelineType, ProductBuildParameters> = hashMapOf(
        PipelineType.DEVELOP to ProductBuildParameters(),
        PipelineType.PROD_LIKE to ProductBuildParameters(),
        PipelineType.PULL_REQUEST to ProductBuildParameters(),
        PipelineType.PRODUCTION to ProductBuildParameters()
    )

    fun params(env: StackEnvironment = StackEnvironment.ALL,
               init: ProductBuildParameters.() -> Unit) {
        params[env]?.init()
    }

    fun pipelineParams(type: PipelineType, init: ProductBuildParameters.() -> Unit) {
        pipelineParams[type]?.init()
    }
}

class RancherEnvConfig {
    var url: String = ""
    var accessKey: String = ""
    var secretKey: String = ""
    var masterNode: String = ""
}

class RancherConfig : WithParams() {
    var repository: String
        get() = params[StackEnvironment.ALL]!!["rancher.repository"]!!.second
        set(value) = params[StackEnvironment.ALL]!!.param("rancher.repository", value)

    fun env(env: StackEnvironment, init: RancherEnvConfig.() -> Unit) {
        val config = RancherEnvConfig().apply(init)

        params[env]?.apply {
            param("env.STACK_ENV", env.name.toLowerCase())
            param("env.RANCHER_URL", config.url)
            param("env.SSH_PROXY_HOST", config.masterNode)
            param("env.DNS_SERVER", config.masterNode)
            param("env.RANCHER_ACCESS_KEY", config.accessKey)
            param("env.RANCHER_SECRET_KEY", config.secretKey)
        }
    }
}

@TeamCityDslMarker
fun DslConfig.rancher(init: RancherConfig.() -> Unit) {
    applyParams(RancherConfig().apply(init))
}


class K8sEnvConfig {
    var cluster: String = ""
    var login: String = ""
    var password: String = ""
}

class K8sConfig : WithParams() {
    var repository: String
        get() = params[StackEnvironment.ALL]!!["k8s.chart.repository.url"]!!.second
        set(value) = params[StackEnvironment.ALL]!!.param("k8s.chart.repository.url", value)

    var branch: String
        get() = params[StackEnvironment.ALL]!!["k8s.chart.repository.branch"]!!.second
        set(value) = params[StackEnvironment.ALL]!!.param("k8s.chart.repository.branch", value)

    fun env(env: StackEnvironment, init: K8sEnvConfig.() -> Unit) {
        val config = K8sEnvConfig().apply(init)

        params[env]?.apply {
            param("env.K8S_CLUSTER", config.cluster)
            param("k8s.user.login", config.login)
            param("k8s.user.password", config.password)
        }
    }

    init {
        branch = "master"
    }
}

@TeamCityDslMarker
fun DslConfig.k8s(init: K8sConfig.() -> Unit) {
    applyParams(K8sConfig().apply(init))
}
