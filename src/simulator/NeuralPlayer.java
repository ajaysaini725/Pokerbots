/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulator;

import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author dzd123
 */
//public class Players {
//}
class ProbCall extends Player {

    public double prob;

    ProbCall(double prob) {
        super();
        this.prob = prob;
    }

    @Override
    public int makeMove(char phase) {
        double rand = Math.random();
        if (rand <= prob) {
            return 0;
        }
        rand = Math.random();
        if (rand < .5) {
            return 10;
        }
        return -1;
    }

    @Override
    public void newHand() {
    }

    @Override
    public void handOver() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

class Network {

    ArrayList<ArrayList<Double>> biases;
    ArrayList<ArrayList<ArrayList<Double>>> weights;
    double bias = 1.0;

   /*
    * precondition: the input and hidden layers have an extra node to act as the bias node
    */
    public Network(ArrayList<ArrayList<ArrayList<Double>>> weights) {
        this.weights = weights;
        int[] sizes = new int[weights.size()];
        for (int i = 0; i < sizes.length; i++) {
            sizes[i] = weights.get(i).size();
        }
        initializeBiases(sizes);
    }

    /*
     * sizes contains the sizes of layers NOT INCLUDING THE INPUT LAYER
     * NOTE: the values are dummy values => they are not actually used
     * bias is implemented as a node that fires off a constant value (equal to bias defined as a class variable)
     * it is expected that the network sent to the constructor provides this extra node in the input and hidden layers 
     * to act as the bias
     */
    private void initializeBiases(int[] sizes) {
        biases = new ArrayList<ArrayList<Double>>();
        for (int i = 0; i < sizes.length; i++) {
            ArrayList<Double> layer = new ArrayList<Double>();
            for (int j = 0; j < sizes[i]; j++) {
                layer.add(0.0);
            }
            biases.add(layer);
        }
    }

    /*
     *call this method to get the outputs of the neural network
     *@param the list of inputs for the neural network
     *@return an array containing the outputs (normalized probabilities of making moves)
     */
    public double[] getOutputs(ArrayList<Double> inputs) {
        ArrayList<Double> currentLayer = inputs;
        currentLayer.add(bias);
        for (int i = 0; i < biases.size(); i++) {
            ArrayList<Double> nextLayer = new ArrayList<Double>();
            for (int j = 0; j < biases.get(i).size(); j++) {
                ArrayList<ArrayList<Double>> currentWeightArray = weights.get(i);
                ArrayList<Double> weightsForCurrentBias = currentWeightArray.get(j);
                double nodeOutput = sigmoid(dotProduct(weightsForCurrentBias, currentLayer));
                nextLayer.add(nodeOutput);
            }
            currentLayer = nextLayer;
            if(i != biases.size() - 1){
            	currentLayer.add(bias);
            }
        }
        return normalize(currentLayer); //at the end of this, current layer is the outputs
    }

    //precondition: a and b are the same size
    private double dotProduct(ArrayList<Double> a, ArrayList<Double> b) {
        double ans = 0;
        for (int i = 0; i < a.size(); i++) {
            ans += a.get(i) * b.get(i);

        }
        return ans;
    }

    private double sigmoid(double inp) {
        return 1 / (1 + Math.exp(-1 * inp));
    }

    private double[] normalize(ArrayList<Double> a) {
        double[] ans = new double[a.size()];
        double sum = 0;
        for (double d : a) {
            sum += d;
        }
        for (int i = 0; i < a.size(); i++) {
            ans[i] = a.get(i) / sum;
        }
        return ans;
    }

    private ArrayList<Double> normalizeList(ArrayList<Double> a) {
        ArrayList<Double> ans = new ArrayList<Double>();
        double sum = 0;
        for (double d : a) {
            sum += d;
        }
        for (int i = 0; i < a.size(); i++) {
            ans.add(a.get(i) / sum);
        }
        return ans;
    }
}

public class NeuralPlayer extends Player {

    ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> pinfo;
    public int leftPlayer;
    public int rightPlayer;

