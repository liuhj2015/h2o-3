def call(final String scriptsDirPath, final String mode, final String commitMessage, final List<String> changes, final boolean overrideDetectionChange) {
    final String BUILD_SUMMARY_SCRIPT_NAME = 'buildSummary.groovy'
    final String BUILD_CONFIG_SCRIPT_NAME = 'buildConfig.groovy'
    final String PIPELINE_UTILS_SCRIPT_NAME = 'pipelineUtils.groovy'
    final String EMAILER_SCRIPT_NAME = 'emailer.groovy'

    def final buildSummaryFactory = load("${scriptsDirPath}/${BUILD_SUMMARY_SCRIPT_NAME}")
    def final buildConfigFactory = load("${scriptsDirPath}/${BUILD_CONFIG_SCRIPT_NAME}")
    def final pipelineUtilsFactory = load("${scriptsDirPath}/${PIPELINE_UTILS_SCRIPT_NAME}")
    def final emailerFactory = load("${scriptsDirPath}/${EMAILER_SCRIPT_NAME}")
    return new PipelineContext(
            buildConfigFactory(this, mode, commitMessage, changes, overrideDetectionChange),
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