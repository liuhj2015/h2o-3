package hex.tree.xgboost;

import hex.DataInfo;
import hex.ModelBuilder;
import hex.ModelCategory;
import hex.ScoreKeeper;
import hex.glm.GLMTask;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import water.H2O;
import water.Key;
import water.exceptions.H2OIllegalArgumentException;
import water.exceptions.H2OModelBuilderIllegalArgumentException;
import water.fvec.Frame;
import water.fvec.Vec;
import water.util.ArrayUtils;
import water.util.FrameUtils;
import water.util.Log;
import water.util.Timer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;

/** Gradient Boosted Trees
 *
 *  Based on "Elements of Statistical Learning, Second Edition, page 387"
 */
public class XGBoost extends ModelBuilder<XGBoostModel,XGBoostModel.XGBoostParameters,XGBoostOutput> {
  @Override public boolean haveMojo() { return true; }

  /**
   * convert an H2O Frame to a sparse DMatrix
   * @param f H2O Frame
   * @param response name of the response column
   * @param weight name of the weight column
   * @param fold name of the fold assignment column
   * @param featureMap featureMap[0] will be populated with the column names and types
   * @return DMatrix
   * @throws XGBoostError
   */
  public static DMatrix convertFrametoDMatrix(Frame f, String response, String weight, String fold, String[] featureMap) throws XGBoostError {
    // one-hot encoding
    FrameUtils.CategoricalOneHotEncoder enc = new FrameUtils.CategoricalOneHotEncoder(f, new String[]{response, weight, fold});
    Frame encoded = enc.exec().get();
    long denseLen = encoded.numRows()*(encoded.numCols() - 1 /*response*/);
    if (denseLen > (1<<28)) throw new IllegalArgumentException("Too many matrix elements.");
    if (encoded.numRows() > (1<<28)) throw new IllegalArgumentException("Too many rows.");

    if (featureMap!=null) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < encoded.numCols(); ++i) {
        sb.append(i).append(" ").append(encoded.name(i)).append(" ");
        if (encoded.vec(i).isBinary()) sb.append("i");
        else if (encoded.vec(i).isInt()) sb.append("int");
        else sb.append("q");
        sb.append("\n");
      }
      featureMap[0] = sb.toString();
    }


    // convert to CSC sparse matrix
    // example matrix:
    // 1 0 2 0
    // 4 0 0 3
    // 3 1 2 0
//    long[] colHeaders = new long[] {0,        3,  4,     6,    7}; //offsets
//    float[] data = new float[]     {1f,4f,3f, 1f, 2f,2f, 3f};      //non-zeros down each column
//    int[] rowIndex = new int[]     {0,1,2,    2,  0, 2,  1};       //row index for each non-zero
    long[] colHeaders = new long[encoded.numCols() - 1 /*response*/ + 1 /*final offset*/];
    float[] data   = new float[(int)denseLen];
    int[] rowIndex = new int[(int)denseLen];

    // extract predictors
    int nz=0;
    int col=0;
    Vec.Reader w = weight == null ? null : encoded.vec(weight).new Reader();
    int nRows = (int)encoded.numRows();
    for (int i=0;i<encoded.numCols();++i) {
      if (encoded.name(i).equals(response)) continue;
      colHeaders[col] = nz;
      Vec.Reader v = encoded.vec(i).new Reader();
      int k=0;
      for (int j=0; j<nRows; ++j) {
        double val = v.at(j);
        double wgt = w == null ? 1 : w.at(j);
        if (wgt!=0) {
          if (!Double.isNaN(val) && val!=0) {
            data[nz] = (float)val;
            rowIndex[nz] = k;
            nz++;
          }
          k++;
        }
      }
      col++;
    }
    // extract response vector
    Vec.Reader respVec = encoded.vec(response).new Reader();
    float[] resp = new float[nRows];
    float[] weights = new float[nRows];
    int j=0;
    for (int i=0;i<nRows;++i) {
      double wgt = w==null?1:w.at(i);
      if (wgt!=0) {
        resp[j] = (float) respVec.at(i);
        weights[j] = (float)wgt;
        j++;
      }
    }

    colHeaders[colHeaders.length-1] = nz;
    data = Arrays.copyOf(data, nz);
    rowIndex = Arrays.copyOf(rowIndex, nz);
    resp = Arrays.copyOf(resp, j);
    weights = Arrays.copyOf(weights, j);

    DMatrix trainMat = new DMatrix(colHeaders, rowIndex, data, DMatrix.SparseType.CSC, 0);
    trainMat.setLabel(resp);
    trainMat.setWeight(weights);