    // Before every round, run this code (more conservative values early on): 
    /*
     VPIP[0][1]++;
     VPIP[1][1]++;
     PFR[0][1]++;
     PFR[1][1]++;
     */
    
    ArrayList<ArrayList<Integer>> recentAggr = new ArrayList<>();
    public int[][] overallAggr;
    
    public int[][] VPIP;
    public int[][] PFR;
    public int[][] AF;
    public int[][] AFq;
    public double storeEquity;
    // Initialize following values to false after every round: 
    public boolean flopDealt = false;
    public boolean[] hasContributed;
    public boolean[] hasRaised;
    Network preflop;
    Network postflop;

    public NeuralPlayer(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> pinfo) {

        super();
        recentAggr.add(new ArrayList<Integer>());
        recentAggr.add(new ArrayList<Integer>());
        
        recentAggr.get(0).add(0);
        recentAggr.get(1).add(0);        
        this.preflop = new Network(pinfo.get(0));
        this.postflop = new Network(pinfo.get(1));
        if (absPlayerNum == 0) {
            leftPlayer = 1;
            rightPlayer = 2;
        } else if (absPlayerNum == 1) {
            leftPlayer = 2;
            rightPlayer = 0;
        } else {
            leftPlayer = 0;
            rightPlayer = 1;
        }
        this.pinfo = pinfo;
        this.overallAggr = new int[2][3];
       this.overallAggr[0][2]=1;
       this.overallAggr[1][2]=1;
       
        this.VPIP = new int[2][2];
        this.PFR = new int[2][2];
        this.AF = new int[2][2];
        this.AFq = new int[2][2];
        this.hasContributed = new boolean[3];
        this.hasRaised = new boolean[3];

    }

    @Override
    public void newHand() {

    }

