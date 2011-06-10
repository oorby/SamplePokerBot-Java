package com.oorby.poker;

import com.oorby.poker.bots.SimpleBot;

public class BotRunner {

	private GameHost gameHost;
	private Bot bot;


	public BotRunner(GameHost gameHost, Bot bot) {
		this.gameHost = gameHost;
		this.bot = bot;
	}


	public void play(int numGames) {
		PlayerAction action = null;
		while (numGames > 0) {
			try {
				GameEvent event = (action == null) ? gameHost.getNextEvent() : gameHost.takeAction(action); 
				switch (event.eventType) {
				case ActionRequired:
					action = bot.takeAction(event);
					break;

				case GameComplete:
					System.out.println("game complete " + numGames);
					numGames--;
					// fallthrough

				default:
					action = null;
					bot.handleEvent(event);
				}
			} catch (Exception ex) {
				action = null;
			}
		}
	}



	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Usage: BotRunner botName devkey [host]");
			System.exit(1);
		}

		String botName = args[0];
		String devkey = args[1];
		String host = (args.length > 2) ? args[2] : "mcp.oorby.com";

		// CHANGE THIS TO USE YOUR OWN BOT IMPLEMENTATION!
		Bot bot = new SimpleBot(); 

		GameHost gameHost = new RemoteGameHost(botName, devkey, host);
		BotRunner runner = new BotRunner(gameHost, bot);
		runner.play(1);
		System.out.println("Bot exiting");
	}
}
