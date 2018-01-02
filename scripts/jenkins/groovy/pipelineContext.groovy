import com.cloudbees.groovy.cps.NonCPS

class PipelineContext{

    private buildConfig
    private buildSummary

    private PipelineContext(final buildConfig, final buildSummary) {
        this.buildConfig = buildConfig
        this.buildSummary = buildSummary
    }

    void getBuildConfig() {
        return buildConfig
    }

    void getBuildSummary() {
        return buildSummary
    }


    void addStageSummary(final context, final String stageName) {
        buildSummary.addStageSummary(stageName)
        updateJobDescription(context)
    }

    void markStageSuccessful(final context, final String stageName) {
        buildSummary.setStageResult(stageName, buildSummary.RESULT_SUCCESS)
        updateJobDescription(context)
    }

    void markStageFailed(final context, final String stageName) {
        buildSummary.setStageResult(stageName, buildSummary.RESULT_FAILURE)
        updateJobDescription(context)
    }

    void setStageDetails(final context, final String stageName, final String nodeName, final String workspacePath) {
        buildSummary.setStageDetails(stageName, nodeName, workspacePath)
        updateJobDescription(context)
    }

    private void updateJobDescription(final context) {
        def stagesTable = ''
        def stagesTableBody = ''

        if (!buildSummary.getStageSummaries().isEmpty()) {
            for (stageSummary in buildSummary.getStageSummaries()) {
                def nodeName = stageSummary['nodeName'] == null ? 'Not yet allocated' : stageSummary['nodeName']
                def result = stageSummary['result'] == null ? 'Pending' : stageSummary['result']
                stagesTableBody += """
          <tr style="background-color: ${buildSummary.stageResultToBgColor(stageSummary['result'])}">
            <td style="border: 1px solid black; padding: 0.2em 1em">${stageSummary['stageName']}</td>
            <td style="border: 1px solid black; padding: 0.2em 1em">${nodeName}</td>
            <td style="border: 1px solid black; padding: 0.2em 1em">${stageSummary['workspacePath']}</td>
            <td style="border: 1px solid black; padding: 0.2em 1em">${result.capitalize()}</td>
          </tr>
        """
            }

            stagesTable = """
        <table style="margin-left: 1em; border-collapse: collapse">
          <thead>
            <tr>
              <th style="border: 1px solid black; padding: 0.5em">Name</th>
              <th style="border: 1px solid black; padding: 0.5em">Node</th>
              <th style="border: 1px solid black; padding: 0.5em">Workspace</th>
              <th style="border: 1px solid black; padding: 0.5em">Result</th>
            </tr>
          </thead>
          <tbody>
            ${stagesTableBody}
          </tbody>
        </table>
      """
        }

        context.currentBuild.description = """
      <div>
        <h3>
          Details
        </h3>
        <p style="margin-left: 1em"><strong>Commit Message:</strong> ${commitMessage}</p>
        <p style="margin-left: 1em"><strong>SHA:</strong> ${context.env.GIT_SHA}</p>
        ${stagesTable}  
      </div>
    """
    }

    static class Factory {

        private static final String BUILD_SUMMARY_SCRIPT_NAME = 'buildSummary.groovy'
        private static final String BUILD_CONFIG_SCRIPT_NAME = 'buildConfig.groovy'

        @NonCPS
        static PipelineContext create(final context, final String scriptsDirPath, final String mode, final String commitMessage, final List<String> changes, final boolean overrideDetectionChange) {
            def final buildSummaryFactory = context.load("${scriptsDirPath}/${BUILD_SUMMARY_SCRIPT_NAME}")
            def final buildConfigFactory = context.load("${scriptsDirPath}/${BUILD_CONFIG_SCRIPT_NAME}")
            return new PipelineContext(
                    buildSummaryFactory,
                    buildConfigFactory.create(context, mode, commitMessage, changes, overrideDetectionChange)
            )
        }
    }

}

return new PipelineContext.Factory()