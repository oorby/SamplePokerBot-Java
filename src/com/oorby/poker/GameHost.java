package com.oorby.poker;

import java.io.IOException;

public interface GameHost {

	GameEvent getNextEvent() throws Exception;

	GameEvent takeAction(PlayerAction action) throws Exception;
}
