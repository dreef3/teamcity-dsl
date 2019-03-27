package com.github.dreef3.teamcity.util.steps

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep
import com.github.dreef3.teamcity.util.snippets.SHEBANG

open class DumpPostgtresDB(init: DumpPostgtresDB.() -> Unit = {}) : ScriptBuildStep() {

    var userNameFrom = "%env.DB_USER%"
    var userPassFrom = "%env.DB_PASSWORD%"
    var dbNameFrom = "%env.DB_NAME%"
    var hostFrom = "%env.DB_HOST%"
    var dbPortFrom = "%env.DB_PORT%"
    var postgresVersion = "9.6"

    init {
        init()
        name = "Create PG dump of $dbNameFrom from $hostFrom"
        scriptContent = """
        $SHEBANG

        mkdir -p dumps

        container_id=`docker run -v %teamcity.build.workingDir%/dumps:/tmp --entrypoint /bin/sh --network host -td postgres:$postgresVersion`

        trap "set +e; docker rm -f ${'$'}{container_id} &> /dev/null" EXIT

        DB_DUMP="PGPASSWORD=$userPassFrom PGOPTIONS=\"-c statement_timeout=0\" pg_dump -c -Ft --no-owner -h $hostFrom -p $dbPortFrom -U $userNameFrom $dbNameFrom > /tmp/dump_$dbNameFrom"

        echo "Saving PG dump to file. This might take some time please wait!"
        docker exec ${'$'}{container_id} /bin/sh -c "${'$'}{DB_DUMP}"
        """.trimIndent()
    }
}

fun BuildSteps.dumpPostgresDB(init: DumpPostgtresDB.() -> Unit = {}) {
    step(DumpPostgtresDB(init))
}

open class RestorePostgtresDB(init: RestorePostgtresDB.() -> Unit = {}) : ScriptBuildStep() {

    var userNameTo = "%env.DB_USER%"
    var userPassTo = "%env.DB_PASSWORD%"
    var dbNameTo = "%env.DB_NAME%"
    var dbNameFrom = "%env.DB_NAME%"
    var hostTo = "%feature.db.host%"
    var dbPortTo = "5432"
    var postgresVersion = "9.6"

    init {
        init()
        name = "Restore PG dump of $dbNameFrom to $dbNameTo on $hostTo"
        scriptContent = """
        $SHEBANG

        mkdir -p dumps

        container_id=`docker run -v %teamcity.build.workingDir%/dumps:/tmp --entrypoint /bin/sh --network host -td postgres:$postgresVersion`

        trap "set +e; docker rm -f ${'$'}{container_id} &> /dev/null" EXIT

        DB_RESTORE="PGPASSWORD=$userPassTo pg_restore -Ft --no-owner --no-acl -h $hostTo -p $dbPortTo -U $userNameTo -d $dbNameTo /tmp/dump_$dbNameFrom"

        echo "Restoring PG dump from file. This might take some time please wait!"
        docker exec ${'$'}{container_id} /bin/sh -c "${'$'}{DB_RESTORE}"
        """.trimIndent()
    }
}

fun BuildSteps.restorePostgresDB(init: RestorePostgtresDB.() -> Unit = {}) {
    val restoreDB = RestorePostgtresDB(init)

    step(CreatePostgresFeatureDB {
        dbHost = restoreDB.hostTo
        dbName = restoreDB.dbNameTo
        userName = restoreDB.userNameTo
        userPass = restoreDB.userPassTo
    })

    step(restoreDB)
}

open class DumpMongoDB(init: DumpMongoDB.() -> Unit = {}) : ScriptBuildStep() {
    var mongoDbFrom = "%env.RAVEN_METADATA_SERVICE_DB_NAME%"

    init {
        init()
        name = "Create MongoDB dump"
        scriptContent = """
            $SHEBANG
            mkdir -p dumps
            container_id=`docker run -v %teamcity.build.workingDir%/dumps:/tmp --entrypoint /bin/sh --network host -td mongo:3.6`
            trap "set +e; docker rm -f ${'$'}{container_id} &> /dev/null" EXIT
            docker exec ${'$'}{container_id} mongodump --host %env.MONGO_DB_HOST% -u "%env.MONGO_DB_USER%" -p %env.MONGO_DB_PASSWORD% --db "$mongoDbFrom" --out /tmp/mongo_dump
            """.trimIndent()
    }
}

open class RestoreMongoDB(init: RestoreMongoDB.() -> Unit = {}) : ScriptBuildStep() {
    var mongoHostTo = ""
    var mongoDbFrom = "%env.RAVEN_METADATA_SERVICE_DB_NAME%"
    var mongoDbTo = ""

    init {
        init()
        name = "Restore MongoDB from dump"
        scriptContent = """
            $SHEBANG
            mkdir -p dumps
            container_id=`docker run -v %teamcity.build.workingDir%/dumps:/tmp --entrypoint /bin/sh --network host -td mongo:3.6`
            trap "set +e; docker rm -f ${'$'}{container_id} &> /dev/null" EXIT
            docker exec ${'$'}{container_id} mongo $mongoHostTo/$mongoDbTo --eval "db.dropDatabase()"
            docker exec ${'$'}{container_id} mongorestore --drop -h $mongoHostTo --db "$mongoDbTo" /tmp/mongo_dump/$mongoDbFrom

            """.trimIndent()
    }
}

fun BuildSteps.dumpMongoDB(init: DumpMongoDB.() -> Unit = {}) {
    step(DumpMongoDB(init))
}

fun BuildSteps.restoreMongoDB(init: RestoreMongoDB.() -> Unit = {}) {
    step(RestoreMongoDB(init))
}
