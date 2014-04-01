package ch.eonum.cryptotrader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import ch.eonum.pipeline.util.Log;

/**
 * Cryptsy market place. https://www.cryptsy.com/ implementing the Cryptsy API:
 * https://www.cryptsy.com/pages/api
 * 
 * @author tim
 * 
 */
public class CryptsyMarket implements Market {

	/** market id. E.g. 132 for DOGE/BTC. */
	private int marketId;
	private String currencyName;

	public CryptsyMarket(int marketId) throws IOException {
		this.marketId = marketId;
		Map<String, Object> json = this
				.retrieveJsonFromUrl("http://pubapi.cryptsy.com/api.php?method=singlemarketdata&marketid="
						+ this.marketId);
		
		Map<String, Object> market = this.getMarketFromJson(json);
		String marketLabel = market.get("label").toString();
		if(!marketLabel.contains("BTC"))
			Log.error("This is no Bitcoin market!");
		this.currencyName = marketLabel.split("/")[0];
	}

	@Override
	public boolean hasNext() {
		/** the market has always more data. */
		return true;
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
		return currencyName;
	}

	@Override
	public void placeBuyOrder(double amount, Double price) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void placeSellOrder(double amount, Double price) {
		// TODO Auto-generated method stub
		
	}
	
	private Map<String, Object> retrieveJsonFromUrl(String urlString) throws IOException {
		URL url = new URL(urlString);
        BufferedReader in = new BufferedReader(
        new InputStreamReader(url.openStream()));
       
        ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> json = mapper.readValue(in, new TypeReference<Map<String, Object>>() { });
		in.close();
		return json ;
		
	   
	}
	

	@SuppressWarnings("unchecked")
	private Map<String, Object> getMarketFromJson(Map<String, Object> json) {
		int success = (Integer) json.get("success");
		if (success != 1) {
			Log.warn("Unsuccessfull");
			return null;
		}
		Map<String, Object> res = (Map<String, Object>) json.get("return");
		if (res == null) {
			Log.warn("no return entry ");
			return null;
		}
		Map<String, Object> markets = (Map<String, Object>) res.get("markets");
		if (markets == null) {
			Log.warn("no markets entry");
			return null;
		}

		return (Map<String, Object>) markets.values().iterator().next();		
	}

}
