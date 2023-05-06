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
    private int m_id;

    private List<Tweet> m_tweets = new ArrayList<>();
    private List<Integer> m_liked = new ArrayList<>();
    private List<Integer> m_retweeted = new ArrayList<>();
    private List<Integer> m_signeted = new ArrayList<>();
    private List<String> m_following = new ArrayList<>();
    private List<String> m_followers = new ArrayList<>();


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

    /**
     * Constructor for Account
     * @param username Username of the account
     * @param userHash Hash of the username
     * @param password Password of the account
     * @param email Email of the account
     * @param phoneNumber Phone number of the account
     * @param birthDate Birthdate of the account
     * @param creationDate Creation date of the account
     * @param bio Bio of the account
     * @param country Country of the account
     * @param id ID of the account
     * @param tweets List of tweets of the account
     * @param liked List of liked tweets of the account
     * @param retweeted List of retweeted tweets of the account
     * @param signeted List of signeted tweets of the account
     * @param following List of accounts followed by the account
     * @param followers List of accounts following the account
     */
    public Account(
            String username,
            String userHash,
            String password,
            String email,
            String phoneNumber,
            Date birthDate,
            Date creationDate,
            String bio,
            Country country,
            int id,
            List<Tweet> tweets,
            List<Integer> liked,
            List<Integer> retweeted,
            List<Integer> signeted,
            List<String> following,
            List<String> followers
    ) {
        m_username = username;
        m_userHash = userHash;
        m_password = password;
        m_email = email;
        m_phoneNumber = phoneNumber;
        m_birthDate = birthDate;
        m_creationDate = creationDate;
        m_bio = bio;
        m_country = country;
        m_id = id;
        m_tweets = tweets;
        m_liked = liked;
        m_retweeted = retweeted;
        m_signeted = signeted;
        m_following = following;
        m_followers = followers;
    }


    public void show() {
        System.out.printf("Username: %s\n@%s\nBio: %s\nCountry: %s\nBirth date: %s\nCreation date: %s\n", m_username, m_userHash, m_bio, m_country, m_birthDate.toString(), m_creationDate);
    }

    public String getBio() { return m_bio; }

    public Country getCountry() { return m_country; }

    public void setBio(String bio) { m_bio = bio; }

    public String getUserHash() { return m_userHash; }

    public String getUsername() { return m_username; }

    public String getPassword() { return m_password; }

    public String getEmail() { return m_email; }

    public String getPhoneNumber() { return m_phoneNumber; }

    public Date getBirthDate() { return m_birthDate; }

    public Date getCreationDate() { return m_creationDate; }

    public int getId() { return m_id; }

    public List<Tweet> getTweets() { return m_tweets; }

    public List<Integer> getLiked() { return m_liked; }

    public List<Integer> getRetweeted() { return m_retweeted; }

    public List<Integer> getSigneted() { return m_signeted; }

    public List<String> getFollowing() { return m_following; }

    public List<String> getFollowers() { return m_followers; }



    public void addTweet(Tweet tweetId) { m_tweets.add(tweetId); }

    public void setTweets(List<Tweet> tweets) { m_tweets.addAll(tweets); }

    public void addLiked(int tweetId) { m_liked.add(tweetId); }

    public void setLiked(List<Integer> liked) { m_liked.addAll(liked); }

    public void addRetweeted(int tweetId) { m_retweeted.add(tweetId); }

    public void setRetweeted(List<Integer> retweeted) { m_retweeted.addAll(retweeted); }

    public void addSigneted(int tweetId) { m_signeted.add(tweetId); }

    public void setSigneted(List<Integer> signeted) { m_signeted.addAll(signeted); }

    public void addFollowing(String username) { m_following.add(username); }

    public void setFollowing(List<String> following) { m_following.addAll(following); }

    public void addFollowers(String username) { m_followers.add(username); }

    public void setFollowers(List<String> followers) { m_followers.addAll(followers); }

    public void setId(int id) { m_id = id; }
}
