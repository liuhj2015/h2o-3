def call(final String h2o3Root, final String mode, final scmEnv, final boolean overrideDetectionChange) {
    final String BUILD_SUMMARY_SCRIPT_NAME = 'buildSummary.groovy'
    final String BUILD_CONFIG_SCRIPT_NAME = 'buildConfig.groovy'
    final String PIPELINE_UTILS_SCRIPT_NAME = 'pipelineUtils.groovy'
    final String EMAILER_SCRIPT_NAME = 'emailer.groovy'
    final String GET_CHANGES_SCRIPT_NAME = 'getChanges.groovy'

    // get commit message
    env.COMMIT_MESSAGE = sh(script: 'cd h2o-3 && git log -1 --pretty=%B', returnStdout: true).trim()
    env.BRANCH_NAME = scmEnv['GIT_BRANCH'].replaceAll('origin/', '')
    env.GIT_SHA = scmEnv['GIT_COMMIT']
    env.GIT_DATE = "${sh(script: 'cd h2o-3 && git show -s --format=%ci', returnStdout: true).trim()}"

    def final buildSummaryFactory = load("${h2o3Root}/scripts/jenkins/groovy/${BUILD_SUMMARY_SCRIPT_NAME}")
    def final buildConfigFactory = load("${h2o3Root}/scripts/jenkins/groovy/${BUILD_CONFIG_SCRIPT_NAME}")
    def final pipelineUtilsFactory = load("${h2o3Root}/scripts/jenkins/groovy/${PIPELINE_UTILS_SCRIPT_NAME}")
    def final emailerFactory = load("${h2o3Root}/scripts/jenkins/groovy/${EMAILER_SCRIPT_NAME}")
    def final getChanges = load("${h2o3Root}/scripts/jenkins/groovy/${GET_CHANGES_SCRIPT_NAME}")

    return new PipelineContext(
            buildConfigFactory(this, mode, env.COMMIT_MESSAGE, getChanges(h2o3Root), overrideDetectionChange),
            buildSummaryFactory(),
            pipelineUtilsFactory(),
            emailerFactory()
    )
}

class PipelineContext{

    private final buildConfig
    private final buildSummary
    private final pipelineUtils
    private final emailer

    private PipelineContext(final buildConfig, final buildSummary, final pipelineUtils, final emailer) {
        this.buildConfig = buildConfig
        this.buildSummary = buildSummary
        this.pipelineUtils = pipelineUtils
        this.emailer = emailer
    }

    def getBuildConfig() {
        return buildConfig
    }

    def getBuildSummary() {
        return buildSummary
    }

    def getUtils() {
        return pipelineUtils
    }

    def getEmailer() {
        return emailer
    }

}

return this