/**
 * 
 */
package structures;

import java.util.HashMap;
 
 
public class Review {
	 
 
	 public HashMap<String,Double> m_VSM;
	
 

	 private String Content;
	
	public boolean isEmpty() {
		return Content==null || Content.isEmpty();
	}

	  
	public Review( ) {
		 
	}

	public String getContent() {
		return Content;
	}

	public void setContent(String content) {
		Content = content;
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

	private String Product_ID;
	 private String Usefulness;
	 private long Time;
	 private double Score;
	 
}
