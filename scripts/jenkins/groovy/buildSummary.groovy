class BuildSummary {

    public static final String RESULT_SUCCESS = 'success'
    public static final String RESULT_FAILURE = 'failure'

    private final List stageSummaries = []

    def addStageSummary(final String stageName) {
        if (findForName(stageName) != null) {
            throw new IllegalArgumentException("Stage Summary for %s already added".format(stageName))
        }
        def summary = [
                stageName: stageName
        ]
        stageSummaries.add(summary)
        return summary
    }

    def setStageDetails(final String stageName, final String nodeName, final String workspacePath) {
        def summary = findOrThrowForName(stageName)
        summary['nodeName'] = nodeName
        summary['workspacePath'] = workspacePath
        return summary
    }

    def setStageResult(final String stageName, final String result) {
        def summary = findOrThrowForName(stageName)
        summary['result'] = result
        return summary
    }

    def getStageSummaries() {
        return stageSummaries
    }

    String stageResultToBgColor(final String result) {
        def BG_COLOR_SUCCESS = '#7fce67'
        def BG_COLOR_FAILURE = '#d56060'
        def BG_COLOR_OTHER = '#fbf78b'

        if (result == buildSummary.RESULT_SUCCESS) {
            return BG_COLOR_SUCCESS
        }
        if (result == buildSummary.RESULT_FAILURE) {
            return BG_COLOR_FAILURE
        }
        return BG_COLOR_OTHER
    }

    @Override
    String toString() {
        return "${stageSummaries}"
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

return new BuildSummary()