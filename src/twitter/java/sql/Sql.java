package twitter.java.sql;

import twitter.java.account.Account;
import twitter.java.account.Country;
import twitter.java.exceptions.ExceptionsReader;
import twitter.java.tweet.Tweet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.sql.Types.NULL;

public class Sql {

    private static final String DB_URL = "jdbc:postgresql://db.aaojrvmiytpbeiksvuve.supabase.co:5432/postgres";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "Demmiw-3nusfo-nubfez";

    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to the PostgreSQL server successfully.");
            updateTweet(connection, 13, 1000, 100, 3);
            List<Tweet> tweets = getTweets(connection);
            for (Tweet tweet : tweets) {
                tweet.show();
                showReplies(connection, tweet.getId(), 1); // call recursive function to show replies
            }
        } catch (SQLException | ExceptionsReader e) {
            throw new RuntimeException(e);
        }
    }


    private static int createAccount(Connection connection, Account account) throws SQLException {
        String sqlQ = "INSERT INTO accounts (username, user_hash, email, phone_number, password, birth_date, creation_date, bio, country) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(sqlQ, Statement.RETURN_GENERATED_KEYS);

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

    private static void updateAccount(Connection connection, Account account) throws ExceptionsReader, SQLException {
        try {
            connection.setAutoCommit(false);

            // Update accounts table
            String SqlQ = "UPDATE accounts SET username = ?, email = ?, phone_number = ?, password = ?, birth_date = ?, bio = ?, country = ? WHERE user_hash = ?";
            PreparedStatement updateAccountsStmt = connection.prepareStatement(SqlQ);
            updateAccountsStmt.setString(1, account.getUsername());
            updateAccountsStmt.setString(2, account.getEmail());
            updateAccountsStmt.setString(3, account.getPhoneNumber());
            updateAccountsStmt.setString(4, account.getPassword());
            updateAccountsStmt.setDate(5, account.getBirthDate());
            updateAccountsStmt.setString(6, account.getBio());
            updateAccountsStmt.setString(7, account.getCountry().toString());
            updateAccountsStmt.setString(8, account.getUserHash());
            updateAccountsStmt.executeUpdate();




            // Update tweets table
            SqlQ = "DELETE FROM tweets WHERE user_hash = ?";
            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
                statement.setString(1, account.getUserHash());
                statement.executeUpdate();
            }

            List<Tweet> tweetsWithoutParentId = new ArrayList<>();
            List<Tweet> tweetsWithParentId = new ArrayList<>();
            for (Tweet tweet : account.getTweets()) {
                if (tweet.getParentId() != 0) {
                    tweetsWithParentId.add(tweet);
                } else {
                    tweetsWithoutParentId.add(tweet);
                }
            }

            SqlQ = "INSERT INTO tweets(message, likes, retweets, signets, is_private, creation_date, user_hash, parent_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
                for (Tweet tweet : tweetsWithParentId) {
                    statement.setString(1, tweet.getMessage());
                    statement.setInt(2, tweet.getLikes());
                    statement.setInt(3, tweet.getRetweets());
                    statement.setInt(4, tweet.getSignets());
                    statement.setBoolean(5, tweet.isPrivate());
                    statement.setDate(6, tweet.getCreationDate());
                    statement.setString(7, account.getUserHash());
                    statement.setInt(8, tweet.getParentId());
                    statement.addBatch();
                }
                statement.executeBatch();
            }

            SqlQ = "INSERT INTO tweets(message, likes, retweets, signets, is_private, creation_date, user_hash) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
                for (Tweet tweet : tweetsWithoutParentId) {
                    statement.setString(1, tweet.getMessage());
                    statement.setInt(2, tweet.getLikes());
                    statement.setInt(3, tweet.getRetweets());
                    statement.setInt(4, tweet.getSignets());
                    statement.setBoolean(5, tweet.isPrivate());
                    statement.setDate(6, tweet.getCreationDate());
                    statement.setString(7, account.getUserHash());
                    statement.addBatch();
                }
                statement.executeBatch();
            }



            // Update followers table
            SqlQ = "DELETE FROM followers WHERE user_hash = ?";
            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
                statement.setString(1, account.getUserHash());
                statement.executeUpdate();
            }
            SqlQ = "INSERT INTO followers(user_hash, follower_user_hash) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
                for (String followerUserHash : account.getFollowers()) {
                    statement.setString(1, account.getUserHash());
                    statement.setString(2, followerUserHash);
                    statement.addBatch();
                }
                statement.executeBatch();
            }

            // Update tweets_liked table
            SqlQ = "DELETE FROM tweets_liked WHERE user_hash = ?";
            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
                statement.setString(1, account.getUserHash());
                statement.executeUpdate();
            }
            SqlQ = "INSERT INTO tweets_liked(tweet_id, user_hash) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
                for (int tweetId : account.getLiked()) {
                    statement.setInt(1, tweetId);
                    statement.setString(2, account.getUserHash());
                    statement.addBatch();
                }
                statement.executeBatch();
            }


            // Update tweets_retweeted table
            SqlQ = "DELETE FROM tweets_retweeted WHERE user_hash = ?";
            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
                statement.setString(1, account.getUserHash());
                statement.executeUpdate();
            }
            SqlQ = "INSERT INTO tweets_retweeted(tweet_id, user_hash) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
                for (int tweetId : account.getRetweeted()) {
                    statement.setInt(1, tweetId);
                    statement.setString(2, account.getUserHash());
                    statement.addBatch();
                }
                statement.executeBatch();
            }


            // Update tweets_signeted table
            SqlQ = "DELETE FROM tweets_signeted WHERE user_hash = ?";
            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
                statement.setString(1, account.getUserHash());
                statement.executeUpdate();
            }
            SqlQ = "INSERT INTO tweets_signeted(tweet_id, user_hash) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
                for (int tweetId : account.getSigneted()) {
                    statement.setInt(1, tweetId);
                    statement.setString(2, account.getUserHash());
                    statement.addBatch();
                }
                statement.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new ExceptionsReader("Account " + account.getUserHash() + " could not be updated");
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static void postTweet(Connection connection, Tweet tweet) throws ExceptionsReader {
        String SqlQ = (tweet.getParentId() != 0 ? "INSERT INTO tweets(message, likes, retweets, signets, is_private, creation_date, user_hash, parent_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)" : "INSERT INTO tweets(message, likes, retweets, signets, is_private, creation_date, user_hash) VALUES (?, ?, ?, ?, ?, ?, ?)");
        try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
            statement.setString(1, tweet.getMessage());
            statement.setInt(2, tweet.getLikes());
            statement.setInt(3, tweet.getRetweets());
            statement.setInt(4, tweet.getSignets());
            statement.setBoolean(5, tweet.isPrivate());
            statement.setDate(6, tweet.getCreationDate());
            statement.setString(7, tweet.getUserHash());
            if (tweet.getParentId() != 0)
                statement.setInt(8, tweet.getParentId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Tweet[" + tweet.getMessage() + "] could not be posted");
        }
    }

    private static void updateTweet(Connection connection, int tweetId, int likes, int retweets, int signets) throws ExceptionsReader {
        String SqlQ = "UPDATE tweets SET likes = ?, retweets = ?, signets = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
            statement.setInt(1, likes);
            statement.setInt(2, retweets);
            statement.setInt(3, signets);
            statement.setInt(4, tweetId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Tweet[" + tweetId + "] not found");
        }
    }

