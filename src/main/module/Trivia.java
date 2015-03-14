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
    
    private String databaseFile = "trivia.db";
    private String triviaFile = "trivia.txt";
    
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
    private long delayCategory = 2*60*1000L; //2 mins
    private long delayQuestion = 10*1000L; //10 secs
    private long delayAnswer = 45*1000L; //45 secs
    private int delayStage = 0; //0 = waiting for category, 1 = waiting for question, 2 = waiting for answer
    
    private boolean skipCategory = false;
    private boolean hideCategory = true;
    
    private long lastLeaderboard = 0L;
    private long delayLeaderboard = 15*1000L;
    
    private long lastInfo = 0L;
    private long delayInfo = 30*1000L; //30 sec
    
    public Trivia(TuxBot b) {
        this.bot = b;
        this.databaseFile = bot.databasePath +"trivia.db";
        this.triviaFile = bot.homePath +"\\trivia.txt";
        
        bot.console("[TRIVIA] Trivia loaded");
    }

    @Override
    public void onTick() {
        if(bot.isConnected() && triviaActive) {
        	Connection connection = null;
            ResultSet resultSet = null;
            Statement statement = null;
            
            if((System.currentTimeMillis() - lastAnswer) > delayCategory && delayStage == 0) {
            	//Check if we have questions
            	loadQuestions();
            	
                //Should we skip the category phase?
                if(!skipCategory) {
                    //Don't skip category phase, but  should we show a category?
                    if(!hideCategory) {
                        //Don't hide category
                    	try {
                            Class.forName("org.sqlite.JDBC");
                            connection = DriverManager.getConnection("jdbc:sqlite:"+ databaseFile);
                            statement = connection.createStatement();
                            resultSet = statement.executeQuery("SELECT category.name AS category "
                                                                + "FROM question "
                                                                + "LEFT JOIN category ON question.category = category.id "
                                                                + "WHERE question.id = '" + questions.get(0) + "'");
                            resultSet.next();
                            bot.chatQueue.addFront(">> The category for the next piece of trivia is: "+ resultSet.getString("category"));
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
            	// Get Answers
                answers.clear();
                
                try {
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection("jdbc:sqlite:"+ databaseFile);
                    statement = connection.createStatement();
                    resultSet = statement.executeQuery("SELECT answer FROM answer WHERE qid = '" + questions.get(0) + "'");
                    while (resultSet.next()) {
                        answers.add(resultSet.getString("answer"));
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
                
                // Ask Question
                questionInfo = "";
                
                try {
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection("jdbc:sqlite:"+ databaseFile);
                    statement = connection.createStatement();
                    resultSet = statement.executeQuery("SELECT category.name AS category, question.question AS question, question.info AS info "
                                                        + "FROM question "
                                                        + "LEFT JOIN category ON question.category = category.id "
                                                        + "WHERE question.id = '" + questions.get(0) + "'");
                    resultSet.next();
                    questionInfo = resultSet.getString("info");
                    questionActive = true;
                    
                    bot.console("");
                    bot.console("Category: " + resultSet.getString("category"));
                    bot.console("Question: " + resultSet.getString("question"));
                    for(int i=0; i<answers.size(); i++) {
                    	bot.console("Answer: " + answers.get(i));
                    }
                    bot.console("");
                    
                    bot.chatQueue.addFront(">> [" + resultSet.getString("category") + "] " + resultSet.getString("question"));
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
                
                lastQuestion = System.currentTimeMillis();
                delayStage = 2; //Waiting for answer
            } else if((System.currentTimeMillis() - lastQuestion) > delayAnswer && delayStage == 2) {
            	questionActive = false;
                questions.remove(0);
                noAnswerCount++;
            	
            	if(noAnswerCount < disableAnswerCount) {
	                String tmpMsg = ">> The correct answer was not detected. ";
	                if(answers.size()> 1) {
	                    tmpMsg += "The " + answers.size() + " possible answers were: ";
	                    for(int i=0; i<answers.size(); i++) {
	                        if(i == answers.size()-1) {
	                            tmpMsg += "and " + answers.get(i) + ". ";
	                        }else{
	                            if(answers.size() == 2) {
	                                tmpMsg += answers.get(i) + " ";
	                            }else{
	                                tmpMsg += answers.get(i) + ", ";
	                            }
	                        }
	                    }
	                }else{
	                    tmpMsg += "The correct answer was: " + answers.get(0) + ". ";
	                }
	                
	                if(questionInfo != null && questionInfo.length() > 0) {
	                    tmpMsg += questionInfo;
	                }
	                
	                bot.chatQueue.addFront(tmpMsg);
            	} else {
            		triviaActive = false;
                    bot.chatQueue.add(">> Trivia ended due to lack of activity!");
            	}
                
                lastAnswer = System.currentTimeMillis();
                delayStage = 0;
            }
        }
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message) {
    	message = message.trim();
        String[] msg = message.split(" ");
        Connection connection = null;
        ResultSet resultSet = null;
        Statement statement = null;
        String tmpMsg="";

        /*
         * Owner Commands
         */
        
        if(bot.isOwner(sender)) {
        	if(msg[0].equalsIgnoreCase("!reset")) {
                try {
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection("jdbc:sqlite:"+ databaseFile);
                    statement = connection.createStatement();
                    statement.executeUpdate("UPDATE user SET points = 0, streak = 0, maxstreak = 0");
                    bot.chatQueue.add(">> All points and streaks have been reset!");
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
        }
        
        /* 
         * Admin Commands
         */
        
        if(bot.isAdmin(sender)) {
        	if(msg[0].equalsIgnoreCase("!ignore")) {
                if(msg.length == 2) {
                    if(!ignore.contains(msg[1].toLowerCase())) {
                        ignore.add(msg[1].toLowerCase());
                        bot.chatQueue.add(bot.capitalize(msg[1]) + " was added to the ignore list.");
                    }
                }else{
                	bot.chatQueue.add(">> Invalid ignore format: !ignore [twitch user]");
                }
            }else if(msg[0].equalsIgnoreCase("!unignore")) {
                if(msg.length == 2) {
                    if(ignore.contains(msg[1].toLowerCase())) {
                        for(int i=0; i < ignore.size(); i++) {
                            if(ignore.get(i).equals(msg[1].toLowerCase())) {
                                ignore.remove(i);
                                bot.chatQueue.add(bot.capitalize(msg[1]) + " was removed from the ignore list.");
                                break;
                            }
                        }
                    }
                }else{
                	bot.chatQueue.add(">> Invalid unignore format: !unignore [twitch user]");
                }
            }else if(msg[0].equalsIgnoreCase("!time")) {
				if(msg.length == 3) {
					if(isInteger(msg[2])) {
						int newTime = Integer.parseInt(msg[2]);
						if(newTime < 10) {
							newTime = 10;
						}else if(newTime > 600) {
							newTime = 600;
						}
					
						if(msg[1].equalsIgnoreCase("category")) {
							delayCategory = newTime*1000L;
							bot.chatQueue.add(">> Time between questions set to: "+ timeToString((int) newTime));
						}else if(msg[1].equalsIgnoreCase("question")) {
							delayQuestion = newTime*1000L;
							bot.chatQueue.add(">> Time between category and question set to: "+ timeToString((int) newTime));
						}else if(msg[1].equalsIgnoreCase("answer")) {
							delayAnswer = newTime*1000L;
							bot.chatQueue.add(">> Time between question and answer set to: "+ timeToString((int) newTime));
						}
					}
				}else if(msg.length == 2) {
                    if(msg[1].equalsIgnoreCase("category")) {
                        bot.chatQueue.add(">> Time between questions: "+ timeToString((int) (delayCategory/1000)));
                    }else if(msg[1].equalsIgnoreCase("question")) {
                        bot.chatQueue.add(">> Time between category and question: "+ timeToString((int) (delayQuestion/1000)));
                    }else if(msg[1].equalsIgnoreCase("answer")) {
                        bot.chatQueue.add(">> Time between question and answer: "+ timeToString((int) (delayAnswer/1000)));
                    }
                }else{
					bot.chatQueue.add(">> Invalid time format: !time [type: delay/question/answer] [time (sec): 10-600]");
				}
			}
        }
        
        /* 
         * Mods Commands
         */
         
        if(bot.isMod(sender)) {
            //Start Trivia
            if(msg[0].equalsIgnoreCase("!start")) {
            	if(!triviaActive) {
	                lastAnswer = System.currentTimeMillis();
	                delayStage = 0;
	                triviaActive = true;
	                questionActive = false;
	                noAnswerCount = 0;
	                bot.chatQueue.add(">> Starting Trivia!");
	            } else {
	            	bot.chatQueue.add(">> Trivia has already been started.");
	            }
                
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
            }else if(msg[0].equalsIgnoreCase("!leaderboard") || msg[0].equalsIgnoreCase("!top")) {
                if((System.currentTimeMillis() - lastLeaderboard) > delayLeaderboard) {
        			lastLeaderboard = System.currentTimeMillis();
                    
                    leaderboard.clear();
                    try {
                        Class.forName("org.sqlite.JDBC");
                        connection = DriverManager.getConnection("jdbc:sqlite:"+ databaseFile);
                        statement = connection.createStatement();
                        resultSet = statement.executeQuery("SELECT * FROM user ORDER BY points DESC");

                        for(int i=1; resultSet.next() && i<=10; i++) {
                            leaderboard.add(resultSet.getString("user") + " [" + resultSet.getString("points") + "]");
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
                    
                    tmpMsg = "";
                    for(int i=0; i<leaderboard.size(); i++) {
                        if(i == leaderboard.size()-1) {
                            tmpMsg += leaderboard.get(i);
                        }else{
                            tmpMsg += leaderboard.get(i) + ", ";
                        }
                    }
                    
                    bot.chatQueue.add(tmpMsg);
                }
            }
        }

        if(msg[0].equalsIgnoreCase("!info")) {
        	if((System.currentTimeMillis() - lastInfo) > delayInfo) {
        		lastInfo = System.currentTimeMillis();
                
                if(msg.length == 2) {
                    try {
                        Class.forName("org.sqlite.JDBC");
                        connection = DriverManager.getConnection("jdbc:sqlite:"+ databaseFile);
                        statement = connection.createStatement();
                        resultSet = statement.executeQuery("SELECT COUNT(*) AS count FROM user WHERE user = '" + msg[1].toLowerCase() + "'");
                        resultSet.next();
                        if(resultSet.getInt("count") == 1) {
                            resultSet = statement.executeQuery("SELECT * FROM user WHERE user = '" + msg[1].toLowerCase() + "'");
                            resultSet.next();
                            bot.chatQueue.add(msg[1] + " [" + resultSet.getInt("points") + "][streak: " + resultSet.getInt("maxstreak") + "]");
                        }else{
                        	bot.chatQueue.add("The user " + msg[1] + " has not scored points.");
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
                }else{
                    try {
                        Class.forName("org.sqlite.JDBC");
                        connection = DriverManager.getConnection("jdbc:sqlite:"+ databaseFile);
                        statement = connection.createStatement();
                        resultSet = statement.executeQuery("SELECT * FROM user WHERE id = '" + getUid(sender.toLowerCase()) + "'");
                        resultSet.next();
                        bot.chatQueue.add(sender + " [points: " + resultSet.getInt("points") + "][streak: " + resultSet.getInt("maxstreak") + "]");
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
            }
        }
        
        /* 
         * Trivia Handling
         */
        
        if(triviaActive && questionActive) {
            if(containsCaseInsensitive(answers, message) && !ignore.contains(sender)) {
                int uid = 0;
                int points = 1;
                int streak = 1;
                int maxstreak = 1;
                noAnswerCount = 0;
                
                // Adjust point count
                try {
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection("jdbc:sqlite:"+ databaseFile);
                    statement = connection.createStatement();
                    resultSet = statement.executeQuery("SELECT * FROM user WHERE id = '" + getUid(sender.toLowerCase()) + "'");
                    uid = resultSet.getInt("id");
                    points = resultSet.getInt("points") + 1;
                    streak = resultSet.getInt("streak") + 1;
                    maxstreak = resultSet.getInt("maxstreak");

                    if(streak > maxstreak) {
                        maxstreak = streak;
                    }
                    
                    statement.executeUpdate("UPDATE user SET points = " + points + ", streak = " + streak + ", maxstreak = " + maxstreak + " WHERE id = " + uid);
                    statement.executeUpdate("UPDATE user SET streak = 0 WHERE id != " + uid);
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
                
                // Correct message
                tmpMsg = ">> Correct, " + bot.capitalize(sender) + ". ";
                if(answers.size()> 1) {
                    tmpMsg += "The " + answers.size() + " possible answers were: ";
                    for(int i=0; i<answers.size(); i++) {
                        if(i == answers.size()-1) {
                            tmpMsg += "and " + answers.get(i) + ". ";
                        }else{
                            if(answers.size() == 2) {
                                tmpMsg += answers.get(i) + " ";
                            }else{
                                tmpMsg += answers.get(i) + ", ";
                            }
                        }
                    }
                }else{
                    tmpMsg += "The answer was: " + answers.get(0) + ". ";
                }
                
                if(questionInfo != null && questionInfo.length() > 0) {
                    tmpMsg += questionInfo + " ";
                }
                
                if(streak > 7) {
                    tmpMsg += bot.capitalize(sender) + " [points: " + points + "][streak: " + streak + "] LEGENDARY!";
                }else if(streak > 6) {
                    tmpMsg += bot.capitalize(sender) + " [points: " + points + "][streak: " + streak + "] GODLIKE!";
                }else if(streak > 5) {
                    tmpMsg += bot.capitalize(sender) + " [points: " + points + "][streak: " + streak + "] DOMINATING!";
                }else if(streak > 4) {
                    tmpMsg += bot.capitalize(sender) + " [points: " + points + "][streak: " + streak + "] UNSTOPPABLE!";
                }else if(streak > 3) {
                    tmpMsg += bot.capitalize(sender) + " [points: " + points + "][streak: " + streak + "] RAMPAGE!";
                }else{
                    tmpMsg += bot.capitalize(sender) + " [points: " + points + "][streak: " + streak + "]";
                }
                
                questionActive = false;
                questions.remove(0);
                
                bot.chatQueue.addFront(tmpMsg);

                lastAnswer = System.currentTimeMillis();
                delayStage = 0;
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
    
    public int getUid(String user) {
        Connection connection = null;
        ResultSet resultSet = null;
        Statement statement = null;
        
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:"+ databaseFile);
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM user WHERE user = '" + user.toLowerCase() + "'");
            if(resultSet.next()) {
                return resultSet.getInt("id");
            }else{
                statement.executeUpdate("INSERT INTO user (user, points, streak, maxstreak) VALUES ('" + user + "', 0, 0, 0)");
                resultSet = statement.executeQuery("SELECT * FROM user WHERE user = '" + user.toLowerCase() + "'");
                resultSet.next();
                return resultSet.getInt("id");
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
        
        return 0;
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
                    createDatabase();
                    
                    bot.console("[TRIVIA] Trivia database created");
                } else {
                    try {
                        Class.forName("org.sqlite.JDBC");
                        connection = DriverManager.getConnection("jdbc:sqlite:"+ databaseFile);
                        statement = connection.createStatement();
                        resultSet = statement.executeQuery("SELECT * FROM question ORDER BY RANDOM()");
                        
                        PrintWriter writer = new PrintWriter(triviaFile, "UTF-8");
            			int line = 1;
                        
                        while (resultSet.next()) {
                            questions.add(resultSet.getInt("id"));
                            writer.println(line + ". " + resultSet.getString("question"));
            				line++;
                        }
                        
                        writer.close();
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
                    
                    bot.console("[TRIVIA] Trivia questions loaded");
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
    
    public boolean containsCaseInsensitive(ArrayList<String> l, String s){
        for (String string : l){
            if (string.equalsIgnoreCase(s)){
                return true;
            }
        }
        return false;
    }
    
    public static boolean isInteger(String s) {
		try { 
			Integer.parseInt(s); 
		} catch(NumberFormatException e) { 
			return false; 
		}
		return true;
	}

}
