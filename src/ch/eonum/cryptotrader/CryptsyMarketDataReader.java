package ch.eonum.cryptotrader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.eonum.pipeline.core.DataPipeline;
import ch.eonum.pipeline.core.Parameters;
import ch.eonum.pipeline.core.SequenceDataSet;
import ch.eonum.pipeline.core.SparseSequence;
import ch.eonum.pipeline.util.Log;
import ch.eonum.pipeline.util.json.JSON;

/**
 * Read cryptsy market data as provided by their API:
 * http://pubapi.cryptsy.com/api.php?method=singlemarketdata&marketid={MARKET
 * ID} or http://pubapi.cryptsy.com/api.php?method=marketdatav2
 * 
 * API specification: https://www.cryptsy.com/pages/api
 * 
 * @author tim
 * 
 */
public class CryptsyMarketDataReader extends Parameters implements DataPipeline<SparseSequence> {
	
	private static List<String> derivatedFeatures = new ArrayList<String>();
	private static final Map<String, String> PARAMETERS = new HashMap<String, String>();
	
	static {
		PARAMETERS.put("floatingAverageFactor", "factor by which the floating average is being adapted 0 < x < 1 (default 0.3)");
		PARAMETERS.put("timeLag", "time lag for the ground truth in 10min units, which is one element in the sequence (default 12.0)");
		PARAMETERS.put("changeNormFactor", "norm the ground truth by this factor (default 40.0)");
		derivatedFeatures.add("price");
		derivatedFeatures.add("deltaMinMaxPrice");
		derivatedFeatures.add("volume");
		derivatedFeatures.add("meanQuantity");
		derivatedFeatures.add("stdPrice");
		derivatedFeatures.add("spread");
	}

	private String inputFolder;
	private Map<String, Double> prevPoint;
	private Map<String, Double> floatingAverage;
	private List<Map<String, Double>> previousPoints;

	public CryptsyMarketDataReader(String inputFolder) {
		this.inputFolder = inputFolder;
		this.setSupportedParameters(CryptsyMarketDataReader.PARAMETERS);
		this.putParameter("floatingAverageFactor", 0.3);
		this.putParameter("timeLag", 12.0);
		this.putParameter("changeNormFactor", 40.0);
	}

