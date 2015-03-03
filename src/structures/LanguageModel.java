/**
 * 
 */
package structures;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author hongning
 * Suggested structure for constructing N-gram language model
 */
public class LanguageModel {

	int m_N; // N-gram
	public HashMap<String, Token> m_model; // sparse structure for storing the maximum likelihood estimation of LM with the seen N-grams
	public LanguageModel m_reference; // pointer to the reference language model for smoothing purpose
	public int TotalNumberOfWords;
	public double m_lambda=0.9; // parameter for linear interpolation smoothing
	public double m_delta=0.1; // parameter for absolute discount smoothing
	public HashMap<String, Integer> m_S;
	public LanguageModel(int N) {
		m_N = N;
		m_model = new HashMap<String, Token>();
		m_S=new HashMap<String, Integer>();

	}
	public void FillS()
	{
		HashMap<String,ArrayList<String>> Added=new HashMap<String,ArrayList<String>>();
		for(Token t:m_model.values())
		{
			String[] tokens=t.getToken().split("-");
			if(!Added.containsKey(tokens[0]))
				Added.put(tokens[0], new ArrayList<String>());
			if(!m_S.containsKey(tokens[0]))
				m_S.put(tokens[0], 0);
			if(!Added.get(tokens[0]).contains(tokens[1])){
				m_S.put(tokens[0], m_S.get(tokens[0])+1);
				Added.get(tokens[0]).add(tokens[1]);
			}
		}
	}
	public double calcMLProb(String token) {
		return m_model.containsKey(token)? m_model.get(token).getValue()/(m_reference.m_model.get(token.split("-")[0]).getValue()):0; // should be something like this

	}

	public double calcLinearSmoothedProb(String token) {
		if (m_N>1) // To make this condition work for m_N>1, we need to have an array of lambdas that sums up to one. Since in the assignment we have only bigrams, I will keep lambda as a scalar.
			return m_lambda  * calcMLProb(token) +  (1.0-m_lambda)* m_reference.calcLinearSmoothedProb(token.split("-")[1]) ;
		else
			return  ((m_model.containsKey(token)? m_model.get(token).getValue():0)+0.1)/(double)(TotalNumberOfWords+0.1*m_model.size()); 
	}

	public double calcAbsoluteDiscountSmoothedProb(String token) {
		if (m_N>1) 
			return m_reference.m_model.containsKey(token.split("-")[0])? (Math.max((m_model.containsKey(token)? m_model.get(token).getValue():0)-m_delta, 0)+m_delta*m_S.get(token.split("-")[0])*m_reference.calcAbsoluteDiscountSmoothedProb(token.split("-")[1]))/(double)m_reference.m_model.get(token.split("-")[0]).getValue():m_reference.calcAbsoluteDiscountSmoothedProb(token.split("-")[1]);
		else
			return  ((m_model.containsKey(token)? m_model.get(token).getValue():0)+0.1)/(double)( TotalNumberOfWords+0.1*m_model.size()); 
	}
	public void Save(String FileName)
	{
		try {
			FileWriter fstream = new FileWriter("./"+FileName+".csv", false);
			BufferedWriter out = new BufferedWriter(fstream);

			for (String token:m_model.keySet())  
			{
				Token t=m_model.get(token);
				out.write(t.getID()+","+t.getToken() + ","+t.getValue()+"\n");
			}
			out.close();
			System.out.println(FileName+" Saved!");
		} catch (Exception e) {
			e.printStackTrace(); 
		}
	}
	public void Load(String FileName) {
		try {
			TotalNumberOfWords=0;
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("./"+FileName+".csv"), "UTF-8"));
			String line;
			HashMap<String,ArrayList<String>> Added=new HashMap<String,ArrayList<String>>();
			while ((line = reader.readLine()) != null) {
				//it is very important that you perform the same processing operation to the loaded stopwords
				//otherwise it won't be matched in the text content
				if (line.isEmpty())continue;
				String[] values=line.split(",");
				m_model.put(values[1],new Token(Integer.parseInt(values[0]),values[1],Double.parseDouble(values[2])));
				TotalNumberOfWords+=(int)Double.parseDouble(values[2]);
				if(m_N>1)
				{
					String[] tokens=values[1].split("-");
					if(!Added.containsKey(tokens[0]))
						Added.put(tokens[0], new ArrayList<String>());
					if(!m_S.containsKey(tokens[0]))
						m_S.put(tokens[0], 0);
					if(!Added.get(tokens[0]).contains(tokens[1])){
						m_S.put(tokens[0], m_S.get(tokens[0])+1);
						Added.get(tokens[0]).add(tokens[1]);
					}
				}
			}
			reader.close();

		} catch(IOException e){
			System.err.format("[Error]Failed to open file !!" );
		}
	}
}
