package simulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;

/**
 *
 * @author dzd123
 */
public class Round {

    public Player sBlind;
    public Player bBlind;
    public Player dealer;
    public Deck deck;
    public int roundNum;
    public Pot pot;
    public boolean[] isActive;
    public Simulator simulator;
    public ArrayList<Move> roundHistory;
    
    public Round(int roundNum, int dealerPlayerNum) {
        //reset current bet
        this.pot = new Pot();
        this.deck = new Deck();
        this.sBlind = Current.players.get((dealerPlayerNum + 1) % Current.simulator.numPlayers);
        this.bBlind = Current.players.get((dealerPlayerNum + 2) % Current.simulator.numPlayers);
        this.dealer = Current.players.get((dealerPlayerNum) % Current.simulator.numPlayers);
        this.roundHistory = new ArrayList<>();
        this.roundNum = roundNum;
    }
    public Round(int roundNum, int dealerPlayerNum, Stack<int[]> seed) {
        //reset current bet
        this.pot = new Pot();
        this.deck = new Deck(seed);
        this.sBlind = Current.players.get((dealerPlayerNum + 1) % Current.simulator.numPlayers);
        this.bBlind = Current.players.get((dealerPlayerNum + 2) % Current.simulator.numPlayers);
        this.dealer = Current.players.get((dealerPlayerNum) % Current.simulator.numPlayers);
        this.roundHistory = new ArrayList<>();
        this.roundNum = roundNum;
    }
    public int position(int playerNum) {
        //returns 0 if small blind to numPlayers-1 if dealer
        return (playerNum - sBlind.playerNum) % Current.simulator.numPlayers;
    }

    public int currentPosition(int playerNum) {
        int counter = 0;
        for (int i = sBlind.playerNum; i < Current.simulator.numPlayers; i++) {
            if (i == playerNum) {
                return counter;
            } else if (isActive[i]) {
                counter++;
            }
        }
        for (int i = 0; i < playerNum; i++) {
            if (isActive[i]) {
                counter++;
            }
        }
        return counter;
    }

