package analyzer;

import java.util.ArrayList;
import java.util.Date;

public class LogisticRegressionClassifier extends Classifier {
	private double ClassifingThreshold;
	private double LearningRate;
	public LogisticRegressionClassifier(int NumberOfParameters,double ClassifingThreshold,double LearningRate) {
		super(NumberOfParameters);
		this.ClassifingThreshold=ClassifingThreshold;
		this.setLearningRate(LearningRate);
	}
	@Override
	public double Classify(double[] NewInstance) {
		double value=0;
		for (int i=0;i< Parameters.length;++i)
			value+=Parameters[i]*NewInstance[i];
		return 1/(double)(1+Math.exp(-1*value)); // sigmod function
	}
	@Override
	public double Classify(double[] NewInstance, double Threshold) {
		return Classify(NewInstance)>Threshold?1:0;
	}
	@Override
	public void Train(ArrayList<double[]> TrainingSet,double[] TrueLabels) {
		// Minimize the cost function using Gradient Descent
		for(int i=0;i<Config.MaxIterations;++i){
			System.out.println(Config.dateFormat.format(new Date())+" Current Iteration:"+i);
			// Calculate error
			double[] errors=new double[TrainingSet.size()];
			for(int j=0;j<TrainingSet.size();++j)
				errors[j]=Classify(TrainingSet.get(j))-TrueLabels[j];
			// Update parameters
			for(int j=0;j<Parameters.length;++j){
				double update=0;
				for(int k=0;k<TrainingSet.size();++k)
					update+=errors[k]*TrainingSet.get(k)[j];
				Parameters[j]-=LearningRate*update;
			}
		}
	}

	public double getClassifingThreshold() {
		return ClassifingThreshold;
	}

	public void setClassifingThreshold(double classifingThreshold) {
		ClassifingThreshold = classifingThreshold;
	}
	public double getLearningRate() {
		return LearningRate;
	}
	public void setLearningRate(double learningRate) {
		LearningRate = learningRate;
	}


}
