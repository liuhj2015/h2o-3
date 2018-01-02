def call(final pipelineContext) {

  def MODE_PR_TESTING_CODE = -1
  def MODE_PR_CODE = 0
  def MODE_BENCHMARK_CODE = 1
  def MODE_MASTER_CODE = 2
  def MODE_NIGHTLY_CODE = 3
  def MODES = [
    [name: 'MODE_PR_TESTING', code: MODE_PR_TESTING_CODE],
    [name: 'MODE_PR', code: MODE_PR_CODE],
    [name: 'MODE_BENCHMARK', code: MODE_BENCHMARK_CODE],
    [name: 'MODE_MASTER', code: MODE_MASTER_CODE],
    [name: 'MODE_NIGHTLY', code: MODE_NIGHTLY_CODE]
  ]

  // Job will execute PR_STAGES only if these are green.
  def SMOKE_STAGES = [
    [
      stageName: 'Py2.7 Smoke', target: 'test-py-smoke', pythonVersion: '2.7',
      timeoutValue: 8, lang: pipelineContext.getBuildConfig().LANG_PY
    ],
    [
      stageName: 'R3.4 Smoke', target: 'test-r-smoke', rVersion: '3.4.1',
      timeoutValue: 8, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'PhantomJS Smoke', target: 'test-phantom-js-smoke',
      timeoutValue: 20, lang: pipelineContext.getBuildConfig().LANG_JS
    ],
    [
      stageName: 'Java8 Smoke', target: 'test-junit-smoke',
      timeoutValue: 20, lang: pipelineContext.getBuildConfig().LANG_JAVA
    ]
  ]

  // Stages executed after each push to PR branch.
  def PR_STAGES = [
    [
      stageName: 'Py2.7 Booklets', target: 'test-py-booklets', pythonVersion: '2.7',
      timeoutValue: 40, lang: pipelineContext.getBuildConfig().LANG_PY
    ],
    [
      stageName: 'Py2.7 Demos', target: 'test-py-demos', pythonVersion: '2.7',
      timeoutValue: 30, lang: pipelineContext.getBuildConfig().LANG_PY
    ],
    [
      stageName: 'Py2.7 Init', target: 'test-py-init', pythonVersion: '2.7',
      timeoutValue: 5, hasJUnit: false, lang: pipelineContext.getBuildConfig().LANG_PY
    ],
    [
      stageName: 'Py2.7 Small', target: 'test-pyunit-small', pythonVersion: '2.7',
      timeoutValue: 90, lang: pipelineContext.getBuildConfig().LANG_PY
    ],
    [
      stageName: 'Py3.5 Small', target: 'test-pyunit-small', pythonVersion: '3.5',
      timeoutValue: 90, lang: pipelineContext.getBuildConfig().LANG_PY
    ],
    [
      stageName: 'Py3.6 Small', target: 'test-pyunit-small', pythonVersion: '3.6',
      timeoutValue: 90, lang: pipelineContext.getBuildConfig().LANG_PY
    ],
    [
      stageName: 'R3.4 Init', target: 'test-r-init', rVersion: '3.4.1',
      timeoutValue: 5, hasJUnit: false, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'R3.4 Small', target: 'test-r-small', rVersion: '3.4.1',
      timeoutValue: 110, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'R3.4 Small Client Mode', target: 'test-r-small-client-mode', rVersion: '3.4.1',
      timeoutValue: 140, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'R3.4 CMD Check', target: 'test-r-cmd-check', rVersion: '3.4.1',
      timeoutValue: 15, hasJUnit: false, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'R3.4 CMD Check as CRAN', target: 'test-r-cmd-check-as-cran', rVersion: '3.4.1',
      timeoutValue: 10, hasJUnit: false, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'R3.4 Booklets', target: 'test-r-booklets', rVersion: '3.4.1',
      timeoutValue: 50, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'R3.4 Demos Small', target: 'test-r-demos-small', rVersion: '3.4.1',
      timeoutValue: 15, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'PhantomJS', target: 'test-phantom-js',
      timeoutValue: 75, lang: pipelineContext.getBuildConfig().LANG_JS
    ],
    [
      stageName: 'Py3.6 Medium-large', target: 'test-pyunit-medium-large', pythonVersion: '3.5',
      timeoutValue: 120, lang: pipelineContext.getBuildConfig().LANG_PY
    ],
    [
      stageName: 'R3.4 Medium-large', target: 'test-r-medium-large', rVersion: '3.4.1',
      timeoutValue: 70, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'R3.4 Demos Medium-large', target: 'test-r-demos-medium-large', rVersion: '3.4.1',
      timeoutValue: 120, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'INFO Check', target: 'test-info',
      timeoutValue: 10, lang: pipelineContext.getBuildConfig().LANG_NONE, additionalTestPackages: [pipelineContext.getBuildConfig().LANG_R]
    ],
    [
      stageName: 'Py3.6 Test Demos', target: 'test-demos', pythonVersion: '3.6',
      timeoutValue: 10, lang: pipelineContext.getBuildConfig().LANG_PY
    ],
    [
      stageName: 'Java 8 JUnit', target: 'test-junit-jenkins', pythonVersion: '2.7',
      timeoutValue: 90, lang: pipelineContext.getBuildConfig().LANG_JAVA, additionalTestPackages: [pipelineContext.getBuildConfig().LANG_PY]
    ]
  ]

  def BENCHMARK_STAGES = [
    [
      stageName: 'GBM Benchmark', executionScript: 'h2o-3/scripts/jenkins/groovy/benchmarkStage.groovy',
      timeoutValue: 120, target: 'benchmark', lang: buildConfig.LANG_NONE,
      additionalTestPackages: [buildConfig.LANG_R], image: buildConfig.BENCHMARK_IMAGE,
      nodeLabel: pipelineContext.getBuildConfig().getBenchmarkNodeLabel(), model: 'gbm', makefilePath: buildConfig.BENCHMARK_MAKEFILE_PATH
    ]
  ]

  // Stages executed in addition to PR_STAGES after merge to master.
  def MASTER_STAGES = [
    [
      stageName: 'Py2.7 Medium-large', target: 'test-pyunit-medium-large', pythonVersion: '2.7',
      timeoutValue: 120, lang: pipelineContext.getBuildConfig().LANG_PY
    ],
    [
      stageName: 'Py3.5 Medium-large', target: 'test-pyunit-medium-large', pythonVersion: '3.5',
      timeoutValue: 120, lang: pipelineContext.getBuildConfig().LANG_PY
    ],
    [
      stageName: 'R3.4 Datatable', target: 'test-r-datatable', rVersion: '3.4.1',
      timeoutValue: 40, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'PhantomJS Small', target: 'test-phantom-js-small',
      timeoutValue: 75, lang: pipelineContext.getBuildConfig().LANG_JS
    ],
    [
      stageName: 'PhantomJS Medium', target: 'test-phantom-js-medium',
      timeoutValue: 75, lang: pipelineContext.getBuildConfig().LANG_JS
    ]
  ]
  MASTER_STAGES += BENCHMARK_STAGES

  // Stages executed in addition to MASTER_STAGES, used for nightly builds.
  def NIGHTLY_STAGES = [
    [
      stageName: 'R3.3 Medium-large', target: 'test-r-medium-large', rVersion: '3.3.3',
      timeoutValue: 70, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'R3.3 Small', target: 'test-r-small', rVersion: '3.3.3',
      timeoutValue: 110, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'R3.3 Small Client Mode', target: 'test-r-small-client-mode', rVersion: '3.3.3',
      timeoutValue: 140, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'R3.3 CMD Check', target: 'test-r-cmd-check', rVersion: '3.3.3',
      timeoutValue: 15, hasJUnit: false, lang: pipelineContext.getBuildConfig().LANG_R
    ],
    [
      stageName: 'R3.3 CMD Check as CRAN', target: 'test-r-cmd-check-as-cran', rVersion: '3.3.3',
      timeoutValue: 10, hasJUnit: false, lang: pipelineContext.getBuildConfig().LANG_R
    ]
  ]

  def modeCode = MODES.find{it['name'] == pipelineContext.getBuildConfig().getMode()}['code']
  if (modeCode == MODE_BENCHMARK_CODE) {
    executeInParallel(BENCHMARK_STAGES, pipelineContext)
  } else {
    executeInParallel(SMOKE_STAGES, pipelineContext)
    def jobs = PR_STAGES
    if (modeCode >= MODE_MASTER_CODE) {
      jobs += MASTER_STAGES
    }
    if (modeCode >= MODE_NIGHTLY_CODE) {
      jobs += NIGHTLY_STAGES
    }
    executeInParallel(jobs, pipelineContext)
  }
}

