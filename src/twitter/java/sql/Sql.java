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
        Connection connection = null;

        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connexion réussie à la base de données Supabase !");

            // create a new account
            Account myAccount = new Account("Grafik", "notgrafik", "123456", "pablopjl64@gmail.com", "06 00 00 00 00", "21/11/2004", Country.France);

            // insert the account into the database
            int accountId = createAccount(connection, myAccount);
            System.out.println("Account created with ID: " + accountId);

            // update the account
            myAccount.setBio("Updated bio");

            updateAccount(connection, accountId, myAccount);
            System.out.println("Account updated");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la connexion à la base de données : " + e.getMessage());
        } catch (ExceptionsReader e) {
            throw new RuntimeException(e);
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

}