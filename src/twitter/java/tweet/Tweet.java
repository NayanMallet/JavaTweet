package twitter.java.tweet;

public class Tweet {
    private String m_message;
    private int m_likes = 0;
    private int m_retweets = 0;
    private int m_signets = 0;
    private boolean isPrivate = false;

    public Tweet(String message) {
        m_message = message;
    }

}