def executeInParallel(final jobs, final pipelineContext) {
  parallel(jobs.collectEntries { c ->
    [
      c['stageName'], {
        invokeStage(pipelineContext) {
          stageName = c['stageName']
          target = c['target']
          pythonVersion = c['pythonVersion']
          rVersion = c['rVersion']
          timeoutValue = c['timeoutValue']
          hasJUnit = c['hasJUnit']
          lang = c['lang']
          additionalTestPackages = c['additionalTestPackages']
          nodeLabel = c['nodeLabel']
          executionScript = c['executionScript']
          image = c['image']
          model = c['model']
          makefilePath = c['makefilePath']
        }
      }
    ]
  })
}

def invokeStage(final pipelineContext, final body) {

  def DEFAULT_PYTHON = '3.5'
  def DEFAULT_R = '3.4.1'
  def DEFAULT_TIMEOUT = 60
  def DEFAULT_EXECUTION_SCRIPT = 'h2o-3/scripts/jenkins/groovy/defaultStage.groovy'

  def config = [:]

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  config.pythonVersion = config.pythonVersion ?: DEFAULT_PYTHON
  config.rVersion = config.rVersion ?: DEFAULT_R
  config.timeoutValue = config.timeoutValue ?: DEFAULT_TIMEOUT
  config.hasJUnit = config.hasJUnit ?: true
  config.additionalTestPackages = config.additionalTestPackages ?: []
  config.nodeLabel = config.nodeLabel ?: pipelineContext.getBuildConfig().getDefaultNodeLabel()
  config.executionScript = config.executionScript ?: DEFAULT_EXECUTION_SCRIPT
  config.image = config.image ?: pipelineContext.getBuildConfig().DEFAULT_IMAGE
  config.makefilePath = config.makefilePath ?: pipelineContext.getBuildConfig().MAKEFILE_PATH

  pipelineContext.getBuildSummary().addStageSummary(this, config.stageName)
  withCustomCommitStates(scm, 'h2o-ops-personal-auth-token', "${pipelineContext.getBuildConfig().getGitHubCommitStateContext(config.stageName)}") {
    try {
      node(config.nodeLabel) {
        pipelineContext.getBuildSummary().setStageDetails(this, config.stageName, env.NODE_NAME, env.WORKSPACE)
        echo "###### Unstash scripts. ######"
        unstash name: pipelineContext.getBuildConfig().PIPELINE_SCRIPTS_STASH_NAME

        if (config.stageDir == null) {
          def stageNameToDirName = load('h2o-3/scripts/jenkins/groovy/stageNameToDirName.groovy')
          config.stageDir = stageNameToDirName(config.stageName)
        }
        sh "rm -rf ${config.stageDir}"

        def script = load(config.executionScript)
        script(pipelineContext, config)
      }
      pipelineContext.getBuildSummary().markStageSuccessful(this, config.stageName)
    } catch (Exception e) {
      pipelineContext.getBuildSummary().markStageFailed(this, config.stageName)
      throw e
    }
  }
}

return this
