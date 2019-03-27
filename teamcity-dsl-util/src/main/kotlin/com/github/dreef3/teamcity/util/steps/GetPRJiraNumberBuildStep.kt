package com.github.dreef3.teamcity.util.steps

import jetbrains.buildServer.configs.kotlin.v2017_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ScriptBuildStep

open class GetPRJiraNumberBuildStep(init: GetPRJiraNumberBuildStep.() -> Unit = {}) : ScriptBuildStep() {
    var pullRequest = "%teamcity.build.branch%"

    init {
        init()
        name = "Get JIRA task number for current build"
        scriptContent = """
            #!/usr/bin/python3
import os
import sys
import re
import requests
from requests.auth import HTTPBasicAuth
from functools import partial


''' Was bad Idea and nobody wants it.
    It causes problems in case when two tasks for one service is done under one parent task.

def get_issue(key):
    issue_url = 'https://%env.JIRA_HOST%/rest/api/2/issue/{}'.format(key)
    return requests.get(issue_url, auth=HTTPBasicAuth(username, password)).json()

def find_jira_task(keys):
    for key in keys:
        info = get_issue(key)
        print(info['key'], info['fields']['issuetype']['name'])
        if info['fields']['issuetype']['name'] == 'Story':
            return info['key']
        if info['fields']['issuetype']['name'] == 'Sub-task':
            return info['fields']['parent']['key']

    return keys[-1]
'''


def get_or_default(value_getter, default_value_getter):
    value = value_getter()

    if not value:
        return default_value_getter()

    return value


def get_credentials():
    username = os.environ.get('JIRA_USERNAME', None)
    password = os.environ.get('JIRA_PASSWORD', None)

    if not username:
        print('Env variable JIRA_USERNAME is not set!')
        sys.exit(1)

    if not password:
        print('Env variable JIRA_PASSWORD is not set!')
        sys.exit(1)

    return username, password
    # return 'login', 'password'


def get_pr_id():
    pr = '%teamcity.build.branch%'

    if not re.compile(r"\d+").match(pr):
        print('Does not look like a PR number, skipping')
        sys.exit(0)

    return pr
    # return 'pr_id'


def get_vcs():  #
    return '%vcsroot.url%'

def get_repository_info(vcs):
    project_name, _, repo_name = re.compile(r"(\w+)(?:(/)(?!.*/))(.+).git${'$'}").search(vcs).groups()

    return project_name, repo_name


def get_pr(repo_info, creadentials, id):
    project_name, repo_name = repo_info
    username, password = creadentials

    url = 'https://%env.BITBUCKET_STASH_HOST%/rest/api/1.0/projects/{}/repos/{}/pull-requests/{}'.format(project_name, repo_name,
                                                                                               id)

    response = requests.get(url, auth=HTTPBasicAuth(username, password))

    if response.status_code != 200:
        print('Stash authentication failed!')
        sys.exit(1)

    return response.json()


def get_issue_from_title(title):
    issue = re.compile(r"(.*\s+)?(\w+-(\d+)).*", re.IGNORECASE).search(title)

    if not issue:
        print('ISSUE FROM TITLE NOT FOUND')

        return None

    print('FOUND ISSUE FROM TITLE: {}'.format(issue.groups()[1]))

    return issue.groups()[1:3]


def get_issue_from_branch(branch):
    issue = re.compile(r"\w+\/(\w+-(\d+)).*", re.IGNORECASE).search(branch)

    if not issue:
        print('ISSUE FROM BRANCH NOT FOUND')

        return None

    print('FOUND ISSUE FROM BRANCH: {}'.format(issue.groups()[0]))

    return issue.groups()[0:2]


def get_issue_id(pr):
    title = pr['title']
    branch = pr['fromRef']['displayId']

    issue = get_or_default(partial(get_issue_from_title, title), partial(get_issue_from_branch, branch))

    if not issue:
        print('No associated issues found!')
        sys.exit(1)

    issue_id, id = issue

    return issue_id.lower(), id


def set_build_issue(issue_id):
    print("##teamcity[setParameter name='env.ISSUE_ID' value='{}']".format(issue_id))


def main():
    credentials = get_credentials()
    pr_id = get_pr_id()
    vcs = get_vcs()
    repo_info = get_repository_info(vcs)

    pr = get_pr(repo_info, credentials, pr_id)
    issue_id, id = get_issue_id(pr)

    set_build_issue(issue_id)


if __name__ == '__main__':
    main()
        """.trimIndent()
    }
}

fun BuildSteps.jiraNumberForPR(init: GetPRJiraNumberBuildStep.() -> Unit = {}) {
    step(GetPRJiraNumberBuildStep(init))
}
