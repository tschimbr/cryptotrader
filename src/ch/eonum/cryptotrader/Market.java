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
	 * Get Bitcoin balance
	 * @return
	 */
	public double getBtcBalance();
	
	/**
	 * Get the balance of the traded currency. 
	 * @return
	 */
	public double getBalance();

	/**
	 * Get the price of currency X in BitCoin.
	 * @return
	 */
	public double getPrice();

	/**
	 * Get portfolio value of this account. 
	 * @return
	 */
	public double getPortfolioValue();

	/**
	 * Get the name of the traded currency.
	 * @return
	 */
	public String getCurrencyName();

	/**
	 * Place a buy order.
	 * @param amount
	 * @param price
	 */
	public void placeBuyOrder(double amount, Double price);
	
	/**
	 * Place a sell order.
	 * @param amount
	 * @param price
	 */
	public void placeSellOrder(double amount, Double price);

}
