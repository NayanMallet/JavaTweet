package twitter.java.tweet;

import twitter.java.exceptions.ExceptionsReader;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


public class Tweet {
    private final String m_message;
    private int m_likes = 0;
    private int m_retweets = 0;
    private int m_signets = 0;
    private boolean m_isPrivate = false;
    private static Date m_creationDate;
    private int m_id;
    private int m_parentId = 0;
    private final String m_userHash;


    public Tweet(String message, String userHash, boolean isPrivate) throws ExceptionsReader {
        if (message.isEmpty()) {
            throw new ExceptionsReader("You can't post an empty tweet !");
        } else if (message.length() > 280) {
            throw new ExceptionsReader("Your tweet must not exceed 280 characters !");
        }
        m_message = message;
        m_creationDate = getDateNow("h:mm a · d MMM yyyy");
        m_userHash = userHash;
        m_isPrivate = isPrivate;
    }

    public Tweet(String message, String userHash,  boolean isPrivate, int parentId) throws ExceptionsReader {
        if (message.isEmpty()) {
            throw new ExceptionsReader("You can't post an empty tweet !");
        } else if (message.length() > 280) {
            throw new ExceptionsReader("Your tweet must not exceed 280 characters !");
        }
        m_message = message;
        m_isPrivate = isPrivate;
        m_creationDate = getDateNow("h:mm a · d MMM yyyy");
        m_userHash = userHash;
        m_parentId = parentId;
    }

    public Tweet(String message, String userHash, boolean isPrivate, int likes, int retweets, int signets, Date creationDate, int id, int parentId) {
        m_message = message;
        m_userHash = userHash;
        m_isPrivate = isPrivate;
        m_likes = likes;
        m_retweets = retweets;
        m_signets = signets;
        m_creationDate = creationDate;
        m_id = id;
        m_parentId = parentId;
    }

    private static Date getDateNow(String pattern) throws ExceptionsReader {
        Date res = null;
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, new Locale("fr"));
            String formattedDate = now.format(formatter);
            java.util.Date date = new SimpleDateFormat(pattern).parse(formattedDate);
            res = new java.sql.Date(date.getTime());
        } catch (ParseException e) {
            throw new ExceptionsReader("Error while getting creation date.");
        }
        return res;
    }



    public void show() {
        System.out.printf("@%s\n%s%s\n%s | %s | %s | %s \n", m_userHash,(isPrivate() ? "[PRIVATE]\n" : ""), m_message, m_likes, m_retweets, m_signets, m_creationDate);
    }

    public int getLikes() { return m_likes; }

    public int getRetweets() { return m_retweets; }

    public int getSignets() { return m_signets; }

    public boolean isPrivate() { return m_isPrivate; }

    public String getMessage() { return m_message; }

    public int getParentId() { return m_parentId; }

    public Date getCreationDate() { return m_creationDate; }

    public int getId() { return m_id; }

    public String getUserHash() { return m_userHash; }


    public void setLikes(int likes) { m_likes = likes; }

    public void setRetweets(int retweets) { m_retweets = retweets; }

    public void setSignets(int signets) { m_signets = signets; }

    public void setId(int id) { m_id = id; }

    public void setParentId(int parentId) { m_parentId = parentId; }

}
