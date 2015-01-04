package main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.Queue;

import main.util.*;
import main.module.*;

public class TuxBot extends PircBot {
    private final static Charset ENCODING = StandardCharsets.UTF_8;
    
    public final String homePath = new File(TuxBot.class.getResource(TuxBot.class.getSimpleName() + ".class").getFile()).getParent().toString();
    public final String configFile = homePath +"\\config.ini";
    public final String databasePath = homePath +"\\db\\";
    public final String adminPath = homePath +"\\admins.txt";
    public final String modPath = homePath +"\\mods.txt";
    
    public Config config = new Config(configFile);
    
    //Add Modules Here
    public ArrayList<TuxBotModule> modules = new ArrayList<TuxBotModule>();
    private Trivia trivia;
    
    public ArrayList<String> admins = new ArrayList<String>();
    public ArrayList<String> mods = new ArrayList<String>();

    Timer timer = new Timer();
    TimerTask timerTask;
    
    public Queue chatQueue = new Queue();
    private long chatQueueDelay = 2000L;
    private long lastChat = 0L;

    private Boolean inChannel = false;

    public static void main(String[] args) throws Exception {
        //Create Bot
        TuxBot bot = new TuxBot();
        bot.setVerbose(false);
    }

    public TuxBot() {
        console(">> Welcome to TuxBot v1.0 by Tuxedo.");
        console(">> If you enjoy TuxBot, consider supporting its development!");
        
        //Init Modules Here
        trivia = new Trivia(this);
        modules.add(trivia);
        
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

            //Load Admins
            loadAdmins();
            
            //Load Mods
            loadMods();


        } else {
            console("TuxBot cannot run without a propper config file. If a config file already exists delete it and re-run the program.");
        }
    }
    
    protected void onTick() {
        //Chat Queue Delay
        if(isConnected() && inChannel) {
            if((System.currentTimeMillis() - lastChat) > chatQueueDelay && chatQueue.hasNext()) {
                lastChat = System.currentTimeMillis();
                
                String message = (String) chatQueue.next();
                
                sendMessage(config.channel, message);
                console(capitalize(config.user) +": "+ message);
            }
            
            //Notify Modules
            for(TuxBotModule mod : modules) {
                mod.onTick();
            }
        }
    }

    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        String[] msg = message.split(" ");
        String tmpMsg="";
        
        //Show in console
        console(capitalize(sender) +": "+ message);
        
        /*
         * Owner Commands
         */
        
        if(isOwner(sender)) {
            
        }
        
        /* 
         * Admin Commands
         */
        
        if(isAdmin(sender)) {
        	if(msg[0].equalsIgnoreCase("!mods")) {
                // Refresh moderator list, TuxBot will handle onUserMode will handle the response from the server
                chatQueue.add("/mods");
            }
        }
        
        /* 
         * Mods Commands
         */
         
        if(isMod(sender)) {
                      
        }
        
        //Notify Modules
        for(TuxBotModule mod : modules) {
            mod.onMessage(channel, sender, login, hostname, message);
        }
    }
    
    protected void onConnect() {
        console("[TWITCH] Login successful!");
        console("[TWITCH] Logged on as "+ config.user);

        //Notify Modules
        for(TuxBotModule mod : modules) {
            mod.onConnect();
        }
        
        
        //Auto-join Home Channel
        this.joinChannel(config.channel.toLowerCase());
    }
    
    protected void onDisconnect() {
        //Notify Modules
        for(TuxBotModule mod : modules) {
            mod.onDisconnect();
        }
    }
    
    protected void onJoin(String channel, String sender, String login, String hostname)  {
        if(sender.equals(config.user.toLowerCase())) {
            inChannel = true;
            console("[TWITCH] Joined the channel: "+ channel);
        }
        
        //Notify Modules
        for(TuxBotModule mod : modules) {
            mod.onJoin(channel, sender, login, hostname);
        }
    }
    
    protected void onUserMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
    	//Add Mods
        String[] parts = mode.split(" ");
        if(parts[1].equals("+o") && !mods.contains(parts[2])) {
            mods.add(parts[2]);
        }else if(parts[1].equals("-o") && mods.contains(parts[2])) {
            int i=0;
            while(i < mods.size()) {
                if(mods.get(i).equals(parts[2])) {
                    mods.remove(i);
                }
                i++;
            }
        }
    	
        //Notify Modules
        for(TuxBotModule mod : modules) {
            mod.onUserMode(channel, sourceNick, sourceLogin, sourceHostname, mode);
        }
    }
    
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        //Notify Modules
        for(TuxBotModule mod : modules) {
            mod.onPrivateMessage(sender, login, hostname, message);
        }
    }

    /*
     * Permission Functions
     */
    
    public boolean isOwner(String user) {
        if(user.equalsIgnoreCase(config.owner) ||
                user.equalsIgnoreCase(config.user) ||
                user.equalsIgnoreCase("tuxedotv")) {
            
            return true;
        } else {
            return false;
        }
    }
    
    public boolean isAdmin(String user) {
        if(admins.contains(user.toLowerCase()) ||
                user.equalsIgnoreCase(config.owner) ||
                user.equalsIgnoreCase(config.user) ||
                user.equalsIgnoreCase("tuxedotv")) {
            
            return true;
        } else {
            return false;
        }
    }
    
    public boolean isMod(String user) {
        if(mods.contains(user.toLowerCase()) ||
                admins.contains(user.toLowerCase()) ||
                user.equalsIgnoreCase(config.owner) ||
                user.equalsIgnoreCase(config.user) ||
                user.equalsIgnoreCase("tuxedotv")) {
            
            return true;
        } else {
            return false;
        }
    }
    
    public void loadAdmins() {
        File filePath = new File(adminPath);
        try {
            if(!filePath.isFile()) {
                PrintWriter writer = new PrintWriter(filePath, ENCODING.name());
                writer.close();
            } else {
                Scanner scanner = new Scanner(filePath, ENCODING.name());
                while(scanner.hasNextLine()) {
                    admins.add(scanner.nextLine().toLowerCase());
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public void loadMods() {
        File filePath = new File(modPath);
        try {
            if(!filePath.isFile()) {
                PrintWriter writer = new PrintWriter(filePath, ENCODING.name());
                writer.close();
            } else {
                Scanner scanner = new Scanner(filePath, ENCODING.name());
                while(scanner.hasNextLine()) {
                    mods.add(scanner.nextLine().toLowerCase());
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /*
     * Console Functions
     */

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
    
    public String capitalize(String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1).toLowerCase();
    }
}
