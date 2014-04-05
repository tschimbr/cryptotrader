package ch.eonum.cryptotrader.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.abwaters.cryptsy.Cryptsy.CryptsyException;

import ch.eonum.cryptotrader.CryptsyMarket;
import ch.eonum.cryptotrader.CryptsyMarketDataReader;

public class CryptsyAPITest {
	
	private CryptsyMarket api;

	@Before
	public void setUp() throws IOException, CryptsyException{
		this.api = new CryptsyMarket(3, new CryptsyMarketDataReader(null), "/home/tim/cryptotrader/data/private-api/");
	}
	
	@Test
	public void testCurrencyName(){
		assertEquals("LTC", this.api.getCurrencyName());
	}
	
	@Test
	public void testGetMarketData(){
		assertTrue(this.api.hasNext());
		Map<String, Double> map = this.api.next();
		assertTrue(map.containsKey("daytime"));
		assertTrue(map.containsKey("deltaBuyOrders"));
		assertTrue(map.containsKey("deltaMinMaxPrice"));
		assertTrue(map.containsKey("deltaSellOrders"));
		assertTrue(map.containsKey("meanQuantity"));
		assertTrue(map.containsKey("price"));
		assertTrue(map.containsKey("spread"));
		assertTrue(map.containsKey("stdPrice"));
		assertTrue(map.containsKey("volume"));
	}
	
	@Test
	public void getBTCBalance(){
		assertTrue(this.api.getBtcBalance() > 0);
	}
	
	@Test
	public void getBalance(){
		assertTrue(this.api.getBalance() > 0);
	}
	
	@Test
	public void getPortfolioValue(){
		assertEquals(this.api.getBalance() * this.api.getPrice() + this.api.getBtcBalance(), this.api.getPortfolioValue(), 0.0000001);
	}
	
	/**
	 * This test has monetary side effects!!!!!
	 * Use it only wisely and rarely.
	 */
//	@Test
//	public void testPlaceOrder(){
//		this.api.next();
//		double price = this.api.getPrice();
//		double amount = this.api.getBalance() * 0.01;
//		this.api.placeSellOrder(amount, price);
//	}

}
