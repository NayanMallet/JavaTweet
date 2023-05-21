package twitter.java.sql;

import twitter.java.account.Account;
import twitter.java.account.Country;
import twitter.java.exceptions.ExceptionsReader;
import twitter.java.message.Message;
import twitter.java.tweet.Tweet;

import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvBuilder;


public class Sql {
    static Dotenv dotenv = new DotenvBuilder().load();
    // Access environment variables
    static String DB_URL = dotenv.get("DB_URL");
    static String DB_USER = dotenv.get("DB_USER");
    static String DB_PASSWORD = dotenv.get("DB_PASSWORD");

    public static void main(String[] args) {
        Connection connection = null;
        try {
//            // Load environment variables from .env file
//            Dotenv dotenv = new DotenvBuilder().load();
//            // Access environment variables
//            String DB_URL = dotenv.get("DB_URL3");
//            String DB_USER = dotenv.get("DB_USER");
//            String DB_PASSWORD = dotenv.get("DB_PASSWORD");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            try {
                updateTweetsLikedTable(connection, "nayantest", List.of(15, 17, 18, 13, 21, 22, 23));
            } catch (ExceptionsReader e) {
                throw new RuntimeException(e);
            }



//            System.out.print(connectToAccount("nayanle2", "12345"));
//            List<Tweet> tweets = getTweets(connection);
//            for (Tweet tweet : tweets) {
//                tweet.show();
//                showReplies(connection, tweet.getId(), 1); // call recursive function to show replies
//            }
//            showMessages(connection, "nayanle2", "notcarla");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la connexion à la base de données : " + e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.out.println("Erreur lors de la fermeture de la connexion : " + e.getMessage());
            }
        }
    }

    /**
     * Create a new account in the database
     *
     * @param connection Connection to the database
     * @param account    The account to create
     * @throws ExceptionsReader If an error occurs in SQL queries
     */
    public static void createAccount(Connection connection, Account account) throws ExceptionsReader {
        try {
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
        } catch (SQLException e) {
            throw new ExceptionsReader("Erreur lors de la création du compte : " + e.getMessage());
        }

    }

    /**
     * Update the tweets table in the database
     *
     * @param connection Connection to the database
     * @param userHash The hash of the user
     * @param tweetList The list of tweets to add to the database
     * @throws ExceptionsReader if the tweets could not be updated
     * @throws SQLException if the SQL query could not be executed
     */
    private static void updateTweetsTable(Connection connection, String userHash, List<Tweet> tweetList) throws ExceptionsReader, SQLException {
        try {
            connection.setAutoCommit(false);
            // Update tweets table
            String SqlQ = "DELETE FROM tweets WHERE user_hash = ?";
            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
                statement.setString(1, userHash);
                statement.executeUpdate();
            }

            List<Tweet> tweetsWithoutParentId = new ArrayList<>();
            List<Tweet> tweetsWithParentId = new ArrayList<>();
            for (Tweet tweet : tweetList) {
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
                    statement.setString(7, userHash);
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
                    statement.setString(7, userHash);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new ExceptionsReader("Account " + userHash + " could not be updated");
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Update the followings table
     *
     * @param connection     the connection to the database
     * @param userHash       the user hash of the account
     * @param followingsList the list of followings
     * @throws ExceptionsReader if the account could not be updated
     * @throws SQLException     if the SQL query could not be executed
     */
    private static void updateFollowingsTable(Connection connection, String userHash, List<String> followingsList) throws ExceptionsReader, SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM following WHERE user_hash = ?");
             PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO following(user_hash, following_user_hash) VALUES (?, ?)")) {

            deleteStatement.setString(1, userHash);
            deleteStatement.executeUpdate();

            for (String followingUserHash : followingsList) {
                insertStatement.setString(1, userHash);
                insertStatement.setString(2, followingUserHash);
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        } catch (SQLException e) {
            connection.rollback();
            throw new ExceptionsReader("Account " + userHash + " could not be updated");
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Update the tweets_liked table
     *
     * @param connection   the connection to the database
     * @param userHash     the user hash of the account
     * @param tweetIdList the list of tweet id
     * @throws ExceptionsReader if the account could not be updated
     * @throws SQLException     if the SQL query could not be executed
     */
    private static void updateTweetsLikedTable(Connection connection, String userHash, List<Integer> tweetIdList) throws ExceptionsReader, SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM tweets_liked WHERE user_hash = ?");
             PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO tweets_liked(tweet_id, user_hash) VALUES (?, ?)")) {

            deleteStatement.setString(1, userHash);
            deleteStatement.executeUpdate();

            for (int tweetId : tweetIdList) {
                insertStatement.setInt(1, tweetId);
                insertStatement.setString(2, userHash);
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        } catch (SQLException e) {
            connection.rollback();
            throw new ExceptionsReader("Account " + userHash + " could not be updated");
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Update the tweets_retweeted table
     *
     * @param connection   the connection to the database
     * @param userHash     the user hash of the account
     * @param tweetIdList the list of tweet id
     * @throws ExceptionsReader if the account could not be updated
     * @throws SQLException     if the SQL query could not be executed
     */
    private static void updateTweetsRetweetedTable(Connection connection, String userHash, List<Integer> tweetIdList) throws ExceptionsReader, SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM tweets_retweeted WHERE user_hash = ?");
             PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO tweets_retweeted(tweet_id, user_hash) VALUES (?, ?)")) {

            deleteStatement.setString(1, userHash);
            deleteStatement.executeUpdate();

            for (int tweetId : tweetIdList) {
                insertStatement.setInt(1, tweetId);
                insertStatement.setString(2, userHash);
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        } catch (SQLException e) {
            connection.rollback();
            throw new ExceptionsReader("Account " + userHash + " could not be updated");
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Update the tweets_signeted table
     *
     * @param connection   the connection to the database
     * @param userHash     the user hash of the account
     * @param tweetIdList the list of tweet id
     * @throws ExceptionsReader if the account could not be updated
     * @throws SQLException     if the SQL query could not be executed
     */
    private static void updateTweetsSignetsTable(Connection connection, String userHash, List<Integer> tweetIdList) throws ExceptionsReader, SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM tweets_signeted WHERE user_hash = ?");
             PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO tweets_signeted(tweet_id, user_hash) VALUES (?, ?)")) {

            deleteStatement.setString(1, userHash);
            deleteStatement.executeUpdate();

            for (int tweetId : tweetIdList) {
                insertStatement.setInt(1, tweetId);
                insertStatement.setString(2, userHash);
                insertStatement.addBatch();
            }

            insertStatement.executeBatch();
        } catch (SQLException e) {
            connection.rollback();
            throw new ExceptionsReader("Account " + userHash + " could not be updated");
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Update an account
     *
     * @param account    the account to update
     * @throws ExceptionsReader if the account could not be updated
     * @throws SQLException     if the SQL query could not be executed
     */
    public static void updateAccount(Connection connection, Account account) throws ExceptionsReader {
        String sqlQuery = "UPDATE accounts SET username = ?, email = ?, phone_number = ?, password = ?, birth_date = ?, bio = ?, country = ? WHERE user_hash = ?";

        boolean initialAutoCommit;

        try {
            initialAutoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            throw new ExceptionsReader("Could not retrieve initial auto commit value");
        }

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement updateAccountsStmt = connection.prepareStatement(sqlQuery)) {
                updateAccountsStmt.setString(1, account.getUsername());
                updateAccountsStmt.setString(2, account.getEmail());
                updateAccountsStmt.setString(3, account.getPhoneNumber());
                updateAccountsStmt.setString(4, account.getPassword());
                updateAccountsStmt.setDate(5, account.getBirthDate());
                updateAccountsStmt.setString(6, account.getBio());
                updateAccountsStmt.setString(7, account.getCountry().toString());
                updateAccountsStmt.setString(8, account.getUserHash());

                int rowsAffected = updateAccountsStmt.executeUpdate();

                if (rowsAffected == 0) {
                    throw new ExceptionsReader("Account " + account.getUserHash() + " does not exist");
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new ExceptionsReader("Account " + account.getUserHash() + " could not be updated");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Could not set auto commit to false");
        } finally {
            try {
                connection.setAutoCommit(initialAutoCommit);
            } catch (SQLException e) {
                throw new ExceptionsReader("Could not restore initial auto commit value");
            }
        }
    }




//    public static void updateAccount(Connection connection, Account account) throws ExceptionsReader, SQLException {
//        try {
//            connection.setAutoCommit(false);
//
//            // Update accounts table
//            String SqlQ = "UPDATE accounts SET username = ?, email = ?, phone_number = ?, password = ?, birth_date = ?, bio = ?, country = ? WHERE user_hash = ?";
//            PreparedStatement updateAccountsStmt = connection.prepareStatement(SqlQ);
//            updateAccountsStmt.setString(1, account.getUsername());
//            updateAccountsStmt.setString(2, account.getEmail());
//            updateAccountsStmt.setString(3, account.getPhoneNumber());
//            updateAccountsStmt.setString(4, account.getPassword());
//            updateAccountsStmt.setDate(5, account.getBirthDate());
//            updateAccountsStmt.setString(6, account.getBio());
//            updateAccountsStmt.setString(7, account.getCountry().toString());
//            updateAccountsStmt.setString(8, account.getUserHash());
//            updateAccountsStmt.executeUpdate();
//
//
//
//
//            // Update tweets table
//            SqlQ = "DELETE FROM tweets WHERE user_hash = ?";
//            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
//                statement.setString(1, account.getUserHash());
//                statement.executeUpdate();
//            }
//
//            List<Tweet> tweetsWithoutParentId = new ArrayList<>();
//            List<Tweet> tweetsWithParentId = new ArrayList<>();
//            for (Tweet tweet : account.getTweets()) {
//                if (tweet.getParentId() != 0) {
//                    tweetsWithParentId.add(tweet);
//                } else {
//                    tweetsWithoutParentId.add(tweet);
//                }
//            }
//
//            SqlQ = "INSERT INTO tweets(message, likes, retweets, signets, is_private, creation_date, user_hash, parent_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
//            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
//                for (Tweet tweet : tweetsWithParentId) {
//                    statement.setString(1, tweet.getMessage());
//                    statement.setInt(2, tweet.getLikes());
//                    statement.setInt(3, tweet.getRetweets());
//                    statement.setInt(4, tweet.getSignets());
//                    statement.setBoolean(5, tweet.isPrivate());
//                    statement.setDate(6, tweet.getCreationDate());
//                    statement.setString(7, account.getUserHash());
//                    statement.setInt(8, tweet.getParentId());
//                    statement.addBatch();
//                }
//                statement.executeBatch();
//            }
//
//            SqlQ = "INSERT INTO tweets(message, likes, retweets, signets, is_private, creation_date, user_hash) VALUES (?, ?, ?, ?, ?, ?, ?)";
//            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
//                for (Tweet tweet : tweetsWithoutParentId) {
//                    statement.setString(1, tweet.getMessage());
//                    statement.setInt(2, tweet.getLikes());
//                    statement.setInt(3, tweet.getRetweets());
//                    statement.setInt(4, tweet.getSignets());
//                    statement.setBoolean(5, tweet.isPrivate());
//                    statement.setDate(6, tweet.getCreationDate());
//                    statement.setString(7, account.getUserHash());
//                    statement.addBatch();
//                }
//                statement.executeBatch();
//            }
//
//
//
//            // Update followers table
//            SqlQ = "DELETE FROM followers WHERE user_hash = ?";
//            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
//                statement.setString(1, account.getUserHash());
//                statement.executeUpdate();
//            }
//            SqlQ = "INSERT INTO followers(user_hash, follower_user_hash) VALUES (?, ?)";
//            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
//                for (String followerUserHash : account.getFollowers()) {
//                    statement.setString(1, account.getUserHash());
//                    statement.setString(2, followerUserHash);
//                    statement.addBatch();
//                }
//                statement.executeBatch();
//            }
//
//            // Update tweets_liked table
//            SqlQ = "DELETE FROM tweets_liked WHERE user_hash = ?";
//            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
//                statement.setString(1, account.getUserHash());
//                statement.executeUpdate();
//            }
//            SqlQ = "INSERT INTO tweets_liked(tweet_id, user_hash) VALUES (?, ?)";
//            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
//                for (int tweetId : account.getLiked()) {
//                    statement.setInt(1, tweetId);
//                    statement.setString(2, account.getUserHash());
//                    statement.addBatch();
//                }
//                statement.executeBatch();
//            }
//
//
//            // Update tweets_retweeted table
//            SqlQ = "DELETE FROM tweets_retweeted WHERE user_hash = ?";
//            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
//                statement.setString(1, account.getUserHash());
//                statement.executeUpdate();
//            }
//            SqlQ = "INSERT INTO tweets_retweeted(tweet_id, user_hash) VALUES (?, ?)";
//            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
//                for (int tweetId : account.getRetweeted()) {
//                    statement.setInt(1, tweetId);
//                    statement.setString(2, account.getUserHash());
//                    statement.addBatch();
//                }
//                statement.executeBatch();
//            }
//
//
//            // Update tweets_signeted table
//            SqlQ = "DELETE FROM tweets_signeted WHERE user_hash = ?";
//            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
//                statement.setString(1, account.getUserHash());
//                statement.executeUpdate();
//            }
//            SqlQ = "INSERT INTO tweets_signeted(tweet_id, user_hash) VALUES (?, ?)";
//            try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
//                for (int tweetId : account.getSigneted()) {
//                    statement.setInt(1, tweetId);
//                    statement.setString(2, account.getUserHash());
//                    statement.addBatch();
//                }
//                statement.executeBatch();
//            }
//
//            connection.commit();
//        } catch (SQLException e) {
//            connection.rollback();
//            throw new ExceptionsReader("Account " + account.getUserHash() + " could not be updated");
//        } finally {
//            connection.setAutoCommit(true);
//        }
//    }

    /**
     * Post a tweet in the database
     *
     * @param connection the connection to the database
     * @param tweet the tweet to post
     * @throws ExceptionsReader if the tweet could not be posted
     */
    public static void postTweet(Connection connection, Tweet tweet) throws ExceptionsReader {
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

    /**
     * Update a tweet in the database
     *
     * @param connection the connection to the database
     * @param tweetId the id of the tweet to update
     * @param likes the new number of likes
     * @param retweets the new number of retweets
     * @param signets the new number of signets
     * @throws ExceptionsReader if the tweet could not be updated
     */
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

    /**
     * Delete a tweet in the database
     *
     * @param connection the connection to the database
     * @param tweetId the id of the tweet to delete
     * @throws ExceptionsReader if the tweet could not be deleted
     */
    private static void deleteTweet(Connection connection, int tweetId) throws ExceptionsReader {
        String SqlQ = "DELETE FROM tweets WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
            statement.setInt(1, tweetId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Tweet[" + tweetId + "] not found");
        }
    }

    /**
     * Get all the tweets in the database
     *
     * @param connection the connection to the database
     * @return the list of all the tweets
     * @throws ExceptionsReader if the tweet could not be found
     */
    public static List<Tweet> getTweets(Connection connection) throws ExceptionsReader {
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

    /**
     * Get all the tweets of a user in the database
     *
     * @param connection the connection to the database
     * @param userHash the hash of the user
     * @return the list of all the tweets of the user
     * @throws ExceptionsReader if the tweet could not be found
     */
    private static List<Tweet> getTweets(Connection connection, String userHash) throws ExceptionsReader {
        List<Tweet> tweets = new ArrayList<>();
        String SqlQ = "SELECT * FROM tweets WHERE parent_id IS NULL AND user_hash = ?";
        try (PreparedStatement statement = connection.prepareStatement(SqlQ)) {
            statement.setString(1, userHash);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String message = resultSet.getString("message");
                int likes = resultSet.getInt("likes");
                int retweets = resultSet.getInt("retweets");
                int signets = resultSet.getInt("signets");
                boolean isPrivate = resultSet.getBoolean("is_private");
                Date creationDate = resultSet.getDate("creation_date");
                int parentId = resultSet.getInt("parent_id");
                tweets.add(new Tweet(message, userHash, isPrivate, likes, retweets, signets, creationDate, id, parentId));
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Tweets could not be shown");
        }
        return tweets;
    }

    public static List<Tweet> getTweets(Connection connection, List<Integer> tweetsIds) throws ExceptionsReader {
        List<Tweet> tweets = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM tweets WHERE id IN (");
        for (int i = 0; i < tweetsIds.size(); i++) {
            queryBuilder.append("?");
            if (i != tweetsIds.size() - 1) {
                queryBuilder.append(",");
            }
        }
        queryBuilder.append(");");
        String query = queryBuilder.toString();
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int i = 0; i < tweetsIds.size(); i++) {
                stmt.setInt(i + 1, tweetsIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String message = rs.getString("message");
                    int likes = rs.getInt("likes");
                    int retweets = rs.getInt("retweets");
                    int signets = rs.getInt("signets");
                    boolean isPrivate = rs.getBoolean("is_private");
                    LocalDate creationDate = rs.getDate("creation_date").toLocalDate();
                    String userHash = rs.getString("user_hash");
                    int parentId = rs.getInt("parent_id");
                    Tweet tweet = new Tweet(message, userHash, isPrivate, likes, retweets, signets, Date.valueOf(creationDate), id, parentId);
                    tweets.add(tweet);
                }
            } catch (SQLException e) {
                throw new ExceptionsReader("Error while getting tweets");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error preparing statement");
        }
        return tweets;
    }


    /**
     * Get all the replies of a tweet in the database
     *
     * @param connection the connection to the database
     * @param tweetId the id of the tweet
     * @return the list of all the replies of the tweet
     * @throws ExceptionsReader if the tweet could not be found
     * TODO: ERROR COME FROM HERE
     */
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

    /**
     * Show all the replies of a tweet
     *
     * @param connection the connection to the database
     * @throws ExceptionsReader if the tweets could not be shown
     * TODO: ERROR COME FROM HERE
     */
    public static void showReplies(Connection connection, int parentId, int indentationLevel) throws ExceptionsReader {
        List<Tweet> replies = getReplies(connection, parentId);
        for (Tweet reply : replies) {
            for (int i = 0; i < indentationLevel; i++) {
                System.out.print("    "); // print indentation
            }
            System.out.println("└─── " + reply.getMessage());
            showReplies(connection, reply.getId(), indentationLevel + 1); // call recursively for each reply
        }
    }

    /**
     * Send a message to another user
     *
     * @param connection the connection to the database
     * @param message the message to send
     * @throws SQLException if the message could not be sent
     */
    public static void sendAMessage(Connection connection, Message message) throws ExceptionsReader {
        String query = "INSERT INTO messages(sender_hash, receiver_hash, message, creation_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, message.getSenderHash());
            statement.setString(2, message.getReceiverHash());
            statement.setString(3, message.getContent());
            statement.setDate(4, message.getCreationDate());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while sending the message.");
        }
    }

    /**
     * Get all the messages of a conversation between two users
     *
     * @param connection the connection to the database
     * @param senderHash the hash of the sender
     * @param receiverHash the hash of the receiver
     * @return the list of all the messages of the conversation
     * @throws ExceptionsReader if the messages could not be found
     */
    public static List<Message> getConversation(Connection connection, String senderHash, String receiverHash) throws ExceptionsReader {
        String sql = "SELECT * FROM messages WHERE (sender_hash = ? AND receiver_hash = ?) OR (sender_hash = ? AND receiver_hash = ?) ORDER BY creation_date ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, senderHash);
            pstmt.setString(2, receiverHash);
            pstmt.setString(3, receiverHash);
            pstmt.setString(4, senderHash);

            try (ResultSet rs = pstmt.executeQuery()) {
                List<Message> messages = new ArrayList<>();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String sender = rs.getString("sender_hash");
                    String receiver = rs.getString("receiver_hash");
                    String message = rs.getString("message");
                    java.sql.Timestamp timestamp = rs.getTimestamp("creation_date");
                    boolean isRead = rs.getBoolean("is_read");
                    Message msg = new Message(id, sender, receiver, message, new Date(timestamp.getTime()), isRead);
                    messages.add(msg);
                }

                // Set all messages to read
                try {
                    setAllMessagesToRead(connection, receiverHash, senderHash);
                } catch (ExceptionsReader e) {
                    throw new ExceptionsReader("Error while setting messages to read.");
                }

                return messages;
            } catch (SQLException e) {
                throw new ExceptionsReader("Error while fetching conversations.");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing query statement.");
        }
    }

//    public static List<Message> getConversation(Connection connection, String senderHash, String receiverHash) throws ExceptionsReader {
//        String sql = "SELECT * FROM messages WHERE (sender_hash = ? AND receiver_hash = ?) OR (sender_hash = ? AND receiver_hash = ?) ORDER BY creation_date ASC";
//        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
//            pstmt.setString(1, senderHash);
//            pstmt.setString(2, receiverHash);
//            pstmt.setString(3, receiverHash);
//            pstmt.setString(4, senderHash);
//            try (ResultSet rs = pstmt.executeQuery()) {
//                List<Message> messages = new ArrayList<>();
//                while (rs.next()) {
//                    int id = rs.getInt("id");
//                    String sender = rs.getString("sender_hash");
//                    String receiver = rs.getString("receiver_hash");
//                    String message = rs.getString("message");
//                    java.sql.Timestamp timestamp = rs.getTimestamp("creation_date");
//                    boolean isRead = rs.getBoolean("is_read");
//                    Message msg = new Message(id, sender, receiver, message, new Date(timestamp.getTime()), isRead);
//                    messages.add(msg);
//                }
//                // Set all messages to read
//                setAllMessagesToRead(connection, senderHash, receiverHash);
//                return messages;
//            } catch (SQLException e) {
//                throw new ExceptionsReader("Error while fetching conversations.");
//            }
//        } catch (SQLException e) {
//            throw new ExceptionsReader("Error while preparing query statement.");
//        }
//    }

    /**
     * Set all the messages of a conversation between two users to read
     *
     * @param connection the connection to the database
     * @param senderHash the hash of the sender
     * @param receiverHash the hash of the receiver
     * @throws SQLException if the messages could not be set to read
     */
    public static void setAllMessagesToRead(Connection connection, String senderHash, String receiverHash) throws ExceptionsReader {
        String sql = "UPDATE messages SET is_read = TRUE WHERE sender_hash = ? AND receiver_hash = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, senderHash);
            preparedStatement.setString(2, receiverHash);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new ExceptionsReader("Error while setting messages to read.");
        }
    }
//    public static void setAllMessagesToRead(Connection connection, String senderHash, String receiverHash) throws ExceptionsReader {
//        String query = "UPDATE messages SET is_read = TRUE WHERE sender_hash = ? AND receiver_hash = ?";
//        try (PreparedStatement stmt = connection.prepareStatement(query)) {
//            stmt.setString(1, senderHash);
//            stmt.setString(2, receiverHash);
//            stmt.executeUpdate();
//        } catch (SQLException e) {
//            throw new ExceptionsReader("Error while preparing statement.");
//        }
//    }

    /**
     * Delete a message
     *
     * @param connection the connection to the database
     * @param messageId the id of the message to delete
     * @throws SQLException if the message could not be deleted
     */
    public static void deleteMessage(Connection connection, int messageId) throws SQLException {
        String query = "DELETE FROM messages WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, messageId);
            stmt.executeUpdate();
        }
    }

    /**
     * Show all the messages of a conversation between two users
     *
     * @param userHash the hash of the user
     * @param otherUserHash the hash of the other user
     * @throws SQLException if the messages could not be shown
     */
    public static void showMessages(Connection connection, String userHash, String otherUserHash) throws ExceptionsReader {
        List<Message> messages = getConversation(connection, userHash, otherUserHash);
        for (Message message : messages) {
            message.show(message.getSenderHash().equals(userHash));
        }
    }

    /**
     * Connect to an account
     *
     * @param connection the connection to the database
     * @param userHash the hash of the user
     * @param password the password of the user
     * @return true if the connection was successful, false otherwise
     * @throws ExceptionsReader if the account could not be found
     */
    public static boolean connectToAccount(Connection connection, String userHash, String password) throws ExceptionsReader {
        String query = "SELECT * FROM accounts WHERE user_hash = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            } catch (SQLException e) {
                throw new ExceptionsReader("Error while executing the query");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    /**
     * Get the tweets liked by a user
     *
     * @param connection the connection to the database
     * @param userHash the hash of the user
     * @return the list of the ids of the tweets liked by the user
     * @throws SQLException if the tweets could not be found
     */
    public static List<Integer> getLikedTableIds(Connection connection, String userHash) throws ExceptionsReader {
        String query = "SELECT tweet_id FROM tweets_liked WHERE user_hash = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Integer> liked = new ArrayList<>();
                while (rs.next()) {
                    liked.add(rs.getInt("tweet_id"));
                }
                return liked;
            } catch (SQLException e) {
                throw new ExceptionsReader("Error while getting the tweets liked by the user");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while executing the query");
        }
    }

    /**
     * Get the tweets retweeted by a user
     *
     * @param connection the connection to the database
     * @param userHash the hash of the user
     * @return the list of the ids of the tweets retweeted by the user
     * @throws SQLException if the tweets could not be found
     */
    public static List<Integer> getRetweetedTableIds(Connection connection, String userHash) throws ExceptionsReader {
        String query = "SELECT tweet_id FROM tweets_retweeted WHERE user_hash = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Integer> retweeted = new ArrayList<>();
                while (rs.next()) {
                    retweeted.add(rs.getInt("tweet_id"));
                }
                return retweeted;
            } catch (SQLException e) {
                throw new ExceptionsReader("Error while getting the tweets retweeted by the user");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while executing the query");
        }
    }

    /**
     * Get the tweets signeted by a user
     *
     * @param connection the connection to the database
     * @param userHash the hash of the user
     * @return the list of the ids of the tweets signeted by the user
     * @throws SQLException if the tweets could not be found
     */
    public static List<Integer> getSignetsTableIds(Connection connection, String userHash) throws ExceptionsReader {
        String query = "SELECT tweet_id FROM tweets_signeted WHERE user_hash = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Integer> signets = new ArrayList<>();
                while (rs.next()) {
                    signets.add(rs.getInt("tweet_id"));
                }
                return signets;
            } catch (SQLException e) {
                throw new ExceptionsReader("Error while getting the tweets signeted by the user");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while executing the query");
        }
    }


    /**
     * Get all the information about an account
     * @param connection the connection to the database
     * @param userHash the hash of the user
     * @return the account
     * @throws ExceptionsReader if the tweets could not be found
     */
    public static Account getAccount(Connection connection, String userHash) throws ExceptionsReader {
        String query = "SELECT * FROM accounts WHERE user_hash = ?";
        Account account = null;
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String username = rs.getString("username");
                    String email = rs.getString("email");
                    String phoneNumber = rs.getString("phone_number");
                    String password = rs.getString("password");
                    Date birthDate = rs.getDate("birth_date");
                    Date creationDate = rs.getDate("creation_date");
                    String biography = rs.getString("bio");
                    Country country =  Country.valueOf(rs.getString("country"));
                    account = new Account(username, userHash, password, email, phoneNumber, birthDate, creationDate, biography, country, id, getFollowersCount(connection, userHash), getFollowingsCount(connection, userHash));
                }
            } catch (SQLException e) {
                throw new ExceptionsReader("The account could not be found");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error preparing the statement");
        }
        return account;
    }