    @Override
    public int makeMove(char phase) {

        int[] stackSizes = new int[3];
        boolean[] activePlayers = new boolean[3];
        stackSizes[0] = money;
        activePlayers[0] = true;
        Player left = Current.simulator.getByAbs(leftPlayer);
        Player right = Current.simulator.getByAbs(rightPlayer);
        int numActive = 0;
        if (left != null) {

            stackSizes[1] = Current.simulator.getByAbs(leftPlayer).money;
            activePlayers[1] = Current.round.isActive[left.playerNum];
            if (activePlayers[1]) {
                numActive++;
            }
        }
        if (right != null) {
            stackSizes[2] = Current.simulator.getByAbs(rightPlayer).money;
            activePlayers[2] = Current.round.isActive[right.playerNum];
            if (activePlayers[2]) {
                numActive++;
            }
        }
        int bigBlind = Current.simulator.bBlindNum;
        int pot = Current.round.pot.totalPot();
        int pos = Current.round.currentPosition(playerNum);
        boolean canCheck = Current.round.pot.minBet - currentBet == 0;
        String lastActionsAsString = history.get(history.size() - 1);

        boolean[] relevantPlayers = getRelevantPlayers(stackSizes, activePlayers);
        updateStats(lastActionsAsString);
        String ours = Deck.cardToString(getHand()[0]) + Deck.cardToString(getHand()[1]) + ":xx:xx";
        String board = "";
        for (int[] i : Current.round.deck.shown) {
            board += Deck.cardToString(i);
        }
        double equity = storeEquity;
        if (lastActionsAsString.contains("DEAL")||lastActionsAsString.contains("POST")) {
            equity = getEquity(getHand(), Current.round.deck.shown, numActive);
            storeEquity = equity;
        }
        
        int callAmount = Current.round.pot.minBet - currentBet;
        
        if (phase == 'p') {
            ArrayList<Double> inputs = new ArrayList<Double>();
            inputs.add(equity);
            inputs.add(pot / 200.0);
            inputs.add(callAmount / 100.0);
            inputs.add(stackSizes[0] / 200.0);
            inputs.add(stackSizes[1] / 200.0);
            inputs.add(stackSizes[2] / 200.0);
            
            inputs.add((2.0 * overallAggr[0][0] + overallAggr[0][1] )/ (double)(overallAggr[0][0] + overallAggr[0][1] + overallAggr[0][2]));
            inputs.add((2.0 * overallAggr[1][0] + overallAggr[1][1] )/(double) (overallAggr[1][0] + overallAggr[1][1] + overallAggr[1][2]));
            
            double sum = 0.0;
            for (Integer i: recentAggr.get(0)) {
            	sum += i;
            }
            inputs.add(sum / recentAggr.get(0).size());
            
            sum = 0.0;
            for (Integer i: recentAggr.get(1)) {
            	sum += i;
            }
            inputs.add(sum / recentAggr.get(1).size());
            
            double[] output = preflop.getOutputs(inputs);
          //  System.out.println("preflop inputs:");
        //    printArrayList(inputs);
          //  System.out.println("preflop outputs:");
           // printArray(output);
            return move(output);

        } 
    	else {
            ArrayList<Double> inputs = new ArrayList<Double>();
            inputs.add(equity);
            inputs.add(pot / 200.0);
            inputs.add(callAmount / 100.0);
            inputs.add(stackSizes[0] / 200.0);
            inputs.add(stackSizes[1] / 200.0);
            inputs.add(stackSizes[2] / 200.0);
            
            inputs.add((2.0 * overallAggr[0][0] + overallAggr[0][1]) / (double)(overallAggr[0][0] + overallAggr[0][1] + overallAggr[0][2]));
            inputs.add((2.0 * overallAggr[1][0] + overallAggr[1][1] )/ (double)(overallAggr[1][0] + overallAggr[1][1] + overallAggr[1][2]));
            
            double sum = 0.0;
            for (Integer i: recentAggr.get(0)) {
            	sum += i;
            }
            inputs.add(sum / recentAggr.get(0).size());
            
            sum = 0.0;
            for (Integer i: recentAggr.get(1)) {
            	sum += i;
            }
            inputs.add(sum / recentAggr.get(1).size());
            
            /**CHANGED SO THAT ONLY ONE NEURAL NET IS USED!!*/
            double[] output = preflop.getOutputs(inputs);
       //     System.out.println("postflop inputs:");
          //  printArrayList(inputs);
        //    System.out.println("postflop outputs:");
         //   printArray(output);
           return move(output);
        }
        
    }
    
    public void printArrayList(ArrayList<Double> a){
    	for(double d: a){
    		System.out.print(d + " ");
    	}
    	System.out.println();
    }
    
    public void printArray(double [] a){
    	for(double d: a){
    		System.out.print(d + " ");
    	}
    	System.out.println();
    }

    public int move(double[] output) {

        double r = Math.random();
        if (r < output[0]) {
            if (Current.round.pot.minBet - super.currentBet == 0) {
                return 0;
            } else {
                return -1;
            }
        } else if (r < output[0] + output[1]) {
            return 0;
        } else if (r < output[0] + output[1] + output[2]) {
            return (int) (Current.round.pot.totalPot() * 0.5);
        } else {
            return (int) (Current.round.pot.totalPot());
        }
    }

