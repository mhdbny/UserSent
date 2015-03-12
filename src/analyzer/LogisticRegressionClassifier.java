package analyzer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class LogisticRegressionClassifier extends Classifier {
	 
	protected double LearningRate;
	public LogisticRegressionClassifier(int NumberOfParameters, double LearningRate,DocAnalyzer analyzer) {
		super(NumberOfParameters,analyzer);
		 
		this.setLearningRate(LearningRate);
	}
	@Override
	public double Classify(int NewInstance) {
		double value=0;
		Set<String> set = analyzer.m_Vocabs.keySet();
		Iterator<String> itr = set.iterator();
		for (int i=0;i<=analyzer.m_Vocabs.size();++i)
			value+=Parameters[i]*(i==0?1:analyzer.Reviews.get(NewInstance).getValueFromVSM(itr.next()));
		return 1/(double)(1+Math.exp(-1*value)); // sigmod function
	}
	@Override
	public double Classify(int NewInstance, double Threshold) {
		return Classify(NewInstance)>Threshold?1:0;
	}
	@Override
	public void Train(ArrayList<Integer> TrainingSet,double[] TrueLabels) {
		// Minimize the cost function using Gradient Descent
		for(int i=0;i<Config.MaxIterations;++i){
			System.out.println(Config.dateFormat.format(new Date())+" Current Iteration:"+i);
			// Calculate error
			double[] errors=new double[TrainingSet.size()];
			for(int j=0;j<TrainingSet.size();++j)
				errors[j]=Classify(TrainingSet.get(j))-TrueLabels[j];
			// Update parameters
			Set<String> set = analyzer.m_Vocabs.keySet();
			Iterator<String> itr = set.iterator();
			for(int j=0;j<=analyzer.m_Vocabs.size();++j){
				double update=0;
				String CurrectTerm=j==0?"":itr.next();
				for(int k=0;k<TrainingSet.size();++k)
					update+=errors[k]*(j==0?1:analyzer.Reviews.get(TrainingSet.get(k)).getValueFromVSM(CurrectTerm));
				Parameters[j]-=LearningRate*update/(double)TrainingSet.size();
			}
		}
	}

 
	public double getLearningRate() {
		return LearningRate;
	}
	public void setLearningRate(double learningRate) {
		LearningRate = learningRate;
	}


}
