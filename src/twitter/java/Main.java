package twitter.java;

import twitter.java.account.Account;
import twitter.java.account.Country;
import twitter.java.exceptions.ExceptionsReader;
import twitter.java.tweet.Tweet;

public class Main {
    public static void main(String[] args) throws ExceptionsReader {
        Tweet myTweet = new Tweet("my first tweet");
        Tweet myReply1 = new Tweet("my first reply");
        Tweet myReply2 = new Tweet("my second reply");
        myReply1.addReplies(new Tweet("my first reply of reply 1"));
        myReply1.addReplies(new Tweet("my second reply of reply 1"));
        myReply2.addReplies(new Tweet("my first reply of reply 2"));
        myReply2.addReplies(new Tweet("my second reply of reply 2"));
        myTweet.addReplies(myReply1);
        myTweet.addReplies(myReply2);
        myTweet.show();

//        Account myAccount = new Account("Grafik", "notgrafik", "123456", "pablopjl64@gmail.com", "06 00 00 00 00", "21/11/2004", Country.France);
//        myAccount.setBio("I love Java");
//        myAccount.postTweet(new Tweet("my first tweet"));
//        myAccount.show();
//        myAccount.showTweets();
    }
}