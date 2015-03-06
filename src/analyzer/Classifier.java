package analyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
 

public abstract class Classifier {

	public double[] Parameters;
	public Classifier(int NumberOfParameters)
	{
		Parameters=new double[NumberOfParameters];
	}
	public abstract double Classify(double[] NewInstance);
	public abstract double Classify(double[] NewInstance,double Threshold);
	public abstract void Train(ArrayList<double[]> TrainingSet,double[] TrueLabels);

	public void Save(String FileName){
		try {
			FileWriter fstream = new FileWriter("./"+FileName+".classifer", false);
			BufferedWriter out = new BufferedWriter(fstream);
			String outstr="";
			for(double param:Parameters)  
				outstr+=param+",";
			out.write(outstr.substring(0, outstr.length()-1));
			out.close();
			System.out.println(FileName+" Classifier Saved!");
		} catch (Exception e) {
			e.printStackTrace(); 
		}
	}
	public void Load(String FileName){
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("./"+FileName+".classifer"), "UTF-8"));
			String[] params=reader.readLine().split(",");
			reader.close();
			for(int i=0;i<params.length;++i)
				Parameters[i]=Double.parseDouble(params[i]);
			System.out.println(FileName+" Classifier Loaded!");
		} catch(IOException e){
			System.err.format("[Error]Failed to open file !!" );
		}
	}
}
