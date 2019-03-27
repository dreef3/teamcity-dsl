package com.github.dreef3.teamcity.dsl.resources

import com.github.dreef3.teamcity.dsl.ServiceResource
import com.github.dreef3.teamcity.dsl.ServiceResources
import com.github.dreef3.teamcity.dsl.TeamCityDslMarker

@TeamCityDslMarker
class DjangoMigrations(init: DjangoMigrations.() -> Unit, base: DjangoMigrations?) : ServiceResource() {
    var imageName = ""
    var args = ""

    var database: Postgres = Postgres()

    init {
        if (base != null) {
            database = base.database
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

fun ServiceResources.djangoMigrations(base: DjangoMigrations? = null, init: DjangoMigrations.() -> Unit = {}) {
    val migrations = DjangoMigrations(init, base)

    if (migrations.imageName.isEmpty()) {
        migrations.imageName = "${service.config.docker.imagePrefix}/${this.service.name}"
    }

    resources += migrations
}
