package com.github.dreef3.teamcity.util.vcs

import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import jetbrains.buildServer.configs.kotlin.v2017_2.VcsRoot
import jetbrains.buildServer.configs.kotlin.v2017_2.toId
import jetbrains.buildServer.configs.kotlin.v2017_2.vcs.GitVcsRoot
import com.github.dreef3.teamcity.util.extensions.toUUID

fun Project.defaultVcsRoots(repository: String,
                            prefix: String = "",
                            defaultBranch: String = "develop") {
    mainBranchVcsRoot(this, repository, prefix, defaultBranch)
    pullRequestVcsRoot(this, repository, prefix, defaultBranch)
}

fun pullRequestVcsRoot(project: Project, repository: String, prefix: String, mainBranch: String = "develop") {
    project.vcsRoot(GitVcsRoot {
        id = "${prefix}PullRequestVCS".toId(project.id)
        uuid = id.toUUID()
        name = id
        url = ""
        branch = mainBranch
        branchSpec = """
            +:refs/pull-requests/*/from
            -:refs/heads/master
            -:refs/heads/$mainBranch
        """.trimIndent()
        authMethod = defaultPrivateKey {
        }
    })
}

fun mainBranchVcsRoot(project: Project, repository: String, prefix: String, mainBranch: String = "develop") {
    project.vcsRoot(GitVcsRoot {
        id = "${prefix}GeneralVCS".toId(project.id)
        uuid = id.toUUID()
        name = id
        url = ""
        branch = mainBranch
        branchSpec = "+:refs/heads/*"
        authMethod = defaultPrivateKey {
        }
    })
}

@Deprecated("Use mainBranch() instead", ReplaceWith("mainBranch(project, prefix)"))
fun general(project: Project, prefix: String = "") = mainBranch(project, prefix)

fun mainBranch(project: Project, prefix: String = ""): VcsRoot {
    return project.roots.find { root ->
        val id = "${prefix}GeneralVCS".toId(project.id)
        root.id == id
    }
        ?: throw IllegalArgumentException("No VCS root found!")
}

fun pullRequest(project: Project, prefix: String = ""): VcsRoot {
    return project.roots.find { root -> root.id == "${prefix}PullRequestVCS".toId(project.id) }
        ?: throw IllegalArgumentException("No VCS root found!")
}
