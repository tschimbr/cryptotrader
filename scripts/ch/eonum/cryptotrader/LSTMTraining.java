package ch.eonum.cryptotrader;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.HashMap;

import com.abwaters.cryptsy.Cryptsy.CryptsyException;

import ch.eonum.pipeline.classification.lstm.LSTM;
import ch.eonum.pipeline.core.DataSet;
import ch.eonum.pipeline.core.Features;
import ch.eonum.pipeline.core.SparseSequence;
import ch.eonum.pipeline.evaluation.Evaluator;
import ch.eonum.pipeline.evaluation.RMSE;
import ch.eonum.pipeline.evaluation.RMSESequence;
import ch.eonum.pipeline.transformation.MinMaxNormalizerSequence;
import ch.eonum.pipeline.util.FileUtil;
import ch.eonum.pipeline.validation.SystemValidator;

public class LSTMTraining {
	public static final String dataset = "data/LTC_BTC/";
	public static final String validationdataset = "data/archiv/LTC_BTC_validation/";
	public static final String testdataset = "data/archiv/LTC_BTC_test/";
	public static final String resultsFolder = "data/lstm-cryptsy-production/";

	/**
	 * Test Validation Script for the evaluation of models. Execute with enough
	 * memory: -Xmx1024m
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws CryptsyException 
	 */
	public static void main(String[] args) throws IOException, ParseException, CryptsyException {
		FileUtil.mkdir(resultsFolder);
		
		CryptsyMarketDataReader readerTraining = new CryptsyMarketDataReader(dataset);
		CryptsyMarketDataReader readerTest = new CryptsyMarketDataReader(validationdataset);
		
		
//		DataSet<SparseSequence> dataValidation = readerValidation.readDataSet(validationdataset);
		DataSet<SparseSequence> dataTraining = readerTraining.readDataSet(dataset);
		
		@SuppressWarnings("unchecked")
		Features features = Features.createFromDataSets(new DataSet[] {
				dataTraining });
		
		features.writeToFile(resultsFolder + "features.txt");
		
		MinMaxNormalizerSequence<SparseSequence> minmax = new MinMaxNormalizerSequence<SparseSequence>(dataTraining, features);
		minmax.setInputDataSet(dataTraining);
		minmax.extract();
		
		DataSet<SparseSequence> dataValidation = dataTraining.extractSubSet(0.2);
		
				
		Evaluator<SparseSequence> rmse = new RMSE<SparseSequence>();
		
		LSTM<SparseSequence> lstm = new LSTM<SparseSequence>();
		lstm.setTestSet(dataValidation);
		lstm.setTrainingSet(dataTraining);
		
		lstm.setForgetGateUse(false);
		lstm.setInputGateUse(true);
		lstm.setOutputGateUse(true);
		lstm.setFeatures(features);
		lstm.setBaseDir(resultsFolder + "lstm/");
		FileUtil.mkdir(resultsFolder + "lstm/");
		
//		lstm.putParameter("gaussRange", 0.8);
//		lstm.putParameter("initRange", 0.12);
		lstm.putParameter("numNets", 2.0);
		lstm.putParameter("numNetsTotal", 4.0);
		lstm.putParameter("maxEpochsAfterMax", 4000);
		lstm.putParameter("maxEpochs", 10000);
		lstm.putParameter("numLSTM", 8.0);
		lstm.putParameter("memoryCellBlockSize", 5.0);
		lstm.putParameter("numHidden", 0.0);
		lstm.putParameter("learningRate", 0.0078125);
		lstm.putParameter("momentum", 0.9);
		lstm.putParameter("batchSize", 60.0);
//		lstm.putParameter("lambda", 0.000001);
		
		SystemValidator<SparseSequence> lstmSystem = new SystemValidator<SparseSequence>(lstm, rmse);
		lstmSystem.setBaseDir(resultsFolder);		
		
		lstmSystem.evaluate(true, "nn-all");
		
		System.out.println("Optimum: " + rmse.evaluate(dataValidation));
		System.out.println("Base line: " + printBaseline(dataValidation, rmse));
//		System.out.println("Base line with same trend: " + printTimeLagBaseline(dataValidation, rmse, (int)readerTraining.getDoubleParameter("timeLag")));
		
		/** visualize. print result. */
		lstm.setTestSet(dataTraining);
		lstm.test();
		lstm.setTestSet(dataValidation);
		lstm.test();
		printPredictions(dataValidation, "predictions.csv", features, false);
		printPredictions(dataTraining, "predictionsTraining.csv", features, false);
		
		PricePredictor pp = new PricePredictor(lstm, minmax);
		
//		Simulator simulator = new Simulator(readerTest, 20, 1);
//		Trader trader = new Trader(pp, simulator, resultsFolder + "tradingLog.txt");
//		trader.putParameter("waitMillis", 0);
		CryptsyMarket cryptsy = new CryptsyMarket(3, new CryptsyMarketDataReader(null), "/home/tim/cryptotrader/data/private-api/");
		Trader trader = new Trader(pp, cryptsy, resultsFolder + "tradingLog.txt");
		trader.putParameter("waitMillis", 1000*60*10);
		
		trader.startTrading();
		trader.close();
		
//		System.out.println("Portfolio Value change: " + simulator.evaluate(null));

	}

	public static double printTimeLagBaseline(
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

	public static double printBaseline(
			DataSet<SparseSequence> data,
			Evaluator<SparseSequence> rmse) {
		double avgGT = 0;
		int n = 0;
		for (SparseSequence s : data) {
			if (!Double.isNaN(s.outcome)) {
				avgGT += s.outcome;
				n++;
			}
		}
		avgGT /= n;
		System.out.println("Average ground truth: " + avgGT);
		

		for(SparseSequence s : data){
			s.putResult("result", avgGT);
		}
		
		return rmse.evaluate(data);
	}

	/**
	 * @param s
	 * @param fileName 
	 * @throws FileNotFoundException
	 */
	public static void printPredictions(DataSet<SparseSequence> s, String fileName, Features features, boolean flat)
			throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new File(resultsFolder + fileName));
		for(int f = 0; f < features.size(); f++)
			pw.print(features.getFeatureByIndex(f) + ";");
		pw.println("groundTruth;prediction");

		
		for(SparseSequence e : s){
			for(int f = 0; f < features.size(); f++)
				pw.print(flat ? e.get(features.getFeatureByIndex(f)) + ";" :
					e.get(e.getSequenceLength() - 1, features.getFeatureByIndex(f)) + ";");
			pw.print(e.outcome + ";" + e.getResult("result"));
			pw.println();
		}
		pw.close();
	}
		
}
