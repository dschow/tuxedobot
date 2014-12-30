package main.util;

public interface TuxBotModule {
	
	void onTick();
	
	void onMessage(String channel, String sender, String login,
			String hostname, String message);
    
	void onConnect();
    
    void onDisconnect();
	
    void onJoin(String channel, String sender, String login,
			String hostname);
    
    void onUserMode(String channel, String sourceNick,
			String sourceLogin, String sourceHostname, String mode);
    
    void onPrivateMessage(String sender, String login, String hostname,
			String message);
}
