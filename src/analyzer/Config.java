package analyzer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Config {
	public static String DataDirPath;
	public static int NumberOfProcessors;
	public static void Load()
	{
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("config.txt"), "UTF-8"));
			 DataDirPath=reader.readLine();
			 NumberOfProcessors=Integer.parseInt(reader.readLine());
			reader.close();
			System.out.format("Loaded Config.\n");
		} catch(IOException e){
			System.err.format("[Error]Failed to open config %s!!" );
		}
	}
}
