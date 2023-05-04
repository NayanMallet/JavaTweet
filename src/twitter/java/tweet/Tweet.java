package twitter.java.tweet;

import twitter.java.exceptions.ExceptionsReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Tweet {
    private final String m_message;
    private int m_likes = 0;
    private int m_retweets = 0;
    private int m_signets = 0;
    private boolean m_isPrivate = false;
    private static String m_creationDate;
    private final List<Tweet> m_replies = new ArrayList<>();

    public Tweet(String message) throws ExceptionsReader {
        if (message.isEmpty()) {
            throw new ExceptionsReader("You can't post an empty tweet !");
        } else if (message.length() > 280) {
            throw new ExceptionsReader("Your tweet must not exceed 280 characters !");
        }
        m_message = message;
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("h:mm a · d MMM yyyy", new Locale("fr"));
        m_creationDate = myDateObj.format(myFormatObj);
    }

    public Tweet(String message, boolean isPrivate) throws ExceptionsReader {
        if (message.isEmpty()) {
            throw new ExceptionsReader("You can't post an empty tweet !");
        } else if (message.length() > 280) {
            throw new ExceptionsReader("Your tweet must not exceed 280 characters !");
        }
        m_message = message;
        m_isPrivate = isPrivate;
    }

    public void show() {
        System.out.printf("%s%s\n%s\n%s",(isPrivate() ? "(private)" : ""), m_message, m_creationDate, (m_replies.size() > 0 ? "" : m_replies.size() + "\n"));
        if (m_replies.size() > 0) {
            for (Tweet reply : m_replies) {
                System.out.println("└─── " + reply.getMessage());
                if (reply.getReplies().size() > 0) {
                    for (Tweet reply2 : reply.getReplies()) {
                        System.out.println("     └─── " + reply2.getMessage());
                    }
                }
            }
        }
    }

    public int getLikes() {
        return m_likes;
    }

    public int getRetweets() {
        return m_retweets;
    }

    public int getSignets() {
        return m_signets;
    }

    public boolean isPrivate() {
        return m_isPrivate;
    }

    public String getMessage() {
        return m_message;
    }

    public String getCreationDate() {
        return m_creationDate;
    }

    public List<Tweet> getReplies() {
        return m_replies;
    }

    public void setLikes(int likes) {
        m_likes = likes;
    }

    public void setRetweets(int retweets) {
        m_retweets = retweets;
    }

    public void setSignets(int signets) {
        m_signets = signets;
    }

    public void addReplies(Tweet reply) {
        m_replies.add(reply);
    }

    public void showReplies() {
        for (Tweet mReply : m_replies) System.out.println(mReply);
    }





}
