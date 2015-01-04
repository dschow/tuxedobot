package main.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import main.TuxBot;

public class Config {
	private final static Charset ENCODING = StandardCharsets.UTF_8;
	private File filePath;

	public String owner;
	public String user;
	public String pass;
	public String channel;

	public Config(String fileName) {
		filePath = new File(fileName);
	}

	public Boolean process(TuxBot bot) {
		try {
			if(!filePath.isFile()) {
				Boolean createConfig = false;
				
				bot.console("");
				bot.console("No config file was found in your directory.");
				
				while(true) {
					bot.console("Would you like to create a config.ini file? [Y/N] ", false);
					
					String createConfigResponse = System.console().readLine().toLowerCase();
					if(createConfigResponse.equals("y")) {
						createConfig = true;
						break;
					} else if(createConfigResponse.equals("n")) {
						break;
					}
				}
				
				if(createConfig) {
					bot.console("");
					bot.console("Twitch.TV Owner Username: ", false);
					owner = System.console().readLine();
					bot.console("");
					bot.console("Twitch.TV Username: ", false);
					user = System.console().readLine();
					bot.console("");
					bot.console("To connect to Twitch chat, the bot needs an OAuth token. If you don't have a token, visit the following url: ");
					bot.console("http://www.twitchtools.com/chat-token.php");
					bot.console("");
					bot.console("Enter your OAuth token:");
					bot.console("oauth:", false);
					pass = "oauth:"+ System.console().readLine();
					bot.console("");
					bot.console("Twitch.TV Channel: ", false);
					channel = System.console().readLine();
					bot.console("");
	
					createConfigFile();
	
					return true;
				}
			} else {
				Scanner scanner = new Scanner(filePath, ENCODING.name());
				while(scanner.hasNextLine()) {
					processLine(scanner.nextLine());
				}
				
				if(!this.user.isEmpty() &&
						!this.pass.isEmpty() &&
						!this.channel.isEmpty()) {
					return true;
				}
			}
		} catch(Exception e) {
			
		}

		return false;
	}

	public void processLine(String line) {
		try {
			Scanner scanner = new Scanner(line);
			scanner.useDelimiter("=");
			if(scanner.hasNext()) {
				String key = scanner.next();
				String value = scanner.next();

				switch(key) {
					case "user":
						this.user = value;
						break;
					case "pass":
						this.pass = value;
						break;
					case "channel":
						this.channel = "#"+ value;
						break;
					case "owner":
						this.owner = value;
						break;
					default:
						break;
				}
			}
		} catch(Exception e) {

		}

		/*
		Scanner scanner = new Scanner(line);
	    scanner.useDelimiter("=");
	    if (scanner.hasNext()){
	      //assumes the line has a certain structure
	      String name = scanner.next();
	      String value = scanner.next();
	      //console("Name is : " + quote(name.trim()) + ", and Value is : " + quote(value.trim()));
	    }
	    else {
	      //console("Empty or invalid line. Unable to process.");
	    }
	    */
	}

	public void createConfigFile() {
		try {
			PrintWriter writer = new PrintWriter(filePath, ENCODING.name());
			writer.println("[Settings]");
			writer.println("user="+ user);
			writer.println("pass="+ pass);
			writer.println("channel="+ channel);
			writer.println("owner="+ owner);
			writer.close();
		} catch(IOException e) {
			
		}
	}
}