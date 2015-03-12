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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import analyzer.Config;


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
	public void Save(String userID){
		try {
			FileWriter fstream = new FileWriter(Paths.get(Config.VSMDirPath, userID+"_"+Product_ID+"_"+Score+".vsm").toString(), false);
			BufferedWriter out = new BufferedWriter(fstream);
			Set<String> set = m_VSM.keySet();
			Iterator<String> itr = set.iterator();
			out.write(userID+"\n"+Product_ID+"\n"+Score+"\n");
			while (itr.hasNext())
			{
				String key = itr.next();
				out.write(key+","+m_VSM.get(key)+"\n" ) ;
			}

			out.close();
		} catch (Exception e) {
			e.printStackTrace(); 
		}
	}
	public void Load(String FileName){
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream( FileName ), "UTF-8"));
			reader.readLine() ; // userID ... might need it later
			Product_ID=  reader.readLine() ;
			Score=Double.parseDouble( reader.readLine());
			String line;
			while ((line = reader.readLine()) != null) {
				if(!line.isEmpty()){
					String[]vals=line.split(",");
					m_VSM.put(vals[0], Double.parseDouble(vals[1]));
				}
			}
			reader.close();
		} catch(IOException e){
			System.err.format("[Error]Failed to open file !!" );
		}
	}
}
