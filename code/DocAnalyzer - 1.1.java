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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;


import structures.Post;
import structures.Token;
/**
 * @author hongning
 * Sample codes for demonstrating OpenNLP package usage 
 * NOTE: the code here is only for demonstration purpose, 
 * please revise it accordingly to maximize your implementation's efficiency!
 */
public class DocAnalyzer   {
	ArrayList<Tokenizer> tokenizer; // need many because of the threading
	//a list of stopwords
	HashSet<String> m_stopwords;

	//you can store the loaded reviews in this arraylist for further processing
	ArrayList<Post> m_reviews;

	//you might need something like this to store the counting statistics for validating Zipf's and computing IDF
	HashMap<String, Token> m_ttf;	
	HashMap<String, Token> m_df;	
	private Object lock1 = new Object();
	//we have also provided sample implementation of language model in src.structures.LanguageModel

	public DocAnalyzer(int NumberOfProcessors) {
		m_reviews = new ArrayList<Post>();
		m_stopwords= new HashSet<String>();
		m_ttf=new HashMap<String, Token>();
		m_df=new HashMap<String, Token>();
		try {
			//tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream("/cslab/home/ma2sm/hw1/data/Model/en-token.bin")));
			tokenizer=new ArrayList<Tokenizer>();
			for(int i=0;i<NumberOfProcessors;++i)
				tokenizer.add( new TokenizerME(new TokenizerModel(new FileInputStream("./data/Model/en-token.bin"))));

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

	public void analyzeDocumentDemo(JSONObject json,int core) {		
		try {
			JSONArray jarray = json.getJSONArray("Reviews");
			for(int i=0; i<jarray.length(); i++) {
				Post review = new Post(jarray.getJSONObject(i));
				ArrayList<String> AddedTokens=new ArrayList<String>();
				// preprocess each token
				for(String token:tokenizer.get(core).tokenize(review.getContent())){
					String finalToken=SnowballStemmingDemo(NormalizationDemo(token));
					if(finalToken.isEmpty()) // if the token is empty, then try next token
						continue;
					// update the hash table.
					synchronized(lock1) {
						if(!m_ttf.containsKey(finalToken)) // create if it does not exist
							m_ttf.put(finalToken, new Token(finalToken));
						m_ttf.get(finalToken).setValue( m_ttf.get(finalToken).getValue()+1);// increase count
						if(AddedTokens.contains(finalToken))continue;
						if(!m_df.containsKey(finalToken)) // create if it does not exist
							m_df.put(finalToken, new Token(finalToken));
						m_df.get(finalToken).setValue( m_df.get(finalToken).getValue()+1);// increase count
						AddedTokens.add(finalToken);
					}

				}



				m_reviews.add(review);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	//sample code for loading a json file
	public JSONObject LoadJson(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			StringBuffer buffer = new StringBuffer(1024);
			String line;

			while((line=reader.readLine())!=null) {
				buffer.append(line);
			}
			reader.close();

			return new JSONObject(buffer.toString());
		} catch (IOException e) {
			System.err.format("[Error]Failed to open file %s!", filename);
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			System.err.format("[Error]Failed to parse json file %s!", filename);
			e.printStackTrace();
			return null;
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


	//sample code for demonstrating how to use Snowball stemmer
	public String SnowballStemmingDemo(String token) {
		SnowballStemmer stemmer = new englishStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}

	//sample code for demonstrating how to perform text normalization
	public String NormalizationDemo(String token) {
		// convert to lower case
		token = token.toLowerCase();
	//	token = token.replaceAll("\\d+star(s)?", "star");// rating by stars
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


	public void ZipfLow(HashMap<String,Token> Map,String Type,boolean RemoveNumericTokens)
	{
		// Sort
		ArrayList<Token> sortedTokens = new ArrayList<Token>(Map.values());
		Collections.sort(sortedTokens, new Comparator<Token>() {

			public int compare(Token T1, Token T2) {
				return Double.compare(T2.getValue(),T1.getValue());
			}
		});
		// Save to cvs file
		try {
			FileWriter fstream = new FileWriter("./"+Type+".cvs", false);
			BufferedWriter out = new BufferedWriter(fstream);
			Iterator<Token> iter = sortedTokens.iterator();
			int index=1;
			while (iter.hasNext())  
			{
				Token t=iter.next();
				if(RemoveNumericTokens&&t.getToken().contains("NUM"))
					continue;
				out.write(index+++","+t.getToken() + ","+t.getValue()+"\n");
			}
			out.close();
			System.out.println(Type+" Saved!");
		} catch (Exception e) {
			e.printStackTrace(); 
		}

	}
	public static void main(String[] args) {	
		int NumberOfProcessors=8;
		DocAnalyzer analyzer = new DocAnalyzer(NumberOfProcessors);

		//codes for demonstrating tokenization and stemming
		//	analyzer.TokenizerDemon("I've practiced for 30 years in pediatrics, and I've never seen anything quite like this.");

		//codes for loading json file
		//	 analyzer.analyzeDocumentDemo(analyzer.LoadJson("./data/Samples/query.json"));

		//when we want to execute it in command line
		// analyzer.LoadDirectory("./data/Samples", ".json");
		// analyzer.LoadDirectory("/home/hw5x/TextMining/MP1/data/Yelp", ".json");
		// System.out.println(analyzer.NormalizationDemo("5stars 12am 1st 2nd 3th 4th a 123 b 3123 c 235.123 d 0 e 0.3 f 213.231.1321 g +123 h -123.123"));
 
		ArrayList<String>Files=analyzer.GetFiles("./data/samples", ".json");
		int FilesSize=Files.size();
		HashMap<Integer,String> ProcessingStatus = new HashMap<Integer, String>(); // used for output purposes
		for (int i = 1; i <= 10; i++)
			ProcessingStatus.put((int)(FilesSize * (i / 10d)), i+"0% ("+(int)(FilesSize * (i / 10d))+" out of "+FilesSize+")." );


		ArrayList<Thread> threads = new ArrayList<Thread>();
		for(int i=0;i<NumberOfProcessors;++i){
			threads.add(  (new Thread() {
				int core;
				public void run() {
					try {
						for (int j = 0; j + core <FilesSize; j +=NumberOfProcessors)
						{
							if (ProcessingStatus.containsKey(j + core))
								System.out.println("Loaded " +ProcessingStatus.get(j + core));
							analyzer.analyzeDocumentDemo(analyzer.LoadJson(Files.get(j+core)),core);
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
		for(int i=0;i<NumberOfProcessors;++i){
			try {
				threads.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
		} 
		System.out.println("Loaded all documents!");
		System.out.println("Save to cvs:");
		analyzer.ZipfLow(analyzer.m_ttf,"Ttf",false);
		analyzer.ZipfLow(analyzer.m_df,"Df",false); 
		analyzer.ZipfLow(analyzer.m_ttf,"Ttf_NoNum",true);
		analyzer.ZipfLow(analyzer.m_df,"Df_NoNum",true); 
	}



}
