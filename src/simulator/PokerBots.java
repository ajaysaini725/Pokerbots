
package simulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.StringTokenizer;
import java.io.*;

public class PokerBots {
	
	static final int numGenerations = 500;
	public static int numPlayers;
	//final int numRandomPlayers = 70;
	final int players = 729; //total number of players: must be a power of 3
	final int preinp = 10;    
	final int prehid = 7; //CHANGED FROM 10 TO 7
	final int preout = 4;
	final int postinp = 10;
	final int posthid = 10;
	final int postout = 4;
	
	final int numTournaments = 7; //number of complete tournaments played each generation
	
	final int numSurviveEachGeneration = 81; //must be a power of 3
	final int numUsedAsParents = 27;
	final int numParentsPerChild = 3;
	
	final double mutationRate = .1;
	final double mutationSD = .1;
	
	ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>> pinfos;

	/*Randomly initializes a number of bots equal to int players*/
	public PokerBots(){
		pinfos = new ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>>();
		final double prob = .25;
		for (int i = 0; i < players; i++) {
			ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> pinfo = new ArrayList<>();

			ArrayList<ArrayList<ArrayList<Double>>> preflop = new ArrayList<>();
			ArrayList<ArrayList<Double>> inphid1 = new ArrayList<>();
			/*NOTE: +1 added to range of for loops to account for bias (implemented in the Network class)*/
			for (int k = 0; k < prehid + 1; k++) {
				ArrayList<Double> row = new ArrayList<>();
				for (int l = 0; l < preinp + 1; l++) {
					if (Math.random() < prob) {
						row.add(0.0);
					} else {
						row.add(Math.random() * 2 - 1);
					}
				}
				inphid1.add(row);
			}
			preflop.add(inphid1);
			ArrayList<ArrayList<Double>> hidout1 = new ArrayList<>();
			for (int k = 0; k < preout; k++) {
				ArrayList<Double> row = new ArrayList<>();
				for (int l = 0; l < prehid + 1; l++) {
					if (Math.random() < prob) {
						row.add(0.0);
					} else {
						row.add(Math.random() * 2 - 1);
					}
				}
				hidout1.add(row);
			}
			preflop.add(hidout1);
			pinfo.add(preflop);
			ArrayList<ArrayList<ArrayList<Double>>> postflop = new ArrayList<>();
			ArrayList<ArrayList<Double>> inphid2 = new ArrayList<>();
			for (int k = 0; k < posthid + 1; k++) {
				ArrayList<Double> row = new ArrayList<>();
				for (int l = 0; l < postinp + 1; l++) {
					if (Math.random() < prob) {
						row.add(0.0);
					} else {
						row.add(Math.random() * 2 - 1);
					}
				}
				inphid2.add(row);
			}
			postflop.add(inphid2);
			ArrayList<ArrayList<Double>> hidout2 = new ArrayList<>();
			for (int k = 0; k < postout; k++) {
				ArrayList<Double> row = new ArrayList<>();
				for (int l = 0; l < posthid + 1; l++) {
					if (Math.random() < prob) {
						row.add(0.0);
					} else {
						row.add(Math.random() * 2 - 1);
					}
				}
				hidout2.add(row);
			}
			postflop.add(hidout2);
			pinfo.add(postflop);
			pinfos.add(pinfo);
		}
	}