//    public static Account getAccount(Connection connection, String userHash) throws ExceptionsReader {
//        String query = "SELECT * FROM accounts WHERE user_hash = ?";
//        Account account = null;
//        try (PreparedStatement stmt = connection.prepareStatement(query)) {
//            stmt.setString(1, userHash);
//            try (ResultSet rs = stmt.executeQuery()) {
//                if (rs.next()) {
//                    int id = rs.getInt("id");
//                    String username = rs.getString("username");
//                    String email = rs.getString("email");
//                    String phoneNumber = rs.getString("phone_number");
//                    String password = rs.getString("password");
//                    Date birthDate = rs.getDate("birth_date");
//                    Date creationDate = rs.getDate("creation_date");
//                    String biography = rs.getString("bio");
//                    Country country =  Country.valueOf(rs.getString("country"));
//                    try {
//                        List<Tweet> tweets = getTweets(connection, userHash);
//                        List<Integer> liked = getLikedTableIds(connection, userHash);
//                        List<Integer> retweeted = getRetweetedTableIds(connection, userHash);
//                        List<Integer> signeted = getSignetsTableIds(connection, userHash);
//                        List<String> following = getFollowings(connection, userHash);
//                        List<String> follower = getFollowers(connection, userHash);
//                        account = new Account(username, userHash, password, email, phoneNumber, birthDate, creationDate, biography, country, id,  tweets, liked, retweeted, signeted, following, follower);
//                    } catch (ExceptionsReader e) {
//                        throw new ExceptionsReader("Error while getting tweets");
//                    }
//                }
//            } catch (SQLException e) {
//                throw new ExceptionsReader("The account could not be found");
//            }
//        } catch (SQLException e) {
//            throw new ExceptionsReader("Error preparing the statement");
//        }
//        return account;
//    }

    public static boolean emailExist(Connection connection, String email) throws ExceptionsReader {
        // Check if email is in valid format
        Pattern emailPattern = Pattern.compile("\\b[\\w.%-]+@[-.\\w]+\\.[A-Za-z]{2,4}\\b");
        Matcher emailMatcher = emailPattern.matcher(email);
        if (!emailMatcher.matches()) {
            System.out.println("Invalid email format");
            return true;
        }

        // Check if email already exists in the database
        String query = "SELECT COUNT(*) FROM accounts WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean result = rs.next() && rs.getInt(1) > 0;
                if (result) {
                    System.out.println("Email already exists");
                }
                return result;
            } catch (SQLException e) {
                throw new ExceptionsReader("Error while checking if email exists");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    public static boolean phoneNumberExist(Connection connection, String phoneNumber) throws ExceptionsReader {
        // Check if the phone number is valid
        boolean isValidPhoneNumber = phoneNumber.matches("^\\+?[0-9]\\d{1,14}$");
        if (!isValidPhoneNumber) {
            System.out.println("Invalid phone number");
            return false;
        }

        String query = "SELECT COUNT(*) FROM accounts WHERE phone_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, phoneNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count > 0) {
                        System.out.println("Phone number already exists");
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            } catch (SQLException e) {
                throw new ExceptionsReader("Error while checking if phone number exists");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }
    
    public static boolean realBirthDate(String birthdate) {
        try {
            // Parse the birthdate string using the format "yyyy/MM/dd"
            LocalDate date = LocalDate.parse(birthdate, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            // Check if the person is more than 14 years old
            LocalDate today = LocalDate.now();
            Period age = Period.between(date, today);
            if (age.getYears() < 14) {
                return false;
            }
            // Subtract 100 years from the birthdate and check if it's still valid
            LocalDate minus100Years = date.minusYears(100);
            if (minus100Years.isAfter(today)) {
                return false;
            }
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * get list of contacts of a user
     * @param connection connection to the database
     * @param userHash user hash
     * @return list of userHash of contacts
     * @throws ExceptionsReader if an error occurred while getting contacts
     */
    public static List<String> getContacts(Connection connection, String userHash) throws ExceptionsReader {
        String query = "SELECT CASE WHEN sender_hash = ? THEN receiver_hash ELSE sender_hash END AS other_user_hash, MAX(creation_date) AS last_message_date FROM messages WHERE sender_hash = ? OR receiver_hash = ? GROUP BY other_user_hash ORDER BY last_message_date DESC;";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            // Remplacement des marqueurs de position par les valeurs appropriées
            stmt.setString(1, userHash);
            stmt.setString(2, userHash);
            stmt.setString(3, userHash);

            // Exécution de la requête SQL
            ResultSet rs = stmt.executeQuery();

            // Traitement des résultats pour construire la liste de contacts
            List<String> contacts = new ArrayList<>();
            while (rs.next()) {
                String contact = rs.getString("other_user_hash");
                contacts.add(contact);
            }

            return contacts;
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    /**
     * Check if a user exists in the database
     * @param connection connection to the database
     * @param userHash user hash
     * @return true if the user exists, false otherwise
     * @throws ExceptionsReader if an error occurred while checking if user exists
     */
    public static boolean userExists(Connection connection, String userHash) throws ExceptionsReader {
        String query = "SELECT COUNT(*) FROM accounts WHERE user_hash = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                } else {
                    throw new ExceptionsReader("Error while checking if user exists: no result");
                }
            } catch (SQLException e) {
                throw new ExceptionsReader("Error while checking if user exists");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }


    public static void setRetweeted(Connection connection, String userHash, int tweetId) throws ExceptionsReader {
        String query = "INSERT INTO tweets_retweeted (user_hash, tweet_id) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            stmt.setInt(2, tweetId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    public static List<String> getFollowings(Connection connection, String userHash) throws ExceptionsReader {
        List<String> followings = new ArrayList<>();

        String sql = "SELECT following_user_hash FROM following WHERE user_hash = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userHash);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                followings.add(rs.getString("following_user_hash"));
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while getting followings.");
        }

        return followings;
    }

    public static List<String> getFollowers(Connection connection, String userHash) throws ExceptionsReader {
        List<String> followers = new ArrayList<>();

        String sql = "SELECT user_hash FROM following WHERE following_user_hash = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userHash);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                followers.add(rs.getString("user_hash"));
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while getting followers.");
        }

        return followers;
    }

    public static int getFollowingsCount(Connection connection, String userHash) throws ExceptionsReader {
        int count = 0;

        String sql = "SELECT COUNT(following_user_hash) FROM following WHERE user_hash = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userHash);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        }  catch (SQLException e) {
            throw new ExceptionsReader("Error while getting followings count.");
        }

        return count;
    }

    public static int getFollowersCount(Connection connection, String userHash) throws ExceptionsReader {
        int count = 0;

        String sql = "SELECT COUNT(user_hash) FROM following WHERE following_user_hash = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userHash);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while getting followers count.");
        }

        return count;
    }


    public static void followUser(Connection connection, String userHash, String followingUserHash) throws ExceptionsReader {
        String sql = "INSERT INTO following (user_hash, following_user_hash) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userHash);
            pstmt.setString(2, followingUserHash);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while following user.");
        }
    }

    public static void unfollowUser(Connection connection, String userHash, String followingUserHash) throws ExceptionsReader {
        String sql = "DELETE FROM following WHERE user_hash = ? AND following_user_hash = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userHash);
            pstmt.setString(2, followingUserHash);
            pstmt.executeUpdate();
        }  catch (SQLException e) {
            throw new ExceptionsReader("Error while unfollowing user.");
        }
    }

    public static void unsetRetweeted(Connection connection, String userHash, int tweetId) throws ExceptionsReader {
        String query = "DELETE FROM tweets_retweeted WHERE user_hash = ? AND tweet_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            stmt.setInt(2, tweetId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    public static boolean isRetweeted(Connection connection, String userHash, int tweetId) throws ExceptionsReader {
        String query = "SELECT COUNT(*) FROM tweets_retweeted WHERE user_hash = ? AND tweet_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            stmt.setInt(2, tweetId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                } else {
                    throw new ExceptionsReader("Error while checking if user exists: no result");
                }
            } catch (SQLException e) {
                throw new ExceptionsReader("Error while checking if user exists");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    public static void setLiked(Connection connection, String userHash, int tweetId) throws ExceptionsReader {
        String query = "INSERT INTO tweets_liked (user_hash, tweet_id) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            stmt.setInt(2, tweetId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    public static void unsetLiked(Connection connection, String userHash, int tweetId) throws ExceptionsReader {
        String query = "DELETE FROM tweets_liked WHERE user_hash = ? AND tweet_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            stmt.setInt(2, tweetId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    public static boolean isLiked(Connection connection, String userHash, int tweetId) throws ExceptionsReader {
        String query = "SELECT COUNT(*) FROM tweets_liked WHERE user_hash = ? AND tweet_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            stmt.setInt(2, tweetId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                } else {
                    throw new ExceptionsReader("Error while checking if user exists: no result");
                }
            } catch (SQLException e) {
                throw new ExceptionsReader("Error while checking if user exists");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    public static void setSigneted(Connection connection, String userHash, int tweetId) throws ExceptionsReader {
        String query = "INSERT INTO tweets_signeted (user_hash, tweet_id) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            stmt.setInt(2, tweetId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    public static void unsetSigneted(Connection connection, String userHash, int tweetId) throws ExceptionsReader {
        String query = "DELETE FROM tweets_signeted WHERE user_hash = ? AND tweet_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            stmt.setInt(2, tweetId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    public static boolean isSigneted(Connection connection, String userHash, int tweetId) throws ExceptionsReader {
        String query = "SELECT COUNT(*) FROM tweets_signeted WHERE user_hash = ? AND tweet_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            stmt.setInt(2, tweetId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                } else {
                    throw new ExceptionsReader("Error while checking if user exists: no result");
                }
            } catch (SQLException e) {
                throw new ExceptionsReader("Error while checking if user exists");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    public static List<Integer> getMyTweetsTableIds(Connection connection, String userHash) throws ExceptionsReader {
        List<Integer> ids = new ArrayList<>();

        // Récupérer les tweets de l'utilisateur
        String sql1 = "SELECT id FROM tweets WHERE user_hash = ?";
        try (PreparedStatement pstmt1 = connection.prepareStatement(sql1)) {
            pstmt1.setString(1, userHash);
            ResultSet rs1 = pstmt1.executeQuery();
            while (rs1.next()) {
                ids.add(rs1.getInt("id"));
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }

        // Récupérer les tweets que l'utilisateur a retweetés
        String sql2 = "SELECT tweet_id FROM tweets_retweeted WHERE user_hash = ?";
        try (PreparedStatement pstmt2 = connection.prepareStatement(sql2)) {
            pstmt2.setString(1, userHash);
            ResultSet rs2 = pstmt2.executeQuery();
            while (rs2.next()) {
                ids.add(rs2.getInt("tweet_id"));
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }

        return ids;
    }

    public static List<Integer> getMyTLTweetsIds(Connection connection, String userHash) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        List<String> following = new ArrayList<>();

        // Récupérer les utilisateurs que l'utilisateur suit
        String sql1 = "SELECT following_user_hash FROM following WHERE user_hash = ?";
        try (PreparedStatement pstmt1 = connection.prepareStatement(sql1)) {
            pstmt1.setString(1, userHash);
            ResultSet rs1 = pstmt1.executeQuery();
            while (rs1.next()) {
                following.add(rs1.getString("following_user_hash"));
            }
        }

        // Pour chaque utilisateur que l'utilisateur suit, récupérer leurs tweets
        String sql2 = "SELECT id FROM tweets WHERE user_hash = ?";
        try (PreparedStatement pstmt2 = connection.prepareStatement(sql2)) {
            for (String user : following) {
                pstmt2.setString(1, user);
                ResultSet rs2 = pstmt2.executeQuery();
                while (rs2.next()) {
                    ids.add(rs2.getInt("id"));
                }
            }
        }

        return ids;
    }

    public static List<String> searchAccounts(Connection connection, String searchTerm) throws SQLException {
        List<String> userHashs = new ArrayList<>();
        String sql = "SELECT user_hash FROM accounts WHERE user_hash LIKE '%" + searchTerm + "%' OR user_hash LIKE '" + searchTerm + "%' OR user_hash LIKE '%" + searchTerm + "'";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                userHashs.add(resultSet.getString("user_hash"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return userHashs;
    }

    public static List<Integer> searchTweets(Connection connection, String searchTerm) throws SQLException {
        List<Integer> tweetIds = new ArrayList<>();
        String query = "SELECT id FROM tweets WHERE to_tsvector('english', message) @@ to_tsquery(?)";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, searchTerm + ":*");
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            tweetIds.add(resultSet.getInt("id"));
        }
        return tweetIds;
    }

    public static void subTo(Connection connection, String userHash, String followingUserHash) throws ExceptionsReader {
        String query = "INSERT INTO following (user_hash, following_user_hash) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            stmt.setString(2, followingUserHash);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    public static void unsubTo(Connection connection, String userHash, String followingUserHash) throws ExceptionsReader {
        String query = "DELETE FROM following WHERE user_hash = ? AND following_user_hash = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            stmt.setString(2, followingUserHash);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }

    public static boolean isSubTo(Connection connection, String userHash, String followingUserHash) throws ExceptionsReader {
        String query = "SELECT COUNT(*) FROM following WHERE user_hash = ? AND following_user_hash = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userHash);
            stmt.setString(2, followingUserHash);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                } else {
                    throw new ExceptionsReader("Error while checking if user exists: no result");
                }
            } catch (SQLException e) {
                throw new ExceptionsReader("Error while checking if user exists");
            }
        } catch (SQLException e) {
            throw new ExceptionsReader("Error while preparing statement");
        }
    }
}