    public ArrayList<Move> run() {
        int[] flipNum = new int[]{0, 3, 1, 1};
        char[] phase = new char[]{'p', 'f', 't', 'r'};
        isActive = new boolean[Current.simulator.numPlayers];
        Arrays.fill(isActive, true);
        Queue<Player> queue = new LinkedList<Player>();
        pot.minBet = Current.simulator.bBlindNum;
        deck.dealToAll();
        String stacks = "";
        for (Player j : Current.players) {
            stacks += j.money + " ";
        }
        for (Player j : Current.players) {

            j.history.add("NEWHAND " + Deck.cardToString(j.getHand()[0]) + " " + Deck.cardToString(j.getHand()[1]) + " " + stacks);
            j.newHand();
        }
        for (Player i : Current.players) {
            i.currentBet = 0;

            //add small blind and big blind amounts
            if (i.playerNum == sBlind.playerNum) {
                if (i.money >= Current.simulator.sBlindNum) {
                    i.money -= Current.simulator.sBlindNum;
                    pot.pot[i.playerNum] += Current.simulator.sBlindNum;
                    i.currentBet += Current.simulator.sBlindNum;
                    roundHistory.add(new Move(i, "POST:" + i.currentBet));
                } else {
                    i.currentBet = i.money;
                    pot.pot[i.playerNum] += i.money;
                    i.money = 0;
                    roundHistory.add(new Move(i, "POST:" + i.currentBet));
                }
            } else if (i.playerNum == bBlind.playerNum) {
                if (i.money >= Current.simulator.bBlindNum) {
                    i.money -= Current.simulator.bBlindNum;
                    pot.pot[i.playerNum] += Current.simulator.bBlindNum;
                    i.currentBet += Current.simulator.bBlindNum;
                    roundHistory.add(new Move(i, "POST:" + i.currentBet));
                } else {
                    i.currentBet = i.money;
                    pot.pot[i.playerNum] += i.money;
                    i.money = 0;
                    roundHistory.add(new Move(i, "POST:" + i.currentBet));
                }
            }
            if (i.money != 0) {
                queue.add(i);
            }
        }

        cycleTo(queue, (bBlind.playerNum + 1) % Current.simulator.numPlayers);
        for (int i = 0; i < 4; i++) {
            deck.flip(flipNum[i]);
            switch (i) {
                case 0:
                    break;
                case 1:
                    roundHistory.add(new Move("DEAL:" + "FLOP"));
                    break;
                case 2:
                    roundHistory.add(new Move("DEAL:" + "TURN"));
                    break;
                case 3:
                    roundHistory.add(new Move("DEAL:" + "RIVER"));
                    break;
            }
            int numCalls = 0;

            while (numCalls < queue.size() && queue.size() > 1) {
                Player currPlayer = queue.poll();
                int counter = 0;
                String handhistory = "";
                for (int j = roundHistory.size() - 1; j >= 0; j--) {
                    if (roundHistory.get(j).player != null) {
                        handhistory = roundHistory.get(j).move + ":" + roundHistory.get(j).player.absPlayerNum + " " + handhistory;
                    } else {
                        handhistory = roundHistory.get(j).move + " " + handhistory;
                    }
                    counter++;
                    if (roundHistory.get(j).player != null && roundHistory.get(j).player.equals(currPlayer)) {
                        break;
                    }
                }
                String stacksizes = "";
                for (Player j : Current.players) {
                    stacksizes += j.money + " ";
                }
                String boards = "";
                for (int[] j : Current.round.deck.shown) {
                    boards = Deck.cardToString(j) + " " + boards;
                }
                currPlayer.history.add("GETACTION " + Current.round.pot.totalPot() + " " + Current.round.deck.shown.size() + " " + boards + stacksizes + counter + " " + handhistory);

                int move = currPlayer.makeMove(phase[i]);
                if (move < 0) {
                    isActive[currPlayer.playerNum] = false;
                    roundHistory.add(new Move(currPlayer, "FOLD"));
                } else {
                    int diff = pot.minBet - currPlayer.currentBet;
                    if (diff <= currPlayer.money) {
                        numCalls++;
                        currPlayer.money -= diff;
                        currPlayer.currentBet = pot.minBet;
                        pot.pot[currPlayer.playerNum] += diff;

                        queue.add(currPlayer);
                        if (diff == 0) {
                            roundHistory.add(new Move(currPlayer, "CHECK"));
                        } else {
                            roundHistory.add(new Move(currPlayer, "CALL:" + pot.minBet));
                        }
                        if (move > 0) {
                            if (move <= currPlayer.money) {
                                currPlayer.money -= move;
                                pot.pot[currPlayer.playerNum] += move;
                                currPlayer.currentBet += move;
                                pot.minBet = currPlayer.currentBet;
                                numCalls = 1;
                                roundHistory.remove(roundHistory.size() - 1);
                                if (diff == 0) {
                                    roundHistory.add(new Move(currPlayer, "BET:" + pot.minBet));
                                } else {
                                    roundHistory.add(new Move(currPlayer, "RAISE:" + pot.minBet));
                                }
                            } else if (currPlayer.money != 0) {
                                currPlayer.currentBet += currPlayer.money;
                                pot.pot[currPlayer.playerNum] += currPlayer.money;
                                pot.minBet = currPlayer.currentBet;
                                roundHistory.remove(roundHistory.size() - 1);
                                roundHistory.add(new Move(currPlayer, "RAISE:" + pot.minBet));
                                currPlayer.money = 0;
                                numCalls = 1;
                            }
                        }
                    } else {
                        currPlayer.currentBet += currPlayer.money;
                        pot.pot[currPlayer.playerNum] += currPlayer.money;
                        roundHistory.add(new Move(currPlayer, "RAISE:" + pot.minBet));
                        currPlayer.money = 0;
                    }
                }
            }
            if (queue.size() <= 1) {
                int won = 0;
                for (int j = 0; j < pot.pot.length; j++) {
                    queue.peek().money += pot.pot[j];
                    won += pot.pot[j];
                }
                roundHistory.add(new Move(queue.peek(), "WIN:" + won));
                String stacksizes = "";
                for (Player j : Current.players) {
                    stacksizes +=  j.money + " ";
                }

                String boards = "";
                for (int[] j : Current.round.deck.shown) {
                    boards += Deck.cardToString(j) + " ";

                }

                for (Player k : Current.players) {
                    String handhistory = "";
                    int counter = 0;
                    for (int j = roundHistory.size() - 1; j >= 0; j--) {
                        if (roundHistory.get(j).player != null) {
                            handhistory = roundHistory.get(j).move + ":" + roundHistory.get(j).player.absPlayerNum + " " + handhistory;
                        } else {
                            handhistory = roundHistory.get(j).move + " " + handhistory;
                        }
                        counter++;
                        if (roundHistory.get(j).player != null && roundHistory.get(j).player.equals(k) && !roundHistory.get(j).move.contains("SHOW")&&!roundHistory.get(j).move.contains("TIE")&&!roundHistory.get(j).move.contains("WIN")) {
                             break;
                        }
                    }
                    k.history.add("HANDOVER " + stacksizes + Current.round.deck.shown.size() + " " + boards + counter + " " + handhistory);
                    k.handOver();
                }
                return roundHistory;
            }
            cycleTo(queue, sBlind.playerNum);
        }
        distribute();
        for (int i = 0; i < Current.simulator.numPlayers; i++) {
            Current.players.get(i).clearHand();
        }
        return roundHistory;
    }

