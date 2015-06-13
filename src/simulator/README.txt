Here is a list of variables and methods a player can access on his or her turn:

Number of players - Current.simulator.numPlayers - returns number of players with nonzero money
Position - Current.round.position(int playerNum) - returns position relative to small blind. SmallBlind = 0, dealer=numPlayers-1
Current Position - Current.round.currentPosition(int playerNum) - returns position relative to small blind, considers only players who have not folded
Hand - hand - holds 2 cards as an int[2][2]. Each card is an int[]. The first number is from 1 to 13 inclusive. The second is from 1 to 4 inclusive.
List of Players - Current.players - returns all players still in game as an array list of Player objects
Current Bet - Current.players.get(i).currentBet - amount of money a player has put into the pot
Money - Current.players.get(i).money - amount of money a player currently has
Active Status - Current.round.isActive[playerNum] - returns True if player has not yet folded
List of Revealed Cards - Current.round.deck.shown - returns array list of all shown cards. See "Hand" for card meanings
Total Pot - Current.round.pot.totalPot - returns amount of money currently in pot
Minimum Bet - Current.round.pot.minBet - returns minimum bet to stay in game (or all-in). Take one's minBet-currentBet to figure amount needed to call
Round Number - Current.round.roundNum - returns current round number
Round History - Current.round.roundHistory - returns a chronological array list of Move objects, reflecting moves made in a given round. Each Move object records the player, round number, move made ('f' - fold, 'c' - call, 'r' - raise, 'a' - allin, 'C' - check), and call amount and raise amount if relevant.
Current Phase - phase - returns char reflecting current phase, ('p' - preflop, 'f' - flop, 't' - turn, 'r' - river)
Small Blind/Big Blind Amount - Current.simulator.sBlindNum/Current.simulator.bBlindNum