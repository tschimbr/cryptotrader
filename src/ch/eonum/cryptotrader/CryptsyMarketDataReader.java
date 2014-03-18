package ch.eonum.cryptotrader;

import ch.eonum.pipeline.core.Features;
import ch.eonum.pipeline.core.SequenceDataSet;
import ch.eonum.pipeline.core.SparseSequence;

/**
 * Read cryptsy market data as provided by their API:
 * http://pubapi.cryptsy.com/api.php?method=singlemarketdata&marketid={MARKET
 * ID}
 * 
 * API specification: https://www.cryptsy.com/pages/api
 * 
 * @author tim
 * 
 */
public class CryptsyMarketDataReader {

	/**
	 * Read the market data sequence from folder.
	 * @param folder
	 * @return
	 */
	public static SparseSequence readSequence(String folder) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Read a dataset from folder. Create ground truth targets.
	 * @param dataset
	 * @param timeLag
	 * @return
	 */
	public static SequenceDataSet<SparseSequence> readDataSet(String dataset,
			int timeLag) {
		SparseSequence s = CryptsyMarketDataReader.readSequence(dataset);
		SequenceDataSet<SparseSequence> data = new SequenceDataSet<SparseSequence>();
		data.add(s);
		Features targetFeatures = new Features();
		targetFeatures.addFeature("price");
		targetFeatures.recalculateIndex();
		data.setTimeLag(timeLag, targetFeatures );
		
		return data;
	}

	

}
