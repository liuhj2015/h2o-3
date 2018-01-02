def call() {
    return new PipelineUtils()
}

class PipelineUtils {
    String stageNameToDirName(stageName) {
        if (stageName != null) {
            return stageName.toLowerCase().replace(' ', '-')
        }
        return null
    }
}

return this