/**
 * 
 */
package structures;

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
	double m_lambda=0.9; // parameter for linear interpolation smoothing
	double m_delta=0.1; // parameter for absolute discount smoothing
	
	public LanguageModel(int N) {
		m_N = N;
		m_model = new HashMap<String, Token>();
	}
	
	public double calcMLProb(String token) {
		  return m_model.containsKey(token)? m_model.get(token).getValue()/(m_reference==null?(double)TotalNumberOfWords:m_reference.m_model.get(token.split("-")[0]).getValue()):0; // should be something like this
		 
	}

	public double calcLinearSmoothedProb(String token) {
		if (m_N>1) // To make this condition work for m_N>1, we need to have an array of lambdas that sums up to one. Since in the assignment we have only bigrams, I will keep lambda as a scalar.
			return m_lambda  * calcMLProb(token) +  (1.0-m_lambda)* m_reference.calcLinearSmoothedProb(token.split("-")[0]);
		else
			return ((m_model.containsKey(token)? m_model.get(token).getValue():0)+1)/(double)(TotalNumberOfWords+m_model.size()); 
	}
	public double calcAbsoluteDiscountSmoothedProb(String token) {
		if (m_N>1) 
			return (Math.max((m_model.containsKey(token)? m_model.get(token).getValue():0)-m_delta, 0)+m_delta*m_reference.m_model.size())/(double)m_reference.m_model.get(token.split("-")[0]).getValue();
		else
			return ((m_model.containsKey(token)? m_model.get(token).getValue():0)+1)/(double)(TotalNumberOfWords+m_model.size()); 
	}
}
