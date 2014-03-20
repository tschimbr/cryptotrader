package ch.eonum.cryptotrader;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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
	public static final String dataset = "data/LTC_BTC/";
	public static final String validationdataset = "data/LTC_BTC_validation/";
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
		Features features = Features.createFromDataSets(new DataSet[] {
				dataTraining });
		
		features.writeToFile(resultsFolder + "features.txt");
//		dataTraining.addAll(dataValidation);
		
		MinMaxNormalizerSequence<SparseSequence> minmax = new MinMaxNormalizerSequence<SparseSequence>(dataTraining, features);
		minmax.setInputDataSet(dataTraining);
		minmax.extract();
//		minmax = new MinMaxNormalizerSequence<SparseSequence>(dataValidation, dims);
		minmax.setInputDataSet(dataValidation);
		minmax.extract();
//		
//		Features targetFeatures = new Features();
//		targetFeatures.addFeature("price");
//		targetFeatures.recalculateIndex();
//		dataTraining.setTimeLag(12, targetFeatures);
//		dataValidation.setTimeLag(12, targetFeatures);
//	
		
				
		Evaluator<SparseSequence> rmse = new RMSESequence<SparseSequence>();
		
		LSTM<SparseSequence> lstm = new LSTM<SparseSequence>();
		lstm.setForgetGateUse(false);
		lstm.setInputGateUse(true);
		lstm.setOutputGateUse(true);
		lstm.setFeatures(features);
		lstm.setBaseDir(resultsFolder + "lstm/");
		FileUtil.mkdir(resultsFolder + "lstm/");
	
		
		lstm.putParameter("numNets", 1.0);
		lstm.putParameter("numNetsTotal", 1.0);
		lstm.putParameter("maxEpochsAfterMax", 600);
		lstm.putParameter("maxEpochs", 1000);
		lstm.putParameter("numLSTM", 4.0);
		lstm.putParameter("memoryCellBlockSize", 7.0);
		lstm.putParameter("numHidden", 0.0);
		lstm.putParameter("learningRate", 0.0078);
		lstm.putParameter("momentum", 0.3);
		

		lstm.setTestSet(dataValidation);
		lstm.setTrainingSet(dataTraining);
		SystemValidator<SparseSequence> lstmSystem = new SystemValidator<SparseSequence>(lstm, rmse);
		lstmSystem.setBaseDir(resultsFolder);
		
		
		
		lstmSystem.evaluate(true, "nn-all");
		lstm.setTestSet(dataValidation);
		lstm.test();
		System.out.println("Optimum: " + rmse.evaluate(dataValidation));
		System.out.println("Base line: " + printBaseline(dataValidation, rmse));
		
		/** visualize. print result. */
		lstm.setTestSet(dataValidation);
		lstm.test();
		lstm.setTestSet(dataTraining);
		lstm.test();
		printPredicitons(dataValidation.get(0), "predictions.csv", features);
		printPredicitons(dataTraining.get(0), "predictionsTraining.csv", features);

	}

	private static double printBaseline(
			SequenceDataSet<SparseSequence> data,
			Evaluator<SparseSequence> rmse) {
		double avgGT = 0;
		int n = 0;
		for(SparseSequence s : data){
			for(int t = 0; t < s.getGroundTruthLength(); t++){
				if(!Double.isNaN(s.groundTruthAt(t, 0))){
					avgGT += s.groundTruthAt(t, 0);
					n++;
				}
			}
		}
		avgGT /= n;
		System.out.println("Average ground truth: " + avgGT);
		

		for(SparseSequence s : data){
			for(int t = 0; t < s.getGroundTruthLength(); t++){
				if(!Double.isNaN(s.groundTruthAt(t, 0))){
					s.addSequenceResult(t, 0, avgGT);
				}
			}
		}
		
		return rmse.evaluate(data);
	}

	/**
	 * @param s
	 * @param fileName 
	 * @throws FileNotFoundException
	 */
	public static void printPredicitons(SparseSequence s, String fileName, Features features)
			throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new File(resultsFolder + fileName));
		for(int f = 0; f < features.size(); f++)
			pw.print(features.getFeatureByIndex(f) + ";");
		pw.println("groundTruth;prediction");

		for(int t = 0; t < s.getSequenceLength(); t++){
			for(int f = 0; f < features.size(); f++)
				pw.print(s.get(t, features.getFeatureByIndex(f)) + ";");
			pw.print(s.groundTruthAt(t, 0) + ";" + s.resultAt(t, 0));
			pw.println();
		}
		pw.close();
	}
		
}
