package com.github.dreef3.dsl

import com.github.dreef3.teamcity.dsl.*
import com.github.dreef3.teamcity.dsl.resources.vault
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@TeamCityDslMarker
class Manifest(init: Manifest.() -> Unit) {
    var productId = ""

    private lateinit var serviceInit: Service.() -> Unit
    private var configInit: DslConfig.() -> Unit = {}

    init {
        init()
    }

    fun service(init: Service.() -> Unit) {
        this.serviceInit = init
    }

    fun config(init: DslConfig.() -> Unit) {
        this.configInit = init
    }

    fun toService(): Service {
        val config = DslConfig(null, configInit)
        val service = Service(config, config.serviceDefaults).apply(serviceInit)

        config.apply {
            params {
                vault {
                    address = ""
                }
            }

            docker {
                imagePrefix = service.repository.split("/")[0]
            }

            k8s {
                repository = service.repository
                branch = "develop"


                env(StackEnvironment.TEST) {
                    cluster = "dev2"
                    login = "sme_saas_rw"
                    password = vault("saascrm/kv/ad", "sme_saas_rw")
                }

                env(StackEnvironment.PRODUCTION) {
                    cluster = "prod"
                    login = "sme_saas_rw"
                    password = vault("saascrm/kv/ad", "sme_saas_rw")
                }
            }
        }

        return service
    }

    companion object {
        fun fromUrl(url: String): Manifest {
            with(ScriptManager.getEngineByExtension("kts")) {
                val contents = Scanner(URL(url).openStream(), StandardCharsets.UTF_8.name()).use {
                    it.useDelimiter("\\A")
                    it.next()
                }

                return eval(contents) as Manifest
            }
        }

        fun fromFile(path: String): Manifest {
            with(ScriptManager.getEngineByExtension("kts")) {
                val contents = String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8)

                return eval(contents) as Manifest
            }
        }
    }
}

fun manifest(init: Manifest.() -> Unit = {}) = Manifest(init)
