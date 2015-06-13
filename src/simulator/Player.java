package simulator;

import java.util.ArrayList;

/**
 *
 * @author dzd123
 */
public abstract class Player {

    public int money;
    private int[][] hand = new int[2][2];
    public static int playerCount;
    public int playerNum;
    public int absPlayerNum;
    public int currentBet;
    public ArrayList<String> history;
    public boolean isDealer = false;

    public Player() {
        this.absPlayerNum = playerCount;
        playerCount += 1;
        history = new ArrayList<>();
    }
    
    public void init(int money, int playerNum) {
        this.money = money;
        this.playerNum = playerNum;

    }

    public final int[][] getHand() {
        return hand;
    }

    public final void clearHand() {
        hand = new int[2][2];
    }
    public abstract void handOver();
    public abstract void newHand();
    public abstract int makeMove(char phase);

    public final void receiveCards(int[][] cards) {
        hand = cards;
    }

    @Override
    public String toString() {
        return "" + absPlayerNum;
    }
}
