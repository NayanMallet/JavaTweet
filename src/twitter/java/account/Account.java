package twitter.java.account;

import twitter.java.exceptions.ExceptionsReader;
import twitter.java.tweet.Tweet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.sql.Date;
import java.util.List;

public class Account {
    private final String m_username;
    private final String m_userHash;
    private final String m_email;
    private final String m_phoneNumber;
    private final String m_password;
    private final Date m_birthDate;
    private final Date m_creationDate;
    private String m_bio = "";
    private Country m_country;

    private final List<Tweet> m_tweets = new ArrayList<>();
    private final List<Tweet> m_liked = new ArrayList<>();

    public Account(String username, String userHash, String password, String email, String phoneNumber, String birthDate, Country country) throws ExceptionsReader {
        if (username.isEmpty()) {
            throw new ExceptionsReader("You can't create an account without a username !");
        } else if (password.isEmpty()) {
            throw new ExceptionsReader("You can't create an account without a password !");
        } else if (email.isEmpty()) {
            throw new ExceptionsReader("You can't create an account without an email !");
        } else if (phoneNumber.isEmpty()) {
            throw new ExceptionsReader("You can't create an account without a phone number !");
        } else if (birthDate.isEmpty()) {
            throw new ExceptionsReader("You can't create an account without a birth date !");
        } else if (country == null) {
            throw new ExceptionsReader("You can't create an account without a country !");
        } else if (userHash.isEmpty()) {
            throw new ExceptionsReader("You can't create an account without a user hash !");
        }

        m_username = username;
        m_userHash = userHash;
        m_password = password;
        m_email = email;
        m_phoneNumber = phoneNumber;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            m_birthDate = new Date(dateFormat.parse(birthDate).getTime());
        } catch (ParseException e) {
            throw new ExceptionsReader("Invalid date format. Please use the format dd/MM/yyyy.");
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
            String formattedDate = now.format(formatter);
            java.util.Date date = new SimpleDateFormat("MMM yyyy").parse(formattedDate);
            m_creationDate = new java.sql.Date(date.getTime());
        } catch (ParseException e) {
            throw new ExceptionsReader("Error while getting creation date.");
        }
        m_country = country;
    }


    public void show() {
        System.out.printf("Username: %s\n@%s\nBio: %s\nCountry: %s\nBirth date: %s\nCreation date: %s\n", m_username, m_userHash, m_bio, m_country, m_birthDate.toString(), m_creationDate);
    }

    public void showTweets() {
        System.out.printf("------ %s's tweets -----\n", m_username);
        for (Tweet tweet : m_tweets) {
            tweet.show();
        }
        System.out.println("-------------------------");
    }

    public void postTweet(Tweet tweet) {
        m_tweets.add(tweet);
    }

    public String getBio() {
        return m_bio;
    }

    public Country getCountry() {
        return m_country;
    }

    public void setBio(String bio) {
        m_bio = bio;
    }

    public String getUserHash() {
        return m_userHash;
    }

    public String getUsername() {
        return m_username;
    }

    public String getPassword() {
        return m_password;
    }

    public String getEmail() {
        return m_email;
    }

    public String getPhoneNumber() {
        return m_phoneNumber;
    }

    public Date getBirthDate() {
        return m_birthDate;
    }

    public Date getCreationDate() {
        return m_creationDate;
    }
}
