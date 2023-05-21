package twitter.java.account;

import twitter.java.exceptions.ExceptionsReader;
import twitter.java.sql.Sql;
import twitter.java.tweet.Tweet;

import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Date;

public class Account {
    private String m_username;
    private final String m_userHash;
    private String m_email;
    private String m_phoneNumber;
    private String m_password;
    private Date m_birthDate;
    private final Date m_creationDate;
    private String m_bio = "";
    private Country m_country;
    private int m_id;
    private int m_followersCount = 0;
    private int m_followingsCount = 0;


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
            int followersCount,
            int followingsCount

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
        m_followersCount = followersCount;
        m_followingsCount = followingsCount;
    }


    public void show(Connection connection) throws ExceptionsReader {
        System.out.printf("------ %s ------\n@%s\nBio: %s\nCountry: %s\nBirth date: %s\nCreation date: %s\nFollowers: %d\nFollowing: %d\n-------%s-------\n",
                m_username, m_userHash, m_bio, m_country, m_birthDate.toString(), m_creationDate, getFollowersCount(), getFollowingsCount(), "-".repeat(m_username.length()));
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

    public void setId(int id) { m_id = id; }

    public void setCountry(Country country) { m_country = country; }

    public void setBirthDate(Date date) { m_birthDate = date; }

    public void setPhoneNumber(String newPhoneNumber) {
        m_phoneNumber = newPhoneNumber;
    }

    public void setEmail(String newEmail) {
        m_email = newEmail;
    }

    public void setPassword(String newPassword) {
        m_password = newPassword;
    }

    public void setUsername(String newUsername) {
        m_username = newUsername;
    }

    public int getFollowersCount() { return m_followersCount; }

    public int getFollowingsCount() { return m_followingsCount; }
}
