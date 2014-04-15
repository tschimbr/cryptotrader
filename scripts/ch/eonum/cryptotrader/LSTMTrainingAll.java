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

public class LSTMTrainingAll {
	public static final String dataset = "data/ALL_MARKETS_Training/";
	public static final String validationdataset = "data/ALL_MARKETS_Validation/";
	public static final String resultsFolder = "data/lstm-all/";

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
		
		CryptsyMarketDataReader readerTraining = new CryptsyMarketDataReader(dataset);
		CryptsyMarketDataReader readerValidation = new CryptsyMarketDataReader(validationdataset);
		readerTraining.putParameter("floatingAverageFactor", 0.3);
		readerValidation.putParameter("floatingAverageFactor", 0.3);
		readerTraining.putParameter("smooth", 0.0);
		readerValidation.putParameter("smooth", 0.0);
		
		DataSet<SparseSequence> dataValidation = readerValidation.readDataSet(validationdataset);
		SequenceDataSet<SparseSequence> dataTraining = readerTraining.readDataSet(dataset);
		
		@SuppressWarnings("unchecked")
		Features features = Features.createFromDataSets(new DataSet[] {
				dataTraining });
		
		features.writeToFile(resultsFolder + "features.txt");
		
		MinMaxNormalizerSequence<SparseSequence> minmax = new MinMaxNormalizerSequence<SparseSequence>(dataTraining, features);
		minmax.setInputDataSet(dataTraining);
		minmax.extract();
		minmax.setInputDataSet(dataValidation);
		minmax.extract();
		
				
		Evaluator<SparseSequence> rmse = new RMSESequence<SparseSequence>();
		
		LSTM<SparseSequence> lstm = new LSTM<SparseSequence>();
		lstm.setTestSet(dataValidation);
		lstm.setTrainingSet(dataTraining);
		
		lstm.setForgetGateUse(false);
		lstm.setInputGateUse(true);
		lstm.setOutputGateUse(true);
		lstm.setFeatures(features);
		lstm.setBaseDir(resultsFolder + "lstm/");
		FileUtil.mkdir(resultsFolder + "lstm/");
		
		lstm.putParameter("numNets", 1.0);
		lstm.putParameter("numNetsTotal", 1.0);
		lstm.putParameter("maxEpochsAfterMax", 150);
		lstm.putParameter("maxEpochs", 1000);
		lstm.putParameter("numLSTM", 6.0);
		lstm.putParameter("memoryCellBlockSize", 5.0);
		lstm.putParameter("numHidden", 0.0);
		lstm.putParameter("learningRate", 0.004);
		lstm.putParameter("momentum", 0.8);
		
		SystemValidator<SparseSequence> lstmSystem = new SystemValidator<SparseSequence>(lstm, rmse);
		lstmSystem.setBaseDir(resultsFolder);		
		
		lstmSystem.evaluate(true, "nn-all");
		
		System.out.println("Optimum: " + rmse.evaluate(dataValidation));
		System.out.println("Base line: " + LSTMTraining.printBaseline(dataValidation, rmse));
		System.out.println("Base line with same trend: " + LSTMTraining.printTimeLagBaseline(dataValidation, rmse, (int)readerTraining.getDoubleParameter("timeLag")));
		
		/** visualize. print result. */
		lstm.setTestSet(dataTraining);
		lstm.test();
		lstm.setTestSet(dataValidation);
		lstm.test();
//		LSTMTraining.printPredicitons(dataValidation.get(0), "predictions.csv", features);
//		LSTMTraining.printPredicitons(dataTraining.get(0), "predictionsTraining.csv", features);

	}

	
}
