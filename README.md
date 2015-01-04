Tuxedo Bot
==========

## Description
Tuxedo Bot is a Twitch.TV chat bot built in Java. The bot is based functionality provided by the [pircbot library](http://www.jibble.org/pircbot.php).

## Requirements
* Java SDK
* [pircbot library](http://www.jibble.org/pircbot.php) (included)
* sqlite-jdbc (included)

## Twitch OAuth Required
You will need an OAuth token for the Twitch API in order to login to the Twitch Chat via IRC.

1. Go to the following url: http://twitchtools.com/chat-token.php
2. Connect to Twitch, using the Twitch Username you will want the bot to run on.
3. Save the text after **oauth:**

When the Tuxedo Bot runs for the first time it will ask you to enter basic information it will use to create a config file. This OAuth token will be one of the things you will need to provide to run the bot.

## Setup
Navigate to the **src** directory and run the following command to build the bot:

    javac -cp main/lib/*;. main/*.java

Then run the following command to start the bot:

    java -cp main/lib/*;. main/TuxBot

## Functions
* Trivia