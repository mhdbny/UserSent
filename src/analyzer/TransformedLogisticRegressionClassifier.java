package analyzer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
 
public class TransformedLogisticRegressionClassifier extends LogisticRegressionClassifier{
public double[] a; // Scaling 
public double[] b; // Shifting
private double Lambda;
private double Sigma;

public structures.User User;
	public TransformedLogisticRegressionClassifier(int NumberOfParameters,  double LearningRate,DocAnalyzer analyzer,structures.User User, double Lambda, double Sigma) {
		super(NumberOfParameters, LearningRate,analyzer);
		this.User=User;
		this.Sigma=Sigma;
		this.Lambda=Lambda;
	}
	public void InitParameters(){
		a=new double[this.Parameters.length];
		// initalize scaling by 1
		for(int i=0;i< this.Parameters.length;++i)
			a[i]=1;
		b=new double[ this.Parameters.length];
	}
	@Override
	public double Classify(int NewInstance) {
		double value=0;
		Set<String> set = analyzer.m_Vocabs.keySet();
		Iterator<String> itr = set.iterator();
		for (int i=0;i<=analyzer.m_Vocabs.size();++i)
			value+=(a[i]*Parameters[i]+b[i])*(i==0?1:analyzer.Reviews.get(NewInstance).getValueFromVSM(itr.next()));
		return 1/(double)(1+Math.exp(-1*value)); // sigmod function
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
				double updateA=0;
				double updateB=0;
				String CurrectTerm=j==0?"":itr.next();
				for(int k=0;k<TrainingSet.size();++k){
					updateA+=errors[k]*(Parameters[j]*(j==0?1:User.Reviews.get(TrainingSet.get(k)).getValueFromVSM(CurrectTerm))+2*Lambda*(a[j]-1));
					updateB+=errors[k]*((j==0?1:User.Reviews.get(TrainingSet.get(k)).getValueFromVSM(CurrectTerm))+2*Lambda*Sigma*a[j]);
				}
				a[j]-=LearningRate*updateA;
				b[j]-=LearningRate*updateB;
			}
		}
	}
}
