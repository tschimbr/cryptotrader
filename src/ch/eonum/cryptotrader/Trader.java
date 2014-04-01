package ch.eonum.cryptotrader;

import java.io.File;
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
		PARAMETERS.put("minimumTrade", "minimum size of trade in % of portfolio value. (0.01)");
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

	public Trader(PricePredictor pricePredictor, Market market, String logFileName) throws IOException {
		this.setSupportedParameters(Trader.PARAMETERS);
		this.market = market;
		this.predictor = pricePredictor;
		this.log = new PrintWriter(new File(logFileName));
		this.previousPredictions = new ArrayList<Double>();
		dateFormatter = new SimpleDateFormat("yyyy-MM-dd kk:mm");
		this.putParameter("startAfter", 6);
		this.putParameter("numConsecutive", 1.0);
		this.putParameter("upperThreshold", 0.55);
		this.putParameter("lowerThreshold", 0.45);
		this.putParameter("minimumTrade", 0.01);
	}
	
	/**
	 * Close all trading operations.
	 */
	public void close(){
		this.close = true;
		this.log.close();
	}
	
	/**
	 * Start trading activities. Depending on the market this function can loop
	 * forever unless stopped using {@link #close()}
	 */
	public void startTrading() {
		double btcBalance = this.market.getBtcBalance();
		double xBalance = this.market.getBalance();
		double price = this.market.getPrice();
		String currency = market.getCurrencyName();
		
		this.log.println("date;prediction;upperThreshold;lowerThreshold;BTCBalance;" + currency + "Balance;"
				+ currency + "Price;portfolioValue;");		
		
		double upperThreshold = getDoubleParameter("upperThreshold");
		double lowerThreshold = getDoubleParameter("lowerThreshold");
		
		while(market.hasNext() && !close){
			Map<String, Double> marketData = market.next();
			double prediction = predictor.nextPrediction(marketData);
			this.previousPredictions.add(prediction);
			log.print(dateFormatter.format(new Date()) + ";" + prediction + ";");
			log.print(upperThreshold + ";" + lowerThreshold + ";");
			int time = this.previousPredictions.size();
			
			btcBalance = this.market.getBtcBalance();
			xBalance = this.market.getBalance();
			price = this.market.getPrice();
			
			double portFolioValue = this.market.getPortfolioValue();
			this.log.print(btcBalance + ";" + xBalance + ";" + price
					+ ";" + portFolioValue + ";");
			
			double minimum = this.getDoubleParameter("minimumTrade") * portFolioValue;
			
			if(time > this.getIntParameter("startAfter")){
				/** start trading. */
				/** buy. */
				if(prediction > upperThreshold
						&& btcBalance > 0){
					boolean isStable = true;
					for(int i = time - 1; i >= time - this.getDoubleParameter("numConsecutive"); i--)
						isStable = isStable && previousPredictions.get(i) > getDoubleParameter("upperThreshold");
					if(isStable){
						double amount = (btcBalance * 0.8) / price;
						if(amount * price > minimum){
							this.market.placeBuyOrder(amount, price);
							this.log.print("Buy;" + amount + ";" + price);
						}
//						upperThreshold += 0.02;
//						lowerThreshold -= 0.02;
					}
				}
				/** sell. */
				else if(prediction < lowerThreshold
						&& xBalance > 0){
					boolean isStable = true;
					for(int i = time - 1; i >= time - this.getDoubleParameter("numConsecutive"); i--)
						isStable = isStable && previousPredictions.get(i) < getDoubleParameter("lowerThreshold");
					if(isStable){
						double amount = xBalance * 0.8;
						if(amount * price > minimum){
							this.market.placeSellOrder(amount, price);
							this.log.print("Sell;" + amount + ";" + price);
						}
//						lowerThreshold += 0.02;
//						upperThreshold -= 0.02;
					}
				} else {
//					lowerThreshold += 0.002;
//					upperThreshold -= 0.002;
				}
				
			}
			this.log.println();
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
