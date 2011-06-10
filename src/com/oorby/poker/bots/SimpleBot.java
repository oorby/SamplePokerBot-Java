package com.oorby.poker.bots;

import java.util.List;
import java.util.Random;

import com.oorby.poker.Bot;
import com.oorby.poker.GameEvent;
import com.oorby.poker.Hand.AvailableAction;
import com.oorby.poker.PlayerAction;

public class SimpleBot implements Bot {

	private Random rand = new Random();

	@Override
	public void handleEvent(GameEvent event) {
		System.out.println("handleEvent " + event);
	}

	@Override
	public PlayerAction takeAction(GameEvent event) {
		System.out.println("take action " + event);
		List<AvailableAction> availableActions = event.hand.availableActions;

		int randIdx = rand.nextInt(availableActions.size());
		AvailableAction action = availableActions.get(randIdx);
		return new PlayerAction(action.action);
	}
}
