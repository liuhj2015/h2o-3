def call(final String commitMessage) {
    return new BuildSummary(commitMessage)
}

class BuildSummary {

    public static final String RESULT_SUCCESS = 'success'
    public static final String RESULT_FAILURE = 'failure'

    private final String commitMessage
    private final List stageSummaries

    BuildSummary(final String commitMessage) {
        this.commitMessage = commitMessage
        stageSummaries = []
    }

    def addStageSummary(final context, final String stageName) {
        if (findForName(stageName) != null) {
            throw new IllegalArgumentException("Stage Summary for %s already added".format(stageName))
        }
        def summary = [
            stageName: stageName
        ]
        stageSummaries.add(summary)
        updateJobDescription(context)
        return summary
    }

    def markStageSuccessful(final context, final String stageName) {
        setStageResult(stageName, RESULT_SUCCESS)
        updateJobDescription(context)
    }

    def markStageFailed(final context, final String stageName) {
        setStageResult(stageName, RESULT_FAILURE)
        updateJobDescription(context)
    }

    def setStageDetails(final context, final String stageName, final String nodeName, final String workspacePath) {
        def summary = findOrThrowForName(stageName)
        summary['nodeName'] = nodeName
        summary['workspacePath'] = workspacePath
        updateJobDescription(context)
        return summary
    }

    @Override
    String toString() {
        return "${stageSummaries}"
    }

    private void updateJobDescription(final context) {
        def stagesTable = ''
        def stagesTableBody = ''

        if (!stageSummaries.isEmpty()) {
            for (stageSummary in stageSummaries) {
                def nodeName = stageSummary['nodeName'] == null ? 'Not yet allocated' : stageSummary['nodeName']
                def result = stageSummary['result'] == null ? 'Pending' : stageSummary['result']
                stagesTableBody += """
          <tr style="background-color: ${stageResultToBgColor(stageSummary['result'])}">
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

    private setStageResult(final String stageName, final String result) {
        def summary = findOrThrowForName(stageName)
        summary['result'] = result
        return summary
    }

    private String stageResultToBgColor(final String result) {
        def BG_COLOR_SUCCESS = '#7fce67'
        def BG_COLOR_FAILURE = '#d56060'
        def BG_COLOR_OTHER = '#fbf78b'

        if (result == RESULT_SUCCESS) {
            return BG_COLOR_SUCCESS
        }
        if (result == RESULT_FAILURE) {
            return BG_COLOR_FAILURE
        }
        return BG_COLOR_OTHER
    }

    private def findForName(final String stageName) {
        return stageSummaries.find({it.stageName == stageName})
    }

    private def findOrThrowForName(final String stageName) {
        def summary = findForName(stageName)
        if (summary == null) {
            throw new IllegalStateException("Cannot find StageSummary for %s".format(stageName))
        }
        return summary
    }

}

return this