	/*
	 * @param bots the indices of the bots playing in the tournament round
	 * @return an ArrayList<Integer> containing the indices of the bots that won (size equals size of ArrayList<Integer> bots / 3)
	 */
	private ArrayList<Integer> playTournamentRound(ArrayList<Integer> bots){
		ArrayList<Integer> winners = new ArrayList<Integer>();
		//permute indices of bots
		ArrayList<Integer> perm = new ArrayList<>();
		for (int i = 0; i < bots.size(); i++) {
			perm.add(bots.get(i));
		}
		Collections.shuffle(perm);
		
		for (int l = 0; l < bots.size() / 3; l++) {

			//int[] winCount = new int[3]; 
			//for (int i = 0; i < 10; i++) {
				ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>> subset = new ArrayList<>();
				subset.add(pinfos.get(perm.get(l * 3)));
				subset.add(pinfos.get(perm.get(l * 3 + 1)));
				subset.add(pinfos.get(perm.get(l * 3 + 2)));

				int[] results = TripSimulator.run(subset); //size 3
				//for (int j = 0; j < results.length; j++) {
				//	winCount[j] += results[j];
				//}

			//}


			int highscore = 0;
			int player = -1;
			//figure out the index of the player with the most wins in the round of triplicate
			for (int i = 0; i < results.length; i++) {
				if (results[i] > highscore) {
					player = perm.get(l * 3 + i);
					highscore = results[i];
				}
			}
			winners.add(player);

		}
		return winners;
	}
	
	/*
	 * @return an int[] containing the number of rounds the player at each index survived in 
	 * all the tournaments total (the index corresponding to the highest number is the index of the best bot)
	 */
	public int [] getTournamentResults(){
		//size = total num players (value 0 = index of loser, value nonzero = index of winner) 
		int[] ranks = new int[pinfos.size()]; 
		
		for(int i = 0; i < numTournaments; i++){
			//System.out.println("Tournament Num: " + i);
			//contains the indices of the remaining players in the current tournament
			ArrayList<Integer> playersLeft = new ArrayList<Integer>(); 
			
			//at the start of the tournament, all players compete
			for(int j = 0; j < pinfos.size(); j++){
				playersLeft.add(j);
			}
			//play tournament rounds until there is a winner
			while(playersLeft.size() > 1){
				//System.out.println("Players Left:");
				//printArrayList(playersLeft);
				
				//same size as pinfos, index of nonzero value is index of remaining player
				ArrayList<Integer> roundWinners = playTournamentRound(playersLeft); 
				
				for(int j = 0; j < roundWinners.size(); j++){
					/*
					 * increment the value corresponding to the index of surviving players
					 * => indices corresponding to high values represent players who survived a lot of rounds 
					 */
					ranks[roundWinners.get(j)]++;
				}
				playersLeft = roundWinners;
				//System.out.println("players left size: " + playersLeft.size());
			}
		}
		return ranks;
	}
	
	/*
	 * @return ArrayList<Integer> of size numNumbersToGet containing random integers from 
	 * 0 to upperBound (not including upperBound)
	 */
	private ArrayList<Integer> getXRandomNumbersInRange(int numNumbersToGet, int upperBound){
		ArrayList<Integer> nums = new ArrayList<Integer>();
		while(nums.size() < numNumbersToGet){
			int num = (int) (Math.random() * upperBound);
			boolean contained = false;
			for(int i: nums){
				if(i == num){
					contained = true;
				}
			}
			if(!contained){
				nums.add(num);
			}
		}
		return nums;
	}
	
	private ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> crossBreed(ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>> parents){
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> child = new ArrayList<>();

		double [] weights = new double[parents.size()];
		for(int i = 0; i < weights.length; i++){
			weights[i] = Math.abs((new Random()).nextGaussian());
		}
		normalize(weights);


		for(int i = 0; i < 2; i++){ //size 2: preflop and postflop
			ArrayList<ArrayList<ArrayList<Double>>> network = new ArrayList<>();
			for(int j = 0; j < 2; j++){ //size 2: 2 layers of weights - inphid and hidout
				ArrayList<ArrayList<Double>> layer = new ArrayList<>();
				for(int k = 0; k < parents.get(0).get(i).get(j).size(); k++){
					ArrayList<Double> row = new ArrayList<Double>();
					for(int l = 0; l < parents.get(0).get(i).get(j).get(k).size(); l++){
						double weight = 0;
						for(int m = 0; m < parents.size(); m++){
							weight += parents.get(m).get(i).get(j).get(k).get(l) * weights[m];
						}
						row.add(weight);
					}
					layer.add(row);
				}
				network.add(layer);
			}
			child.add(network);
		}
		return child;
	}
	
