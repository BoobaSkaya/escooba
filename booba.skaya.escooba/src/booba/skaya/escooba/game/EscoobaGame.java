package booba.skaya.escooba.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import booba.skaya.escooba.util.Combinator;

public class EscoobaGame implements Serializable {
	
	private static final long serialVersionUID = -7716044527403680360L;

	private static final int CARD_PER_COLOR = 10;
	/** The number of players */
	public static final int PLAYERS_NB = 4;
	/** The number of card in a player's hand*/
	private static final int HAND_SIZE = 3;
	/** The number of card on the table*/
	private static final int TABLE_CARD_NUMBER = 4;

	/** Card sum must be 15*/
	private static final int ESCOBA_TARGET_VALUE = 15;

	/** The cards ordered */
	private final ArrayList<EscoobaCard> _deck;
	
	/** The cards pile - usually shuffled*/
	private final ArrayList<EscoobaCard> _pile;
	
	/** The cards on the table */
	private final ArrayList<EscoobaCard> _table;
	
	/**The players */
	private final ArrayList<EscoobaPlayer> _players;
	
	private transient EscoobagameListener _listener;
	
	private int _currentPlayer;
	private int _round;
	
	public EscoobaGame() {
		_deck = new ArrayList<EscoobaCard>();
		_pile  = new ArrayList<EscoobaCard>();
		_table = new ArrayList<EscoobaCard>();
		_players = new ArrayList<EscoobaPlayer>();
		initDeck();
		initPlayers();
		newGame();
		newRound();
		warnListener();
	}

	private void initPlayers() {
		for(int i = 0;i<PLAYERS_NB;i++){
			_players.add(new EscoobaPlayer(i));
		}
	}

	private void initDeck(){
		_deck.clear();
		for(EscoobaColor color : EscoobaColor.values()){
			//init 10 cards per color
			for(int cardValue = 0;cardValue<CARD_PER_COLOR;cardValue++){
				//card value start at 1 (cardValue+1)
				_deck.add(new EscoobaCard(color, cardValue+1));
			}
		}
		//card has been initialized
	}

	/**
	 * Init a new round is:
	 * - empty player's hands
	 * - shuffling the decks
	 */
	public void newGame() {
		for(EscoobaPlayer p : _players){
			p.newGame();
		}
		_pile.clear();
		ArrayList<EscoobaCard> toto = new ArrayList<EscoobaCard>(_deck);
		while(!toto.isEmpty()){
			int randomRank = (int) (Math.random() * toto.size());
			_pile.add(toto.remove(randomRank));
		}
		_table.clear();
		//put 4 card on table 
		for(int i = 0;i<TABLE_CARD_NUMBER;i++){
			_table.add(_pile.remove(0));
		}
		_round = 0;
		warnListener();
	}
	
	public void newRound() {
		_round++;
		//give 3 cards to each player
		for(int i = 0;i<HAND_SIZE;i++){
			for(EscoobaPlayer p : _players){
				if(_pile.size() > 0){
					p.receiveCardToHand(_pile.remove(0));
				}
			}
		}
		_currentPlayer = 0;
		warnListener();
	}
	
	public void registerGameListener(EscoobagameListener listener){
		_listener = listener;
	}
	
	private void warnListener(){
		if(_listener != null){
			_listener.somethingHappen();
		}
	}

	public Collection<EscoobaCard> getTableCards() {
		return _table;
	}
	
	public Collection<EscoobaCard> getPlayerHand(int playerId) {
		return _players.get(playerId).getHand();
	}
	
	public void play(ArrayList<EscoobaCard> playedCards){
		if(playedCards != null && isPossible(playedCards)){
			if(playedCards.size() > 1){ //some table cards means the player pick up cards
				getCurrentPlayer().addTrick(playedCards, playedCards.size() == _table.size()+1);
			}else if(playedCards.size() == 1){
				//ok player must throw his card
				_table.add(playedCards.get(0));
			}
			_table.removeAll(playedCards.subList(1, playedCards.size()));
			getCurrentPlayer().removeCard(playedCards.get(0));
			//move to next player !
			_currentPlayer = (_currentPlayer + 1) % PLAYERS_NB;
			//if next player has no more card, start new round
			if(getCurrentPlayer().getHand().size() == 0){
				newRound();
			}
		}
	}
	
