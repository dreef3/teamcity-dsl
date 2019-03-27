package com.github.dreef3.teamcity.util.snippets

val SKIP_STEP_CHECK = { skip: Boolean ->
    """
    SKIP=${'$'}{$skip:-${'$'}SKIP_ALL}
    if [[ "${'$'}SKIP" == "true" ]]; then
        echo "I was told to skip this step"
        exit 0
    fi
    """.trimIndent()
}

val SHEBANG = """
#!/bin/bash
set -e
set -x
""".trimIndent()
