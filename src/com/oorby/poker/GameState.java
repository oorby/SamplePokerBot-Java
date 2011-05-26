package com.oorby.poker;

import java.util.Map;


public class GameState {
	String gameId;
	String gameManagerHost;
	String infoUrl;
	int currentSmallBlind;
	int currentBigBlind;
	int smallBet;
	int bigBet;
	int rake;
	int ante;
	int maxRaisesPerRound;
	Map<String, Player> playerStakes;
}
