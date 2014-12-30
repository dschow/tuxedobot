package main.module;

import main.util.TuxBotModule;

import main.TuxBot;

public class Trivia implements TuxBotModule {
	private TuxBot bot;

	public Trivia(TuxBot b) {
		this.bot = b;

		bot.console("[MODULE] Trivia loaded");
		bot.chatQueue.add("beep boop");
	}

	@Override
	public void onTick() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessage(String channel, String sender, String login,
			String hostname, String message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnect() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDisconnect() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onJoin(String channel, String sender, String login,
			String hostname) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserMode(String channel, String sourceNick,
			String sourceLogin, String sourceHostname, String mode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPrivateMessage(String sender, String login, String hostname,
			String message) {
		// TODO Auto-generated method stub

	}

}