    public static void cycleTo(Queue<Player> queue, int playerNum) {
        if (queue.size() <= 1) {
            return;
        }
        Player previous = queue.poll();
        queue.add(previous);
        while (true) {
            if ((previous.playerNum < playerNum && playerNum <= queue.peek().playerNum) || (previous.playerNum > queue.peek().playerNum && (playerNum > previous.playerNum || playerNum <= queue.peek().playerNum))) {
                return;
            } else {
                previous = queue.poll();
                queue.add(previous);
            }
        }

    }

    public static int highscore(int current, int updated) {
        if (updated > current) {
            return updated;
        } else {
            return current;
        }
    }

    public static int updateHighScore(ArrayList<int[]> cards,int highscore){
        
    	
                if (cards.get(0)[0] == cards.get(3)[0] || cards.get(1)[0] == cards.get(4)[0]) {
                    if (cards.get(0)[0] == cards.get(1)[0]) {
                        return 14 * 14 * 14 * 14 * 14 * 7 + 14 * cards.get(2)[0] + cards.get(4)[0];
                    } else {
                        return 14 * 14 * 14 * 14 * 14 * 7 + 14 * cards.get(2)[0] + cards.get(0)[0];
                    }
                } else if (cards.get(0)[0] == cards.get(2)[0]) {
                    if (cards.get(3)[0] == cards.get(4)[0]) {
                        return 14 * 14 * 14 * 14 * 14 * 6 + 14 * cards.get(0)[0] + cards.get(3)[0];
                    } else {
                        return 14 * 14 * 14 * 14 * 14 * 3 + 14 * 14 * cards.get(0)[0] + 14 * cards.get(4)[0] + cards.get(3)[0];
                    }
                } else if (cards.get(2)[0] == cards.get(4)[0]) {
                    if (cards.get(0)[0] == cards.get(1)[0]) {
                        return 14 * 14 * 14 * 14 * 14 * 6 + 14 * cards.get(2)[0] + cards.get(0)[0];
                    } else {
                        return 14 * 14 * 14 * 14 * 14 * 3 + 14 * 14 * cards.get(2)[0] + 14 * cards.get(1)[0] + cards.get(0)[0];
                    }
                } else if (cards.get(1)[0] == cards.get(3)[0]) {
                    return 14 * 14 * 14 * 14 * 14 * 3 + 14 * 14 * cards.get(1)[0] + 14 * cards.get(4)[0] + cards.get(0)[0];
                } else if (cards.get(0)[0] == cards.get(1)[0]) {
                    if (cards.get(2)[0] == cards.get(3)[0]) {
                        return 14 * 14 * 14 * 14 * 14 * 2 + 14 * 14 * cards.get(2)[0] + 14 * cards.get(0)[0] + cards.get(4)[0];
                    } else if (cards.get(3)[0] == cards.get(4)[0]) {
                        return 14 * 14 * 14 * 14 * 14 * 2 + 14 * 14 * cards.get(3)[0] + 14 * cards.get(0)[0] + cards.get(2)[0];
                    } else {
                        return 14 * 14 * 14 * 14 * 14 + 14 * 14 * 14 * cards.get(0)[0] + 14 * 14 * cards.get(4)[0] + 14 * cards.get(3)[0] + cards.get(2)[0];
                    }
                } else if (cards.get(1)[0] == cards.get(2)[0]) {
                    if (cards.get(3)[0] == cards.get(4)[0]) {
                        return 14 * 14 * 14 * 14 * 14 * 2 + 14 * 14 * cards.get(3)[0] + 14 * cards.get(1)[0] + cards.get(0)[0];
                    } else {
                        return 14 * 14 * 14 * 14 * 14 + 14 * 14 * 14 * cards.get(1)[0] + 14 * 14 * cards.get(4)[0] + 14 * cards.get(3)[0] + cards.get(0)[0];
                    }
                } else if (cards.get(2)[0] == cards.get(3)[0]) {
                    return 14 * 14 * 14 * 14 * 14 + 14 * 14 * 14 * cards.get(2)[0] + 14 * 14 * cards.get(4)[0] + 14 * cards.get(1)[0] + cards.get(0)[0];
                } else if (cards.get(3)[0] == cards.get(4)[0]) {
                    return 14 * 14 * 14 * 14 * 14 + 14 * 14 * 14 * cards.get(3)[0] + 14 * 14 * cards.get(2)[0] + 14 * cards.get(1)[0] + cards.get(0)[0];
                } else if (cards.get(0)[0] + 4 == cards.get(4)[0]) {
                    return Math.max(14 * 14 * 14 * 14 * 14 * 4 + cards.get(4)[0],14 * 14 * 14 * 14 * 14 * 3 + flush(cards));
                    
                } else {
                   return Math.max( 14 * 14 * 14 * 14 * cards.get(4)[0] + 14 * 14 * 14 * cards.get(3)[0] + 14 * 14 * cards.get(2)[0] + 14 * cards.get(1)[0] + cards.get(0)[0],flush(cards));
                }
                
    }
    public static int score(ArrayList<int[]> cards){
        ArrayList<int[]> sorted = (ArrayList<int[]>)cards.clone();
        Collections.sort(sorted, new Comparator<int[]>() {
            @Override
            public int compare(int[]  o1, int[]  o2)
            {

                return  o1[0]-o2[0];
            }
        });
        return score(sorted,sorted.size(),0,0);
    }
    public static int score(ArrayList<int[]> cards,int num,int ind,int highscore) {
        if(num==5){
            return updateHighScore(cards,highscore);
        }
        
        for (int i = ind; i < num; i++) {
            int[] first = cards.remove(i);
            highscore = highscore(highscore,score(cards,num-1,i,highscore));
            cards.add(i,first);
        }
        return highscore;
    }
    public static int flush(ArrayList<int[]> cards) {
        if (cards.get(0)[1] == cards.get(1)[1] && cards.get(0)[1] == cards.get(2)[1] && cards.get(0)[1] == cards.get(3)[1] && cards.get(0)[1] == cards.get(4)[1]) {
            return 14 * 14 * 14 * 14 * 14 * 5 + 14 * 14 * 14 * 14 * cards.get(4)[0] + 14 * 14 * 14 * cards.get(3)[0] + 14 * 14 * cards.get(2)[0] + 14 * cards.get(1)[0] + cards.get(0)[0];
        } else {
            return 0;
        }
    }

