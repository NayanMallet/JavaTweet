package twitter.java;

import twitter.java.exceptions.ExceptionsReader;
import twitter.java.tweet.Tweet;

public class Main {
    public static void main(String[] args) throws ExceptionsReader {
        Tweet myTweet = new Tweet("ehfggfefgnfds");
        Tweet myPrivateTweet = new Tweet("zizi", true);
        myPrivateTweet.show();
        myTweet.show();
    }
}