	private ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> mutate(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> a){
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> mutatedBot = new ArrayList<>();
		
		for(int i = 0; i < a.size(); i++){ //size 2: 2 networks - preflop and postflop
			ArrayList<ArrayList<ArrayList<Double>>> network = new ArrayList<>();
			for(int j = 0; j < a.get(i).size(); j++){ //size 2: 2 layers of weights - inphid and hidout
				ArrayList<ArrayList<Double>> layer = new ArrayList<>();
				for(int k = 0; k < a.get(i).get(j).size(); k++){ 
					ArrayList<Double> row = new ArrayList<Double>();
					for(int l = 0; l < a.get(i).get(j).get(k).size(); l++){
						double mean = a.get(i).get(j).get(k).get(l);
						double newVal = mean;
						if(Math.random() < mutationRate){
							newVal = (new Random()).nextGaussian() * mutationSD + mean;
						}
						row.add(newVal);
					}
					layer.add(row);
				}
				network.add(layer);
			}
			mutatedBot.add(network);
		}
		return mutatedBot;
	}

	public void makeNextGeneration(int [] ranks, PrintWriter out){
		//System.out.println("started making new generation");
		int [] indices = sortIndicesByCorrespondingValue(ranks);
		out.println("Num wins of best bot: " + ranks[indices[0]]);
		ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>> newpinfos = new ArrayList<>();
		
		for(int i = 0; i < numSurviveEachGeneration; i++){
			if(i < numUsedAsParents){ //don't mutate the parents
				newpinfos.add(pinfos.get(indices[i]));
			}
			else{ //other bots (non-parents) allowed to survive get mutated
				newpinfos.add(mutate(pinfos.get(indices[i])));
			}
			
		}
		
		for(int i = 0; i < players - numSurviveEachGeneration; i++){
			ArrayList<Integer> playerNums = getXRandomNumbersInRange(numParentsPerChild, numUsedAsParents);
			ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>> parents = new ArrayList<>();
			for(int j = 0; j < playerNums.size(); j++){
				parents.add(pinfos.get(indices[playerNums.get(j)]));
			}
			newpinfos.add(mutate(crossBreed(parents)));
		}
		pinfos = newpinfos;
		//System.out.println("finished making new generation");
	}
	
	/*
	 * sorts the indices of an array by the values they correspond to (from highest to lowest)
	 * @param the array containing the values
	 * @return the array containing the indices
	 */
	private int [] sortIndicesByCorrespondingValue(int [] b){
		int [] sortedIndices = new int[b.length];
		int [] a = new int[b.length];
		for(int i = 0; i < a.length; i++){
			a[i] = b[i];
		}
		for(int i = 0; i < a.length; i++){
			sortedIndices[i] = i;
		}
		for(int i = 0; i < a.length; i++){
			for(int j = i + 1; j < a.length; j++){
				if(a[i] < a[j]){
					int temp = a[i];
					a[i] = a[j];
					a[j] = temp;
					
					temp = sortedIndices[i];
					sortedIndices[i] = sortedIndices[j];
					sortedIndices[j] = temp;
				}
			}
		}
		return sortedIndices;
	}

	public void printResults(PrintWriter out){
		for(int i=0;i<pinfos.size();i++){
			out.println(i);
			for(int j=0;j<pinfos.get(i).size();j++){
				for(int k=0;k<pinfos.get(i).get(j).size();k++){
					for(int l=0;l<pinfos.get(i).get(j).get(k).size();l++){
						for(int m=0;m<pinfos.get(i).get(j).get(k).get(l).size();m++){
							out.print(pinfos.get(i).get(j).get(k).get(l).get(m)+" ");
						}
						out.println();
					}
					if(!(j == pinfos.get(i).size() - 1 && k == pinfos.get(i).get(j).size() - 1)){out.println();}
				}
			}
		}
	}

