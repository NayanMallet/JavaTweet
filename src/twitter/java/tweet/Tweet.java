package twitter.java.tweet;

import twitter.java.exceptions.ExceptionsReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Tweet {
    private static String m_message;
    private int m_likes = 0;
    private int m_retweets = 0;
    private int m_signets = 0;
    private boolean m_isPrivate = false;
    private static String m_creationDate;

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
        System.out.printf("Username\n@Username\n%s\n%s\n", m_message, m_creationDate);
    }


}
