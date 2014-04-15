package ch.eonum.cryptotrader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.eonum.pipeline.classification.Classifier;
import ch.eonum.pipeline.core.SequenceDataSet;
import ch.eonum.pipeline.core.SparseSequence;
import ch.eonum.pipeline.transformation.MinMaxNormalizerSequence;
import ch.eonum.pipeline.util.Log;

/**
 * Get market data, apply it to a trained model and predict prices.
 * @author tim
 *
 */
public class PricePredictor {

	/** Feature transformation and normalization */
	private MinMaxNormalizerSequence<SparseSequence> normalizer;
	/** pre trained sequence regressor. */
	private Classifier<SparseSequence> predictor;
	/** market data. */
	private SparseSequence marketData;

	/**
	 * Constructor. 
	 * 
	 * @param predictor
	 * 			pre trained sequence regressor.
	 * @param normalizer
	 */
	public PricePredictor(Classifier<SparseSequence> predictor,
			MinMaxNormalizerSequence<SparseSequence> normalizer) {
		this.normalizer = normalizer;
		this.predictor = predictor;
		SequenceDataSet<SparseSequence> dataSet = new SequenceDataSet<SparseSequence>();
		this.marketData = new SparseSequence("", "", new HashMap<String, Double>());
		this.marketData.initGroundTruthSequence();
		this.marketData.initSequenceResults();
		dataSet.add(marketData);
		this.predictor.setTestSet(dataSet);
	}

	/**
	 * Get the next prediction.
	 * @param marketData
	 * @return
	 */
	public double nextPrediction(Map<String, Double> marketDataNextPoint) {
		this.normalizer.normSingleTimePoint(marketDataNextPoint);
		this.marketData.addTimePoint(marketDataNextPoint);		
		List<Double> gt = new ArrayList<Double>();
		gt.add(0.);
		this.marketData.addGroundTruth(gt);
		if(this.marketData.getSequenceLength() > 24)
			this.marketData.removeSequenceElementAt(0);
		
		Log.puts(this.marketData);
		this.predictor.test();
		return this.marketData.resultAt(this.marketData.getSequenceLength() - 1, 0);
	}

}
