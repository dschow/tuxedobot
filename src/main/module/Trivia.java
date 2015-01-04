package main.module;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Scanner;

import main.util.TuxBotModule;

import main.TuxBot;

public class Trivia implements TuxBotModule {
	private TuxBot bot;
	
	private String databaseFile = "triv.db";
	
	private ArrayList<String> leaderboard = new ArrayList<String>();
	private ArrayList<Integer> questions = new ArrayList<Integer>();
	private ArrayList<String> answers = new ArrayList<String>();
	private ArrayList<String> ignore = new ArrayList<String>();
	

	private boolean triviaActive = false;
	private boolean questionActive = false;
		
	private String questionInfo;
	
	private int noAnswerCount = 0;
	private int disableAnswerCount = 3;
	
	private long lastCategory = 0L;
	private long lastQuestion = 0L;
	private long lastAnswer = 0L;
	private long delayCategory = 10*1000L; //2 mins
	private long delayQuestion = 10*1000L; //10 secs
	private long delayAnswer = 10*1000L; //45 secs
	private int delayStage = 0; //0 = waiting for category, 1 = waiting for question, 2 = waiting for answer
	
	private boolean skipCategory = false;
	private boolean hideCategory = false;
	
	private long lastLeaderboard = 0L;
	private long delayLeaderboard = 15*1000L;
	
	private long lastInfo = 0L;
	private long delayInfo = 30*1000L;	
	
	public Trivia(TuxBot b) {
		this.bot = b;
		this.databaseFile = this.bot.databasePath +"triv.db";
		
		bot.console("[TRIVIA] Trivia loaded");
		bot.chatQueue.add("beep boop");
	}

	@Override
	public void onTick() {
		Connection connection = null;
        ResultSet resultSet = null;
        Statement statement = null;
        
        if(bot.isConnected() && triviaActive) {
        	if((System.currentTimeMillis() - lastAnswer) > delayAnswer && delayStage == 0) {
        		//Should we skip the category phase?
        		if(!skipCategory) {
        			//Don't skip category phase, but  should we show a category?
        			if(!hideCategory) {
        				//Don't hide category
        				bot.chatQueue.addFront(">> The category for the next piece of trivia is: ");
        			} else {
        				//Hide category
        				bot.chatQueue.addFront(">> The next question in "+ timeToString((int) (delayQuestion/1000)) +"...");
        			}
        			
        			lastCategory = System.currentTimeMillis();
        			delayStage = 1; //Waiting for question
        		} else {
        			//Skip category phase
        			lastCategory = 0;
        			delayStage = 1; //Waiting for question
        		}
        	} else if((System.currentTimeMillis() - lastCategory) > delayQuestion && delayStage == 1) {
        		bot.chatQueue.addFront("trivia question");
        		
        		lastQuestion = System.currentTimeMillis();
    			delayStage = 2; //Waiting for answer
        	} else if((System.currentTimeMillis() - lastQuestion) > delayAnswer && delayStage == 2) {
        		bot.chatQueue.addFront("trivia answer");
        		
        		lastAnswer = System.currentTimeMillis();
        		delayStage = 0;
        	}
        }
	}

	@Override
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
        Connection connection = null;
        ResultSet resultSet = null;
        Statement statement = null;
        String tmpMsg="";

        /*
         * Owner Commands
         */
        
        if(bot.isOwner(sender)) {
        	
        }
        
        /* 
         * Admin Commands
         */
        
        if(bot.isAdmin(sender)) {
        	
        }
        
        /* 
         * Mods Commands
         */
		 
