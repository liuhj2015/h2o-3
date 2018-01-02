class PipelineContext{

    private buildConfig
    private buildSummary

    private PipelineContext(final buildConfig, final buildSummary) {
        this.buildConfig = buildConfig
        this.buildSummary = buildSummary
    }

    def getBuildConfig() {
        return buildConfig
    }

    def getBuildSummary() {
        return buildSummary
    }

    static class Factory {

        private static final String BUILD_SUMMARY_SCRIPT_NAME = 'buildSummary.groovy'
        private static final String BUILD_CONFIG_SCRIPT_NAME = 'buildConfig.groovy'

        static PipelineContext create(final context, final String scriptsDirPath, final String mode, final String commitMessage, final List<String> changes, final boolean overrideDetectionChange) {
            def final buildSummaryFactory = context.load("${scriptsDirPath}/${BUILD_SUMMARY_SCRIPT_NAME}")
            def final buildConfigFactory = context.load("${scriptsDirPath}/${BUILD_CONFIG_SCRIPT_NAME}")
            return new PipelineContext(
                    buildConfigFactory(context, mode, commitMessage, changes, overrideDetectionChange),
                    buildSummaryFactory(commitMessage)
            )
        }
    }

}

return new PipelineContext.Factory()