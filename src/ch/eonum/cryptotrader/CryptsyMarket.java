package ch.eonum.cryptotrader;

import java.util.Map;

/**
 * Cryptsy market place. https://www.cryptsy.com/ implementing the Cryptsy API:
 * https://www.cryptsy.com/pages/api
 * 
 * @author tim
 * 
 */
public class CryptsyMarket implements Market {

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<String, Double> next() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getPortfolioValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getBtcBalance() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getBalance() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getPrice() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCurrencyName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void placeBuyOrder(double amount, Double price) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void placeSellOrder(double amount, Double price) {
		// TODO Auto-generated method stub
		
	}

}
