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

	public String toString() {
		return "GameState [gameId=" + gameId + ", gameManagerHost="
				+ gameManagerHost + ", infoUrl=" + infoUrl
				+ ", currentSmallBlind=" + currentSmallBlind
				+ ", currentBigBlind=" + currentBigBlind + ", smallBet="
				+ smallBet + ", bigBet=" + bigBet + ", rake=" + rake
				+ ", ante=" + ante + ", maxRaisesPerRound=" + maxRaisesPerRound
				+ ", playerStakes=" + playerStakes + "]";
	}
}
