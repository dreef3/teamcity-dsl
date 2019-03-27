package com.github.dreef3.teamcity.util.features

import jetbrains.buildServer.configs.kotlin.v2017_2.buildFeatures.CommitStatusPublisher

object BuildStatusToStashFeature : CommitStatusPublisher({
    publisher = bitbucketServer {
        url = "https://%env.BITBUCKET_STASH_HOST%"
        userName = "%env.BITBUCKET_STASH_LOGIN%"
        password = "%env.BITBUCKET_STASH_PASSWORD%"
    }
})
