/**
 * 
 */
package structures;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


public class Review {


	public HashMap<String,Double> m_VSM;
	private double Norm;


	private String Content;
	public void CalculateNorm()
	{
		Norm=0;
		// Calculate the d_j norm
		Set<String> 	set = m_VSM.keySet();
		Iterator<String>  itr = set.iterator();
		while (itr.hasNext())
		{
			String key = itr.next();
			Norm+=m_VSM.get(key)*m_VSM.get(key);
		}
	}
	public boolean isEmpty() {
		return Content==null || Content.isEmpty();
	}


	public Review( ) {
		m_VSM=new HashMap<String, Double>();
	}
	public double getValueFromVSM(String key)
	{
		return m_VSM.containsKey(key)?m_VSM.get(key):0;
	}
	public String getContent() {
		return Content;
	}

	public void setContent(String content) {
		Content = content;
	}
	public double getLabel()
	{
		return (Score==5||Score==4)?1:0;
	}
	public String getProduct_ID() {
		return Product_ID;
	}


	public void setProduct_ID(String product_ID) {
		Product_ID = product_ID;
	}

	public String getUsefulness() {
		return Usefulness;
	}


	public void setUsefulness(String usefulness) {
		Usefulness = usefulness;
	}

	public long getTime() {
		return Time;
	}


	public void setTime(long time) {
		Time = time;
	}

	public double getScore() {
		return Score;
	}


	public void setScore(double score) {
		Score = score;
	}

	public double getNorm() {
		return Norm;
	}


	private String Product_ID;
	private String Usefulness;
	private long Time;
	private double Score;

}
