package ch.eonum.cryptotrader;

/**
 * Interface for a market. Provides methods to buy, sell and cancel offers, get
 * market data and portfolio balances.
 * 
 * @author tim
 * 
 */
public interface Market {

	/** has the market more market data. */
	boolean hasNext();

}