	public EscoobaPlayer getCurrentPlayer() {
		return _players.get(_currentPlayer);
	}

	public boolean isPossible(ArrayList<EscoobaCard> playedCards){
		if(playedCards != null && playedCards.size() > 1){
			int sum = 0;
			for(EscoobaCard card :playedCards){
				sum += card.getValue();
			}
			return sum == ESCOBA_TARGET_VALUE;
		}else if( playedCards != null && playedCards.size() == 1){
			return true;
		}
		return false;
	}

	public int getRound() {
		return _round;
	}

	public void setRound(int round) {
		_round = round;
	}
	
	public int[] getRoundScore(){
		int[] score = new int[PLAYERS_NB];
		//rule 1: 1 pt for the player with max card
		ArrayList<EscoobaPlayer> maxCardPlayers = getPlayerWithMax(new ScoreCounterInterface() {
			@Override
			public int score(EscoobaPlayer p) {
				return p.getTrickSize();
			}
		});
		if(maxCardPlayers.size() == 1){
			score[maxCardPlayers.get(0).getId()] += 1;
		}
		//Rule 2: 1pt for the player with max ORO
		ArrayList<EscoobaPlayer> maxOrosPlayers = getPlayerWithMax(new ScoreCounterInterface() {
			@Override
			public int score(EscoobaPlayer p) {
				return p.getOrosNb();
			}
		});
		if(maxOrosPlayers.size() == 1){
			score[maxOrosPlayers.get(0).getId()] += 1;
		}
		//Rule 3: 1pt for the player with the 7 de ORO
		for(EscoobaPlayer p : _players){
			if(p.has7deOro()){
				score[p.getId()]+=1;
				break;
			}
		}
		//Rule 4: Add escobas
		for(EscoobaPlayer p : _players){
			score[p.getId()]+=p.getEscobaNumber();
		}
		return score;
	}

	
	private ArrayList<EscoobaPlayer> getPlayerWithMax(ScoreCounterInterface counter) {
		ArrayList<EscoobaPlayer> playersWithMax = new ArrayList<EscoobaPlayer>();
		int maxNb       = counter.score(_players.get(0));
		for(int i =1; i < PLAYERS_NB; i++){
			int playerScore = counter.score(_players.get(i));
			if(playerScore > maxNb){
				//store the new max 
				// - remove previous maxes
				maxNb = playerScore;
				playersWithMax.clear();
				playersWithMax.add(_players.get(i));
			}else if(playerScore == maxNb){
				//There is a TIE - no points will be score if no further max is found
				playersWithMax.add(_players.get(i));
			}
		}
		return playersWithMax;
	}
	
	public void autoplay() {
		//ok we want to autoplay - autoplay
		//Compute all possible plays
		ArrayList<ArrayList<EscoobaCard>> allCombinations 	   = Combinator.combinations(_table);
		ArrayList<ArrayList<EscoobaCard>> possibleCombinations = new ArrayList<ArrayList<EscoobaCard>>();
		for(EscoobaCard c :getCurrentPlayer().getHand()){
			//Test this card against each possible combination
			for(ArrayList<EscoobaCard> combination : allCombinations){
				ArrayList<EscoobaCard> play = new ArrayList<EscoobaCard>();
				play.add(c);
				play.addAll(combination);
				if(isPossible(play)){
					possibleCombinations.add(play);
				}
			}
			//add lonely card play
			ArrayList<EscoobaCard> play = new ArrayList<EscoobaCard>();
			play.add(c);
			possibleCombinations.add(play);
		}
		//we have all the possible combination in possibleCombinations
		//choose the first for now
		play(possibleCombinations.get(0));
	}
	
	interface ScoreCounterInterface {
		int score(EscoobaPlayer p);
	}

	public Collection<EscoobaPlayer> getPlayers() {
		return _players;
		
	}
}