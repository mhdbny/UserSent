package analyzer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Config {
	public static String DataDirPath;
	public static int NumberOfProcessors;
	public static int NumberOfReviewsInTraining;
	public static int MaxIterations;
	public static double LearningRate;
	public static double ClassifierThreshold;
	public static double PercentageOfTraining;
	public static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	public static void Load()
	{
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("config.txt"), "UTF-8"));
			reader.readLine(); DataDirPath=reader.readLine();
			reader.readLine(); NumberOfProcessors=Integer.parseInt(reader.readLine());
			reader.readLine(); NumberOfReviewsInTraining=Integer.parseInt(reader.readLine());
			reader.readLine(); MaxIterations=Integer.parseInt(reader.readLine());
			reader.readLine(); LearningRate=Double.parseDouble(reader.readLine());
			reader.readLine(); ClassifierThreshold=Double.parseDouble(reader.readLine());
			reader.readLine(); PercentageOfTraining=Double.parseDouble(reader.readLine());
			reader.close();
			System.out.format("Loaded Config.\n");
		} catch(IOException e){
			System.err.format("[Error]Failed to open config %s!!" );
		}
	}
}
