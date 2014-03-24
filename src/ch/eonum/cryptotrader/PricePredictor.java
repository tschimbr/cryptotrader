package ch.eonum.cryptotrader;

import ch.eonum.pipeline.classification.Classifier;
import ch.eonum.pipeline.core.SparseSequence;
import ch.eonum.pipeline.transformation.Transformer;

/**
 * Get market data, apply it to a trained model and predict prices.
 * @author tim
 *
 */
public class PricePredictor {

	/** Feature transformation and normalization */
	private Transformer<SparseSequence> normalizer;
	/** pre trained sequence regressor. */
	private Classifier<SparseSequence> predictor;

	/**
	 * Constructor. 
	 * 
	 * @param predictor
	 * 			pre trained sequence regressor.
	 * @param normalizer
	 */
	public PricePredictor(Classifier<SparseSequence> predictor,
			Transformer<SparseSequence> normalizer) {
		this.normalizer = normalizer;
		this.predictor = predictor;
	}

}