		if(bot.isMod(sender)) {
			//Start Trivia
			if(msg[0].equalsIgnoreCase("!start")) {
                lastAnswer = System.currentTimeMillis();
                triviaActive = true;
                questionActive = false;
                noAnswerCount = 0;
                bot.chatQueue.add(">> Starting Trivia!");
                
            //Stop Trivia
            } else if (msg[0].equalsIgnoreCase("!stop") || msg[0].equalsIgnoreCase("!end")) {
                triviaActive = false;
                bot.chatQueue.add(">> Trivia ended!");
                
            //Skip Category 
            } else if(msg[0].equalsIgnoreCase("!skipcategory")) {
            	if(msg.length == 2) {
            		//Change Setting
            		if(msg[1].equalsIgnoreCase("true")) {
            			skipCategory = true;
            			bot.chatQueue.add(">> Categories are now being skipped.");
            		} else if(msg[1].equalsIgnoreCase("false")) {
            			skipCategory = false;
            			bot.chatQueue.add(">> Categories are now being included.");
            		}
            		
            	} else {
            		//Display Setting
            		if(!skipCategory) {
            			bot.chatQueue.add(">> Categories are not being skipped.");
            		} else {
            			bot.chatQueue.add(">> Categories are being skipped.");
            		}
            	}
            	
            //Hide Category
            } else if(msg[0].equalsIgnoreCase("!hidecategory")) {
            	if(msg.length == 2) {
            		//Change Setting
            		if(msg[1].equalsIgnoreCase("true")) {
            			hideCategory = true;
            			bot.chatQueue.add(">> Categories are now being hidden.");
            		} else if(msg[1].equalsIgnoreCase("false")) {
            			hideCategory = false;
            			bot.chatQueue.add(">> Categories are now being shown.");
            		}
            		
            	} else {
            		//Display Setting
            		if(!hideCategory) {
            			bot.chatQueue.add(">> Categories are not being hidden.");
            		} else {
            			bot.chatQueue.add(">> Categories are being hidden.");
            		}
            	}
            
            //Show Category (opposite of hide category)
            } else if(msg[0].equalsIgnoreCase("!showcategory")) {
            	if(msg.length == 2) {
            		//Change Setting
            		if(msg[1].equalsIgnoreCase("true")) {
            			hideCategory = false;
            			bot.chatQueue.add(">> Categories are now being shown.");
            		} else if(msg[1].equalsIgnoreCase("false")) {
            			hideCategory = true;
            			bot.chatQueue.add(">> Categories are now being hidden.");
            		}
            		
            	} else {
            		//Display Setting
            		if(!hideCategory) {
            			bot.chatQueue.add(">> Categories are not being hidden.");
            		} else {
            			bot.chatQueue.add(">> Categories are being hidden.");
            		}
            	}
            }
		}
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
	
	private void loadQuestions() {
		if(questions.size() == 0) {
			Connection connection = null;
	        ResultSet resultSet = null;
	        Statement statement = null;
	        
	        File filePath = new File(bot.databasePath);
	        File file = new File(databaseFile);
	        
	    	try {
	    		if(!filePath.exists()) {
	    			filePath.mkdir();			
	    		}
	    		
				if(!file.isFile()) {		
					bot.console("[TRIVIA] Trivia database created");
					createDatabase();				
				} else {
					bot.console("[TRIVIA] Trivia questions loaded");
					try {
			            Class.forName("org.sqlite.JDBC");
			            connection = DriverManager.getConnection("jdbc:sqlite:"+ databaseFile);
			            statement = connection.createStatement();
			            resultSet = statement.executeQuery("SELECT * FROM question ORDER BY RANDOM()");
			            while (resultSet.next()) {
			                questions.add(resultSet.getInt("id"));
			            }
			        } catch (Exception e) {
			            e.printStackTrace();
			        } finally {
			            try {
			                resultSet.close();
			                statement.close();
			                connection.close();
			            } catch (Exception e) {
			                e.printStackTrace();
			            }
			        }
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void createDatabase() {
		Connection connection = null;
        ResultSet resultSet = null;
        Statement statement = null;
        
		try {
			Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:"+ databaseFile);
            statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE answer (id INTEGER PRIMARY KEY, qid NUMERIC, answer TEXT);" +
            		"CREATE TABLE category (id INTEGER PRIMARY KEY, name TEXT);" +
            		"CREATE TABLE question (id INTEGER PRIMARY KEY, category NUMERIC, question TEXT, info TEXT);" +
            		"CREATE TABLE user (id INTEGER PRIMARY KEY, user TEXT, points NUMERIC, streak NUMERIC, maxstreak NUMERIC);");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                statement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}
	
	public static String timeToString(Integer s) {
		int min = s / 60;
		int sec = s % 60;
		if(min > 0) {
			if(sec > 0) {
				return min+"m "+sec+"s";
			}else{
				return min+"m";
			}
		}else{
			return sec + "s";
		}
	}

}
