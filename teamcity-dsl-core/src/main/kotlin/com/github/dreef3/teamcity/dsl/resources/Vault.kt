package com.github.dreef3.teamcity.dsl.resources

import com.github.dreef3.teamcity.dsl.ProductBuildParameters
import com.github.dreef3.teamcity.dsl.ServiceResource
import com.github.dreef3.teamcity.dsl.ServiceResources
import com.github.dreef3.teamcity.dsl.TeamCityDslMarker

@TeamCityDslMarker
class Vault(init: Vault.() -> Unit = {}, base: Vault?) : ServiceResource(base = base as ServiceResource?) {
    var role: String = ""

    init {
        init()
    }
}

fun ServiceResources.vault(base: Vault? = null, init: Vault.() -> Unit) {
    val result = Vault(init, base)

    resources += result
}

class VaultConfig: ProductBuildParameters() {
    var address
        get() = this["env.VAULT_ADDR"]?.second
        set(value) = this.param("env.VAULT_ADDR", value ?: "")

    var username
        get() = this["env.VAULT_CI_USER"]?.second
        set(value) = this.param("env.VAULT_CI_USER", value ?: "")

    var password
        get() = this["env.VAULT_CI_PASSWORD"]?.second
        set(value) = this.param("env.VAULT_CI_PASSWORD", value ?: "")
}

fun ProductBuildParameters.vault(init: VaultConfig.() -> Unit) {
    putAll(VaultConfig().apply(init))
}
