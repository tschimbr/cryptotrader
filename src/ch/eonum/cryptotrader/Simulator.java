package ch.eonum.cryptotrader;

import java.util.HashMap;
import java.util.Map;

import ch.eonum.pipeline.core.DataSet;
import ch.eonum.pipeline.core.SequenceDataSet;
import ch.eonum.pipeline.core.SparseSequence;
import ch.eonum.pipeline.evaluation.Evaluator;

/**
 * Market place simulator using test data.
 * @author tim
 *
 */
public class Simulator implements Evaluator<SparseSequence>, Market  {
	
	/** market data reader with simulation data. */
	private CryptsyMarketDataReader reader;
	/** simulation data. */
	private SparseSequence marketData;
	/** current time in the sequence. */
	private int currentIndex;
	private Map<String, Double> balances;
	private Map<String, Double> prices;
	private String currencyName;

	public Simulator(CryptsyMarketDataReader dataReader, double initialBalanceX, double initialBalanceBTC) {
		this.reader = dataReader;
		SequenceDataSet<SparseSequence> data = dataReader.testSystem();
		this.marketData = data.get(0);
		this.currentIndex = 0;
		this.balances = new HashMap<String, Double>();
		this.currencyName = marketData.id.replace("/BTC", "");
		this.balances.put(currencyName , initialBalanceX);
		this.balances.put("BTC", initialBalanceBTC);
		this.prices = new HashMap<String, Double>();
		this.prices.put(currencyName, 0.0);
		this.prices.put("BTC", 1.0);
	}
	
	@Override
	public boolean hasNext() {
		return this.currentIndex < marketData.getSequenceLength();
	} 
	
	@Override
	public Map<String, Double> next() {
		return new HashMap<String, Double>(marketData.getTimePoint(currentIndex++));
		// #TODO update price
	}
	
	@Override
	public Map<String, Double> getBalances() {
		return this.balances;
	}

	@Override
	public Map<String, Double> getPrices() {
		return this.prices;
	}

	@Override
	public double getPortfolioValue() {
		double pfValue = 0;
		for(String currency : this.balances.keySet())
			pfValue += this.balances.get(currency) * this.prices.get(currency);
		return pfValue;
	}

	@Override
	public double evaluate(DataSet<SparseSequence> dataset) {
		// TODO Auto-generated method stub
		return 0;
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
