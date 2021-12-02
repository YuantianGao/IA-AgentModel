package group7;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.UserModel;

public class OpponentModel {
	private List<Issue> issueList = new ArrayList<Issue>();				// save all issue for traversal
	// record the index value of all values for extracting evaluation data from valueUtil, also record the affiliations between values and issues
	private HashMap<Issue,HashMap<Value, Integer>> valueHash = new HashMap<Issue, HashMap<Value, Integer>>();
	// record the index value of all issues for extracting weight data from issueWeights
	private HashMap<Issue, Integer> issueHash = new HashMap<Issue, Integer>();
	private List<Bid> opponentBids = new ArrayList<Bid>();				// record all opponent bids
	
	private int[] valueNum;					// record the number of all values
	private int[] valueOrder;				// record the order of all values
	private double[] valueUtil;				// record the evaluation value of every values
	private double[] issueWeights;			// record the weight of every weights
	
	private int numOfValues = 0;			// the number of all values
	private int numOfIssues = 0;			// the number of all issues
	
	
	// constructor function
	public OpponentModel(UserModel userModel) {
		
		initValuesAndIssues(userModel);			// initialize the values and issues
	}
	
	// initialize the values and issues
	private void initValuesAndIssues(UserModel userModel) {
		issueList = userModel.getDomain().getIssues();	// get all issues
		// traversal all issues
		for(Issue issue : issueList) {
			issueHash.put(issue, numOfIssues);
			numOfIssues++;
			HashMap<Value, Integer> valueTemp = new HashMap<Value, Integer>();	// temporarily record the index value of values
		    IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
		    
			for(ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
		    	valueTemp.put(valueDiscrete, numOfValues);				// add index value of values
		        numOfValues++;
			}
			
		    valueHash.put(issue, valueTemp);
		}
		
		// establish variables
		this.valueUtil = new double[numOfValues];
		this.valueNum = new int[numOfValues];
		this.issueWeights = new double[numOfIssues];
		this.valueOrder = new int[numOfValues];	
		
		// initialize the variables
		for(int i = 0; i < valueUtil.length; i++) {
			valueUtil[i] = 0;
			valueNum[i] = 0;
		}
		
	}
	
	// get opponent's bid
	public void inputOpponentBid(Bid bid) {
		opponentBids.add(bid);											// save opponent's bid

		for(Issue issue: issueList) {
			valueNum[valueHash.get(issue).get(bid.getValue(issue))]++;	// the count variable corresponding to every values in opponent bid plus one
		}
		
		updateOpponent();
	}
	
	// update the opponent model
	public void updateOpponent() {
		// update the order of values first
		updateOrder();
		
		// update the evaluation value of every values
		for(Issue issue: issueList) {
			IssueDiscrete issueDiscrete = (IssueDiscrete)issue;
			List<ValueDiscrete> values = issueDiscrete.getValues();
			for(Value value : values) {
				valueUtil[valueHash.get(issue).get(value)] = 1.0 * (values.size() - valueOrder[valueHash.get(issue).get(value)] + 1) / values.size();		// Vo = (k - no + 1) / k
			}
			
		}
		
		double sumOfWeights = 0.0;
		// update the weights of every issues
		for(Issue issue: issueList) {
			double sumOfSqrt = 0.0;
			IssueDiscrete issueDiscrete = (IssueDiscrete)issue;
			for(Value value : issueDiscrete.getValues()) {
				sumOfSqrt += Math.pow(valueNum[valueHash.get(issue).get(value)] / numOfValues, 2);		// wi = fo^2/t^2
			}
			issueWeights[issueHash.get(issue)] = sumOfSqrt;
			sumOfWeights += sumOfSqrt;
		}
		
		// normalize weights
		for(Issue issue: issueList) {
			issueWeights[issueHash.get(issue)] = issueWeights[issueHash.get(issue)] / sumOfWeights;
		}
	}
	
	// update the order of values
	public void updateOrder() {
		
		// traversal all issues
		for(Issue issue: issueList) {
			IssueDiscrete issueDiscrete = (IssueDiscrete)issue;
			for(Value valueA : issueDiscrete.getValues()) {
				int rank = 0;		// record current order
				for(Value valueB : issueDiscrete.getValues()) {
					// the judgement condition is >=, so rank starts from 0
					if(valueNum[valueHash.get(issue).get(valueB)] >= valueNum[valueHash.get(issue).get(valueA)]) {
						rank++;
					}
				}
				valueOrder[valueHash.get(issue).get(valueA)] = rank;
			}
		}
	}
	
	// print out results
	public void printResult() {
		for(Issue issue: issueList) {
			System.out.println("___________________________________________________");
			System.out.println("The weight of issue: "+ issue + " " + issueWeights[issueHash.get(issue)]);
			IssueDiscrete issueDiscrete = (IssueDiscrete)issue;
			for(Value value : issueDiscrete.getValues()) {
				System.out.println("The evaluation of value: " + value + " " + valueUtil[valueHash.get(issue).get(value)]);
				System.out.println("The number of value: " + value + " " + valueNum[valueHash.get(issue).get(value)]);
			}
		}
	}
	
	// calculate the utility of input bid
	public double getUtility(Bid bid) {
		
		double sumOfUtility = 0.0;
		for(Issue issue : issueList) {
			sumOfUtility += valueUtil[valueHash.get(issue).get(bid.getValue(issue))] * issueWeights[issueHash.get(issue)];
		}
		
		return sumOfUtility;
	}
}
