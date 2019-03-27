package com.github.dreef3.teamcity.dsl.impl.resources

import jetbrains.buildServer.configs.kotlin.v2017_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.dsl.PipelineStageType
import com.github.dreef3.teamcity.dsl.resources.Vault
import com.github.dreef3.teamcity.dsl.impl.ServiceResourceImpl
import com.github.dreef3.teamcity.dsl.impl.PipelineExtension
import com.github.dreef3.teamcity.util.snippets.SHEBANG

class VaultImpl : ServiceResourceImpl<Vault>() {
    override fun pipelineExtensions(resource: Vault): List<PipelineExtension> = listOf(
        PipelineExtension(PipelineStageType.DEPLOY, order = -1, fn = {
            params {
                text("env.VAULT_ROLE_ID", "", display = ParameterDisplay.HIDDEN)
                text("env.VAULT_SECRET_ID", "", display = ParameterDisplay.HIDDEN)
            }

            steps {
                script {
                    name = "Set Vault credentials for role ${resource.role}"
                    scriptContent = """
                    $SHEBANG

                    token=$(curl -XPOST --data "{\"password\": \"${'$'}{VAULT_CI_PASSWORD}\"}" \
                        ${'$'}{VAULT_ADDR}/v1/auth/userpass/login/${'$'}{VAULT_CI_USER} | jq -r '.auth.client_token')

                    role_id=$(curl -H "X-Vault-Token: ${'$'}token" \
                        ${'$'}{VAULT_ADDR}/v1/auth/approle/role/${resource.role}/role-id | jq -r '.data.role_id' )
                    secret_id=${'$'}(curl -XPOST -H "X-Vault-Token: ${'$'}token" \
                        ${'$'}{VAULT_ADDR}/v1/auth/approle/role/${resource.role}/secret-id | jq -r '.data.secret_id')

                    echo "##teamcity[setParameter name='env.VAULT_ROLE_ID' value='${'$'}role_id']"
                    echo "##teamcity[setParameter name='env.VAULT_SECRET_ID' value='${'$'}secret_id']"
                    """.trimIndent()
                }
            }
        })
    )
}

