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
		
		CryptsyMarketDataReader readerTraining = new CryptsyMarketDataReader(dataset);
		CryptsyMarketDataReader readerValidation = new CryptsyMarketDataReader(validationdataset);
		readerTraining.putParameter("floatingAverageFactor", 0.3);
		readerValidation.putParameter("floatingAverageFactor", 0.3);
		
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
		lstm.putParameter("maxEpochsAfterMax", 600);
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
		System.out.println("Base line: " + printBaseline(dataValidation, rmse));
		System.out.println("Base line with same trend: " + printTimeLagBaseline(dataValidation, rmse, (int)readerTraining.getDoubleParameter("timeLag")));
		
		/** visualize. print result. */
		lstm.setTestSet(dataTraining);
		lstm.test();
		lstm.setTestSet(dataValidation);
		lstm.test();
		printPredicitons(dataValidation.get(0), "predictions.csv", features);
		printPredicitons(dataTraining.get(0), "predictionsTraining.csv", features);
		
		PricePredictor pp = new PricePredictor(lstm, minmax);
		Simulator simulator = new Simulator(readerValidation, 20, 1);
		Trader trader = new Trader(pp, simulator, resultsFolder + "tradingLog.txt");
		trader.startTrading();
		trader.close();
		
		System.out.println("Portfolio Value change: " + simulator.evaluate(null));

	}

	private static double printTimeLagBaseline(
			DataSet<SparseSequence> data,
			Evaluator<SparseSequence> rmse, int timeLag) {
		for(SparseSequence s : data){
			for(int t = 0; t < s.getGroundTruthLength(); t++){
				if(!Double.isNaN(s.groundTruthAt(t, 0))){
					if(t > timeLag)
						s.addSequenceResult(t, 0, s.groundTruthAt(t - timeLag - 1, 0));
					else
						s.addSequenceResult(t, 0, s.groundTruthAt(t, 0));
				}
			}
		}
		
		return rmse.evaluate(data);
	}

	private static double printBaseline(
			DataSet<SparseSequence> data,
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
