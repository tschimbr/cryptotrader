package ch.eonum.cryptotrader;


import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.eonum.pipeline.classification.lstm.LSTM;
import ch.eonum.pipeline.core.DataSet;
import ch.eonum.pipeline.core.Features;
import ch.eonum.pipeline.core.Parameters;
import ch.eonum.pipeline.core.SequenceDataSet;
import ch.eonum.pipeline.core.SparseSequence;
import ch.eonum.pipeline.evaluation.Evaluator;
import ch.eonum.pipeline.evaluation.RMSESequence;
import ch.eonum.pipeline.transformation.MinMaxNormalizerSequence;
import ch.eonum.pipeline.util.FileUtil;
import ch.eonum.pipeline.util.Log;
import ch.eonum.pipeline.validation.ParameterValidation;
import ch.eonum.pipeline.validation.SystemValidator;

public class LSTMValidation {
	public static final String dataset = "data/LTC_BTC/";
	public static final String validationdataset = "data/LTC_BTC_validation/";
	public static final String resultsFolder = "data/lstm-validation-forget/";

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
		
		SequenceDataSet<SparseSequence> data = readerTraining.readDataSet(dataset);
				
		@SuppressWarnings("unchecked")
		Features features = Features.createFromDataSets(new DataSet[] {
				data });
		
		features.writeToFile(resultsFolder + "features.txt");
//		dataTraining.addAll(dataValidation);
		
		MinMaxNormalizerSequence<SparseSequence> minmax = new MinMaxNormalizerSequence<SparseSequence>(data, features);
		minmax.addInputTest(readerValidation);
		minmax.addInputTraining(readerTraining);
				
		Evaluator<SparseSequence> rmse = new RMSESequence<SparseSequence>();
		
		LSTM<SparseSequence> lstm = new LSTM<SparseSequence>();
		lstm.addInputTest(minmax);
		lstm.addInputTraining(minmax);
		lstm.setForgetGateUse(true);
		lstm.setInputGateUse(true);
		lstm.setOutputGateUse(true);
		lstm.setFeatures(features);
		lstm.setBaseDir(resultsFolder + "lstm/");
		FileUtil.mkdir(resultsFolder + "lstm/");
	
		
		lstm.putParameter("numNets", 1.0);
		lstm.putParameter("numNetsTotal", 1.0);
		lstm.putParameter("maxEpochsAfterMax", 200);
		lstm.putParameter("maxEpochs", 500);
		lstm.putParameter("numLSTM", 6.0);
		lstm.putParameter("memoryCellBlockSize", 5.0);
		lstm.putParameter("numHidden", 0.0);
		lstm.putParameter("learningRate", 0.004);
		lstm.putParameter("momentum", 0.8);
		
		SystemValidator<SparseSequence> lstmSystem = new SystemValidator<SparseSequence>(lstm, rmse);
		lstmSystem.setBaseDir(resultsFolder);
		
		List<ParameterValidation> paramsGradientAscent = new ArrayList<ParameterValidation>();
		
		paramsGradientAscent.add(new ParameterValidation(new Parameters[] {
				readerTraining, readerValidation }, "floatingAverageFactor", 0.01, 0.99, 0.0001,
				0.999, 0.3, 0.04, false));		
		paramsGradientAscent.add(new ParameterValidation(new Parameters[] {
				lstm }, "numLSTM", 4.0, 12.0, 1.0,
				20.0, 6.0, 1.0, false));
		paramsGradientAscent.add(new ParameterValidation(new Parameters[] {
				lstm }, "momentum", 0.0, 0.9, 0.0,
				0.99, 0.8, 0.1, false));
//		paramsGradientAscent.add(new ParameterValidation(new Parameters[] {
//				lstm }, "batchSize", 1.0, 100.0, 1.0,
//				200.0, 1.0, 20.0, false));
		paramsGradientAscent.add(new ParameterValidation(new Parameters[] {
				lstm }, "learningRate", -14, -2, -8,
				0.0, 0.004, 1.0, true));
		paramsGradientAscent.add(new ParameterValidation(new Parameters[] {
				lstm }, "memoryCellBlockSize", 1.0, 8.0, 1.0,
				20.0, 5.0, 1.0, false));
		

		Map<ParameterValidation, Double> params = lstmSystem.gradientAscent(paramsGradientAscent, 5, resultsFolder + "parameter_validation/");
		Log.puts("Optimal Parameters: " + params);
		ParameterValidation.updateParameters(params);

	}
		
}
