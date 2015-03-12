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
import java.util.Random;
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
	//ArrayList<User> Users;
	ArrayList<Review> Reviews;
	ArrayList<Tokenizer> tokenizer; // need many because of the threading
	//a list of stopwords
	HashSet<String> m_stopwords;
	Random RandomGen;
	//you can store the loaded reviews in this arraylist for further processing
	//ArrayList<Post> m_reviews;
	int reviewsCount ;
	//you might need something like this to store the counting statistics for validating Zipf's and computing IDF
	public HashMap<String, Token> m_Vocabs;	
	public int TotalPos;
	public int TotalNeg;
	private Object lock1 = new Object();
	private Object lock2 = new Object();
	private int MaxTokenID;
	//we have also provided sample implementation of language model in src.structures.LanguageModel

	public DocAnalyzer( ) {
		RandomGen=new Random();
		TotalPos=0;
		TotalNeg=0;
		//	Users=new ArrayList<User>();
		Reviews=new ArrayList<Review>();
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
				//String previousToken="";
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
							//	if(!previousToken.isEmpty()&&!m_Vocabs.containsKey(previousToken+"-"+finalToken)&&!(m_stopwords.contains(previousToken)&&m_stopwords.contains(finalToken)))
							//	m_Vocabs.put(previousToken+"-"+finalToken, new Token(MaxTokenID++,previousToken+"-"+finalToken));

							if(m_Vocabs.containsKey(finalToken)	&&!AddedTokens.contains(finalToken) ){
								m_Vocabs.get(finalToken).setValue( m_Vocabs.get(finalToken).getValue()+1);// increase count
								AddedTokens.add(finalToken);}
							// bigram
							//	if(m_Vocabs.containsKey(previousToken+"-"+finalToken)&&!AddedTokens.contains(finalToken+"-"+finalToken)){
							//		m_Vocabs.get(previousToken+"-"+finalToken).setValue( m_Vocabs.get(previousToken+"-"+finalToken).getValue()+1);// increase count
							//		AddedTokens.add(previousToken+"-"+finalToken);
							//	}
						}
					}
					//	previousToken=finalToken;
				}
				synchronized(lock2) {
					reviewsCount++;
					if(score>3)
						TotalPos++;
					else
						TotalNeg++;
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

				//String previousToken="";
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
						/*if(m_Vocabs.containsKey(previousToken+"-"+finalToken)){
							String vocabID=previousToken+"-"+finalToken;
							if(!r.m_VSM.containsKey(vocabID))
								r.m_VSM.put(vocabID, 0.0);
							r.m_VSM.put(vocabID,r.m_VSM.get(vocabID)+1);// increase count
						}*/

					}
					//previousToken=finalToken;
				}
				if(r.m_VSM.size()==0)// empty vector .. do not add
					continue;
				reviewsCount++;

				//r.CalculateNorm();
				user.Reviews.add(r);

			}
			// normalize TF (Sub-linear TF scaling) and them multiply by IDF to obtain TF-IDF
			for(Review r:user.Reviews){
				Set<String> set = r.m_VSM.keySet();
				Iterator<String> itr = set.iterator();
				while (itr.hasNext())
				{
					String key = itr.next();
					r.m_VSM.put(key,(1+Math.log10(r.m_VSM.get(key)))*(1+Math.log10(reviewsCount/m_Vocabs.get(key).getValue())));
				}
			}
			reader.close();
			return user;
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
			return null;
		}

	}
	public void analyzeVSM(String filename,int core,Boolean save) {		
		try {
			//User user=new User();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;
			//user.setID(filename.substring(filename.lastIndexOf("\\")+1, filename.length()-4));
			//user.setName(reader.readLine());
			reader.readLine();
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

				//String previousToken="";
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
							/*if(m_Vocabs.containsKey(previousToken+"-"+finalToken)){
								String vocabID=previousToken+"-"+finalToken;
								if(!r.m_VSM.containsKey(vocabID))
									r.m_VSM.put(vocabID, 0.0);
								r.m_VSM.put(vocabID,r.m_VSM.get(vocabID)+1);// increase count
							}*/
						}
					}
					//	previousToken=finalToken;
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
				//user.Reviews.add(r);
				synchronized(lock2) {
					reviewsCount++;
					Reviews.add(r);
					if(r.getScore()>3)
						TotalPos++;
					else
						TotalNeg++;
					// Save review
					if(save)
						r.Save(filename.substring(filename.lastIndexOf("\\")+1, filename.length()-4));
				}
			}
			reader.close();
			//	synchronized(lock1) {
			//Users.add(user);

			//}
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


	public static void LoadVSM(DocAnalyzer analyzer)
	{

		ArrayList<String>Files=analyzer.GetFiles(Config.VSMDirPath, ".vsm");
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
							analyzer.LoadReview(Files.get(j+core));
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


	}
	protected void LoadReview(String Filename) {
		Review r=new Review();
		r.Load(Filename); 
		synchronized(lock2) {
			Reviews.add(r);
			reviewsCount++;
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
		System.out.println("# pos:"+analyzer.TotalPos);
		System.out.println("# neg:"+analyzer.TotalNeg);
		analyzer.Save(analyzer.m_Vocabs,"Vocab");

	}
	public static void BuildAndSaveVectorSpaceModel(DocAnalyzer analyzer,Boolean save){



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
							analyzer.analyzeVSM(  Files.get(j+core) ,core,save);
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
	public static void VerifySampleDist(double[]Sample)
	{
		// calculate percentage
		int Label1Count=0;
		for(int i=0;i<Sample.length;++i)
			if(Sample[i]==1.0d)
				Label1Count++;
		// Check with expected Sample and report
		double expectedPosPer=Config.NumberOfPostiveReviewsInTraining/(double)Config.NumberOfReviewsInTraining;
		int expectedPosNum=(int)(expectedPosPer*Sample.length);
		System.out.println("Expected: "+expectedPosPer+" positive ("+expectedPosNum+") and "+(1-expectedPosPer)+" negative ("+(Sample.length-expectedPosNum)+").");
		double actualPosPer=Label1Count/(double)Sample.length;
		System.out.println("Actual: "+actualPosPer+" positive ("+Label1Count+") and "+(1-actualPosPer)+" negative ("+(Sample.length-Label1Count)+").");
	}
	public static void BuildTestAndSaveGlobalClassifier(DocAnalyzer analyzer,boolean TestClassifier,boolean SaveClassifier){


		// Build global classifier
		// Build Training and Testing Sets
		System.out.println(Config.dateFormat.format(new Date())+" Building Training and testing sets:");

		int TrainingPostiveSize=  (int) (analyzer.TotalPos*Config.PercentageOfTraining);
		int TrainingNegativeSize=  (int) (analyzer.TotalNeg*Config.PercentageOfTraining);
		int TrainingSize=TrainingPostiveSize+TrainingNegativeSize;
		ArrayList<Integer> TrainingSet=new ArrayList<Integer>();
		ArrayList<Integer> TestingSet=new ArrayList<Integer>();

		double[]TrainingTrueLabels=new double[TrainingSize];
		double[]TestingTrueLabels=new double[analyzer.reviewsCount-TrainingSize];
		// Generate Random training and testing samples that has the same pos/neg percentage as the overall set
		for(int i=0;i<TrainingSize;++i){
			int NextItem=(int)(analyzer.RandomGen.nextDouble()*analyzer.reviewsCount);
			while(TrainingSet.contains(NextItem)||(i<TrainingPostiveSize&&analyzer.Reviews.get(NextItem).getScore()<3)||(i>=TrainingPostiveSize&&analyzer.Reviews.get(NextItem).getScore()>3)) // if already added or not positive (when adding positive) or not negative (when adding negative), then choose another review
				NextItem=(int)(analyzer.RandomGen.nextDouble()*analyzer.reviewsCount);
			TrainingSet.add(NextItem);
			TrainingTrueLabels[i]=analyzer.Reviews.get(i).getLabel();
		}
		// Add the remaining to test
		if(Config.PercentageOfTraining==1.0d)//Use all for testing, copy training to testing
		{
			TestingTrueLabels=new double[analyzer.reviewsCount];
			for(int i=0;i<analyzer.reviewsCount;++i)
			{TestingSet.add(TrainingSet.get(i));TestingTrueLabels[i]=TrainingTrueLabels[i];}
		}
		else{
			int index=0;
			for(int i=0;i<analyzer.reviewsCount;++i)
				if(!TrainingSet.contains(i))
				{TestingSet.add(i);TestingTrueLabels[index++]=analyzer.Reviews.get(i).getLabel();}
		}
		// Verify distribution of both training and testing sample
		VerifySampleDist(TrainingTrueLabels);
		VerifySampleDist(TestingTrueLabels);

		System.out.println(Config.dateFormat.format(new Date())+" Training Classifier:");
		// Create Classifier
		LogisticRegressionClassifier Classifier=new LogisticRegressionClassifier(analyzer.m_Vocabs.size()+1,   Config.LearningRate,analyzer);
		Classifier.Train(TrainingSet, TrainingTrueLabels);
		if(TestClassifier){
			System.out.println(Config.dateFormat.format(new Date())+" Test Classifier:");
			// Test Classifier
			double maxF=-1,maxThreshold=0;
			for(double threshold=Config.ClassifierThreshold==-1?0.5:Config.ClassifierThreshold;threshold<(Config.ClassifierThreshold==-1?1d:(Config.ClassifierThreshold+0.001));threshold+=0.005){
				int CorrectClassifications=0;
				int PosClassified=0,PosCorrectClassified=0;
				int NegClassified=0,NegCorrectClassified=0;
				System.out.println(Config.dateFormat.format(new Date())+" Threshold: "+threshold);
				for(int i=0;i<TestingSet.size();++i){
					double label=Classifier.Classify(TestingSet.get(i),threshold);
					if(label==TestingTrueLabels[i])
						CorrectClassifications++;
					if(label==1.0d)PosClassified++;else NegClassified++;
					if(label==1.0d&label==TestingTrueLabels[i])PosCorrectClassified++; 
					if(label==0d&label==TestingTrueLabels[i])NegCorrectClassified++; 
				}
				double posPre=PosCorrectClassified/(double)PosClassified;
				double posRec=PosCorrectClassified/(double)(Config.PercentageOfTraining==1d?analyzer.TotalPos:analyzer.TotalPos*(1d-Config.PercentageOfTraining));
				double posF=2*posPre*posRec/(posPre+posRec);
				double negPre=NegCorrectClassified/(double)NegClassified;
				double negRec=NegCorrectClassified/(double)(Config.PercentageOfTraining==1d?analyzer.TotalNeg:analyzer.TotalNeg*(1d-Config.PercentageOfTraining));
				double negF=2*negPre*negRec/(negPre+negRec);
				if((negF*posF)>maxF){
					maxF=negF*posF;maxThreshold=threshold;
					System.out.println(Config.dateFormat.format(new Date())+" Classification rate for global classifier: "+CorrectClassifications/(double)TestingSet.size());
					System.out.println(Config.dateFormat.format(new Date())+" Positive precision: "+posPre);
					System.out.println(Config.dateFormat.format(new Date())+" Positive recall: "+posRec);
					System.out.println(Config.dateFormat.format(new Date())+" Positive FMeasure: "+posF);
					System.out.println(Config.dateFormat.format(new Date())+" Negative precision: "+negPre);
					System.out.println(Config.dateFormat.format(new Date())+" Negative recall: "+negRec);
					System.out.println(Config.dateFormat.format(new Date())+" Negative FMeasure: "+negF);
				}
			}
			System.out.println(Config.dateFormat.format(new Date())+" Max FMeasure: "+maxF+" for threshold: "+maxThreshold);
		}

		// Save Classifier
		if(SaveClassifier)
			Classifier.Save("global");
	}
	public static void SaveClassifierPerformance(ArrayList<double[]> PerformanceArray,String FileName)
	{
		try {
			FileWriter fstream = new FileWriter("./"+FileName+".performance", false);
			BufferedWriter out = new BufferedWriter(fstream);
			for(int i=0;i<PerformanceArray.size();++i){
				String outstr="";
				for(double param:PerformanceArray.get(i))  
					outstr+=param+",";
				out.write(outstr.substring(0, outstr.length()-1)+"\n");
			}
			out.close();
			System.out.println(FileName+" Classifier Performance Saved!");
		} catch (Exception e) {
			e.printStackTrace(); 
		}
	}
	public static void main(String[] args) {	


		// Load Config file
		Config.Load();


		DocAnalyzer analyzer = new DocAnalyzer( );
		// Build controlled vocabulary
		// BuildVocab(analyzer);

		analyzer.LoadVocab("./Vocab.csv");
		// Build Vector Space Model
		// BuildAndSaveVectorSpaceModel(analyzer,false);
		//	LoadVSM(analyzer);
		// Build and save global classifier
		//  BuildTestAndSaveGlobalClassifier(analyzer,true,true);

		// Load Classifier 
		 LogisticRegressionClassifier Classifier=new LogisticRegressionClassifier(analyzer.m_Vocabs.size()+1,  Config.LearningRate,analyzer);
		 Classifier.Load("global");

		// Test the global classifier on a new user
		//Load User
		User user=analyzer.LoadUser("./A1VYFBHW6OHA59.txt");
		for(Review r:user.Reviews)
		{ 
			analyzer.Reviews.add(r);
			if(r.getScore()>3)analyzer.TotalPos++;
			else if(r.getScore()<3)analyzer.TotalNeg++;
			analyzer.reviewsCount++;
		}
		// Classify all User Reviews using global
		//user.ClassifyReviews(Classifier, analyzer,user.Reviews.size(),true);

		// Build User-personalized classifier
		TransformedLogisticRegressionClassifier PersonalizedClassifier=new TransformedLogisticRegressionClassifier(analyzer.m_Vocabs.size()+1 ,Config.LearningRate,analyzer, user, Config.AdaptationLambda , Config.AdaptationSigma) ;
		PersonalizedClassifier.Load("global");
		ArrayList<Integer>TrainingSet=new ArrayList<Integer>();
		double[] TrueLabels=new double[user.Reviews.size()];
		ArrayList<double[]>GlobalClassifierPerformance=new ArrayList<double[]>();
		ArrayList<double[]>PersonalizedClassifierPerformance=new ArrayList<double[]>();
		// online training
		for(int i=0;i<user.Reviews.size();++i){
			TrainingSet.add(i);
			// Get labels
			TrueLabels[i]=user.Reviews.get(i).getLabel();
			PersonalizedClassifier.InitParameters();
			PersonalizedClassifier.Train(TrainingSet, TrueLabels);
			// Test global classifier vs personalized
			GlobalClassifierPerformance.add(user.ClassifyReviews(Classifier, analyzer,i,false));
			PersonalizedClassifierPerformance.add(user.ClassifyReviews(PersonalizedClassifier, analyzer,i,false));
		}
		// Save Performance report
	 	SaveClassifierPerformance(GlobalClassifierPerformance,"global");
		SaveClassifierPerformance(PersonalizedClassifierPerformance,"personalized");



	}



}
