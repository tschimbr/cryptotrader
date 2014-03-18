package ch.eonum.cryptotrader;


import java.io.IOException;
import java.text.ParseException;

import ch.eonum.pipeline.classification.lstm.LSTM;
import ch.eonum.pipeline.core.DataSet;
import ch.eonum.pipeline.core.Features;
import ch.eonum.pipeline.core.SequenceDataSet;
import ch.eonum.pipeline.core.SparseSequence;
import ch.eonum.pipeline.evaluation.Evaluator;
import ch.eonum.pipeline.evaluation.RMSESequence;
import ch.eonum.pipeline.transformation.MinMaxNormalizerSequence;
import ch.eonum.pipeline.util.FileUtil;
import ch.eonum.pipeline.validation.SystemValidator;

public class LSTMTraining {
	public static final String dataset = "data/DOGE_BTC/";
	public static final String validationdataset = "data/DOGE_BTC_validation/";
	public static final String resultsFolder = "data/lstm/";

	/**
	 * Test Validation Script for the evaluation of models. Execute with enough
	 * memory: -Xmx1024m
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
		FileUtil.mkdir(resultsFolder);
		
		SequenceDataSet<SparseSequence> dataTraining = CryptsyMarketDataReader.readDataSet(dataset, 12);
		SequenceDataSet<SparseSequence> dataValidation = CryptsyMarketDataReader.readDataSet(validationdataset, 12);
				
		@SuppressWarnings("unchecked")
		Features dims = Features.createFromDataSets(new DataSet[] {
				dataTraining });
		
		dims.writeToFile(resultsFolder + "features.txt");
		
		MinMaxNormalizerSequence<SparseSequence> minmax = new MinMaxNormalizerSequence<SparseSequence>(dataTraining, dims);
		minmax.setInputDataSet(dataTraining);
		minmax.extract();
		minmax.setInputDataSet(dataValidation);
		minmax.extract();
	
//		dataTraining.addAll(dataValidation);
				
		Evaluator<SparseSequence> rmse = new RMSESequence<SparseSequence>();
		
		LSTM<SparseSequence> lstm = new LSTM<SparseSequence>();
		lstm.setForgetGateUse(false);
		lstm.setInputGateUse(true);
		lstm.setOutputGateUse(true);
		lstm.setFeatures(dims);
		lstm.setBaseDir(resultsFolder + "lstm/");
		FileUtil.mkdir(resultsFolder + "lstm/");
	
		
		lstm.putParameter("numNets", 1.0);
		lstm.putParameter("numNetsTotal", 1.0);
		lstm.putParameter("maxEpochsAfterMax", 50);
		lstm.putParameter("maxEpochs", 1500);
		lstm.putParameter("numLSTM", 9.0);
		lstm.putParameter("memoryCellBlockSize", 2.0);
		lstm.putParameter("numHidden", 0.0);
		lstm.putParameter("learningRate", 0.003125);
		

		lstm.setTestSet(dataValidation);
		lstm.setTrainingSet(dataTraining);
		SystemValidator<SparseSequence> lstmSystem = new SystemValidator<SparseSequence>(lstm, rmse);
		lstmSystem.setBaseDir(resultsFolder);
		
		
		
		lstmSystem.evaluate(true, "nn-all");
		System.out.println("Optimum: " + rmse.evaluate(dataValidation));

	}
		
}
