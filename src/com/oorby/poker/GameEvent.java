package com.oorby.poker;

public class GameEvent {

	public enum EventType {
		GameStarted,
		ActionRequired,
		HandComplete,
		GameComplete
	}

	public EventType eventType;
	public String eventId;
	public GameState gameState;
	public Hand hand;
}
