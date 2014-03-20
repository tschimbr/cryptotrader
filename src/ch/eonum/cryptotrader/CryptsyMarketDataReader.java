package ch.eonum.cryptotrader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	public static final double floatingAverageFactor = 0.3;
	public static List<String> derivatedFeatures = new ArrayList<String>();
	
	static {
		derivatedFeatures.add("price");
		derivatedFeatures.add("volume");
		derivatedFeatures.add("meanQuantity");
		derivatedFeatures.add("stdPrice");
	}

	/**
	 * Read the market data sequence from folder.
	 * @param folder
	 * @param timeLag 
	 * @return
	 * @throws IOException 
	 */
	public static SparseSequence readSequence(String folder, int timeLag) throws IOException {
		SparseSequence seq = new SparseSequence("", "", new HashMap<String, Double>());
		seq.initGroundTruthSequence();
		File directory = new File(folder);
		File[] files = directory .listFiles();

		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return Integer.valueOf(f1.getName().compareTo(f2.getName()));
			}
		});
		
		Map<String, Double> prevPoint = null;
		Map<String, Double> floatingAverage = extractFeatures(files[0]);
		List<Map<String, Double>> previousPoints = new ArrayList<Map<String, Double>>();

		for (int n = 0; n < files.length; n++) {
			File file = files[n];
			Map<String, Double> point = extractFeatures(file);
			
			if (point != null){
				prevPoint = new HashMap<String, Double>(point);		
			} else {
				point = prevPoint;
				prevPoint = new HashMap<String, Double>(prevPoint);
			}
			
			
			Map<String, Double> derivatives = new HashMap<String, Double>();
			for(String f : floatingAverage.keySet()){
				if(derivatedFeatures.contains(f)){
					derivatives.put(f, (point.get(f) - floatingAverage.get(f))
							/ floatingAverage.get(f));
					floatingAverage.put(f, (1 - floatingAverageFactor)
							* floatingAverage.get(f) + floatingAverageFactor
							* point.get(f));
				} else {
					derivatives.put(f, point.get(f));
				}
			}
			seq.addTimePoint(derivatives);
			
			List<Double> gt = new ArrayList<Double>();
			gt.add(Double.NaN);
			seq.addGroundTruth(gt);
			
			if(n > timeLag){
				double oldPrice = previousPoints.get(previousPoints.size() - timeLag - 1).get("price");
				double change = (point.get("price") - oldPrice) / oldPrice;
				change *= 40.0;
				change += 0.5;	
//				change = Math.max(0, change);
//				change = Math.min(1, change);
				seq.addGroundTruth(n - timeLag - 1, 0, change);
				derivatives.put("change_time_lag", change);
			}
			
			previousPoints.add(point);
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
		if (success != 1) {
			Log.warn("Unsuccessfull: " + file.getName());
			return null;
		}
		Map<String, Object> res = (Map<String, Object>) json.get("return");
		if (res == null) {
			Log.warn("no result entry: " + file.getName());
			return null;
		}
		Map<String, Object> markets = (Map<String, Object>) res.get("markets");
		if (markets == null) {
			Log.warn("no market entry: " + file.getName());
			return null;
		}
		Map<String, Object> market = (Map<String, Object>) markets.get(markets.keySet().iterator().next());
//		point.put("price", Double.parseDouble((String) market.get("lasttradeprice")));
		point.put("volume", Double.parseDouble((String) market.get("volume")));
		point.put("daytime", Double.parseDouble(((String) market.get("lasttradetime")).split(" ")[1].substring(0,2)));
		
		List<Object> recentTrades = (List<Object>) market.get("recenttrades");
		int numTrades = recentTrades.size(); /** currently this is always 50. */

		
		List<Double> prices = new ArrayList<Double>();
		List<Double> quantities = new ArrayList<Double>();
		for(Object t : recentTrades){
			Map<String, String> trade = (Map<String, String>) t;
			quantities.add(Double.parseDouble(trade.get("quantity")));
			prices.add(Double.parseDouble(trade.get("price")));		
		}

		point.put("meanQuantity",  mean(quantities));
		double meanPrice = mean(prices);
		point.put("price", meanPrice);
		point.put("stdPrice", std(prices, meanPrice));
		
		return point;
	}

	private static Double std(List<Double> values, double mean) {
		double sum = 0;
		for(Double d : values)
			sum  += Math.pow(Math.abs(d - mean), 2);
		return  Math.sqrt(sum / (values.size() - 1));
	}

	private static double mean(List<Double> values) {
		double sum = 0;
		for(Double d : values)
			sum  += d;
		return sum/values.size();
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
		SparseSequence s = CryptsyMarketDataReader.readSequence(dataset, timeLag);
		SequenceDataSet<SparseSequence> data = new SequenceDataSet<SparseSequence>();
		data.add(s);
		
		return data;
	}

	

}