//    String SqlQ = (tweet.getParentId() != 0 ? "UPDATE tweets SET message = ?, likes = ?, retweets = ?, signets = ?, is_private = ?, creation_date = ?, user_hash = ?, parent_id = ? WHERE id = ?" : "UPDATE tweets SET message = ?, likes = ?, retweets = ?, signets = ?, is_private = ?, creation_date = ?, user_hash = ? WHERE id = ?");
//        try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
//        statement.setString(1, tweet.getMessage());
//        statement.setInt(2, tweet.getLikes());
//        statement.setInt(3, tweet.getRetweets());
//        statement.setInt(4, tweet.getSignets());
//        statement.setBoolean(5, tweet.isPrivate());
//        statement.setDate(6, tweet.getCreationDate());
//        statement.setString(7, tweet.getUserHash());
//        if (tweet.getParentId() != 0)
//            statement.setInt(8, tweet.getParentId());
//        statement.setInt(9, tweet.getId());
//        statement.executeUpdate();
//    } catch (SQLException e) {
//        throw new ExceptionsReader("Tweet[" + tweet.getId() + "] not found");
//    }

    private static void deleteTweet(Connection connection, int tweetId) throws ExceptionsReader {
        String SqlQ = "DELETE FROM tweets WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
            statement.setInt(1, tweetId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Tweet[" + tweetId + "] not found");
        }
    }

    private static List<Tweet> getTweets(Connection connection) throws ExceptionsReader {
        List<Tweet> tweets = new ArrayList<>();
        String SqlQ = "SELECT * FROM tweets WHERE parent_id IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String message = resultSet.getString("message");
                int likes = resultSet.getInt("likes");
                int retweets = resultSet.getInt("retweets");
                int signets = resultSet.getInt("signets");
                boolean isPrivate = resultSet.getBoolean("is_private");
                Date creationDate = resultSet.getDate("creation_date");
                String userHash = resultSet.getString("user_hash");
                int parentId = resultSet.getInt("parent_id");
                tweets.add(new Tweet(message, userHash, isPrivate, likes, retweets, signets, creationDate, id, parentId));
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Tweets could not be shown");
        }
        return tweets;
    }

    private static List<Tweet> getReplies(Connection connection, int tweetId) throws ExceptionsReader {
        List<Tweet> replies = new ArrayList<>();
        String SqlQ = "SELECT * FROM tweets WHERE parent_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
            statement.setInt(1, tweetId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String message = resultSet.getString("message");
                int likes = resultSet.getInt("likes");
                int retweets = resultSet.getInt("retweets");
                int signets = resultSet.getInt("signets");
                boolean isPrivate = resultSet.getBoolean("is_private");
                Date creationDate = resultSet.getDate("creation_date");
                String userHash = resultSet.getString("user_hash");
                int parentId = resultSet.getInt("parent_id");
                replies.add(new Tweet(message, userHash, isPrivate, likes, retweets, signets, creationDate, id, parentId));
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Tweets could not be shown");
        }
        return replies;
    }



    private static void showReplies(Connection connection, int parentId, int indentationLevel) throws ExceptionsReader {
        List<Tweet> replies = getReplies(connection, parentId);
        for (Tweet reply : replies) {
            for (int i = 0; i < indentationLevel; i++) {
                System.out.print("    "); // print indentation
            }
            System.out.println("└─── " + reply.getMessage());
            showReplies(connection, reply.getId(), indentationLevel + 1); // call recursively for each reply
        }
    }

