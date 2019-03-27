package com.github.dreef3.teamcity.dsl.impl

import jetbrains.buildServer.configs.kotlin.v2017_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import jetbrains.buildServer.configs.kotlin.v2017_2.toId
import com.github.dreef3.teamcity.dsl.*
import com.github.dreef3.teamcity.dsl.impl.internal.createTriggerAllStage
import com.github.dreef3.teamcity.dsl.impl.internal.toBuildParams
import com.github.dreef3.teamcity.dsl.impl.resources.*
import com.github.dreef3.teamcity.dsl.impl.stages.*
import com.github.dreef3.teamcity.dsl.resources.*
import com.github.dreef3.teamcity.dsl.stages.*
import com.github.dreef3.teamcity.util.extensions.toUUID

val BundledStages = mapOf(
    BuildStage() to BuildStageImpl(),
    DeployStage() to DeployStageImpl(),
    ApiTestStage() to ApiTestStageImpl(),
    E2ETestStage() to E2ETestStageImpl(),
    DependenciesDeployStage() to DependenciesDeployStageImpl(),
    DeployProdLikeStage() to DeployProdLikeStageImpl()
).map { (d, i) -> Pair(d::class.java, i) }.toMap()

val BundledResources = mapOf(
    FlywayMigrations::class.java to FlywayMigrationsImpl(),
    Postgres::class.java to PostgresImpl(),
    Mongo::class.java to MongoImpl(),
    MongoMigrations::class.java to MongoMigrationsImpl(),
    LiquibaseMigrations::class.java to LiquiBaseMigrationsImpl(),
    Swagger::class.java to SwaggerImpl(),
    Vault::class.java to VaultImpl(),
    KafkaTopic::class.java to KafkaTopicImpl(),
    DjangoMigrations::class.java to DjangoMigrationsImpl(),
    Helm::class.java to HelmImpl(),
    Rancher::class.java to RancherImpl(),
    ServiceBlueGreenDeploy::class.java to ServiceBlueGreenDeployImpl()
)

val BundledProductResources = mapOf(
    BlueGreenDeploy::class.java to BlueGreenDeployImpl()
) as Map<Class<out ProductResource>, ProductResourceImpl<out ProductResource>>

open class DslBlueprint(val stages: Map<Class<out Stage>, StageImpl<out Stage>>,
                        val resources: Map<Class<out ServiceResource>, ServiceResourceImpl<out ServiceResource>>,
                        val productResources: Map<Class<out ProductResource>, ProductResourceImpl<out ProductResource>>)

class ProductBlueprint(private val blueprint: DslBlueprint,
                       private val config: DslConfig) {
    fun bindProducts(parent: Project, products: List<Product>) {
        val resourcesByProduct = products
            .map {
                Pair(it, it.resources.items.map { def ->
                    Pair(def, blueprint.productResources[def::class.java] as ProductResourceImpl<ProductResource>)
                })
            }
            .toMap()

        for (product in products) {
            val resources = resourcesByProduct[product] ?: emptyList()

            resources
                .map { (def, impl) -> impl.serviceExtensions(def, product) }
                .forEach { ext ->
                    product.services.items.forEach { service -> service.apply(ext) }
                }
            resources.forEach { (def, impl) ->
                product.apply(impl.productExtensions(def))
            }
        }

        products
            .flatMap { collectParentExtensions(it) }
            .distinctBy { (key) -> key }
            .filter { (key) -> !parent.params.hasParam(key) }
            .forEach { (key, ext) ->
                parent.apply(ext)
                parent.params.text(key, "true", display = ParameterDisplay.HIDDEN)
            }

        for (product in products) {
            parent.subProject(toProject(parent, product, resourcesByProduct[product] ?: emptyList()))
        }
    }

    private fun collectParentExtensions(product: Product): List<Pair<String, (Project) -> Unit>> {
        val allResources = product.services.items
            .flatMap { s -> s.resources.resources }
            .map { def ->
                Pair(def, blueprint.resources[def::class.java] as ServiceResourceImpl<ServiceResource>)
            }

        return allResources.flatMap { (def, impl) ->
            val key = "dsl.${def.javaClass.simpleName.toLowerCase()}.applied"

            impl.parentProjectExtensions(config, def).map { Pair(key, it) }
        }
    }

    private fun toProject(parent: Project, product: Product,
                          resources: List<Pair<ProductResource, ProductResourceImpl<ProductResource>>>): Project {
        config.params.forEach { (e, params) ->
            params.forEach { (k, v) -> product.params[e]?.putIfAbsent(k, v) }
        }
        config.pipelineParams.forEach { (e, params) ->
            params.forEach { (k, v) -> product.pipelineParams[e]?.putIfAbsent(k, v) }
        }

        val services = product.services.items.map { ServiceBlueprint(blueprint, config, it, parent) }
        val info = BuildInfo(product, services, parent)

        return Project {
            id = product.id.toId(parent.id)
            uuid = id.toUUID()
            name = product.id
            parentId = parent.id

            params(toBuildParams(product.params[StackEnvironment.ALL]))

            services.forEach {
                subProject(it.toProject(info))
            }

            buildType(createTriggerAllStage(product, this@Project))

            subProject {
                id = productionProjectId(this@Project)
                uuid = id.toUUID()
                parentId = this@Project.id
                name = "PROD"

                params(toBuildParams(product.params[StackEnvironment.PRODUCTION]))

                buildType(createDeployAllStage(this, product, services))
            }

            resources.forEach { (def, impl) ->
                this@Project.apply(impl.productProjectExtensions(def, product))
            }
        }
    }

    fun bindServices(project: Project, services: List<Service>) {
        val items = services.map { ServiceBlueprint(blueprint, config, it, project) }
        val info = BuildInfo(Product(config, { id = "empty" }), items, project)

        for (service in items) {
            project.subProject(service.toProject(info))
        }
    }

    companion object {
        fun productionProjectId(project: Project) = "PROD".toId(project.id)
    }
}

data class BuildInfo(val product: Product,
                     val services: List<ServiceBlueprint>,
                     val rootProject: Project)