	/**
	 * Read the market data sequence from folder.
	 * @param folder
	 * @param timeLag 
	 * @return
	 * @throws IOException 
	 */
	public SparseSequence readSequence(String folder) throws IOException {
		double floatingAverageFactor = this.getDoubleParameter("floatingAverageFactor");
		double changeNormFactor = this.getDoubleParameter("changeNormFactor");
		int timeLag = (int) this.getDoubleParameter("timeLag");
		
		SparseSequence seq = new SparseSequence("", "", new HashMap<String, Double>());
		seq.initGroundTruthSequence();
		File directory = new File(folder);
		File[] files = directory .listFiles();

		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return Integer.valueOf(f1.getName().compareTo(f2.getName()));
			}
		});
		
		prevPoint = null;
		floatingAverage = singleMarketFeatureExtractionFromFile(files[0]);
		previousPoints = new ArrayList<Map<String, Double>>();

		for (int n = 0; n < files.length; n++) {
			File file = files[n];
			addPointToSequenceBySingleMarketFile(floatingAverageFactor, changeNormFactor,
					timeLag, seq, n, file);
		}

		return seq;
	}

	/**
	 * Add a single Point to the sequence. This method can also be used for real
	 * time updating of a sequence.
	 * 
	 * @param floatingAverageFactor
	 * @param changeNormFactor
	 * @param timeLag
	 * @param seq
	 * @param n
	 * @param file
	 * @throws IOException
	 */
	public void addPointToSequenceBySingleMarketFile(double floatingAverageFactor,
			double changeNormFactor, int timeLag, SparseSequence seq, int n,
			File file) throws IOException {
		Map<String, Double> point = singleMarketFeatureExtractionFromFile(file);
		
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
			change *= changeNormFactor;
			change += 0.5;
			seq.addGroundTruth(n - timeLag - 1, 0, change);
			derivatives.put("change_time_lag", change);
		}
		
		previousPoints.add(point);
	}

	/**
	 * Single market files feature extraction
	 * @param file
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Double> singleMarketFeatureExtractionFromFile(File file) throws IOException {
		
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
		/** get the first market. */
		Map<String, Object> market = (Map<String, Object>) markets.get(markets.keySet().iterator().next());
		
		return singleMarketFeatureExtractionFromJSON(market);
	}

	/**
	 * Single market feature extraction.
	 * @param markets
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Double> singleMarketFeatureExtractionFromJSON(
			Map<String, Object> market) {
		Map<String, Double> point = new HashMap<String, Double>();
//		point.put("price", Double.parseDouble((String) market.get("lasttradeprice")));
		point.put("volume", Double.parseDouble((String) market.get("volume")));
		point.put("daytime", Double.parseDouble(((String) market.get("lasttradetime")).split(" ")[1].substring(0,2)));
		
		List<Object> recentTrades = (List<Object>) market.get("recenttrades");
		List<Double> prices = new ArrayList<Double>();
		List<Double> quantities = new ArrayList<Double>();
		for(Object t : recentTrades){
			Map<String, String> trade = (Map<String, String>) t;
			Double quantity = Double.parseDouble(trade.get("quantity"));
			quantities.add(quantity );
			Double price = Double.parseDouble(trade.get("price"));
			prices.add(price );	
		}
		
	
		point.put("meanQuantity",  mean(quantities));
		double meanPrice = mean(prices);
		
		point.put("price", meanPrice);
		point.put("deltaMinMaxPrice", (max(prices) - min(prices))/meanPrice);
		point.put("stdPrice", std(prices, meanPrice));
		
		List<Object> sellorders = (List<Object>) market.get("sellorders");
		List<Object> buyorders = (List<Object>) market.get("buyorders");
		List<Double> sellPrices = new ArrayList<Double>();
		List<Double> buyPrices = new ArrayList<Double>();
		for(Object t : sellorders){
			Map<String, String> order = (Map<String, String>) t;
			sellPrices.add(Double.parseDouble(order.get("price")));		
		}
		for(Object t : buyorders){
			Map<String, String> order = (Map<String, String>) t;
			buyPrices.add(Double.parseDouble(order.get("price")));		
		}
		
		point.put("spread", mean(sellPrices) - mean(buyPrices));
		
		point.put("deltaSellOrders", (mean(sellPrices) - meanPrice));
		point.put("deltaBuyOrders", (mean(buyPrices) - meanPrice));
		return point;
	}

	private static Double min(List<Double> values) {
		double min = Double.POSITIVE_INFINITY;
		for(Double d : values)
			min  = Math.min(min, d);
		return  min;
	}
	
	private static Double max(List<Double> values) {
		double max = Double.NEGATIVE_INFINITY;
		for(Double d : values)
			max  = Math.max(max, d);
		return  max;
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
	public SequenceDataSet<SparseSequence> readDataSet(String dataset) throws IOException {
		SparseSequence s = readSequence(dataset);
		SequenceDataSet<SparseSequence> data = new SequenceDataSet<SparseSequence>();
		data.add(s);
		
		return data;
	}

	@Override
	public SequenceDataSet<SparseSequence> trainSystem(boolean isResultDataSetNeeded) {
		try {
			return this.readDataSet(inputFolder);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	@Override
	public SequenceDataSet<SparseSequence> testSystem() {
		try {
			return this.readDataSet(inputFolder);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	@Override
	public void addInputTraining(DataPipeline<SparseSequence> input) {
		Log.error("A DataSetReader can only be at the begining of a pipeline");
	}

	@Override
	public void addInputTest(DataPipeline<SparseSequence> input) {
		Log.error("A DataSetReader can only be at the begining of a pipeline");
	}
}
