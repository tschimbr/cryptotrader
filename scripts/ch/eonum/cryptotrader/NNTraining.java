package ch.eonum.cryptotrader;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;

import com.abwaters.cryptsy.Cryptsy.CryptsyException;

import ch.eonum.pipeline.classification.nn.NeuralNet;
import ch.eonum.pipeline.core.DataSet;
import ch.eonum.pipeline.core.Features;
import ch.eonum.pipeline.core.SequenceDataSet;
import ch.eonum.pipeline.core.SparseSequence;
import ch.eonum.pipeline.evaluation.Evaluator;
import ch.eonum.pipeline.evaluation.RMSE;
import ch.eonum.pipeline.transformation.MinMaxNormalizer;
import ch.eonum.pipeline.util.FileUtil;
import ch.eonum.pipeline.validation.SystemValidator;

public class NNTraining {
	public static final String dataset = "data/archiv/LTC_BTC/";
	public static final String validationdataset = "data/archiv/LTC_BTC_validation/";
	public static final String testdataset = "data/archiv/LTC_BTC_test/";
	public static final String resultsFolder = "data/nn/";

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
		CryptsyMarketDataReader readerValidation = new CryptsyMarketDataReader(validationdataset);
//		CryptsyMarketDataReader readerTest = new CryptsyMarketDataReader(testdataset);
		
		
		SequenceDataSet<SparseSequence> dataValidation = readerValidation.readDataSet(validationdataset);
		SequenceDataSet<SparseSequence> dataTraining = readerTraining.readDataSet(dataset);
		
		dataTraining.levelSequencesTimeWindow();
		dataValidation.levelSequencesTimeWindow();
		
		Features features = new Features();
		for(String f : dataTraining.get(0).features()){
			for(int i = 0; i < 12; i++)
				features.addFeature(i + f);
		}
		features.recalculateIndex();
		
		features.writeToFile(resultsFolder + "features.txt");
		
		MinMaxNormalizer<SparseSequence> minmax = new MinMaxNormalizer<SparseSequence>(dataTraining);
		minmax.setInputDataSet(dataTraining);
		minmax.extract();
		minmax.setInputDataSet(dataValidation);
		minmax.extract();
		
				
		Evaluator<SparseSequence> rmse = new RMSE<SparseSequence>();
		
		NeuralNet<SparseSequence> nn = new NeuralNet<SparseSequence>(features);
		nn.setTestSet(dataValidation);
		nn.setTrainingSet(dataTraining);
	
		nn.setFeatures(features);
		nn.setBaseDir(resultsFolder + "nn/");
		FileUtil.mkdir(resultsFolder + "nn/");
		
		nn.putParameter("numNets", 1.0);
		nn.putParameter("numNetsTotal", 1.0);
		nn.putParameter("maxEpochsAfterMax", 300.0);
		nn.putParameter("maxEpochs", 2000);
		nn.putParameter("hidden", 10.0);
		nn.putParameter("learningRate", 0.04);
		nn.putParameter("momentum", 0.8);
		nn.putParameter("batchSize", 20.0);
		
		SystemValidator<SparseSequence> lstmSystem = new SystemValidator<SparseSequence>(nn, rmse);
		lstmSystem.setBaseDir(resultsFolder);		
		
		lstmSystem.evaluate(true, "nn-all");
		
		System.out.println("Optimum: " + rmse.evaluate(dataValidation));
		System.out.println("Base line: " + printBaseline(dataValidation, rmse));
//		System.out.println("Base line with same trend: " + printTimeLagBaseline(dataValidation, rmse, (int)readerTraining.getDoubleParameter("timeLag")));
		
		/** visualize. print result. */
		nn.setTestSet(dataTraining);
		nn.test();
		nn.setTestSet(dataValidation);
		nn.test();
		printPredictions(dataValidation, "predictions.csv", features, true);
		printPredictions(dataTraining, "predictionsTraining.csv", features, true);
		
//		PricePredictor pp = new PricePredictor(lstm, minmax);
//		
//		Simulator simulator = new Simulator(readerTest , 20, 1);
//		Trader trader = new Trader(pp, simulator, resultsFolder + "tradingLog.txt");
////		CryptsyMarket cryptsy = new CryptsyMarket(3, new CryptsyMarketDataReader(null), "/home/tim/cryptotrader/data/private-api/");
////		Trader trader = new Trader(pp, cryptsy, resultsFolder + "tradingLog.txt");
//		trader.putParameter("waitMillis", 0);//1000*60*10);
//		trader.startTrading();
//		trader.close();
//		
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