	public void printBot(PrintWriter out, ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> bot){
		for(int i=0;i<bot.size();i++){
			for(int j=0;j<bot.get(i).size();j++){
				for(int k=0;k<bot.get(i).get(j).size();k++){
					for(int l=0;l<bot.get(i).get(j).get(k).size();l++){
						out.print(bot.get(i).get(j).get(k).get(l)+" ");
					}
					out.println();
				}
				out.println();
			}
		}

	}
	
	public ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> getBestBot(PrintWriter out){
		int [] overallRanks = getTournamentResults();
		int bestIndex = 0;
		for(int i = 1; i < overallRanks.length; i++){
			if(overallRanks[bestIndex] < overallRanks[i]){
				bestIndex = i;
			}
		}//System.out.println("best index: " + bestIndex + " with value: " + overallRanks[bestIndex]);
		out.println("Bot Num: " + bestIndex + " with num wins: " + overallRanks[bestIndex]);
		out.println("Final Results:");
		int [] indices = sortIndicesByCorrespondingValue(overallRanks);
		for(int i = 0; i < indices.length; i++){
			out.println("Index: " + indices[i] + ", Num Wins: " + overallRanks[indices[i]]);
		}
		return pinfos.get(bestIndex);
	}
	


	public static void main(String[] args) throws IOException{
		char [] a = new char[5];
		System.out.println(a[1]=='\0');
		System.exit(0);
	    
	    PokerBots botPlayer = new PokerBots(); //randomly initializes bots
		PrintWriter out_all = new PrintWriter("NetworkOutput_All.txt");
		PrintWriter out_winner = new PrintWriter("NetworkOutput_Winner.txt");
		PrintWriter out_botWins = new PrintWriter("BestBotPerTornament.txt");
		for (int i = 0; i < PokerBots.numGenerations; i++) {
			System.out.println(i);
			int [] overallRanks = botPlayer.getTournamentResults();
			//System.out.println("GOT OVERALL RANKS");
			botPlayer.makeNextGeneration(overallRanks, out_botWins);
		}
		
		System.out.println("finished simulations");
		botPlayer.printResults(out_all);
		System.out.println("printed all");
		out_all.close();
		
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> bestBot = botPlayer.getBestBot(out_botWins);
		out_botWins.close();
		System.out.println("determined best bot");
		botPlayer.printBot(out_winner, bestBot);
		System.out.println("printed best bot");
		out_winner.close();
	}


	public static ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>> getPlayersFromFile(String fileName, int numBots) throws IOException{
		BufferedReader f = new BufferedReader(new FileReader(fileName));

		ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>> players = new ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>>();
		f.readLine();
		String current = f.readLine();	


		StringTokenizer st = new StringTokenizer(current);

		for(int k = 0; k < numBots; k++){
			ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> player = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
			for(int j = 0; j < 2; j++){
				ArrayList<ArrayList<ArrayList<Double>>> network = new ArrayList<ArrayList<ArrayList<Double>>>();
				for(int i = 0; i < 2; i++){
					ArrayList<ArrayList<Double>> layer = new ArrayList<ArrayList<Double>>();
					while(current != null && current.length() > 3){
						ArrayList<Double> row = new ArrayList<Double>();
						st = new StringTokenizer(current);
						while(st.hasMoreTokens()){
							row.add(Double.parseDouble(st.nextToken()));
						}
						layer.add(row);
						current = f.readLine();
					}
					network.add(layer);
					//System.out.println("network Size: " + network.size());
					current = f.readLine();
				}
				player.add(network);
				//System.out.println("player Size: " + player.size());
			}
			players.add(player);
			//System.out.println("players Size: " + players.size());
		}
		return players;
	}
	
	/*
	 * precondition: all entries in a are nonnegative
	 */
    private void normalize(double [] a) {
        double sum = 0;
        for (double d : a) {
            sum += d;
        }
        for(int i = 0; i < a.length; i++){
        	a[i] = a[i] / sum;
        }

    }
    
	private void printArray(int [] a){
		for(int i: a){
			System.out.print(i + " ");
		}
	}

	private void printArrayList(ArrayList<Integer> a){
		for(int i: a){
			System.out.print(i + " ");
		}
	}
}
