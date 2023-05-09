package twitter.java.sql;

import twitter.java.account.Account;
import twitter.java.account.Country;
import twitter.java.exceptions.ExceptionsReader;
import twitter.java.message.Message;
import twitter.java.tweet.Tweet;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.sql.Types.NULL;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvBuilder;


public class Sql {

    public static void main(String[] args) {
        // Load environment variables from .env file
        Dotenv dotenv = new DotenvBuilder().load();

        // Access environment variables
        String DB_URL = dotenv.get("DB_URL3");
        String DB_USER = dotenv.get("DB_USER");
        String DB_PASSWORD = dotenv.get("DB_PASSWORD");

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to the PostgreSQL server successfully.");
//            List<Tweet> tweets = getTweets(connection);
//            for (Tweet tweet : tweets) {
//                tweet.show();
//                showReplies(connection, tweet.getId(), 1); // call recursive function to show replies
//            }
            showMessages(connection, "nayanle2", "notcarla");
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
     * Update the followers table
     *
     * @param connection   the connection to the database
     * @param userHash     the user hash of the account
     * @param followersList the list of followers
     * @throws ExceptionsReader if the account could not be updated
     * @throws SQLException     if the SQL query could not be executed
     */
    private static void updateFollowersTable(Connection connection, String userHash, List<String> followersList) throws ExceptionsReader, SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM followers WHERE user_hash = ?");
             PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO followers(user_hash, follower_user_hash) VALUES (?, ?)")) {

            deleteStatement.setString(1, userHash);
            deleteStatement.executeUpdate();

            for (String followerUserHash : followersList) {
                insertStatement.setString(1, userHash);
                insertStatement.setString(2, followerUserHash);
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

    public static void sendAMessage(Connection connection, Message message) throws SQLException {
        String query = "INSERT INTO messages(sender_hash, receiver_hash, message, creation_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, message.getSenderHash());
            statement.setString(2, message.getReceiverHash());
            statement.setString(3, message.getContent());
            statement.setDate(4, message.getCreationDate());
            statement.executeUpdate();
        }
    }

    public static List<Message> getConversation(Connection connection, String senderHash, String receiverHash) throws SQLException {
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
                setAllMessagesToRead(connection, senderHash, receiverHash);
                return messages;
            }
        }
    }

    public static void setAllMessagesToRead(Connection connection, String senderHash, String receiverHash) throws SQLException {
        String query = "UPDATE messages SET is_read = TRUE WHERE sender_hash = ? AND receiver_hash = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, senderHash);
            stmt.setString(2, receiverHash);
            stmt.executeUpdate();
        }
    }

    public static void deleteMessage(Connection connection, int messageId) throws SQLException {
        String query = "DELETE FROM messages WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, messageId);
            stmt.executeUpdate();
        }
    }


    public static void showMessages(Connection connection, String userHash, String otherUserHash) throws SQLException {
        List<Message> messages = getConversation(connection, userHash, otherUserHash);
        for (Message message : messages) {
            if (message.getSenderHash().equals(userHash)) {
                message.show(true);
            } else {
                message.show(false);
            }
        }
        setAllMessagesToRead(connection, userHash, otherUserHash);
    }

}

//    CREATE TABLE accounts (
//        id SERIAL PRIMARY KEY,
//        username VARCHAR(50) NOT NULL,
//    user_hash VARCHAR(100) NOT NULL UNIQUE,
//    email VARCHAR(50) NOT NULL,
//    phone_number VARCHAR(20),
//    password VARCHAR(100) NOT NULL,
//    birth_date DATE,
//    creation_date DATE NOT NULL,
//    bio TEXT,
//    country VARCHAR(50)
//);
//
//        CREATE TABLE tweets (
//        id SERIAL PRIMARY KEY,
//        message VARCHAR(280) NOT NULL,
//        likes INT DEFAULT 0,
//        retweets INT DEFAULT 0,
//        signets INT DEFAULT 0,
//        is_private BOOLEAN DEFAULT FALSE,
//        creation_date DATE NOT NULL,
//        user_hash VARCHAR(100) NOT NULL REFERENCES accounts(user_hash),
//        parent_id INT,
//        FOREIGN KEY (parent_id) REFERENCES tweets(id) ON DELETE SET NULL
//        );
//
//
//        CREATE TABLE following (
//        id SERIAL PRIMARY KEY,
//        user_hash VARCHAR(100) NOT NULL REFERENCES accounts(user_hash),
//        following_user_hash VARCHAR(100) NOT NULL REFERENCES accounts(user_hash)
//        );
//
//        CREATE TABLE followers (
//        id SERIAL PRIMARY KEY,
//        user_hash VARCHAR(100) NOT NULL REFERENCES accounts(user_hash),
//        follower_user_hash VARCHAR(100) NOT NULL REFERENCES accounts(user_hash)
//        );
//
//        CREATE TABLE tweets_liked (
//        id SERIAL PRIMARY KEY,
//        tweet_id INT NOT NULL REFERENCES tweets(id),
//        user_hash VARCHAR(100) NOT NULL REFERENCES accounts(user_hash)
//        );
//
//        CREATE TABLE tweets_retweeted (
//        id SERIAL PRIMARY KEY,
//        tweet_id INT NOT NULL REFERENCES tweets(id),
//        user_hash VARCHAR(100) NOT NULL REFERENCES accounts(user_hash)
//        );
//
//        CREATE TABLE tweets_signeted (
//        id SERIAL PRIMARY KEY,
//        tweet_id INT NOT NULL REFERENCES tweets(id),
//        user_hash VARCHAR(100) NOT NULL REFERENCES accounts(user_hash)
//        );
