package com.oorby.poker;

import com.oorby.poker.bots.SimpleBot;

public class BotRunner {

	private GameHost gameHost;
	private Bot bot;
	private boolean continuePlaying = true;


	public BotRunner(GameHost gameHost, Bot bot) {
		this.gameHost = gameHost;
		this.bot = bot;
	}


	public void play() {
		PlayerAction action = null;
		while (continuePlaying) {
			try {
				GameEvent event = (action == null) ? gameHost.getNextEvent() : gameHost.takeAction(action); 
				switch (event.eventType) {
				case ActionRequired:
					action = bot.takeAction(event);
					break;

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
		String host = (args.length >= 2) ? args[2] : "mcp.oorby.com";

		Bot bot = new SimpleBot(); 

		GameHost gameHost = new RemoteGameHost(botName, devkey, host);
		BotRunner runner = new BotRunner(gameHost, bot);
		runner.play();
	}
}
