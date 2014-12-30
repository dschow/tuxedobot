package main;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.Queue;

import main.util.Config;
import main.module.*;

public class TuxBot extends PircBot {
	private final String configPath = new File(TuxBot.class.getResource(TuxBot.class.getSimpleName() + ".class").getFile()).getParent().toString();
    private final String configFile = configPath +"\\config.ini";
    private Config config = new Config(configFile);

    private Trivia trivia;

    Timer timer = new Timer();
    TimerTask timerTask;
    
    public Queue chatQueue = new Queue();
    private long lastChat = 0L;
    private long chatQueueDelay = 2000L;

    private Boolean inChannel = false;

    public static void main(String[] args) throws Exception {
        //Create Bot
        TuxBot bot = new TuxBot();
        bot.setVerbose(false);
    }

    public TuxBot() {
    	console(">> Welcome to TuxBot v1.0 by Tuxedo.");
        console(">> If you enjoy TuxBot, consider supporting its development!");
        
        //Init modules
        trivia = new Trivia(this);
        
        if(config.process(this)) {
            console("[TWITCH] Connecting to server...");

            this.setName(config.user);
            try {
            	this.connect("irc.twitch.tv", 6667, config.pass);
    		} catch (IOException | IrcException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}    

            //Setup Timer
            timer.scheduleAtFixedRate(timerTask = new TimerTask() {
            	@Override
                public void run() {
                    onTick();
                }
            }, 100, 100);
        } else {
            console("TuxBot cannot run without a propper config file. If a config file already exists delete it and re-run the program.");
        }
    }

    protected void onConnect() {
        console("[TWITCH] Login successful!");
        console("[TWITCH] Logged on as "+ config.user);

        this.joinChannel(config.channel.toLowerCase());
    }

    protected void onMessage(String channel, String sender, String consolein, String hostname, String message) {
        console(sender +": "+ message);

        if(sender.equals("tuxedotv")) {
            chatQueue.add("test1");
            chatQueue.add("test2");
        }
    }
    
    protected void onTick() {
        if(inChannel) {
        	if((System.currentTimeMillis() - lastChat) > chatQueueDelay && chatQueue.hasNext()) {
        		lastChat = System.currentTimeMillis();
        		
        		String message = (String) chatQueue.next();
        		sendMessage(config.channel, message);
        		console(config.user +": "+ message);
        	}
        }
    }
    
    protected void onJoin(String channel, String sender, String login, String hostname)  {
    	if(sender.equals(config.user.toLowerCase())) {
            inChannel = true;
    		console("[TWITCH] Joined the channel: "+ channel);
    	}
    }

    public void console(String text, Boolean newLine) {
        Date date = new Date(System.currentTimeMillis());
        DateFormat formatter = new SimpleDateFormat("hh:mm:ss aa");
        String dateFormatted = formatter.format(date);

        if(newLine) {
            System.out.println("["+dateFormatted+"] "+text);
        } else {
            System.out.print("["+dateFormatted+"] "+text);
        }
    }

    public void console(String text) {
        console(text, true);
    }
}
