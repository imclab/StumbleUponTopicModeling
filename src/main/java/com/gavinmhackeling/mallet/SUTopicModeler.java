package com.gavinmhackeling.mallet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.iterator.SimpleFileLineIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.types.InstanceList;

/**
 * @author ghackeling
 *
 * /media/Storage/workspace/topicmodeling/src/main/resources/full.corpus
 * /media/Storage/workspace/topicmodeling/src/main/resources/su.model
 * /media/Storage/workspace/topicmodeling/src/main/resources/topics
 * /media/Storage/workspace/topicmodeling/src/main/resources/test.corpus
 * /media/Storage/workspace/topicmodeling/src/main/resources/report.csv
 *
 */
public class SUTopicModeler 
{
	private static final int NUM_THREADS = 4;
	private static final int NUM_TOPICS = 120;
	private static final int NUM_ITERATIONS = 2000;
	private static InstanceList instances;
	private static ParallelTopicModel model;
	private static Alphabet dataAlphabet;
	private static ArrayList<TreeSet<IDSorter>> topicSortedWords;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();

		loadModel();
		createModel(args[0], args[1], args[2]);
		predict(args[3], args[4]);

		long duration = System.currentTimeMillis()-startTime;
		System.out.println("Completed in " + duration + " milliseconds");

	}

	private static void loadModel() {
		// TODO Auto-generated method stub
		
	}

	private static void predict(String testSetFileName, String outReportFileName) throws IOException 
	{
		InstanceList testing = new InstanceList(instances.getPipe());
		testing.addThruPipe(new SimpleFileLineIterator(new File(testSetFileName)));
		TopicInferencer inferencer = model.getInferencer();
		FileWriter fileWriter = new FileWriter(outReportFileName);

		for (int i=0; i<testing.size(); i++)
		{
			double[] testProbabilities = inferencer.getSampledDistribution(testing.get(i), 10, 1, 5);
			for (int j=0; j<testProbabilities.length; j++)
			{
				fileWriter.append(String.valueOf(testProbabilities[j]));
				if (j+1<testProbabilities.length) fileWriter.append(',');
			}
			fileWriter.append('\n');
		}
		fileWriter.flush();
		fileWriter.close();		
	}

	private static void createModel(String trainingFileName, String outModelName, String outTopicsListName) throws IOException 
	{
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
		pipeList.add(new CharSequence2TokenSequence(Pattern.compile("[\\p{L}\\p{N}_]+")));
		//        pipeList.add(new TokenSequenceRemoveStopwords(new File("stoplists/en.txt"), "UTF-8", false, false, false)); // file has been stop filtered, but not aggressively
		pipeList.add(new TokenSequence2FeatureSequence());
		instances = new InstanceList (new SerialPipes(pipeList));
		instances.addThruPipe(new SimpleFileLineIterator(new File(trainingFileName)));

		// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
		//  Note that the first parameter is passed as the sum over topics, while
		//  the second is the parameter for a single dimension of the Dirichlet prior.
		System.out.println("Creating topic model");

		model = new ParallelTopicModel(NUM_TOPICS, 1.0, 0.01);
		model.addInstances(instances);
		model.setNumThreads(NUM_THREADS);
		model.setNumIterations(NUM_ITERATIONS);
		model.estimate();

		// The data alphabet maps word IDs to strings
		dataAlphabet = instances.getDataAlphabet();

		model.write(new File(outModelName));

		// Get an array of sorted sets of word ID/count pairs
		topicSortedWords = model.getSortedWords();

		FileWriter fileWriter = new FileWriter(new File(outTopicsListName));
		for (int i=0; i<topicSortedWords.size(); i++)
		{
			Iterator<IDSorter> iterator = topicSortedWords.get(i).iterator();
			int rank = 0;
			fileWriter.write("Topic: " + i + ": ");
			while (iterator.hasNext() && rank < 12) {
				IDSorter idCountPair = iterator.next();
				fileWriter.write(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
				rank++;
			}
			fileWriter.write('\n');
		}
		fileWriter.flush();
		fileWriter.close();
	}
}
