import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import com.github.dreef3.teamcity.util.snippets.SHEBANG

fun BuildSteps.cleanFeatureStacks() {
    script {
        name = "Cleanup feature stacks"
        workingDir = "./deployment/scripts"
        scriptContent = """
        $SHEBANG

        rm -rf ${'$'}PWD/venv

        virtualenv -p python2 ${'$'}PWD/venv

        source ${'$'}PWD/venv/bin/activate

        pip install -U pip

        pip install -r ./requirements.txt

        python ./cleanup_feature_stacks.py

        python ./cleanup_elastic_indexies.py --host %env.ELASTICSEARCH_HOST%

        deactivate
        """.trimIndent()
    }
}
