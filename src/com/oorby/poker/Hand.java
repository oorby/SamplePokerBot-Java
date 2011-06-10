package com.oorby.poker;

import java.util.List;
import java.util.Map;

import com.oorby.poker.PlayerAction.Action;

public class Hand {

	public static class AvailableAction {
		public Action action;
		public int costOfAction;

		@Override
		public String toString() {
			return action + " " + costOfAction;
		}
	}

	public static class PlayerHole {
		public String botName;
		public List<String> hole;
		public List<String> bestHand;
	}

	public static class RoundAction {
		public String botName;
		public Action action;
		public int costOfAction;
	}

	public static class Round {
		public List<RoundAction> actions;
		public int totalRoundSize;
	}

	public static class Pot {
		public int amountInPot;
		public List<String> playersInPot;
	}

	public static class HandResult {
		public String botName;
		public int netStackChange;
	}

	public int handNumber;
	public Map<String, Player> players; // botname -> player
	public boolean handComplete;
	public List<String> hole;
	public List<String> communityCards;

	public List<AvailableAction> availableActions;
	public List<PlayerHole> showdownPlayerHoles;

	public Round preFlop;
	public Round flop;
	public Round turn;
	public Round river;

	public List<Pot> pots;
	public List<HandResult> results;

	@Override
	public String toString() {
		return "Hand [handNumber=" + handNumber + ", players=" + players
				+ ", handComplete=" + handComplete + ", hole=" + hole
				+ ", communityCards=" + communityCards + ", availableActions="
				+ availableActions + ", showdownPlayerHoles="
				+ showdownPlayerHoles + ", preFlop=" + preFlop + ", flop="
				+ flop + ", turn=" + turn + ", river=" + river + ", pots="
				+ pots + ", results=" + results + "]";
	}
}
