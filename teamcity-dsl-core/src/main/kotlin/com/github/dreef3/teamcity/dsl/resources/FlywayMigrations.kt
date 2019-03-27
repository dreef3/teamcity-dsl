package com.github.dreef3.teamcity.dsl.resources

import com.github.dreef3.teamcity.dsl.ServiceResource
import com.github.dreef3.teamcity.dsl.ServiceResources
import com.github.dreef3.teamcity.dsl.TeamCityDslMarker

@TeamCityDslMarker
class FlywayMigrations(init: FlywayMigrations.() -> Unit, base: FlywayMigrations?) : ServiceResource() {
    var dockerFile = "migrations.Dockerfile"
    var imageName = ""
    var args = ""

    var database: Postgres = Postgres()

    init {
        if (base != null) {
            database = base.database
            dockerFile = base.dockerFile
            imageName = base.imageName
            args = base.args
        }

        init()

        dependencies += database
    }

    fun db(base: Postgres? = null, init: Postgres.() -> Unit) {
        database = Postgres(init, base)
    }
}

fun ServiceResources.flywayMigrations(base: FlywayMigrations? = null, init: FlywayMigrations.() -> Unit = {}) {
    val migrations = FlywayMigrations(init, base)

    if (migrations.imageName.isEmpty()) {
        migrations.imageName = "${service.config.docker.imagePrefix}/${this.service.name}-migrations"
    }

    resources += migrations
}
