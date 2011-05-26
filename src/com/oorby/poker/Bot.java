package com.oorby.poker;


public interface Bot {

	void handleEvent(GameEvent event);

	PlayerAction takeAction(GameEvent event);
}