//    trainMat.setGroup(null); //fold //FIXME
    encoded.remove();
    return trainMat;
  }

  @Override public ModelCategory[] can_build() {
    return new ModelCategory[]{
      ModelCategory.Regression,
      ModelCategory.Binomial,
      ModelCategory.Multinomial,
    };
  }

  // Called from an http request
  public XGBoost(XGBoostModel.XGBoostParameters parms                   ) { super(parms     ); init(false); }
  public XGBoost(XGBoostModel.XGBoostParameters parms, Key<XGBoostModel> key) { super(parms, key); init(false); }
  public XGBoost(boolean startup_once) { super(new XGBoostModel.XGBoostParameters(),startup_once); }
  public boolean isSupervised(){return true;}

  @Override protected int nModelsInParallel() {
    return 1;
  }

  /** Start the XGBoost training Job on an F/J thread. */
  @Override protected XGBoostDriver trainModelImpl() {
    return new XGBoostDriver();
  }

  /** Initialize the ModelBuilder, validating all arguments and preparing the
   *  training frame.  This call is expected to be overridden in the subclasses
   *  and each subclass will start with "super.init();".  This call is made
   *  by the front-end whenever the GUI is clicked, and needs to be fast;
   *  heavy-weight prep needs to wait for the trainModel() call.
   *
   *  Validate the learning rate and distribution family. */
  @Override public void init(boolean expensive) {
    super.init(expensive);

    // Initialize response based on given distribution family.
    // Regression: initially predict the response mean
    // Binomial: just class 0 (class 1 in the exact inverse prediction)
    // Multinomial: Class distribution which is not a single value.

    // However there is this weird tension on the initial value for
    // classification: If you guess 0's (no class is favored over another),
    // then with your first GBM tree you'll typically move towards the correct
    // answer a little bit (assuming you have decent predictors) - and
    // immediately the Confusion Matrix shows good results which gradually
    // improve... BUT the Means Squared Error will suck for unbalanced sets,
    // even as the CM is good.  That's because we want the predictions for the
    // common class to be large and positive, and the rare class to be negative
    // and instead they start around 0.  Guessing initial zero's means the MSE
    // is so bad, that the R^2 metric is typically negative (usually it's
    // between 0 and 1).

    // If instead you guess the mean (reversed through the loss function), then
    // the zero-tree XGBoost model reports an MSE equal to the response variance -
    // and an initial R^2 of zero.  More trees gradually improves the R^2 as
    // expected.  However, all the minority classes have large guesses in the
    // wrong direction, and it takes a long time (lotsa trees) to correct that
    // - so your CM sucks for a long time.
    if (expensive) {
      if (error_count() > 0)
        throw H2OModelBuilderIllegalArgumentException.makeFromBuilder(XGBoost.this);
      if (hasOffsetCol()) {
        error("_offset_column", "Offset is not supported for XGBoost.");
      }
    }

    switch( _parms._distribution) {
    case bernoulli:
      if( _nclass != 2 /*&& !couldBeBool(_response)*/)
        error("_distribution", H2O.technote(2, "Binomial requires the response to be a 2-class categorical"));
      break;
    case modified_huber:
      if( _nclass != 2 /*&& !couldBeBool(_response)*/)
        error("_distribution", H2O.technote(2, "Modified Huber requires the response to be a 2-class categorical."));
      break;
    case multinomial:
      if (!isClassifier()) error("_distribution", H2O.technote(2, "Multinomial requires an categorical response."));
      break;
    case huber:
      if (isClassifier()) error("_distribution", H2O.technote(2, "Huber requires the response to be numeric."));
      break;
    case poisson:
      if (isClassifier()) error("_distribution", H2O.technote(2, "Poisson requires the response to be numeric."));
      break;
    case gamma:
      if (isClassifier()) error("_distribution", H2O.technote(2, "Gamma requires the response to be numeric."));
      break;
    case tweedie:
      if (isClassifier()) error("_distribution", H2O.technote(2, "Tweedie requires the response to be numeric."));
      break;
    case gaussian:
      if (isClassifier()) error("_distribution", H2O.technote(2, "Gaussian requires the response to be numeric."));
      break;
    case laplace:
      if (isClassifier()) error("_distribution", H2O.technote(2, "Laplace requires the response to be numeric."));
      break;
    case quantile:
      if (isClassifier()) error("_distribution", H2O.technote(2, "Quantile requires the response to be numeric."));
      break;
    case AUTO:
      break;
    default:
      error("_distribution","Invalid distribution: " + _parms._distribution);
    }

    if( !(0. < _parms._learn_rate && _parms._learn_rate <= 1.0) )
      error("_learn_rate", "learn_rate must be between 0 and 1");
    if( !(0. < _parms._col_sample_rate && _parms._col_sample_rate <= 1.0) )
      error("_col_sample_rate", "col_sample_rate must be between 0 and 1");
  }

  static DataInfo makeDataInfo(Frame train, Frame valid, XGBoostModel.XGBoostParameters parms, int nClasses) {
    DataInfo dinfo = new DataInfo(
            train,
            valid,
            1, //nResponses
            true, //all factor levels
            DataInfo.TransformType.NONE, //do not standardize
            DataInfo.TransformType.NONE, //do not standardize response
            parms._missing_values_handling == XGBoostModel.XGBoostParameters.MissingValuesHandling.Skip, //whether to skip missing
            false, // do not replace NAs in numeric cols with mean
            true,  // always add a bucket for missing values
            parms._weights_column != null, // observation weights
            parms._offset_column != null,
            parms._fold_column != null
    );
    // Checks and adjustments:
    // 1) observation weights (adjust mean/sigmas for predictors and response)
    // 2) NAs (check that there's enough rows left)
    GLMTask.YMUTask ymt = new GLMTask.YMUTask(dinfo, nClasses,nClasses == 1, parms._missing_values_handling == XGBoostModel.XGBoostParameters.MissingValuesHandling.Skip, true).doAll(dinfo._adaptedFrame);
    if (ymt.wsum() == 0 && parms._missing_values_handling == XGBoostModel.XGBoostParameters.MissingValuesHandling.Skip)
      throw new H2OIllegalArgumentException("No rows left in the dataset after filtering out rows with missing values. Ignore columns with many NAs or set missing_values_handling to 'MeanImputation'.");
    if (parms._weights_column != null && parms._offset_column != null) {
      Log.warn("Combination of offset and weights can lead to slight differences because Rollupstats aren't weighted - need to re-calculate weighted mean/sigma of the response including offset terms.");
    }
    if (parms._weights_column != null && parms._offset_column == null /*FIXME: offset not yet implemented*/) {
      dinfo.updateWeightedSigmaAndMean(ymt.predictorSDs(), ymt.predictorMeans());
      if (nClasses == 1)
        dinfo.updateWeightedSigmaAndMeanForResponse(ymt.responseSDs(), ymt.responseMeans());
    }
    return dinfo;
  }

  // ----------------------
  private class XGBoostDriver extends Driver {
    @Override
    public void computeImpl() {
      init(true); //this can change the seed if it was set to -1
      long cs = _parms.checksum();
      // Something goes wrong
      if (error_count() > 0)
        throw H2OModelBuilderIllegalArgumentException.makeFromBuilder(XGBoost.this);
      buildModel();
      //check that _parms isn't changed during DL model training
      long cs2 = _parms.checksum();
      assert(cs == cs2);
    }

    final void buildModel() {
      XGBoostModel model = new XGBoostModel(_result,_parms,new XGBoostOutput(XGBoost.this));
      model.write_lock(_job);
      String[] featureMap = new String[]{""};
      try {
        DMatrix trainMat = convertFrametoDMatrix(_parms._train.get(),
                _parms._response_column, _parms._weights_column, _parms._fold_column, featureMap);

        DMatrix validMat = _parms._valid != null ? convertFrametoDMatrix(_parms._valid.get(),
                _parms._response_column, _parms._weights_column, _parms._fold_column, featureMap) : null;

        OutputStream os;
        try {
          os = new FileOutputStream("featureMap.txt");
          os.write(featureMap[0].getBytes());
          os.close();
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }

        HashMap<String, DMatrix> watches = new HashMap<>();
        if (validMat!=null)
          watches.put("valid", validMat);
        else
          watches.put("train", trainMat);

        model.model_info()._booster = ml.dmlc.xgboost4j.java.XGBoost.train(trainMat, model.createParams(), 0, watches, null, null);
        scoreAndBuildTrees(model, trainMat, validMat);
        doScoring(model, model.model_info()._booster, trainMat, validMat, true);
        model.model_info().nativeToJava();
      } catch (XGBoostError xgBoostError) {
        xgBoostError.printStackTrace();
      }
      model._output._boosterBytes = model.model_info()._boosterBytes;
      model.unlock(_job);
    }

    protected final void scoreAndBuildTrees(XGBoostModel model, DMatrix trainMat, DMatrix validMat) throws XGBoostError {
      for( int tid=0; tid< _parms._ntrees; tid++) {
        // During first iteration model contains 0 trees, then 1-tree, ...
        boolean scored = doScoring(model, model.model_info()._booster, trainMat, validMat, false);
        if (scored && ScoreKeeper.stopEarly(model._output.scoreKeepers(), _parms._stopping_rounds, _nclass > 1, _parms._stopping_metric, _parms._stopping_tolerance, "model's last", true)) {
          doScoring(model, model.model_info()._booster, trainMat, validMat, true);
          _job.update(_parms._ntrees-model._output._ntrees); //finish
          return;
        }

        Timer kb_timer = new Timer();
        try {
          model.model_info()._booster.update(trainMat, tid);
        } catch (XGBoostError xgBoostError) {
          xgBoostError.printStackTrace();
        }
        Log.info((tid + 1) + ". tree was built in " + kb_timer.toString());
        _job.update(1);
        // Optional: for convenience
//          model.update(_job);
//          model.model_info().nativeToJava();
        model._output._ntrees++;
        model._output._scored_train = ArrayUtils.copyAndFillOf(model._output._scored_train, model._output._ntrees+1, new ScoreKeeper());
        model._output._scored_valid = model._output._scored_valid != null ? ArrayUtils.copyAndFillOf(model._output._scored_valid, model._output._ntrees+1, new ScoreKeeper()) : null;
        model._output._training_time_ms = ArrayUtils.copyAndFillOf(model._output._training_time_ms, model._output._ntrees+1, System.currentTimeMillis());
      }
      doScoring(model, model.model_info()._booster, trainMat, validMat, true);
    }

    long _firstScore = 0;
    long _timeLastScoreStart = 0;
    long _timeLastScoreEnd = 0;
    private boolean doScoring(XGBoostModel model, Booster booster, DMatrix trainMat, DMatrix validMat, boolean finalScoring) throws XGBoostError {
      boolean scored = false;
      long now = System.currentTimeMillis();
      if (_firstScore == 0) _firstScore = now;
      long sinceLastScore = now - _timeLastScoreStart;
      _job.update(0, "Built " + model._output._ntrees + " trees so far (out of " + _parms._ntrees + ").");

      boolean timeToScore = (now - _firstScore < _parms._initial_score_interval) || // Score every time for 4 secs
          // Throttle scoring to keep the cost sane; limit to a 10% duty cycle & every 4 secs
          (sinceLastScore > _parms._score_interval && // Limit scoring updates to every 4sec
              (double) (_timeLastScoreEnd - _timeLastScoreStart) / sinceLastScore < 0.1); //10% duty cycle

      boolean manualInterval = _parms._score_tree_interval > 0 && model._output._ntrees % _parms._score_tree_interval == 0;

      // Now model already contains tid-trees in serialized form
      if (_parms._score_each_iteration || finalScoring || // always score under these circumstances
          (timeToScore && _parms._score_tree_interval == 0) || // use time-based duty-cycle heuristic only if the user didn't specify _score_tree_interval
          manualInterval) {
        _timeLastScoreStart = now;
        model.doScoring(booster, trainMat, validMat);
        _timeLastScoreEnd = System.currentTimeMillis();
        model.computeVarImp(booster.getFeatureScore("featureMap.txt"));
        model.update(_job);
        Log.info(model);
        scored = true;
      }
      return scored;
    }
  }



}