//
//    public static Account getAccount(String username, String password) throws ExceptionsReader {
//        Connection connection = null;
//        PreparedStatement statement = null;
//        ResultSet resultSet = null;
//
//        try {
//            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
//            statement = connection.prepareStatement("SELECT * FROM accounts WHERE username = ? AND password = ?");
//            statement.setString(1, username);
//            statement.setString(2, password);
//            resultSet = statement.executeQuery();
//
//            if (resultSet.next()) {
//                String email = resultSet.getString("email");
//                String phoneNumber = resultSet.getString("phone_number");
//                Date birthDate = resultSet.getDate("birth_date");
//                Date creationDate = resultSet.getDate("creation_date");
//                String bio = resultSet.getString("bio");
//                Country country = Country.valueOf(resultSet.getString("country"));
//                String userHash = resultSet.getString("user_hash");
//                int id = resultSet.getInt("id");
//
//                return new Account(username, userHash, password, email, phoneNumber, birthDate, creationDate, bio, country, id);
//            } else {
//                throw new ExceptionsReader("Account not found for username " + username);
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException("Error executing SQL query", e);
//        } finally {
//            try {
//                if (resultSet != null) resultSet.close();
//                if (statement != null) statement.close();
//                if (connection != null) connection.close();
//            } catch (SQLException e) {
//                System.out.println("Error closing resources: " + e.getMessage());
//            }
//        }
//    }
//
//    public List<Tweet> getTweetsByUserHash(String userHash) throws SQLException, ExceptionsReader {
//        Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
//        PreparedStatement statement = connection.prepareStatement("SELECT * FROM tweets WHERE user_hash = ?");
//        statement.setString(1, userHash);
//        ResultSet resultSet = statement.executeQuery();
//        List<Tweet> tweets = new ArrayList<>();
//        while (resultSet.next()) {
//            String content = resultSet.getString("content");
//            Date date = resultSet.getDate("date");
//            try {
//                Tweet tweet = new Tweet(content, userHash, date);
//                tweets.add(tweet);
//            } catch (ExceptionsReader e) {
//                throw new ExceptionsReader("Error creating tweet");
//            }
//        }
//        resultSet.close();
//        statement.close();
//        connection.close();
//        return tweets;
//    }
//
//    public static void postTweet(Tweet tweet) throws SQLException {
//        Connection connection = null;
//        PreparedStatement statement = null;
//
//        try {
//            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
//
//            // Prepare the SQL statement
//            String query = "INSERT INTO tweets (message, likes, retweets, signets, is_private, creation_date, user_hash) " +
//                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
//            statement = connection.prepareStatement(query);
//
//            // Set the parameters of the SQL statement
//            statement.setString(1, tweet.getMessage());
//            statement.setInt(2, tweet.getLikes());
//            statement.setInt(3, tweet.getRetweets());
//            statement.setInt(4, tweet.getSignets());
//            statement.setBoolean(5, tweet.isPrivate());
//            statement.setDate(6, tweet.getCreationDate());
//            statement.setString(7, tweet.getUserHash());
//
//            // Execute the SQL statement
//            statement.executeUpdate();
//            System.out.println("Tweet posted successfully!");
//
//        } catch (SQLException e) {
//            System.out.println("Error posting tweet: " + e.getMessage());
//        } finally {
//            if (statement != null) {
//                statement.close();
//            }
//            if (connection != null) {
//                connection.close();
//            }
//        }
//    }
//
//    public static void updateTweet(Tweet tweet) throws SQLException {
//        Connection connection = null;
//        PreparedStatement statement = null;
//
//        try {
//            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
//
//            // Prepare the SQL statement
//            String query = "UPDATE tweets SET message=?, likes=?, retweets=?, signets=?, is_private=?, creation_date=?, user_hash=? WHERE id=?";
//            statement = connection.prepareStatement(query);
//
//            // Set the parameters of the SQL statement
//            statement.setString(1, tweet.getMessage());
//            statement.setInt(2, tweet.getLikes());
//            statement.setInt(3, tweet.getRetweets());
//            statement.setInt(4, tweet.getSignets());
//            statement.setBoolean(5, tweet.isPrivate());
//            statement.setDate(6, tweet.getCreationDate());
//            statement.setString(7, tweet.getUserHash());
//            statement.setInt(8, tweet.getId());
//
//            // Execute the SQL statement
//            statement.executeUpdate();
//            System.out.println("Tweet updated successfully!");
//
//        } catch (SQLException e) {
//            System.out.println("Error updating tweet: " + e.getMessage());
//        } finally {
//            if (statement != null) {
//                statement.close();
//            }
//            if (connection != null) {
//                connection.close();
//            }
//        }
//    }

//    public static void testGetAccount() {
//        try {
//            Account retrievedAccount = getAccount("Grafik", "123456");
//            retrievedAccount.show();
//        } catch (ExceptionsReader e) {
//            System.out.println("Error: " + e.getMessage());
//        }
//    }
//
//    public static void testPostTweet() throws ExceptionsReader, SQLException {
//        Tweet tweet = new Tweet("This is a test tweet", "Grafik", new Date(2021, 1, 1));
//        postTweet(tweet);
//
//    }
//
//    public static void testUpdateTweet() throws ExceptionsReader, SQLException {
//        Tweet tweet = new Tweet("This is a test tweet", "Grafik", new Date(2021, 1, 1));
//        tweet.setId(1);
//        updateTweet(tweet);
//    }

}