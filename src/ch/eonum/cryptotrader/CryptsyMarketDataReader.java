package ch.eonum.cryptotrader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import ch.eonum.pipeline.core.Features;
import ch.eonum.pipeline.core.SequenceDataSet;
import ch.eonum.pipeline.core.SparseSequence;
import ch.eonum.pipeline.util.Log;
import ch.eonum.pipeline.util.json.JSON;

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
	 * @throws IOException 
	 */
	public static SparseSequence readSequence(String folder) throws IOException {
		SparseSequence seq = new SparseSequence("", "", new HashMap<String, Double>());
		File directory = new File(folder);
		File[] files = directory .listFiles();

		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return Integer.valueOf(f1.getName().compareTo(f2.getName()));
			}
		});
		
		Map<String, Double> prevPoint = null;
		for(File file : files){
			Map<String, Double> point = extractFeatures(file);
			if(point != null)
				seq.addTimePoint(point);
			else if(prevPoint != null)
				seq.addTimePoint(prevPoint);
			
			prevPoint = point;
		}

		return seq;
	}

	/**
	 * Read a single Point from file.
	 * @param file
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Double> extractFeatures(File file) throws IOException {
		Map<String, Double> point = new HashMap<String, Double>();
		Map<String, Object> json = JSON.readJSON(file);
		
		int success = (Integer) json.get("success");
		if(success != 1){
			Log.warn("Unsuccessfull: " + file.getName());
			return null;
		}
		
		Map<String, Object> res = (Map<String, Object>) json.get("return");
		if(res == null){
			Log.warn("no result entry: " + file.getName());
			return null;
		}
		Map<String, Object> markets = (Map<String, Object>) res.get("markets");
		if(markets == null){
			Log.warn("no market entry: " + file.getName());
			return null;
		}
		Map<String, Object> market = (Map<String, Object>) markets.get(markets.keySet().iterator().next());
		point .put("price", Double.parseDouble((String) market.get("lasttradeprice")));
		point .put("volume", Double.parseDouble((String) market.get("volume")));
		
		return point;
	}

	/**
	 * Read a dataset from folder. Create ground truth targets.
	 * @param dataset
	 * @param timeLag
	 * @return
	 * @throws IOException 
	 */
	public static SequenceDataSet<SparseSequence> readDataSet(String dataset,
			int timeLag) throws IOException {
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
