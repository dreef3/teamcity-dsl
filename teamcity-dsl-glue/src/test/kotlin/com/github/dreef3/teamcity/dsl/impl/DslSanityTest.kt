package com.github.dreef3.teamcity.dsl.impl

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import com.github.dreef3.teamcity.dsl.*
import com.github.dreef3.teamcity.dsl.resources.helm
import com.github.dreef3.teamcity.dsl.resources.rancher

class DslSanityTest {
    val config = DslConfig().apply {
        candidate = ""
        stable = ""
        docker {
            imagePrefix = ""
        }
    }

    @Test
    internal fun `can convert Product to Project`() {
        val dsl = TeamCityDsl.createDefault(config)
        val project = Project()
        val product = dsl.product {
            id = "foo"
            services {
                backend {
                    name = "foobar"
                    repository = "test/foobar"
                }
            }
        }

        dsl.bind(project, products = listOf(product))

        assertThat(project.subProjects).hasSize(1)
    }

    @Test
    internal fun `can define custom stages`() {
        class StageStub : Stage()
        class StageStubImpl : StageImpl<StageStub>() {
            override fun build(def: StageStub, ctx: StageBuildContext,
                               init: PipelineBuildType.() -> Unit): PipelineBuildType = PipelineBuildType(ctx, def)
        }

        val customStages = mapOf(StageStub::class.java to StageStubImpl())
        val impl = ProductBlueprint(DslBlueprint(BundledStages + customStages, BundledResources, BundledProductResources), config)
        val dsl = TeamCityDsl(config, impl)
        val project = Project()
        val product = dsl.product {
            id = "foo"
            services {
                backend {
                    name = "foobar"
                    repository = "test/foobar"

                    stages {
                        stage(StageStub())
                    }
                }
            }
        }

        dsl.bind(project, products = listOf(product))
    }

    @Test
    internal fun `can define Rancher deployment config`() {
        val config = config.apply {
            rancher {
                env(StackEnvironment.TEST) {
                    url = "http://rancher.test"
                    accessKey = "keytest"
                    secretKey = "secrettest"
                }

                env(StackEnvironment.PRODUCTION) {
                    url = "http://rancher.prod"
                    accessKey = "keyprod"
                    secretKey = "secretprod"
                }
            }
        }

        val dsl = TeamCityDsl.createDefault(config)
        val project = Project()

        val product = dsl.product {
            id = "foo"
            services {
                backend {
                    name = "foobar"
                    repository = "test/foobar"
                }
            }
        }

        dsl.bind(project, products = listOf(product))

        val productProject = project.subProjects.first()

        assertThat(productProject.params.params.map { it.name })
            .doesNotContain("env.RANCHER_URL", "env.RANCHER_ACCESS_KEY", "env.RANCHER_SECRET_KEY")

        val serviceProject = productProject.subProjects.first()
        val testProject = serviceProject.subProjects.find { it.name == "QA" }

        assertThat(testProject?.params?.params)
            .anyMatch { it.name == "env.RANCHER_URL" && it.value == "http://rancher.test" }
        assertThat(testProject?.params?.params)
            .anyMatch { it.name == "env.RANCHER_ACCESS_KEY" && it.value == "keytest" }
        assertThat(testProject?.params?.params)
            .anyMatch { it.name == "env.RANCHER_SECRET_KEY" && it.value == "secrettest" }

        val prod = serviceProject.subProjects.find { it.name == "PROD" }

        assertThat(prod?.params?.params)
            .anyMatch { it.name == "env.RANCHER_URL" && it.value == "http://rancher.prod" }
        assertThat(prod?.params?.params)
            .anyMatch { it.name == "env.RANCHER_ACCESS_KEY" && it.value == "keyprod" }
        assertThat(prod?.params?.params)
            .anyMatch { it.name == "env.RANCHER_SECRET_KEY" && it.value == "secretprod" }
    }

    @Test
    internal fun `can define default service blueprint with k8s deployment`() {
        val config = DslConfig(config) {
            serviceDefaults = {
                stages {
                    build()
                    deploy()
                }

                resources {
                    helm()
                }
            }

            k8s {
                repository = "test/test"
            }
        }
        val dsl = TeamCityDsl.createDefault(config)
        val project = Project()
        val product = dsl.product {
            id = "foo"
            services {
                backend {
                    name = "foobar"
                    repository = "test/foobar"
                }
            }
        }

        dsl.bind(project, products = listOf(product))

        assertThat(project.roots).anyMatch { it.id == "K8sChartsGeneralVCS" }

        val deploys = allBuildTypes(project)
            .filter { it is PipelineBuildType && it.stageType == PipelineStageType.DEPLOY }

        assertThat(deploys).isNotEmpty

        assertThat(deploys).allSatisfy {
            assertThat(it.vcs.items).containsKey("K8sChartsGeneralVCS")
        }
    }

    @Test
    internal fun `can define default service blueprint with Rancher deployment`() {
        val config = DslConfig(config) {
            serviceDefaults = {
                stages {
                    build()
                    deploy()
                }

                resources {
                    rancher()
                }
            }

            rancher {
                repository = "test/test"
            }
        }
        val dsl = TeamCityDsl.createDefault(config)
        val project = Project()
        val product = dsl.product {
            id = "foo"
            services {
                backend {
                    name = "foobar"
                    repository = "test/foobar"
                }
            }
        }

        dsl.bind(project, products = listOf(product))

        assertThat(project.roots).anyMatch { it.id == "RancherGeneralVCS" }

        val deploys = allBuildTypes(project)
            .filter { it is PipelineBuildType && it.stageType == PipelineStageType.DEPLOY }

        assertThat(deploys).isNotEmpty

        assertThat(deploys).allSatisfy {
            assertThat(it.vcs.items).doesNotContainKey("K8sChartsGeneralVCS")
            assertThat(it.vcs.items).containsKey("RancherGeneralVCS")
        }
    }

    @Test
    internal fun `can define service without product`() {
        val dsl = TeamCityDsl.createDefault(config)
        val project = Project()
        val service = dsl.service {
            name = "foobar"
            repository = "test/foobar"
        }

        dsl.bind(project, services = listOf(service))

        assertThat(project.subProjects).hasSize(1)

        val serviceProject = project.subProjects.first()

        assertThat(serviceProject.id).isEqualTo("Foobar")
    }

    private fun allBuildTypes(project: Project): List<BuildType> = project.subProjects.fold(emptyList()) { builds: List<BuildType>, proj: Project ->
        builds + proj.buildTypes + allBuildTypes(proj)
    }
}