    /*
     Hand equity
     M ratio - primarily for preflop
     Ratio of pot to average stack size - primarily for flop
     Position
     Array of booleans specifying which players are active and still able to bet
     Effective stack size
     Pot odds
     Array of play styles - VPIP, PFR for preflop, AF for postflop
     */
    // Parameters - relevantPlayers (just call method), stackSizes, bigBlind, pot, seat (integer 1-3 with 1 representing button), activePlayers, canCheck (just call method), minBet, legalActions
    // Inputs
    public double getEquity(int[][] hand, ArrayList<int[]> board, int activeOpp) {

        boolean[][] used = new boolean[13][4];
        int wins = 0;
        int losses = 0;
        int ties = 0;
        for (int[] i : hand) {
            used[i[0] - 1][i[1] - 1] = true;
        }
        for (int[] i : board) {
            used[i[0] - 1][i[1] - 1] = true;
        }

        for (int i = 0; i < 12; i++) {
            ArrayList<int[]> added = new ArrayList<>();
            while (board.size() != 5) {
                int[] store = new int[]{(int) (Math.random() * 13) + 1, (int) (Math.random() * 4) + 1};
                if (used[store[0] - 1][store[1] - 1]) {
                    continue;
                }
                used[store[0] - 1][store[1] - 1] = true;
                added.add(store);
                board.add(store);
            }
            board.add(hand[0]);
            board.add(hand[1]);
            int score = Round.score(board);
            board.remove(board.size() - 1);
            board.remove(board.size() - 1);
            for (int j = 0; j < 2; j++) {
                ArrayList<int[]> added1 = new ArrayList<>();
                while (board.size() != 7) {
                    int[] store = new int[]{(int) (Math.random() * 13) + 1, (int) (Math.random() * 4) + 1};
                    if (used[store[0] - 1][store[1] - 1]) {
                        continue;
                    }
                    used[store[0] - 1][store[1] - 1] = true;
                    added1.add(store);
                    board.add(store);
                }
                int oppScore = Round.score(board);
                if (score > oppScore) {
                    wins++;
                } else if (score < oppScore) {
                    losses += activeOpp;
                } else {
                    ties++;
                }
                while (added1.size() != 0) {
                    int[] store = added1.remove(0);
                    used[store[0] - 1][store[1] - 1] = false;
                    board.remove(board.size() - 1);
                }
            }
            while (added.size() != 0) {
                int[] store = added.remove(0);
                used[store[0] - 1][store[1] - 1] = false;
                board.remove(board.size() - 1);
            }
        }
    // Return equity

        return (double) (wins + .5 * ties) / (wins + losses + ties);
    }

    // Primarily for preflop
    public double[] getMRatios(boolean[] relevantPlayers, int[] stackSizes, int bb) {
        int sum = 3 / 2 * bb;
        double[] m = new double[3];
        for (int i = 0; i < 3; i++) {
            if (relevantPlayers[i]) {
                m[i] = stackSizes[i] / sum;
                if (m[i] < 1.0) {
                    m[i] = 1.0;
                }
            }
        }
        return m;
    }

    // Primarily for postflop
    public double[] getPSRatios(boolean[] relevantPlayers, int[] stackSizes, int potSize) {
        double[] ps = new double[3];
        for (int i = 0; i < 3; i++) {
            if (relevantPlayers[i]) {
                ps[i] = potSize / stackSizes[i];
            }
        }
        return ps;
    }

    public boolean[] getRelevantPlayers(int[] stackSizes, boolean[] activePlayers) {
        boolean[] p = new boolean[3];
        for (int i = 0; i < 3; i++) {
            if (activePlayers[i] && stackSizes[i] > 0) {
                p[i] = true;
            }
        }
        return p;
    }

    //
    public int getEffectiveStackSize(boolean[] relevantPlayers, int[] stackSizes) {
        ArrayList<Integer> s = new ArrayList<Integer>();
        for (int i = 0; i < relevantPlayers.length; i++) {
            if (relevantPlayers[i] && i != 0) {
                s.add(stackSizes[i]);
            }
        }
        int x;
        if (s.size() > 1) {
            x = Math.max(s.get(0), s.get(1));
        } else {
            x = stackSizes[0];
        }
        return Math.min(stackSizes[0], x);
    }

    /*
    public double getPotOdds(boolean canCheck, int callAmount, int potSize) {
        if (canCheck) {
            return 3;
        } else {
            return (double) callAmount / (callAmount + potSize);
        }
    }
    */

