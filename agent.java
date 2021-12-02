package group7;

import java.io.PrintStream;
import java.util.List;
import java.io.FileDescriptor;
import java.io.FileOutputStream;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.timeline.*;

@SuppressWarnings("serial")
public class Agent7 extends AbstractNegotiationParty 
{
	private static double MINIMUM_TARGET = 0.88;
	private static double MAXMUM_TARGET = 1;
	private Bid lastOffer;
	private static double threshold = 1;
	private static int totalRound = 0;
	private static int currentRound = 0;
	OpponentModel OM;
	private static double maxUsUtility = Double.MIN_VALUE;
	private static double maxUsUtilityOPOffer = Double.MIN_VALUE;
	GradientDescent GD;
	private static double opponentAveUtil = 0.0;

	private double[] nashPoint = new double[2];
	private boolean first = false;

	@Override
	public void init(NegotiationInfo info) 
	{
		super.init(info);
		System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));

		OM = new OpponentModel(userModel);
		GD = new GradientDescent(userModel);

		totalRound = ((DiscreteTimeline) timeline).getTotalRounds();
	}
	@Override
	public Action chooseAction(List<Class<? extends Action>> possibleActions) 
	{
		currentRound = ((DiscreteTimeline) timeline).getRound();
		if(lastOffer == null) {
			first = true;
		}
		
		if(lastOffer !=null && GD.getPredictUtility(lastOffer) > maxUsUtilityOPOffer) {
			maxUsUtilityOPOffer = GD.getPredictUtility(lastOffer);
			maxUsUtility = 0.99;
		}
		if(currentRound >= (totalRound * 0.15)) {
			nashPoint = calculateNP();
			threshold = MAXMUM_TARGET - 1.0 * currentRound * (MAXMUM_TARGET - MINIMUM_TARGET) / (totalRound * 0.85);
		}

		if(lastOffer != null && currentRound != 0) {
			OM.inputOpponentBid(lastOffer);
		}

		int round = ((DiscreteTimeline) timeline).getRound();
		int totalround = ((DiscreteTimeline) timeline).getTotalRounds();
		Bid bid = null;

		if(round > totalround * 0.5) {
			if(GD.getPredictUtility(lastOffer) - opponentAveUtil > 0.3 && Math.abs(threshold - GD.getPredictUtility(lastOffer)) > 0.81) {
				return new Accept(getPartyId(), lastOffer);
			}
		}

		if(lastOffer != null) {
			opponentAveUtil = (opponentAveUtil * (currentRound - 1) + GD.getPredictUtility(lastOffer) )/ currentRound;
		}

		if(round < totalround * 0.1) {
			bid = generateRandomBidAboveTarget();
			return new Offer(getPartyId(), bid);
		} else if (round < totalround * 0.6) {
			if(GD.getPredictUtility(lastOffer) > threshold ) {
				return new Accept(getPartyId(), lastOffer);
			}
			bid = generateBidWithOM();
			return new Offer(getPartyId(), bid);
		} else if (round < totalround * 0.8) {
			if(GD.getPredictUtility(lastOffer) > threshold) {
				return new Accept(getPartyId(), lastOffer);
			}
			bid = generateBidWithOM();
			return new Offer(getPartyId(), bid);
		} 
		else if (round < totalround * 0.85) {
			if(GD.getPredictUtility(lastOffer) > threshold || GD.getPredictUtility(lastOffer) > maxUsUtility ) {
				return new Accept(getPartyId(), lastOffer);
			}
			bid = generateBidWithOM();
			return new Offer(getPartyId(), bid);
		} 
		else if (round < totalround * 0.9){
			if(GD.getPredictUtility(lastOffer) > threshold) {
				return new Accept(getPartyId(), lastOffer);
			}
			bid = generateBidWithOM();
			return new Offer(getPartyId(), bid);
		} 
		else if (round < totalround * 0.95) {
			if(GD.getPredictUtility(lastOffer) > threshold) {
				return new Accept(getPartyId(), lastOffer);
			}
			bid = generateBidWithOM();
			return new Offer(getPartyId(), bid);
		} 
		else if (round < totalround * 0.99) {
			if(GD.getPredictUtility(lastOffer) > threshold) {
				return new Accept(getPartyId(), lastOffer);
			}
			bid = generateBidWithOM();
			return new Offer(getPartyId(), bid);
		} 
		else {
			return new Accept(getPartyId(), lastOffer);
		}
	}

	public Bid generateBidWithOM() {

		double minNash = Double.MAX_VALUE;
		Bid randomBid;
		Bid nashBid = null;
		Bid maxUiltBid_OP = null;
		Bid maxUiltBid_US = null;
		double maxUtil_OP = Double.MIN_VALUE;
		double maxUtil_US = Double.MIN_VALUE;
		double secondMax_OP = Double.MIN_VALUE;
		long numOfPossibleBids = utilitySpace.getDomain().getNumberOfPossibleBids();
		for(int i = 0; i < numOfPossibleBids / 2 ; i++) {
			randomBid = generateRandomBid();

			if(currentRound >= totalRound * 0.5) {
				double OpUtil = OM.getUtility(randomBid);
				double UsUtil = GD.getPredictUtility(randomBid);

				double distance = 0.0;
				if(first) {
					distance = Math.sqrt(Math.pow(OpUtil - nashPoint[0], 2) + Math.pow(UsUtil - nashPoint[1], 2));
				}else {
					distance = Math.sqrt(Math.pow(OpUtil - nashPoint[1], 2) + Math.pow(UsUtil - nashPoint[0], 2));
				}

				if(distance < minNash) {
					minNash = distance;
					nashBid = randomBid;
				}

			}else {
				if(OM.getUtility(randomBid) > secondMax_OP && GD.getPredictUtility(randomBid) > threshold) {
					if(OM.getUtility(randomBid) < maxUtil_OP && GD.getPredictUtility(randomBid) > threshold) {
						maxUiltBid_OP = randomBid;
						secondMax_OP = OM.getUtility(randomBid);
					}else {
						maxUiltBid_OP = randomBid;
						maxUtil_OP = OM.getUtility(randomBid);
					}
				}
				if(GD.getPredictUtility(randomBid) > maxUtil_US) {
					maxUiltBid_US = randomBid;
					maxUtil_US = GD.getPredictUtility(randomBid);
				}
			}
		}

		if(nashBid != null) {
			return nashBid;
		}

		if(maxUiltBid_OP != null) {
			return maxUiltBid_OP;
		}else {
			return maxUiltBid_US;
		}
	}

	public double[] calculateNP() {
		Bid randomBid;
		long numOfPossibleBids = utilitySpace.getDomain().getNumberOfPossibleBids();
		double minOpUtil = Double.MAX_VALUE;
		double maxOpUtil = Double.MIN_VALUE;
		double minUsUtil = Double.MAX_VALUE;
		double maxUsUtil = Double.MIN_VALUE;
		double[] nashPoint_ = new double[] {0.0, 0.0};
		
		for(int i = 0; i < numOfPossibleBids; i++) {
			randomBid = generateRandomBid();
			double OpUtil = OM.getUtility(randomBid);
			double UsUtil = GD.getPredictUtility(randomBid);

			if(minOpUtil > OpUtil) {
				if(maxUsUtil < UsUtil) {
					maxUsUtil = UsUtil;
					minOpUtil = OpUtil;
				}
			}

			if(minUsUtil > UsUtil) {
				if(maxOpUtil < OpUtil) {
					maxOpUtil = OpUtil;
					minUsUtil = UsUtil;
				}
			}
		}

		double[] point1 = new double[] {maxUsUtil, minOpUtil};
		double[] middle = new double[] {0.0,0.0};
		middle[0] = (maxUsUtil + minUsUtil) / 2;
		middle[1] = (maxOpUtil + minOpUtil) / 2;

		nashPoint_[1] = middle[0] + Math.abs(middle[1] - point1[1]) + 0.1;
		nashPoint_[0] = middle[1] + Math.abs(point1[0] - middle[0]) + 0.1;
		return nashPoint_;
	}

	private Bid generateRandomBidAboveTarget() 
	{
		Bid randomBid;
		double util;
		int i = 0;
		do 
		{
			randomBid = generateRandomBid();
			util = GD.getPredictUtility(randomBid);
		} 
		while (util < threshold && i++ < 100);		
		return randomBid;
	}

	@Override
	public void receiveMessage(AgentID sender, Action action) 
	{
		if (action instanceof Offer) 
		{
			lastOffer = ((Offer) action).getBid();
		}
	}

	@Override
	public String getDescription() 
	{
		return "Agent7";
	}
}
