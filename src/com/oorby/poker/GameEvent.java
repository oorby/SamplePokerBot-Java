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

	public String toString() {
		return "GameEvent [eventType=" + eventType + ", eventId=" + eventId
				+ ", gameState=" + gameState + ", hand=" + hand + "]";
	}
}
