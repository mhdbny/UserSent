package structures;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

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
	public void ClassifyReviews(Classifier classifier,DocAnalyzer  analyzer  ){
		// Build testing set
		ArrayList<double[]> TestingSet=new ArrayList<double[]>();
		double[]TestingTrueLabels=new double[Reviews.size()];
		int index=0;
		for(Review review: Reviews)
		{
			double[] point=new double[analyzer.m_Vocabs.size()+1]; // size of vector space model + 1 for beta_0
			point[0]=1;
			Set<String> set = analyzer.m_Vocabs.keySet();
			Iterator<String> itr = set.iterator();
			int vocabIndex=1;
			while (itr.hasNext())
				point[vocabIndex++]=review.getValueFromVSM(itr.next());

			TestingSet.add(point);
			TestingTrueLabels[index++]=review.getLabel();
		}
		// Run Classifier
		int CorrectClassifications=0;
		for(int i=0;i<TestingSet.size();++i){
			if(classifier.Classify(TestingSet.get(i),Config.ClassifierThreshold)==TestingTrueLabels[i])
				CorrectClassifications++;
		}

		System.out.println(Config.dateFormat.format(new Date())+" Classification rate for global classifier: "+CorrectClassifications/(double)Reviews.size());
	}
}
