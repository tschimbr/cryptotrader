package ch.eonum.cryptotrader.test;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import ch.eonum.cryptotrader.CryptsyMarket;

public class CryptsyAPITest {
	
	private CryptsyMarket api;

	@Before
	public void setUp() throws IOException{
		this.api = new CryptsyMarket(3);
	}
	
	@Test
	public void testCurrencyName(){
		assertEquals("LTC", this.api.getCurrencyName());
	}

}
