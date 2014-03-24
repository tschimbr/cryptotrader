package ch.eonum.cryptotrader;

import java.util.HashMap;
import java.util.Map;

import ch.eonum.pipeline.core.DataSet;
import ch.eonum.pipeline.core.SequenceDataSet;
import ch.eonum.pipeline.core.SparseSequence;
import ch.eonum.pipeline.evaluation.Evaluator;
import ch.eonum.pipeline.util.Log;

/**
 * Market place simulator using test data.
 * @author tim
 *
 */
public class Simulator implements Evaluator<SparseSequence>, Market  {
	
	/** market place fees in %. */
	public static final double MARKET_FEE = 0.002;
	/** simulation data. */
	private SparseSequence marketData;
	/** current time in the sequence. */
	private int currentIndex;
	/** name of the traded currency. */
	private String currencyName;
	private double initialPortfolioValue;
	/** bitcoin balance. */
	private double btcBalance;
	/** balance of traded currency X. */
	private Double xBalance;
	/** price of traded currency X in bitcoins. */
	private Double price;

	public Simulator(CryptsyMarketDataReader dataReader, double initialBalanceX, double initialBalanceBTC) {
		dataReader.doStorePriceData();
		SequenceDataSet<SparseSequence> data = dataReader.testSystem();
		this.marketData = data.get(0);
		this.currentIndex = 0;
		this.btcBalance = initialBalanceBTC;
		this.currencyName = marketData.id.replace("/BTC", "");
		this.xBalance = initialBalanceX;
		this.price = marketData.getTimePoint(0).get("marketPrice");
		this.initialPortfolioValue = this.getPortfolioValue();
	}
	
	@Override
	public boolean hasNext() {
		return this.currentIndex < marketData.getSequenceLength();
	} 
	
	@Override
	public Map<String, Double> next() {
		Map<String, Double> map = marketData.getTimePoint(currentIndex++);	
		price = map.get("marketPrice");
		return new HashMap<String, Double>(map);
	}
	
	@Override
	public void placeBuyOrder(double amount, Double buyPrice) {
		if(buyPrice < price){
			Log.warn("buy order could not be processed at " + buyPrice
					+ " (current market price is " + price);
			return;
		}
		if(amount * buyPrice > btcBalance){
			Log.warn("sell order could not be processed. Not enough Bitcoins (" + btcBalance + ")");
			return;
		}
		this.btcBalance -= amount * buyPrice;
		this.xBalance += (1-Simulator.MARKET_FEE) * amount;
	}
	
	@Override
	public void placeSellOrder(double amount, Double sellPrice) {
		if(sellPrice > price){
			Log.warn("sell order could not be processed at " + sellPrice
					+ " (current market price is " + price);
			return;
		}
		if(amount > xBalance){
			Log.warn("sell order could not be processed. Not enough " + currencyName
					+ " (" + xBalance + ")");
			return;
		}
		this.xBalance -= amount;
		this.btcBalance += (1-Simulator.MARKET_FEE) * amount  * sellPrice;
	}
	
	@Override
	public double getBtcBalance() {
		return this.btcBalance;
	}
	
	@Override
	public double getBalance() {
		return this.xBalance;
	}

	@Override
	public double getPrice() {
		return this.price;
	}
	
	@Override
	public String getCurrencyName() {
		return this.currencyName;
	}

	@Override
	public double getPortfolioValue() {
		return this.btcBalance + this.xBalance * this.price;
	}

	@Override
	public double evaluate(DataSet<SparseSequence> dataset) {
		return this.getPortfolioValue() - this.initialPortfolioValue;
	}

	@Override
	public void printResults(String fileName) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void printResultsAndGnuplot(String fileName) {
		// TODO Auto-generated method stub	
	}
}
