package com.github.dreef3.teamcity.dsl.resources

import com.github.dreef3.teamcity.dsl.ServiceResource
import com.github.dreef3.teamcity.dsl.ServiceResources
import com.github.dreef3.teamcity.dsl.TeamCityDslMarker

@TeamCityDslMarker
class MongoMigrations(init: MongoMigrations.() -> Unit = {}, base: MongoMigrations?) : ServiceResource(base = base as ServiceResource?) {
    var dockerFile = "migrations.Dockerfile"
    var imageName = ""
    var args = ""

    var database: Mongo = Mongo()

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

    fun db(base: Mongo? = null, init: Mongo.() -> Unit) {
        database = Mongo(init, base)
    }
}

fun ServiceResources.mongoMigrations(base: MongoMigrations? = null, init: MongoMigrations.() -> Unit = {}) {
    val migrations = MongoMigrations(init, base)

    if (migrations.imageName.isEmpty()) {
        migrations.imageName = "${service.config.docker.imagePrefix}/${this.service.name}-migrations"
    }

    resources += migrations
}
