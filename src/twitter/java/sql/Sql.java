package twitter.java.sql;

import twitter.java.account.Account;
import twitter.java.account.Country;
import twitter.java.exceptions.ExceptionsReader;
import twitter.java.tweet.Tweet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Sql {

    private static final String DB_URL = "jdbc:postgresql://db.aaojrvmiytpbeiksvuve.supabase.co:5432/postgres";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "Demmiw-3nusfo-nubfez";

    public static void main(String[] args) {
        try {
            testUpdateTweet();
        } catch (ExceptionsReader | SQLException exceptionsReader) {
            exceptionsReader.printStackTrace();
        }

//        Connection connection = null;
//
//        try {
//            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
//            System.out.println("Connexion réussie à la base de données Supabase !");
//
//            // create a new account
//            Account myAccount = new Account("Grafik", "notgrafik", "123456", "pablopjl64@gmail.com", "06 00 00 00 00", "21/11/2004", Country.France);
//
//            // insert the account into the database
//            int accountId = createAccount(connection, myAccount);
//            System.out.println("Account created with ID: " + accountId);
//
//            // update the account
//            myAccount.setBio("Updated bio");
//
//            updateAccount(connection, accountId, myAccount);
//            System.out.println("Account updated");
//        } catch (SQLException e) {
//            System.out.println("Erreur lors de la connexion à la base de données : " + e.getMessage());
//        } catch (ExceptionsReader e) {
//            throw new RuntimeException(e);
//        } finally {
//            try {
//                if (connection != null) {
//                    connection.close();
//                }
//            } catch (SQLException e) {
//                System.out.println("Erreur lors de la fermeture de la connexion : " + e.getMessage());
//            }
//        }
    }

    private static int createAccount(Connection connection, Account account) throws SQLException {
        String query = "INSERT INTO accounts (username, user_hash, email, phone_number, password, birth_date, creation_date, bio, country) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

        statement.setString(1, account.getUsername());
        statement.setString(2, account.getUserHash());
        statement.setString(3, account.getEmail());
        statement.setString(4, account.getPhoneNumber());
        statement.setString(5, account.getPassword());
        statement.setDate(6, account.getBirthDate());
        statement.setDate(7, account.getCreationDate());
        statement.setString(8, account.getBio());
        statement.setString(9, account.getCountry().toString());

        statement.executeUpdate();

        ResultSet resultSet = statement.getGeneratedKeys();
        int accountId = -1;
        if (resultSet.next()) {
            accountId = resultSet.getInt(1);
        }

        statement.close();
        resultSet.close();

        return accountId;
    }

    public static void updateAccount(Connection connection, int accountId, Account account) throws SQLException {
        String sql = "UPDATE accounts SET username = ?, user_hash = ?, email = ?, phone_number = ?, password = ?, birth_date = ?, creation_date = ?, bio = ?, country = ? WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, account.getUsername());
        statement.setString(2, account.getUserHash());
        statement.setString(3, account.getEmail());
        statement.setString(4, account.getPhoneNumber());
        statement.setString(5, account.getPassword());
        statement.setDate(6, account.getBirthDate());
        statement.setDate(7, account.getCreationDate());
        statement.setString(8, account.getBio());
        statement.setString(9, account.getCountry().toString());
        statement.setInt(10, accountId);
        int rowsUpdated = statement.executeUpdate();
        System.out.println(rowsUpdated + " rows updated in accounts table for account id " + accountId);
        statement.close();
    }

    public static Account getAccount(String username, String password) throws ExceptionsReader {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            statement = connection.prepareStatement("SELECT * FROM accounts WHERE username = ? AND password = ?");
            statement.setString(1, username);
            statement.setString(2, password);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String email = resultSet.getString("email");
                String phoneNumber = resultSet.getString("phone_number");
                Date birthDate = resultSet.getDate("birth_date");
                Date creationDate = resultSet.getDate("creation_date");
                String bio = resultSet.getString("bio");
                Country country = Country.valueOf(resultSet.getString("country"));
                String userHash = resultSet.getString("user_hash");
                int id = resultSet.getInt("id");

                return new Account(username, userHash, password, email, phoneNumber, birthDate, creationDate, bio, country, id);
            } else {
                throw new ExceptionsReader("Account not found for username " + username);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error executing SQL query", e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    public List<Tweet> getTweetsByUserHash(String userHash) throws SQLException, ExceptionsReader {
        Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM tweets WHERE user_hash = ?");
        statement.setString(1, userHash);
        ResultSet resultSet = statement.executeQuery();
        List<Tweet> tweets = new ArrayList<>();
        while (resultSet.next()) {
            String content = resultSet.getString("content");
            Date date = resultSet.getDate("date");
            try {
                Tweet tweet = new Tweet(content, userHash, date);
                tweets.add(tweet);
            } catch (ExceptionsReader e) {
                throw new ExceptionsReader("Error creating tweet");
            }
        }
        resultSet.close();
        statement.close();
        connection.close();
        return tweets;
    }

    public static void postTweet(Tweet tweet) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // Prepare the SQL statement
            String query = "INSERT INTO tweets (message, likes, retweets, signets, is_private, creation_date, user_hash) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            statement = connection.prepareStatement(query);

            // Set the parameters of the SQL statement
            statement.setString(1, tweet.getMessage());
            statement.setInt(2, tweet.getLikes());
            statement.setInt(3, tweet.getRetweets());
            statement.setInt(4, tweet.getSignets());
            statement.setBoolean(5, tweet.isPrivate());
            statement.setDate(6, tweet.getCreationDate());
            statement.setString(7, tweet.getUserHash());

            // Execute the SQL statement
            statement.executeUpdate();
            System.out.println("Tweet posted successfully!");

        } catch (SQLException e) {
            System.out.println("Error posting tweet: " + e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    public static void updateTweet(int tweetId, Tweet updatedTweet) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // Prepare the SQL statement
            String query = "UPDATE tweets SET message = ?, likes = ?, retweets = ?, signets = ?, is_private = ?, creation_date = ?, user_hash = ? WHERE id = ?";
            statement = connection.prepareStatement(query);

            // Set the parameters of the SQL statement
            statement.setString(1, updatedTweet.getMessage());
            statement.setInt(2, updatedTweet.getLikes());
            statement.setInt(3, updatedTweet.getRetweets());
            statement.setInt(4, updatedTweet.getSignets());
            statement.setBoolean(5, updatedTweet.isPrivate());
            statement.setDate(6, updatedTweet.getCreationDate());
            statement.setString(7, updatedTweet.getUserHash());
            statement.setInt(8, tweetId);

            // Execute the SQL statement
            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Tweet updated successfully!");
            } else {
                System.out.println("Tweet not found or could not be updated.");
            }

        } catch (SQLException e) {
            System.out.println("Error updating tweet: " + e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }


    public static void testGetAccount() {
        try {
            Account retrievedAccount = getAccount("Grafik", "123456");
            retrievedAccount.show();
        } catch (ExceptionsReader e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void testPostTweet() throws ExceptionsReader, SQLException {
        Tweet tweet = new Tweet("This is a test tweet", "Grafik", new Date(2021, 1, 1));
        postTweet(tweet);

    }

    public static void testUpdateTweet() throws ExceptionsReader, SQLException {
        Tweet tweet = new Tweet("This is an updated test tweet", "Grafik", new Date(2021, 1, 1), 1, 1, 1, false);
        updateTweet(1, tweet);
    }

}