package group7;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.ExperimentalUserModel;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;
import genius.core.utility.UncertainAdditiveUtilitySpace;

public class GradientDescent {
	private UserModel userModel;
	// record the relation between bids and values, 1 means the value is selected by this bid, 0 means isn't
	private int[][] bidsSelectValue;
	// record the index value of all values for extracting evaluation data from valueUtil, also record the affiliations between values and issues
	private HashMap<Issue,HashMap<Value, Integer>> valueHash = new HashMap<Issue, HashMap<Value, Integer>>();
	private double[] valueUtil;				// record the evaluation value of values
	private double[] bidsUtil;				// the utility of bids we set for training
	List<Bid> bidRight = new ArrayList<>();	// the right bid ranking
	private int numOfBids = 0;				// number of all bids
	private int numOfValues = 0;			// number of all value
	private int numOfIssues = 0;			// number of all issue
	private double learningRate = 0.01;		// learning rate
	private int maxIteration = 2000; 		// number of iteration
	private List<Issue> issueList = new ArrayList<Issue>();		// record all issues
	private double[] issueWeights;
	// record the index value of all issues for extracting weight data from issueWeights
	private HashMap<Issue, Integer> issueHash = new HashMap<Issue, Integer>();
	private AdditiveUtilitySpaceFactory aUSF;	// record utility Space for returning


	public GradientDescent(UserModel userModel) {

		this.userModel = userModel;
		// initialization
		Init();
		// the part of gradient descent
		for(int i = 0; i < maxIteration; i++) {
			GD();
		}
	}

	// initialization
	public void Init() {		
		// set proper utility value of all bids
		setBidsUtility();

		// initialize valueHash evaluation value of all values
		initValues();

	}

	// set proper utility value of all bids
	private double[] setBidsUtility() {
		BidRanking bidRanking = userModel.getBidRanking();
		bidRight = bidRanking.getBidOrder();				// the right bid ranking
		int size = bidRanking.getBidOrder().size();			// number of all bids
		double[] randomUtil = new double[size];
		Random r = new Random();

		// the initialization utilities of bids conform to a Gaussian distribution with a mean of 0.6 and variance of 0.1
		for(int i = 0; i < size; i++) {
			randomUtil[i] = 0.25 * r.nextGaussian() + 0.5;
		}

		// sort the utility
		Arrays.sort(randomUtil);

		this.numOfBids = size;
		bidsUtil = new double[size];
		for(int i = 0; i < size; i++) {
			this.bidsUtil[i] = randomUtil[i];				// set the utility of bids
		}

		return this.bidsUtil;
	}
	
	// initialize the evaluation of value, number of value and bidsSelectValue which records relation between bids and values
	private void initValues() {
		issueList = userModel.getDomain().getIssues();	// get all issues

		// traversal all issues
		for(Issue issue: issueList) {
			numOfIssues++;

			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;

			HashMap<Value, Integer> valueTemp = new HashMap<Value, Integer>();
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				valueTemp.put(valueDiscrete, numOfValues);				// add index value of values
				numOfValues++;
			}
			valueHash.put(issue, valueTemp);
		}

		this.valueUtil = new double[numOfValues];
		// initialize the evaluation of all values with 1/N, N is the number of values
		for(int i = 0; i < valueUtil.length; i++) {
			valueUtil[i] = 1.0 / numOfValues;
		}

		// initialize bidsSelectValue
		bidsSelectValue = new int[numOfBids][numOfValues];		// number of rows equal to the number of bids, number of columns equal to the number of values
		
		for(int i = 0; i < numOfBids; i++) {
			for(int j = 0; j < numOfValues; j++) {
				bidsSelectValue[i][j] = 0;
			}
		}