    public static String scoreToHand(int score) {
        int hand = score / (14 * 14 * 14 * 14 * 14);
        String msg = "";
        switch (hand) {
            case 0:
                msg += "best card ";
                break;
            case 1:
                msg += "pair ";
                break;
            case 2:
                msg += "two pair ";
                break;
            case 3:
                msg += "triple ";
                break;
            case 4:
                msg += "straight ";
                break;
            case 5:
                msg += "flush ";
                break;
            case 6:
                msg += "full house ";
                break;
            case 7:
                msg += "four of a kind ";
                break;
            case 8:
                msg += "straight flush ";
                break;

        }
        score -= 14 * 14 * 14 * 14 * 14 * hand;
        msg += score / (14 * 14 * 14 * 14) + " ";
        score -= (score / (14 * 14 * 14 * 14)) * 14 * 14 * 14 * 14;
        msg += score / (14 * 14 * 14) + " ";
        score -= (score / (14 * 14 * 14)) * 14 * 14 * 14;
        msg += score / (14 * 14) + " ";
        score -= (score / (14 * 14)) * 14 * 14;
        msg += score / (14) + " ";
        score -= (score / 14) * 14;
        msg += score + " ";
        return msg;
    }

    public void distribute() {
        int[][] scores = new int[Current.simulator.numPlayers][2];
        for (Player i : Current.players) {
            if (isActive[i.playerNum]) {
                roundHistory.add(new Move(i, "SHOW:" + Deck.cardToString(i.getHand()[0]) + Deck.cardToString(i.getHand()[1])));
                
                ArrayList<int[]> combined = (ArrayList<int[]>) deck.shown.clone();
                combined.add(i.getHand()[0]);
                combined.add(i.getHand()[1]);
                scores[i.playerNum][0] = score(combined);
                scores[i.playerNum][1] = i.playerNum;
            }
        }

        Arrays.sort(scores, new Comparator<int[]>() {
            @Override
            public int compare(int[] o1, int[] o2) {
                if (o1[0] > o2[0]) {
                    return -1;
                }
                return 1;
            }
        });

        ArrayList<ArrayList<Integer>> bid = new ArrayList<>();
        for (int i = 0; i < pot.pot.length; i++) {
            bid.add(new ArrayList<Integer>());
        }
        int[] won = new int[Current.players.size()];
        boolean[] isTie = new boolean[Current.players.size()];
        for (int i = 0; i < scores.length; i++) {
            int moneyleft = 0;

            for (int j = 0; j < pot.pot.length; j++) {
                moneyleft += pot.pot[j];
                if (Current.players.get(scores[i][1]).currentBet >= pot.pot[j]) {

                    bid.get(j).add(scores[i][1]);
                }
            }
            if (moneyleft == 0) {
                for (int j = 0; j < won.length; j++) {
                    if (won[j] != 0) {
                        if (!isTie[j]) {
                            roundHistory.add(new Move(Current.players.get(j), "WIN:" + won[j]));
                        } else {
                            roundHistory.add(new Move(Current.players.get(j), "TIE:" + won[j]));
                        }
                    }
                }
                break;
            }
            if (i == scores.length - 1 || scores[i][0] > scores[i + 1][0]) {
                for (int j = 0; j < bid.size(); j++) {
                    for (Integer k : bid.get(j)) {
                        if (bid.get(j).size() != 1) {
                            isTie[j] = true;
                        }
                        won[k] += pot.pot[j] / bid.get(j).size();
                        Current.players.get(k).money += pot.pot[j] / bid.get(j).size();
                    }
                    if (!bid.get(j).isEmpty()) {
                        pot.pot[j] = 0;
                        bid.get(j).clear();
                    }
                }
            }
        }

        String stacksizes = "";
        for (Player j : Current.players) {
            stacksizes +=  j.money + " ";
        }

        String boards = "";
        for (int[] j : Current.round.deck.shown) {
            boards += Deck.cardToString(j) + " ";

        }

        for (Player k : Current.players) {
            String handhistory = "";
            int counter = 0;
            for (int j = roundHistory.size() - 1; j >= 0; j--) {
                if (roundHistory.get(j).player != null) {
                    handhistory = roundHistory.get(j).move + ":" + roundHistory.get(j).player.absPlayerNum + " " + handhistory;
                } else {
                    handhistory = roundHistory.get(j).move + " " + handhistory;
                }
                counter++;
                if (roundHistory.get(j).player != null && roundHistory.get(j).player.equals(k) && !roundHistory.get(j).move.contains("SHOW")&&!roundHistory.get(j).move.contains("TIE")&&!roundHistory.get(j).move.contains("WIN")) {
                    break;
                }
            }
            k.history.add("HANDOVER " + stacksizes + Current.round.deck.shown.size() + " " + boards + counter + " " + handhistory);
            k.handOver();
        }
    }

}

