package com.oorby.poker;


public interface GameHost {

	GameEvent getNextEvent() throws Exception;

	GameEvent takeAction(PlayerAction action) throws Exception;
}
