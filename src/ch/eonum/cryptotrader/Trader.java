package ch.eonum.cryptotrader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.eonum.pipeline.core.DataPipeline;
import ch.eonum.pipeline.core.DataSet;
import ch.eonum.pipeline.core.Parameters;
import ch.eonum.pipeline.core.SparseSequence;

/**
 * Trading bot. A Trader uses a PricePredictor to predict market prices and
 * places buy/sell orders according these predictions in a certain Market.
 * 
 * @author tim
 * 
 */
public class Trader extends Parameters implements DataPipeline<SparseSequence> {
	
	private static final Map<String, String> PARAMETERS = new HashMap<String, String>();
	
	static {
		PARAMETERS.put("startAfter", "Initial phase where no trades are being made and only data is collected (default 6)");
		PARAMETERS.put("upperThreshold", "Above this threshold we buy (default 0.65)");
		PARAMETERS.put("lowerThreshold", "Below this threshold we sell (default 0.35)");
		PARAMETERS.put("numConsecutive", "At least numConsecutive points have to be above/below threshold (default 3)");
	}

	/** market forecasting unit. */
	private PricePredictor predictor;
	/** market on which should be traded. */
	private Market market;
	/** Trading and market log. */
	private PrintWriter log;
	/**
	 * close flag. when true, all trading operations will be shut down after the
	 * next iteration.
	 */
	private boolean close;
	private List<Double> previousPredictions;
	private SimpleDateFormat dateFormatter;
	private PrintWriter balanceLog;

	public Trader(PricePredictor pricePredictor, Market market, String logFileName) throws IOException {
		this.market = market;
		this.predictor = pricePredictor;
		this.log = new PrintWriter(new File(logFileName));
		this.balanceLog = new PrintWriter(new File(logFileName + ".balances"));
		this.previousPredictions = new ArrayList<Double>();
		dateFormatter = new SimpleDateFormat("yyyy-MM-dd kk:mm");
		this.putParameter("startAfter", 6);
		this.putParameter("numConsecutive", 3.0);
		this.putParameter("upperThreshold", 0.65);
		this.putParameter("lowerThreshold", 0.35);
	}
	
	/**
	 * Close all trading operations.
	 */
	public void close(){
		this.close = true;
		this.log.close();
		this.balanceLog.close();
	}
	
	/**
	 * Start trading activities. Depending on the market this function can loop
	 * forever unless stopped using {@link #close()}
	 */
	public void startTrading() {
		Map<String, Double> balances = this.market.getBalances();
		Map<String, Double> prices = this.market.getPrices();
		for(String currency : balances.keySet()){
			this.balanceLog.print(currency + " balance;" + currency + " price;");
		}
		this.balanceLog.println("portfolio value;");
		
		while(market.hasNext() && !close){
			Map<String, Double> marketData = market.next();
			double prediction = predictor.nextPrediction(marketData);
			this.previousPredictions.add(prediction);
			log.println(dateFormatter.format(new Date()) + " prediction: " + prediction);
			int time = this.previousPredictions.size();
			
			balances = this.market.getBalances();
			prices = this.market.getPrices();
			double portFolioValue = this.market.getPortfolioValue();
			for(String currency : balances.keySet()){
				this.balanceLog.print(balances.get(currency) + ";");
				this.balanceLog.print(prices.get(currency) + ";");
			}
			this.balanceLog.println(portFolioValue + ";");
			
			if(time > this.getIntParameter("startAfter")){
				/** start trading. */
				
			}
		}
	}
	
	/** pipeline methods. */

	@Override
	public DataSet<SparseSequence> trainSystem(boolean isResultDataSetNeeded) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataSet<SparseSequence> testSystem() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addInputTraining(DataPipeline<SparseSequence> input) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addInputTest(DataPipeline<SparseSequence> input) {
		// TODO Auto-generated method stub
		
	}

}
