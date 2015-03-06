/**
 * 
 */
package analyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date; 
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;






import structures.Review;
import structures.Token;
import structures.User;
/**
 * @author hongning
 * Sample codes for demonstrating OpenNLP package usage 
 * NOTE: the code here is only for demonstration purpose, 
 * please revise it accordingly to maximize your implementation's efficiency!
 */
public class DocAnalyzer   {
	ArrayList<User> Users;
	ArrayList<Tokenizer> tokenizer; // need many because of the threading
	//a list of stopwords
	HashSet<String> m_stopwords;

	//you can store the loaded reviews in this arraylist for further processing
	//ArrayList<Post> m_reviews;
	int reviewsCount ;
	//you might need something like this to store the counting statistics for validating Zipf's and computing IDF
	public HashMap<String, Token> m_Vocabs;	

	private Object lock1 = new Object();
	private Object lock2 = new Object();
	private int MaxTokenID;
	//we have also provided sample implementation of language model in src.structures.LanguageModel

	public DocAnalyzer( ) {

		Users=new ArrayList<User>();
		m_stopwords= new HashSet<String>();
		m_Vocabs=new HashMap<String, Token>();
		MaxTokenID=0;
		reviewsCount=0;
		try {
			//tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream("/cslab/home/ma2sm/hw1/data/Model/en-token.bin")));
			tokenizer=new ArrayList<Tokenizer>();
			for(int i=0;i<Config.NumberOfProcessors;++i)
				tokenizer.add( new TokenizerME(new TokenizerModel(new FileInputStream("./data/Model/en-token.bin"))));
			// Load Stopwards
			LoadStopwords("./data/custom.stop");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void LoadVocab(String filename)
	{
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;

			while ((line = reader.readLine()) != null) {
				//it is very important that you perform the same processing operation to the loaded stopwords
				//otherwise it won't be matched in the text content
				if (line.isEmpty())continue;
				String[] values=line.split(",");
				m_Vocabs.put(values[1],new Token(Integer.parseInt(values[0]),values[1],Double.parseDouble(values[2])));
			}
			reader.close();

		} catch(IOException e){
			System.err.format("[Error]Failed to open file !!" );
		}
	}

	//sample code for demonstrating how to use Snowball stemmer
	public String SnowballStemmingDemo(String token) {
		SnowballStemmer stemmer = new englishStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}
	//sample code for loading a list of stopwords from file
	//you can manually modify the stopword file to include your newly selected words
	public void LoadStopwords(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;

			while ((line = reader.readLine()) != null) {
				//it is very important that you perform the same processing operation to the loaded stopwords
				//otherwise it won't be matched in the text content
				line = SnowballStemmingDemo(NormalizationDemo(line));
				if (!line.isEmpty())
					m_stopwords.add(line);
			}
			reader.close();
			System.out.format("Loading %d stopwords from %s\n", m_stopwords.size(), filename);
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}
	}
	public void analyzeDocumentDemo(String filename,int core) {		
		try {

			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;

			reader.readLine() ;
			while ((line = reader.readLine()) != null&&!line.isEmpty()) {
				String content=reader.readLine() ;
				reader.readLine() ;
				double score=Double.parseDouble(reader.readLine()) ;
				reader.readLine() ;
				if(score==3)// skip neutral reviews
					continue;

				// process Content
				ArrayList<String> AddedTokens=new ArrayList<String>();
				String previousToken="";
				for(String token:tokenizer.get(core).tokenize(content)){
					String finalToken=SnowballStemmingDemo(NormalizationDemo(token));
					if(!finalToken.isEmpty()) // if the token is empty, then try next token
					{ 

						// add uni-grams and bigrams to the hashmap.
						synchronized(lock1) {
							// unigram
							if(!m_Vocabs.containsKey(finalToken)&&!m_stopwords.contains(finalToken))
								m_Vocabs.put(finalToken, new Token(MaxTokenID++,finalToken));
							// bigram
							if(!previousToken.isEmpty()&&!m_Vocabs.containsKey(previousToken+"-"+finalToken)&&!(m_stopwords.contains(previousToken)&&m_stopwords.contains(finalToken)))
								m_Vocabs.put(previousToken+"-"+finalToken, new Token(MaxTokenID++,previousToken+"-"+finalToken));

							if(m_Vocabs.containsKey(finalToken)	&&!AddedTokens.contains(finalToken) ){
								m_Vocabs.get(finalToken).setValue( m_Vocabs.get(finalToken).getValue()+1);// increase count
								AddedTokens.add(finalToken);}
							// bigram
							if(m_Vocabs.containsKey(previousToken+"-"+finalToken)&&!AddedTokens.contains(finalToken+"-"+finalToken)){
								m_Vocabs.get(previousToken+"-"+finalToken).setValue( m_Vocabs.get(previousToken+"-"+finalToken).getValue()+1);// increase count
								AddedTokens.add(previousToken+"-"+finalToken);
							}
						}
					}
					previousToken=finalToken;
				}
				synchronized(lock2) {
					reviewsCount++;
				}
			}
			reader.close();
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}

	}
	public User LoadUser(String filename)
	{
		try {
			User user=new User();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;
			user.setID(filename.substring(filename.lastIndexOf("\\")+1, filename.length()-4));
			user.setName(reader.readLine());
			while ((line = reader.readLine()) != null&&!line.isEmpty()) {
				Review r=new Review();
				r.setProduct_ID(line);
				r.setContent(reader.readLine());
				r.setUsefulness(reader.readLine());
				r.setScore(Double.parseDouble(reader.readLine()));
				r.setTime( Long.parseLong(reader.readLine()));
				if(r.getScore()==3)// skip neutral reviews
					continue;
				// process Content

				String previousToken="";
				for(String token:tokenizer.get(0).tokenize(r.getContent())){
					String finalToken=SnowballStemmingDemo(NormalizationDemo(token));
					if(!finalToken.isEmpty()) // if the token is empty, then try next token
					{ 

						// add uni-grams and bigrams to the hashmap.

						if(m_Vocabs.containsKey(finalToken)){ 
							String vocabID=finalToken;
							if(!r.m_VSM.containsKey(vocabID))
								r.m_VSM.put(vocabID, 0.0);
							r.m_VSM.put(vocabID,r.m_VSM.get(vocabID)+1);// increase count
						}
						// bigram
						if(m_Vocabs.containsKey(previousToken+"-"+finalToken)){
							String vocabID=previousToken+"-"+finalToken;
							if(!r.m_VSM.containsKey(vocabID))
								r.m_VSM.put(vocabID, 0.0);
							r.m_VSM.put(vocabID,r.m_VSM.get(vocabID)+1);// increase count
						}

					}
					previousToken=finalToken;
				}
				if(r.m_VSM.size()==0)// empty vector .. do not add
					continue;
				// normalize TF (Sub-linear TF scaling) and them multiply by IDF to obtain TF-IDF
				Set<String> set = r.m_VSM.keySet();
				Iterator<String> itr = set.iterator();
				while (itr.hasNext())
				{
					String key = itr.next();
					r.m_VSM.put(key,(1+Math.log10(r.m_VSM.get(key)))*(1+Math.log10(Config.NumberOfReviewsInTraining/m_Vocabs.get(key).getValue())));
				}
				//r.CalculateNorm();
				user.Reviews.add(r);

			}
			reader.close();
			return user;
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
			return null;
		}

	}
	public void analyzeVSM(String filename,int core) {		
		try {
			User user=new User();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;
			user.setID(filename.substring(filename.lastIndexOf("\\")+1, filename.length()-4));
			user.setName(reader.readLine());
			while ((line = reader.readLine()) != null&&!line.isEmpty()) {
				Review r=new Review();
				r.setProduct_ID(line);
				r.setContent(reader.readLine());
				r.setUsefulness(reader.readLine());
				r.setScore(Double.parseDouble(reader.readLine()));
				r.setTime( Long.parseLong(reader.readLine()));
				if(r.getScore()==3)// skip neutral reviews
					continue;
				// process Content

				String previousToken="";
				for(String token:tokenizer.get(core).tokenize(r.getContent())){
					String finalToken=SnowballStemmingDemo(NormalizationDemo(token));
					if(!finalToken.isEmpty()) // if the token is empty, then try next token
					{ 

						// add uni-grams and bigrams to the hashmap.
						synchronized(lock1) {
							if(m_Vocabs.containsKey(finalToken)){ 
								String vocabID=finalToken;
								if(!r.m_VSM.containsKey(vocabID))
									r.m_VSM.put(vocabID, 0.0);
								r.m_VSM.put(vocabID,r.m_VSM.get(vocabID)+1);// increase count
							}
							// bigram
							if(m_Vocabs.containsKey(previousToken+"-"+finalToken)){
								String vocabID=previousToken+"-"+finalToken;
								if(!r.m_VSM.containsKey(vocabID))
									r.m_VSM.put(vocabID, 0.0);
								r.m_VSM.put(vocabID,r.m_VSM.get(vocabID)+1);// increase count
							}
						}
					}
					previousToken=finalToken;
				}
				if(r.m_VSM.size()==0)// empty vector .. do not add
					continue;
				// normalize TF (Sub-linear TF scaling) and them multiply by IDF to obtain TF-IDF
				Set<String> set = r.m_VSM.keySet();
				Iterator<String> itr = set.iterator();
				while (itr.hasNext())
				{
					String key = itr.next();
					r.m_VSM.put(key,(1+Math.log10(r.m_VSM.get(key)))*(1+Math.log10(Config.NumberOfReviewsInTraining/m_Vocabs.get(key).getValue())));
				}
				//r.CalculateNorm();
				user.Reviews.add(r);
				synchronized(lock2) {
					reviewsCount++;
				}
			}
			reader.close();
			synchronized(lock1) {
				Users.add(user);
			}
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}

	}
	public ArrayList<String> GetFiles(String folder, String suffix) {
		File dir = new File(folder);
		ArrayList<String> Files=new ArrayList<String>();
		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(suffix)){
				Files.add(f.getAbsolutePath());
			}
			else if (f.isDirectory())
				Files.addAll(GetFiles(f.getAbsolutePath(), suffix)) ;
		}
		return Files;
	}
	//sample code for demonstrating how to perform text normalization
	public String NormalizationDemo(String token) {
		// convert to lower case
		token = token.toLowerCase();
		//token = token.replaceAll("\\d+star(s)?", "RATE");// rating by stars
		// Some scales and measures
		//token = token.replaceAll("\\d+(oz|lb|lbs|cent|inch|piec)", "SCALE");
		// convert some of the dates/times formats
		//token = token.replaceAll("\\d{2}(:\\d{2})?(\\s)?(a|p)m", "TIME"); // 12 hours format
		//token = token.replaceAll("\\d{2}:\\d{2}", "TIME"); // 24 hours format
		//token = token.replaceAll("\\d{1,2}(th|nd|st|rd)", "DATE");// 1st 2nd 3rd 4th date format
		// convert numbers
		token = token.replaceAll("\\d+.\\d+", "NUM");		
		token = token.replaceAll("\\d+(ish)?", "NUM");
		// tested on "a 123 b 3123 c 235.123 d 0 e 0.3 f 213.231.1321 g +123 h -123.123"
		// remove punctuations
		token = token.replaceAll("\\p{Punct}", ""); 
		//tested on this string:  "This., -/ is #! an <>|~!@#$%^&*()_-+=}{[]\"':;?/>.<, $ % ^ & * example ;: {} of a = -_ string with `~)() punctuation" 
		return token;
	}

	public void RemoveVocabTail()
	{
		Set<String> set = m_Vocabs.keySet();
		Iterator<String> itr = set.iterator();
		while (itr.hasNext())
		{
			String key = itr.next();
			if (m_Vocabs.get(key).getValue()<50)  
				itr.remove(); 
		}
	}
	public void Save(HashMap<String,Token> Map,String Type)
	{
		// Sort
		ArrayList<Token> sortedTokens = new ArrayList<Token>(Map.values());
		Collections.sort(sortedTokens, new Comparator<Token>() {

			public int compare(Token T1, Token T2) {
				return Double.compare(T2.getValue(),T1.getValue());
			}
		});
		// Save to csv file
		try {
			FileWriter fstream = new FileWriter("./"+Type+".csv", false);
			BufferedWriter out = new BufferedWriter(fstream);
			Iterator<Token> iter = sortedTokens.iterator();
			while (iter.hasNext())  
			{
				Token t=iter.next();
				out.write(t.getID()+","+t.getToken() + ","+t.getValue()+"\n");
			}
			out.close();
			System.out.println(Type+" Saved!");
		} catch (Exception e) {
			e.printStackTrace(); 
		}

	}



	public static void BuildVocab(DocAnalyzer analyzer)
	{

		ArrayList<String>Files=analyzer.GetFiles(Config.DataDirPath, ".txt");
		int FilesSize=Files.size();
		HashMap<Integer,String> ProcessingStatus = new HashMap<Integer, String>(); // used for output purposes
		for (int i = 1; i <= 10; i++)
			ProcessingStatus.put((int)(FilesSize * (i / 10d)), i+"0% ("+(int)(FilesSize * (i / 10d))+" out of "+FilesSize+")." );


		ArrayList<Thread> threads = new ArrayList<Thread>();
		for(int i=0;i<Config.NumberOfProcessors;++i){
			threads.add(  (new Thread() {
				int core;
				public void run() {
					try {
						for (int j = 0; j + core <FilesSize; j +=Config.NumberOfProcessors)
						{
							if (ProcessingStatus.containsKey(j + core))
								System.out.println(Config.dateFormat.format(new Date())+" - Loaded " +ProcessingStatus.get(j + core));
							analyzer.analyzeDocumentDemo( Files.get(j+core) ,core);
						}
					} catch (Exception e) {
						e.printStackTrace();
					} 

				}
				private Thread initialize(int core) {
					this.core = core;
					return this;
				}
			}).initialize(i));
			threads.get(i).start();
		}
		for(int i=0;i<Config.NumberOfProcessors;++i){
			try {
				threads.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
		} 
		System.out.println("Loaded all documents!");

		analyzer.RemoveVocabTail(); 
		System.out.println("Vocab size:"+analyzer.m_Vocabs.size());
		System.out.println("# docs:"+analyzer.reviewsCount);
		analyzer.Save(analyzer.m_Vocabs,"Vocab");

	}
	public static void BuildVectorSpaceModel(DocAnalyzer analyzer){



		ArrayList<String>Files=analyzer.GetFiles(Config.DataDirPath, ".txt");
		int FilesSize=Files.size();
		HashMap<Integer,String> ProcessingStatus = new HashMap<Integer, String>(); // used for output purposes
		for (int i = 1; i <= 10; i++)
			ProcessingStatus.put((int)(FilesSize * (i / 10d)), i+"0% ("+(int)(FilesSize * (i / 10d))+" out of "+FilesSize+")." );


		ArrayList<Thread> threads = new ArrayList<Thread>();
		for(int i=0;i<Config.NumberOfProcessors;++i){
			threads.add(  (new Thread() {
				int core;
				public void run() {
					try {
						for (int j = 0; j + core <FilesSize; j +=Config.NumberOfProcessors)
						{
							if (ProcessingStatus.containsKey(j + core))
								System.out.println(Config.dateFormat.format(new Date())+" - Loaded " +ProcessingStatus.get(j + core));
							analyzer.analyzeVSM(  Files.get(j+core) ,core);
						}
					} catch (Exception e) {
						e.printStackTrace();
					} 

				}
				private Thread initialize(int core) {
					this.core = core;
					return this;
				}
			}).initialize(i));
			threads.get(i).start();
		}
		for(int i=0;i<Config.NumberOfProcessors;++i){
			try {
				threads.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
		} 
		System.out.println("Loaded all documents!");
	}
	public static void BuildTestAndSaveGlobalClassifier(DocAnalyzer analyzer,boolean TestClassifier,boolean SaveClassifier){


		// Build global classifier
		// Build Training and Testing Sets
		System.out.println(Config.dateFormat.format(new Date())+" Building Training and testing sets:");
		int TrainingSize=  (int) (Config.NumberOfReviewsInTraining*Config.PercentageOfTraining) ;
		ArrayList<double[]> TrainingSet=new ArrayList<double[]>();
		ArrayList<double[]> TestingSet=new ArrayList<double[]>();

		double[]TrainingTrueLabels=new double[TrainingSize];
		double[]TestingTrueLabels=new double[Config.NumberOfReviewsInTraining-TrainingSize];

		int index=0;
		for(User user:analyzer.Users)
			for(Review review:user.Reviews)
			{
				double[] point=new double[analyzer.m_Vocabs.size()+1]; // size of vector space model + 1 for beta_0
				point[0]=1;
				Set<String> set = analyzer.m_Vocabs.keySet();
				Iterator<String> itr = set.iterator();
				int vocabIndex=1;
				while (itr.hasNext())
					point[vocabIndex++]=review.getValueFromVSM(itr.next());
				if(index<TrainingSize){
					TrainingSet.add(point);
					TrainingTrueLabels[index++]=review.getLabel();
				}
				else
				{ 
					TestingSet.add(point);
					TestingTrueLabels[index-TrainingSize]=review.getLabel();index++;
				}
			}
		System.out.println(Config.dateFormat.format(new Date())+" Training Classifier:");
		// Create Classifier
		LogisticRegressionClassifier Classifier=new LogisticRegressionClassifier(analyzer.m_Vocabs.size()+1, Config.ClassifierThreshold, Config.LearningRate);
		Classifier.Train(TrainingSet, TrainingTrueLabels);
		if(TestClassifier){
			System.out.println(Config.dateFormat.format(new Date())+" Test Classifier:");
			// Test Classifier
			int CorrectClassifications=0;
			if(Config.PercentageOfTraining==1d)// test on training set
			{
				TestingSet=TrainingSet;
				TestingTrueLabels=TrainingTrueLabels;
			}
			for(int i=0;i<TestingSet.size();++i){
				if(Classifier.Classify(TestingSet.get(i),Config.ClassifierThreshold)==TestingTrueLabels[i])
					CorrectClassifications++;
			}

			System.out.println(Config.dateFormat.format(new Date())+" Classification rate for global classifier: "+CorrectClassifications/(double)TestingSet.size());
		}

		// Save Classifier
		if(SaveClassifier)
			Classifier.Save("global");
	}

	public static void main(String[] args) {	

		// Load Config file
		Config.Load();


		DocAnalyzer analyzer = new DocAnalyzer( );
		// Build controlled vocabulary
		//	BuildVocab(analyzer);

		analyzer.LoadVocab("./Vocab.csv");
		// Build Vector Space Model
		// BuildVectorSpaceModel(analyzer);

		// Build and save global classifier
	//	 BuildTestAndSaveGlobalClassifier(analyzer,true,true);

		// Load Classifier 
		 LogisticRegressionClassifier Classifier=new LogisticRegressionClassifier(analyzer.m_Vocabs.size()+1, Config.ClassifierThreshold, Config.LearningRate);
		 Classifier.Load("global");
		
		// Test the global classifier on a new user
		//Load User
		  User user=analyzer.LoadUser("./A1VYFBHW6OHA59.txt");
		// Classify User Reviews
		  user.ClassifyReviews(Classifier, analyzer);
	}



}
