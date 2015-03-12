package structures;

import java.util.ArrayList;
import java.util.Date;



import analyzer.Classifier;
import analyzer.Config;
import analyzer.DocAnalyzer;

public class User {
	public ArrayList<Review> Reviews=new ArrayList<Review>();
	private String ID;
	private String Name;
	public String getID() {
		return ID;
	}
	public void setID(String iD) {
		ID = iD;
	}
	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public double[] ClassifyReviews(Classifier classifier,DocAnalyzer  analyzer,int NumberOfReviewsToClassify,Boolean PrintOutput  ){
		// Build testing set
		ArrayList<Integer> TestingSet=new ArrayList<Integer>();
		double[]TestingTrueLabels=new double[Reviews.size()];
		for(int i=0;i<NumberOfReviewsToClassify;++i)
		{
			TestingSet.add(i);
			TestingTrueLabels[i]=Reviews.get(i).getLabel();
		}
		// Run Classifier
		double maxF=-1,maxThreshold=0;
		double[] Mesures=new double[8];
		for(double threshold=Config.ClassifierThreshold==-1?0.5:Config.ClassifierThreshold;threshold<(Config.ClassifierThreshold==-1?1d:(Config.ClassifierThreshold+0.001));threshold+=0.005){
			int CorrectClassifications=0;
			int PosClassified=0,PosCorrectClassified=0;
			int NegClassified=0,NegCorrectClassified=0;
			if(PrintOutput)
				System.out.println(Config.dateFormat.format(new Date())+" Threshold: "+threshold);
			for(int i=0;i<TestingSet.size();++i){
				double label=classifier.Classify(TestingSet.get(i),threshold);
				if(label==TestingTrueLabels[i])
					CorrectClassifications++;
				if(label==1.0d)PosClassified++;else NegClassified++;
				if(label==1.0d&label==TestingTrueLabels[i])PosCorrectClassified++; 
				if(label==0d&label==TestingTrueLabels[i])NegCorrectClassified++; 
			}
			double posPre=PosCorrectClassified/(double)PosClassified;
			double posRec=PosCorrectClassified/(double) analyzer.TotalPos ;
			double posF=2*posPre*posRec/(posPre+posRec);
			double negPre=NegCorrectClassified/(double)NegClassified;
			double negRec=NegCorrectClassified/(double) analyzer.TotalNeg ;
			double negF=2*negPre*negRec/(negPre+negRec);
			if((negF*posF)>maxF){
				maxF=negF*posF;maxThreshold=threshold;
				Mesures[0]=posPre;
				Mesures[1]=posRec;
				Mesures[2]=posF;
				Mesures[3]=negPre;
				Mesures[4]=negRec;
				Mesures[5]=negF;
				Mesures[6]=CorrectClassifications/(double)TestingSet.size();
				Mesures[7]=threshold;
				if(PrintOutput){
					System.out.println(Config.dateFormat.format(new Date())+" Classification rate for global classifier: "+Mesures[6]);
					System.out.println(Config.dateFormat.format(new Date())+" Positive precision: "+posPre);
					System.out.println(Config.dateFormat.format(new Date())+" Positive recall: "+posRec);
					System.out.println(Config.dateFormat.format(new Date())+" Positive FMeasure: "+posF);
					System.out.println(Config.dateFormat.format(new Date())+" Negative precision: "+negPre);
					System.out.println(Config.dateFormat.format(new Date())+" Negative recall: "+negRec);
					System.out.println(Config.dateFormat.format(new Date())+" Negative FMeasure: "+negF);
				}
			}
		}
		if(PrintOutput) 
			System.out.println(Config.dateFormat.format(new Date())+" Max FMeasure: "+maxF+" for threshold: "+maxThreshold);
		return Mesures;
	}
}
