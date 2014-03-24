package ch.eonum.cryptotrader;

import java.util.Map;

/**
 * Interface for a market. Provides methods to buy, sell and cancel offers, get
 * market data and portfolio balances.
 * 
 * @author tim
 * 
 */
public interface Market {

	/** has the market more market data. */
	public boolean hasNext();

	/**
	 * Get next market data time point. Depending on the market's implementation
	 * this might take some time. (e.g. wait ten minutes)
	 * 
	 * @return
	 */
	public Map<String, Double> next();

	/**
	 * Get all balances in this account by currency.
	 * @return
	 */
	public Map<String, Double> getBalances();

	/**
	 * Get all prices in BitCoin by currency.
	 * @return
	 */
	public Map<String, Double> getPrices();

	/**
	 * Get portfolio value of this account. 
	 * @return
	 */
	public double getPortfolioValue();

}
