package simulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

/**
 *
 * @author dzd123
 */
class TripSimulator {

    public static int[] run(ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>> pinfos) {
        ArrayList<ArrayList<int[]>> seeds = new ArrayList<>();
        
        for (int k = 0; k < 1001; k++) {
            ArrayList<int[]> seed = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                for (int j = 1; j <= 13; j++) {
                    seed.add(new int[]{j, i});
                }
            }
            Collections.shuffle(seed);
            ArrayList<int[]> adder = seed;
            seeds.add(adder);
        }
        
        int[][] orders = new int[][]{{0,1,2},{1,2,0},{2,1,0},{0,2,1},{1,0,2},{2,0,1}};
        int[] winCount=new int[3];
        for(int i=0;i<orders.length;i++){
            ArrayList<Stack<int[]>> newseed=new ArrayList<>();
            for(int j=0;j<seeds.size();j++){
            Stack<int[]> seed = new Stack<>();
            for(int k=0;k<seeds.get(j).size();k++)
                seed.push(seeds.get(j).get(k));
            newseed.add(seed);
            }
        Simulator game = new Simulator(newseed,orders[i],pinfos);
        Current.simulator = game;
        Result result = game.run();
        winCount[result.winner.absPlayerNum]++;
        }
        return winCount;
    }
}

public class Simulator {

    public ArrayList<Player> players = new ArrayList<>();
    public int numPlayers;
    public int sBlindNum = 1;
    public int bBlindNum = 2 * sBlindNum;
    public ArrayList<Player> permPlayers = new ArrayList<>();
    public ArrayList<Stack<int[]>> seeds = new ArrayList<>();
    public int[] order = null;
    ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>> pinfos = new ArrayList<>();
    public Simulator() {
        
    }

    public Simulator(ArrayList<Stack<int[]>> seeds, int[] order,ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>> pinfos) {
        this.seeds = seeds;
        this.order = order;
        this.pinfos = pinfos;
    }
    public Player getByAbs(int abs){
        for(Player i: players){
            if(i.absPlayerNum==abs)
                return i;
        }
        return null;
    }
    public void init() {
        //initalizes round number
        roundNum = 0;

        //ADD PLAYERS BELOW
        Player.playerCount = 0;
//        players.add(new ProbCall(.9));
//        players.add(new ProbCall(.7));
//        players.add(new ProbCall(.7));
        for(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> i:pinfos){
            players.add(new NeuralPlayer(i));
        }
        //ADD PLAYERS ABOVE ^
        if (order != null) {
            ArrayList<Player> neworder = new ArrayList<>();
            for (Integer i : order) {
                neworder.add(players.get(i));
            }
            players = neworder;
        }
        for (Player i : players) {
            permPlayers.add(i);
        }
        //initalizes starting money for each player
        int startMoney = 200;

        for (int i = 0; i < players.size(); i++) {
            players.get(i).init(startMoney, i);
        }
        Current.players = players;
        numPlayers = players.size();
        PokerBots.numPlayers = players.size();

    }
    private int roundNum;

    public Result run() {
        init();
        int dealer = 0;
        ArrayList<Move> history = new ArrayList<>();
        while (numPlayers > 1) {
            roundNum++;
            if (roundNum > 1000) {
                int highscore = 0;
                Player winner = null;
                for (int i = 0; i < numPlayers; i++) {
                    if (players.get(i).money > highscore) {
                        winner = players.get(i);
                        highscore = players.get(i).money;
                    }

                }

                for (int j = 0; j < permPlayers.get(0).history.size(); j++) {
                    //       System.out.println(permPlayers.get(0).history.get(j));
                }

                return new Result(winner, history);

            }
            Round round;
            if (roundNum - 1 < seeds.size()) {
                round = new Round(roundNum, dealer, seeds.get(roundNum - 1));

            } else {
                round = new Round(roundNum, dealer);
            }
            Current.round = round;

            history.addAll(round.run());
            dealer = (dealer + 1) % numPlayers;
            boolean isChanged = false;
            for (int i = 0; i < numPlayers; i++) {
                if (players.get(i).money == 0) {
                    players.remove(i);
                    numPlayers--;
                    if (dealer > i) {
                        dealer -= 1;
                    }
                    dealer %= numPlayers;
                    i--;
                    isChanged = true;
                }
            }
            if (isChanged) {
                for (int i = 0; i < numPlayers; i++) {
                    players.get(i).playerNum = i;
                }
            }
        }

        for (int j = 0; j < permPlayers.get(0).history.size(); j++) {
            //        System.out.println(permPlayers.get(0).history.get(j));
        }
        Player winner = players.get(0);
        return new Result(winner, history);
    }
}

class Result {

    ArrayList<Move> history;
    Player winner;

    public Result(Player winner, ArrayList<Move> history) {
        this.history = history;
        this.winner = winner;
    }

    @Override
    public String toString() {
        return "Winner: " + winner;
    }
}

class Current {

    public static Simulator simulator;
    public static ArrayList<Player> players;
    public static Round round;

}
