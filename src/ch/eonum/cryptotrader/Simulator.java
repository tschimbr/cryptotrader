package ch.eonum.cryptotrader;

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

	public Simulator(CryptsyMarketDataReader dataReader) {
		this.reader = dataReader;
		SequenceDataSet<SparseSequence> data = dataReader.testSystem();
		this.marketData = data.get(0);
		this.currentIndex = 0;
	}
	
	@Override
	public boolean hasNext() {
		return this.currentIndex < marketData.getSequenceLength();
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