		int currentBid = 0;
		for(Bid bid : bidRight) {								// get the selected value of all bids
			for(Issue issue: issueList) {
				Value valueCurrent = null; 
				valueCurrent = bid.getValue(issue);	
				bidsSelectValue[currentBid][valueHash.get(issue).get(valueCurrent)] = 1;		// the valueCurrent is selected by currentBid, so set it 1
			}

			currentBid++;
		}
	}

	// the main body of Gradient Descent algorithm
	public void GD() {
		double[] newValue = new double[numOfValues];

		for(int i = 0; i < numOfValues; i++) {
			newValue[i] = 0;
		}

		// traversal values, use Gradient Descent for everyone
		for(Issue issue: issueList) {								// traversal all issues				
			for(Value value: valueHash.get(issue).keySet()) {		// traversal all values of current issue

				double sumOfBias = 0.0;								// record the sum of deviation utility
				int indexOfValue = valueHash.get(issue).get(value);	// index value of current value
				int currentBid = 0;									// index value of current bid

				for(int z = 0; z < bidRight.size(); z++) {			// traversal all bids
					double bidUtility = 0.0;						// the estimated utility of current bid
					double currentBias = 0.0;
					
					// calculate the estimated utility of current bid, xi*w
					for(int i = 0; i < numOfValues; i++) {	
						bidUtility += bidsSelectValue[currentBid][i] * valueUtil[i];
					}

					currentBias = bidUtility - bidsUtil[currentBid];			// xi*w - yi, the difference value between estimated utility and right utility
					currentBias *= bidsSelectValue[currentBid][indexOfValue];		// xik*(xi*w - yi), if this bid contains this value, count this deviation in
					currentBid ++;									// index value of current bid
					sumOfBias += currentBias;						// sigma(xik*(xi*w - yi)), the sum of all deviation values of all bids
				}

				sumOfBias /= numOfBids;								// sigma(xik*(xi*w - yi))/N

				// record the new evaluation of current value, we used a regular terms to avoid overfitting
				newValue[indexOfValue] = valueUtil[indexOfValue] * (1 - 8.81 * learningRate / numOfBids) - learningRate * sumOfBias;
			}
		}

		valueUtil = newValue;								// update the evaluation of values
	}

	//	print the results
	public void printResult() {



		ExperimentalUserModel e = ( ExperimentalUserModel ) userModel ;
		UncertainAdditiveUtilitySpace realUSpace = e. getRealUtilitySpace();

		List< Issue > issues = realUSpace.getDomain().getIssues();

		for (Issue issue : issues) {
			int issueNumber = issue.getNumber();
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) realUSpace.getEvaluator(issueNumber);
			System.out.println("________________________________________________________");
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				System.out.println(valueDiscrete.getValue());
				System.out.println("Evaluation(getValue): " + evaluatorDiscrete.getValue(valueDiscrete));
				try {
					System.out.println("Evaluation(getEvaluation)ESTIMATE: " + valueUtil[valueHash.get(issue).get(valueDiscrete)]);
					System.out.println("Evaluation(getEvaluation)TRUE: " + evaluatorDiscrete.getEvaluation(valueDiscrete));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}		
	}


	// normalization for inputing into utilitySpace, make the most desired value's evaluation equals 1 and least equals 0
	// but this part doesn't work as well as expectation, so we didn't use this part in our agent
	public void normalization() {

		for(Issue issue:userModel.getDomain().getIssues()) {
			double max = Double.MIN_VALUE;
			double min = Double.MAX_VALUE;
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			// find maxmum and minimum
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				if(valueUtil[valueHash.get(issue).get(valueDiscrete)] > max) {
					max = valueUtil[valueHash.get(issue).get(valueDiscrete)];
				}
				if(valueUtil[valueHash.get(issue).get(valueDiscrete)] < min) {
					min = valueUtil[valueHash.get(issue).get(valueDiscrete)];
				}
			}
			// nomalization
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				double update = (valueUtil[valueHash.get(issue).get(valueDiscrete)] - min) / (max - min);
				valueUtil[valueHash.get(issue).get(valueDiscrete)] = update;
			}
		}

		// normalize weights of issues
		this.issueWeights = new double[numOfIssues];
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		int i = 0;
		for(Issue issue: issueList) {
			issueHash.put(issue, i);
			double sumOfValues = 0.0;
			
			// add all evaluation value from same issue
			for(Value value: valueHash.get(issue).keySet()) {
				sumOfValues += valueUtil[valueHash.get(issue).get(value)];
			}

			issueWeights[i] = sumOfValues;
			if(max < sumOfValues) {
				max = sumOfValues;
			}
			if(min > sumOfValues) {
				min = sumOfValues;
			}
			i++;
		}

		for(Issue issue: issueList) {
			issueWeights[issueHash.get(issue)] = (issueWeights[issueHash.get(issue)] - min) / (max - min);
		}
	}
	
	// calculate the utility of inputted bid
	public double getPredictUtility(Bid bid) {
		double utility = 0.0;
		HashMap<Integer, Value> values = bid.getValues();
		int i = 1;
		for(Issue issue : issueList) {
			utility += valueUtil[valueHash.get(issue).get(values.get(i))];
			i++;
		}

		return utility;
	}
	
	// return the utilitySpace
	public AdditiveUtilitySpace getUtilitySpace() {
		aUSF =new AdditiveUtilitySpaceFactory(userModel.getDomain());

		for(Issue issue: issueList) {
			aUSF.setWeight(issue, issueWeights[issueHash.get(issue)]);
			for(Value value: valueHash.get(issue).keySet()) {
				aUSF.setUtility(issue, (ValueDiscrete)value, valueUtil[valueHash.get(issue).get(value)]);
			}
		}

		aUSF.normalizeWeights();
		return aUSF.getUtilitySpace();
	}
}
