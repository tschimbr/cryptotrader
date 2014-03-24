package ch.eonum.cryptotrader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ch.eonum.pipeline.core.DataPipeline;
import ch.eonum.pipeline.core.DataSet;
import ch.eonum.pipeline.core.SparseSequence;

/**
 * Trading bot. A Trader uses a PricePredictor to predict market prices and
 * places buy/sell orders according these predictions in a certain Market.
 * 
 * @author tim
 * 
 */
public class Trader implements DataPipeline<SparseSequence>  {

	/** market forecasting unit. */
	private PricePredictor predictor;
	/** market on which should be traded. */
	private Market market;
	/** Trading and market log. */
	private PrintWriter log;
	/**
	 * close flag. when true, all trading operations will be shut down after the
	 * next iteration.
	 */
	private boolean close;
	private List<Double> previousPredictions;
	private SimpleDateFormat dateFormatter;

	public Trader(PricePredictor pricePredictor, Market market, String logFileName) throws IOException {
		this.market = market;
		this.predictor = pricePredictor;
		this.log = new PrintWriter(new File(logFileName));
		this.previousPredictions = new ArrayList<Double>();
		dateFormatter = new SimpleDateFormat("yyyy-MM-dd kk:mm");
	}
	
	/**
	 * Close all trading operations.
	 */
	public void close(){
		this.close = true;
		this.log.close();
	}
	
	/**
	 * Start trading activities. Depending on the market this function can loop
	 * forever unless stopped using {@link #close()}
	 */
	public void startTrading() {
		while(market.hasNext() && !close){
			Map<String, Double> marketData = market.next();
			double prediction = predictor.nextPrediction(marketData);
			this.previousPredictions.add(prediction);
			log.println(dateFormatter.format(new Date()) + " prediction: " + prediction);
			
		}
	}
	
	/** pipeline methods. */

	@Override
	public DataSet<SparseSequence> trainSystem(boolean isResultDataSetNeeded) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataSet<SparseSequence> testSystem() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addInputTraining(DataPipeline<SparseSequence> input) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addInputTest(DataPipeline<SparseSequence> input) {
		// TODO Auto-generated method stub
		
	}

}