class Move {

    public Player player;
    public String move = null;

    public Move(Player player, String move) {
        this.player = player;
        this.move = move;

    }

    public Move(String move) {
        this.move = move;
    }
}

class Deck {

    private Stack<int[]> hidden;
    public ArrayList<int[]> shown = new ArrayList<>();

    public Deck() {
        hidden = new Stack<>();
        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 13; j++) {
                hidden.push(new int[]{j, i});
            }
        }
        shuffle();
    }
    public Deck(Stack<int[]> seed){
        hidden=seed;
    }
    public static String cardToString(int[] card) {
        String x = "";
        switch (card[0]) {
            case 1:
                x = "2";
                break;
            case 2:
                x = "3";
                break;
            case 3:
                x = "4";
                break;
            case 4:
                x = "5";
                break;
            case 5:
                x = "6";
                break;
            case 6:
                x = "7";
                break;
            case 7:
                x = "8";
                break;
            case 8:
                x = "9";
                break;
            case 9:
                x = "T";
                break;
            case 10:
                x = "J";
                break;
            case 11:
                x = "Q";
                break;
            case 12:
                x = "K";
                break;
            case 13:
                x = "A";
                break;
        }
        String y = "";
        switch (card[1]) {
            case 1:
                y = "c";
                break;
            case 2:
                y = "d";
                break;
            case 3:
                y = "h";
                break;
            case 4:
                y = "s";
                break;
        }
        return x + y;
    }

    public void dealToAll() {
        for (Player i : Current.players) {
            int[][] hand = new int[][]{hidden.pop(), hidden.pop()};
            i.receiveCards(hand);
        }
    }

    public void flip(int n) {
        for (int i = 0; i < n; i++) {
            shown.add(hidden.pop());
        }
    }

    public void shuffle() {
        Collections.shuffle(hidden);
    }
}

class Pot {

    public int minBet = 0;
    public int[] pot = new int[Current.simulator.numPlayers];

    public int totalPot() {
        int x = 0;
        for (int i = 0; i < pot.length; i++) {
            x += pot[i];
        }
        return x;
    }
}
