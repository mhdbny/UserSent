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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
	HashMap<String, Token> m_initialVocabs;	

	private Object lock1 = new Object();
	private Object lock2 = new Object();
	private int MaxTokenID;
	//we have also provided sample implementation of language model in src.structures.LanguageModel
	private int NumberOfProcessors;
	public DocAnalyzer(int NumberOfProcessors) {
		this.NumberOfProcessors=NumberOfProcessors;
		Users=new ArrayList<User>();
		m_stopwords= new HashSet<String>();
		m_initialVocabs=new HashMap<String, Token>();
		MaxTokenID=0;
		reviewsCount=0;
		try {
			//tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream("/cslab/home/ma2sm/hw1/data/Model/en-token.bin")));
			tokenizer=new ArrayList<Tokenizer>();
			for(int i=0;i<NumberOfProcessors;++i)
				tokenizer.add( new TokenizerME(new TokenizerModel(new FileInputStream("./data/Model/en-token.bin"))));
			// Load Stopwards
			LoadStopwords("./data/english.stop");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
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
	//sample code for demonstrating how to use Snowball stemmer
	public String SnowballStemmingDemo(String token) {
		SnowballStemmer stemmer = new englishStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}
	public void analyzeDocumentDemo(String filename,int core) {		
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
				user.Reviews.add(r);
				// process Content
				ArrayList<String> AddedTokens=new ArrayList<String>();
				String previousToken="";
				for(String token:tokenizer.get(core).tokenize(r.getContent())){
					String finalToken=SnowballStemmingDemo(NormalizationDemo(token));
					if(!finalToken.isEmpty()) // if the token is empty, then try next token
					{ 
						 
						// add uni-grams and bigrams to the hashmap.
						synchronized(lock1) {
							// unigram
							if(!m_initialVocabs.containsKey(finalToken)&&!m_stopwords.contains(finalToken))
								m_initialVocabs.put(finalToken, new Token(MaxTokenID++,finalToken));
							// bigram
							if(!previousToken.isEmpty()&&!m_initialVocabs.containsKey(previousToken+"-"+finalToken)&&!(m_stopwords.contains(previousToken)&&m_stopwords.contains(finalToken)))
								m_initialVocabs.put(previousToken+"-"+finalToken, new Token(MaxTokenID++,previousToken+"-"+finalToken));

							if(m_initialVocabs.containsKey(finalToken)	&&!AddedTokens.contains(finalToken) ){
								m_initialVocabs.get(finalToken).setValue( m_initialVocabs.get(finalToken).getValue()+1);// increase count
								AddedTokens.add(finalToken);}
							// bigram
							if(m_initialVocabs.containsKey(previousToken+"-"+finalToken)&&!AddedTokens.contains(finalToken+"-"+finalToken)){
								m_initialVocabs.get(previousToken+"-"+finalToken).setValue( m_initialVocabs.get(previousToken+"-"+finalToken).getValue()+1);// increase count
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
	public void FindNewStopWords()
	{
		// Sort
		ArrayList<Token> sortedTokens = new ArrayList<Token>(m_initialVocabs.values());
		Collections.sort(sortedTokens, new Comparator<Token>() {

			public int compare(Token T1, Token T2) {
				return Double.compare(T2.getValue(),T1.getValue());
			}
		});
		ArrayList<String> newStopWords=new ArrayList<String>();
		//  Get first 100
		for	(int i=0;i<100;++i)
			if(!m_stopwords.contains(sortedTokens.get(i).getToken()))
				newStopWords.add(sortedTokens.get(i).getToken());
		// Save newStopWords and merge them with the initial ones
		try {
			FileWriter fstream = new FileWriter("./newStop.txt", false);
			BufferedWriter out = new BufferedWriter(fstream);
			for(String newWord:newStopWords){
				out.write(newWord+"\n");
				m_stopwords.add(newWord);
			}
			out.close();
			System.out.println("New stop words are saved!");
		} catch (Exception e) {
			e.printStackTrace(); 
		}
	}
	public void RemoveVocabTailAndStop()
	{
		Set<String> set = m_initialVocabs.keySet();
		Iterator<String> itr = set.iterator();
		while (itr.hasNext())
		{
			String key = itr.next();
			if (m_initialVocabs.get(key).getValue()<50)  
				itr.remove(); 
			// check for new stop words
			else if(m_stopwords.contains(key) )// unigram
				itr.remove();
			else if(key.contains("-")){
				boolean foundinStop=true;
				for(String subkey:key.split("-"))
					if(!m_stopwords.contains(subkey))
						foundinStop=false;
				if(foundinStop)
					itr.remove();
			}
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

	public void SaveTopBottomN(int N)
	{
		// Sort
		ArrayList<Token> sortedTokens = new ArrayList<Token>(m_initialVocabs.values());
		Collections.sort(sortedTokens, new Comparator<Token>() {

			public int compare(Token T1, Token T2) {
				return Double.compare(T2.getValue(),T1.getValue());
			}
		});
		// Save Top N and Bottom N to csv files
		try {
			// Top N
			FileWriter fstream = new FileWriter("./Top"+N+".csv", false);
			BufferedWriter out = new BufferedWriter(fstream);

			int index=1;
			for(int i=0;i<N&&i<sortedTokens.size();++i) 
				out.write(index+++","+sortedTokens.get(i).getToken() + ","+(1+Math.log10(reviewsCount/sortedTokens.get(i).getValue()))+"\n");
			out.close();
			System.out.println("Top"+N+" Saved!");
			// Bottom N
			fstream = new FileWriter("./Bottom"+N+".csv", false);
			out = new BufferedWriter(fstream);

			index=1;
			for(int i=sortedTokens.size()-N;i<sortedTokens.size();++i)
				out.write(index+++","+sortedTokens.get(i).getToken() + ","+(1+Math.log10(reviewsCount/sortedTokens.get(i).getValue()))+"\n");
			out.close();
			System.out.println("Bottom"+N+" Saved!");

		} catch (Exception e) {
			e.printStackTrace(); 
		}

	}
	public static void main(String[] args) {	
		// Load Config file
		Config.Load();


		DocAnalyzer analyzer = new DocAnalyzer(Config.NumberOfProcessors);
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
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
								System.out.println(dateFormat.format(new Date())+" - Loaded " +ProcessingStatus.get(j + core));
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
		// analyzer.CaculateDF();
		 	System.out.println("Fine new stop words:");
		analyzer.FindNewStopWords();
		System.out.println("Remove tail:");
		analyzer.RemoveVocabTailAndStop(); 
		System.out.println("Vocab size:"+analyzer.m_initialVocabs.size());
		System.out.println("# docs:"+analyzer.reviewsCount);
		analyzer.Save(analyzer.m_initialVocabs,"init");
		analyzer.SaveTopBottomN(50); 
/*
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("./init.csv"), "UTF-8"));
			String line;

			while ((line = reader.readLine()) != null) {
				//it is very important that you perform the same processing operation to the loaded stopwords
				//otherwise it won't be matched in the text content
				if (line.isEmpty())continue;
				String[] values=line.split(",");
				analyzer.m_initialVocabs.put(values[1],new Token(Integer.parseInt(values[0]),values[1],Double.parseDouble(values[2])));
			}
			reader.close();

		} catch(IOException e){
			System.err.format("[Error]Failed to open file !!" );
		}
		System.out.format("vocab %d  \n", analyzer.m_initialVocabs.size() );
		analyzer.RemoveVocabTailAndStop(); 
		analyzer.Save(analyzer.m_initialVocabs,"init2"); */
	}



}
