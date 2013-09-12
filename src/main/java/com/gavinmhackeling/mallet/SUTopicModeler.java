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
 */
public class SUTopicModeler 
{
	private static final int NUM_THREADS = 4;
	private static final int NUM_TOPICS = 100;
	private static final int NUM_ITERATIONS = 50;
	private static final String TEST = "Homemade Enchilada Sauce Lynn s Kitchen Adventures I usually buy my enchilada sauce Yes I knew I should be making it but I had never found a recipe that I was really happy with I had tried several and they just weren t very good So I stuck to the canned stuff you can get at the grocery store I was recently talking to a friend of mine about this She lived in Mexico for a few years so she knows some about Mexican cooking I asked her how she made her enchilada sauce She told me the basics and then gave me an exact recipe I decided to give it a try This recipe was really good This was the best enchilada sauce that I have made It had great flavor I think it was even better than the canned sauce My husband thought it could have been spicier But he likes his enchiladas spicy You can always add more chili powder or chilies if you like it really spicy The kids and I thought it was really good just like it is I did change two things It called for green onions I did not have any so I used regular onions I thought they worked great so I will probably continue to make it this way I also pureed everything in the blender I wanted a very smooth sauce If you want it more chunky just mix the ingredients together and do not blend Enchiladas are a pretty frugal meal and homemade enchilada sauce is a great way to make enchiladas even more frugal 2 8 ounce cans tomato sauce 1 4 ounce can chopped green chilies undrained 1 2 cup onion chopped 2 teaspoons chili powder 1 teaspoon ground cumin 1 4 teaspoon dried oregano 1 clove garlic minced Combine all tomato sauce ingredients and place in a blender Puree ingredients Then place in a saucepan Heat over medium heat until heated through about 5 minutes Use as desired for enchiladas Get your free Quick Easy Breakfasts ebook Just subscribe for free email updates from Lynn s Kitchen Adventures Like this article Share it this homemade enchilada sauce recipe came from a friend of mine who spent several years in mexico.";
	private static final String TEST2 = "lolpics Stun grenade ar funny pictures at lolpics.se. the best funny images on the internet funny photo images, funny videos, can has cheezburger ,roliga bilder, lol pics, lolpics";
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

		createModel(args[0], args[1]);
		predict(args[2], args[3]);

		long duration = System.currentTimeMillis()-startTime;
		System.out.println("Completed in " + duration + " milliseconds");

	}

	private static void predict(String testSetFileName, String outReportFileName) throws IOException 
	{
		System.out.println("\n=====================================================\nPrediction for TEST:");
		InstanceList testing = new InstanceList(instances.getPipe());
		testing.addThruPipe(new SimpleFileLineIterator(new File(testSetFileName)));
		TopicInferencer inferencer = model.getInferencer();
		FileWriter fileWriter = new FileWriter(outReportFileName);

		for (int i=0; i<testing.size(); i++)
		{
			double[] testProbabilities = inferencer.getSampledDistribution(testing.get(i), 10, 1, 5);
			for (int j=0; j<testProbabilities.length; j++)
			{
				fileWriter.append(testProbabilities[j] + ",");
				if (testProbabilities[j] > 0.2)
				{
					System.out.println(j + ": " + testProbabilities[j]);
					Iterator<IDSorter> iterator = topicSortedWords.get(j).iterator();
					int rank = 0;
					while (iterator.hasNext() && rank < 10) {
						IDSorter idCountPair = iterator.next();
						System.out.print(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
						rank++;
					}
				}
			}
			fileWriter.append('\n');
		}
		fileWriter.flush();
		fileWriter.close();		
	}

	private static void createModel(String trainingFileName, String outModelName) throws IOException 
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

		for (int i=0; i<topicSortedWords.size(); i++)
		{
			Iterator<IDSorter> iterator = topicSortedWords.get(i).iterator();
			int rank = 0;
			System.out.print("Topic: " + i + ": ");
			while (iterator.hasNext() && rank < 5) {
				IDSorter idCountPair = iterator.next();
				System.out.print(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
				rank++;
			}
			System.out.println();
		}
	}
}
