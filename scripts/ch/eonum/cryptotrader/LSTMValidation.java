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
	public static final String dataset = "data/DOGE_BTC/";
	public static final String validationdataset = "data/DOGE_BTC_validation/";
	public static final String resultsFolder = "data/lstm-validation/";

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
		lstm.putParameter("maxEpochs", 500);
		lstm.putParameter("numLSTM", 9.0);
		lstm.putParameter("memoryCellBlockSize", 2.0);
		lstm.putParameter("numHidden", 0.0);
		lstm.putParameter("learningRate", 0.003125);
		

		lstm.setTestSet(dataValidation);
		lstm.setTrainingSet(dataTraining);
		SystemValidator<SparseSequence> lstmSystem = new SystemValidator<SparseSequence>(lstm, rmse);
		lstmSystem.setBaseDir(resultsFolder);
		
		List<ParameterValidation> paramsGradientAscent = new ArrayList<ParameterValidation>();
		
				
		paramsGradientAscent.add(new ParameterValidation(new Parameters[] {
				lstm }, "numLSTM", 4.0, 12.0, 1.0,
				20.0, 2.0, 1.0, false));
		paramsGradientAscent.add(new ParameterValidation(new Parameters[] {
				lstm }, "momentum", 0.0, 0.9, 0.0,
				0.99, 0.0, 0.1, false));
		paramsGradientAscent.add(new ParameterValidation(new Parameters[] {
				lstm }, "batchSize", 1.0, 100.0, 1.0,
				200.0, 1.0, 20.0, false));
		paramsGradientAscent.add(new ParameterValidation(new Parameters[] {
				lstm }, "learningRate", -14, -2, -8,
				0.0, 0.01, 1.0, true));
		paramsGradientAscent.add(new ParameterValidation(new Parameters[] {
				lstm }, "memoryCellBlockSize", 1.0, 8.0, 1.0,
				20.0, 1.0, 1.0, false));
		

		Map<ParameterValidation, Double> params = lstmSystem.gradientAscent(paramsGradientAscent, 5, resultsFolder + "parameter_validation/");
		Log.puts("Optimal Parameters: " + params);
		ParameterValidation.updateParameters(params);

	}
		
}
