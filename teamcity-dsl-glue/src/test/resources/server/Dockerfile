FROM jetbrains/teamcity-server:2017.2.4

COPY database.properties "${TEAMCITY_DATA_PATH}/config/"
COPY teamcity-startup.properties "${TEAMCITY_DIST}/conf/"
COPY postgresql-42.2.5.jar "${TEAMCITY_DATA_PATH}/lib/jdbc/"
