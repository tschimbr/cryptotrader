package ch.eonum.cryptotrader.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ch.eonum.cryptotrader.CryptsyMarket;
import ch.eonum.cryptotrader.CryptsyMarketDataReader;

public class CryptsyAPITest {
	
	private CryptsyMarket api;

	@Before
	public void setUp() throws IOException{
		this.api = new CryptsyMarket(3, new CryptsyMarketDataReader(null));
	}
	
	@Test
	public void testCurrencyName(){
		assertEquals("LTC", this.api.getCurrencyName());
	}
	
	@Test
	public void testGetMarketData(){
		assertTrue(this.api.hasNext());
		Map<String, Double> map = this.api.next();
		assertTrue(map.containsKey("change_time_lag"));
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

}
