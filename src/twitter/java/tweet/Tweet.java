package twitter.java.tweet;

import twitter.java.exceptions.ExceptionsReader;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private static Date m_creationDate;
    private final List<Tweet> m_replies = new ArrayList<>();
    private int m_id;
    private String m_userHash;


    public Tweet(String message, String userHash) throws ExceptionsReader {
        if (message.isEmpty()) {
            throw new ExceptionsReader("You can't post an empty tweet !");
        } else if (message.length() > 280) {
            throw new ExceptionsReader("Your tweet must not exceed 280 characters !");
        }
        m_message = message;
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a · d MMM yyyy", new Locale("fr"));
            String formattedDate = now.format(formatter);
            java.util.Date date = new SimpleDateFormat("MMM yyyy").parse(formattedDate);
            m_creationDate = new java.sql.Date(date.getTime());
        } catch (ParseException e) {
            throw new ExceptionsReader("Error while getting creation date.");
        }
        m_userHash = userHash;
    }

    public Tweet(String message, String userHash, Date creationDate) throws ExceptionsReader {
        if (message.isEmpty()) {
            throw new ExceptionsReader("You can't post an empty tweet !");
        } else if (message.length() > 280) {
            throw new ExceptionsReader("Your tweet must not exceed 280 characters !");
        }
        m_message = message;
        m_creationDate = creationDate;
        m_userHash = userHash;
    }

    public Tweet(String message, String userHash,  boolean isPrivate) throws ExceptionsReader {
        if (message.isEmpty()) {
            throw new ExceptionsReader("You can't post an empty tweet !");
        } else if (message.length() > 280) {
            throw new ExceptionsReader("Your tweet must not exceed 280 characters !");
        }
        m_message = message;
        m_isPrivate = isPrivate;
        m_userHash = userHash;
    }

    public Tweet(String message, String userHash,  Date creationDate, boolean isPrivate) throws ExceptionsReader {
        if (message.isEmpty()) {
            throw new ExceptionsReader("You can't post an empty tweet !");
        } else if (message.length() > 280) {
            throw new ExceptionsReader("Your tweet must not exceed 280 characters !");
        }
        m_message = message;
        m_isPrivate = isPrivate;
        m_creationDate = creationDate;
        m_userHash = userHash;
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

    public Date getCreationDate() {
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

    public int getId() {
        return m_id;
    }

    public void setId(int id) {
        m_id = id;
    }

    public String getUserHash() {
        return m_userHash;
    }

    public void addReplies(Tweet reply) {
        m_replies.add(reply);
    }

    public void showReplies() {
        for (Tweet mReply : m_replies) System.out.println(mReply);
    }
}