    public void updateStats(String lastActionsAsString) {
        String[] lastActions = lastActionsAsString.split(" ");
        for (String s : lastActions) {

            String[] split = s.split(":");
            String action = split[0];

            // Should run only once per game
            if (action.equals("RAISE")) {
            	int pos = Integer.parseInt(split[2]);
            	if (pos == leftPlayer) {
            		overallAggr[0][0]++;
            		if (recentAggr.get(0).size() < 25) {
            			recentAggr.get(0).add(2);
                	}
            		else {
            			recentAggr.get(0).remove(0);
            			recentAggr.get(0).add(2);
            		}
            	}
            	else if (pos == rightPlayer) {
            		overallAggr[1][0]++;
            		if (recentAggr.get(1).size() < 25) {
            			recentAggr.get(1).add(2);
                	}
            		else {
            			recentAggr.get(1).remove(0);
            			recentAggr.get(1).add(2);
            		}
            	}
            }
            else if (action.equals("CALL")) {
            	int pos = Integer.parseInt(split[2]);
            	if (pos == leftPlayer) {
            		overallAggr[0][1]++;
            		if (recentAggr.get(0).size() < 25) {
            			recentAggr.get(0).add(1);
                	}
            		else {
            			recentAggr.get(0).remove(0);
            			recentAggr.get(0).add(1);
            		}
            	}
            	else if (pos == rightPlayer) {
            		overallAggr[1][1]++;
            		if (recentAggr.get(1).size() < 25) {
            			recentAggr.get(1).add(1);
                	}
            		else {
            			recentAggr.get(1).remove(0);
            			recentAggr.get(1).add(1);
            		}
            	}
            }
            else if (action.equals("CHECK") || action.equals("FOLD")) {
            	int pos = Integer.parseInt(split[1]);
            	if (pos == leftPlayer) {
            		overallAggr[0][2]++;
            		if (recentAggr.get(0).size() < 25) {
            			recentAggr.get(0).add(0);
                	}
            		else {
            			recentAggr.get(0).remove(0);
            			recentAggr.get(0).add(0);
            		}
            	}
            	else if (pos == rightPlayer) {
            		overallAggr[1][2]++;
            		if (recentAggr.get(1).size() < 25) {
            			recentAggr.get(1).add(0);
                	}
            		else {
            			recentAggr.get(1).remove(0);
            			recentAggr.get(1).add(0);
            		}
            	}
            }
        }
    }

    public double[] getVPIPs() {
        double[] VPIPs = new double[2];
        for (int i = 0; i < 2; i++) {
            int n = VPIP[i][1];
            if (n != 0) {
                VPIPs[i] = VPIP[i][0] / (double)n;
            } else {
                VPIPs[i] = 0;
            }
        }
        return VPIPs;
    }

    public double[] getPFRs() {
        double[] PFRs = new double[2];
        for (int i = 0; i < 2; i++) {
            int n = PFR[i][1];
            if (n != 0) {
                PFRs[i] = PFR[i][0] / (double)n;
            } else {
                PFRs[i] = 0;
            }
        }
        return PFRs;
    }

    public double[] getAFs() {
        double[] AFs = new double[2];
        for (int i = 0; i < 2; i++) {
            int n = AF[i][1];
            if (n != 0) {
                AFs[i] = AF[i][0] / (double) n;
            } else {
                AFs[i] = 3;
            }
        }
        return AFs;
    }

    public double[] getAFqs() {
        double[] AFqs = new double[2];
        for (int i = 0; i < 2; i++) {
            int n = AFq[i][1];
            if (n != 0) {
                AFqs[i] = AFq[i][0] / (double) n;
            } else {
                AFqs[i] = 0;
            }
        }
        return AFqs;
    }

    // Helper methods
    public boolean canCheck(String[] legalActions) {
        return Arrays.asList(legalActions).contains("CHECK");
    }

    @Override
    public void handOver() {
        flopDealt = false;
        hasContributed = new boolean[2];
        hasRaised = new boolean[2];
        updateStats(history.get(history.size() - 1));
    }

}
