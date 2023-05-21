package twitter.java;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvBuilder;
import twitter.java.account.Account;
import twitter.java.account.Country;
import twitter.java.exceptions.ExceptionsReader;
import twitter.java.message.Message;
import twitter.java.sql.Sql;
import twitter.java.tweet.Tweet;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static twitter.java.sql.Sql.*;

public class Main {
    public static void main(String[] args) {
        JavaTweet();
    }

    public static void JavaTweet() {
        Connection connection = null;
        try {
            // Load environment variables from .env file
            Dotenv dotenv = new DotenvBuilder().load();
            // Access environment variables
            String DB_URL = dotenv.get("DB_URL");
            String DB_USER = dotenv.get("DB_USER");
            String DB_PASSWORD = dotenv.get("DB_PASSWORD");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            Scanner scanner = new Scanner(System.in);

//            Account connectedAccount = getAccount(connection, "nayantest");
            Account connectedAccount = null;

//            connectedAccount.show();


            int choice = -1;
            while (connectedAccount == null) {
                choice = choiceSelector("Welcome to Twitter !", List.of("Login", "Signup"));
                switch (choice) {
                    case 0 -> {
                        System.out.println("See you soon !");
                        System.exit(0);
                    }
                    case 1 -> {
                        connectedAccount = loginAccountForm(connection);
                    }
                    case 2 -> {
                        connectedAccount = creatingAccountForm(connection);
                    }
                }
            }
            choice = -1;
            while (choice != 0) {
                choice = choiceSelector("What do you want to do ?", List.of("My profile", "Tweet", "My TL", "Messages", "Search"));
                switch (choice) {
                    case 1 -> {
                        // My Profile
                        connectedAccount = getAccount(connection, connectedAccount.getUserHash());
                        connectedAccount.show(connection);
                        int choiceMyProfile = choiceSelector("What do you want to do ?", List.of("Edit profile", "My Tweets", "Liked Tweets", "Signets Tweets"));
                        switch (choiceMyProfile) {
                            // TODO: UPDATE ALL
                            case 1 -> {
                                // Edit profile
                                updateAccountForm(connection, connectedAccount);
                            }
                            case 2 -> {
                                // My tweets
                                listOfTweetsForm(connection, Sql.getMyTweetsTableIds(connection, connectedAccount.getUserHash()), "My", connectedAccount.getUserHash());
                            }
                            case 3 -> {
                                // Liked Tweets
                                listOfTweetsForm(connection, Sql.getLikedTableIds(connection, connectedAccount.getUserHash()), "Liked", connectedAccount.getUserHash());
                            }
                            case 4 -> {
                                // Signets Tweets
                                listOfTweetsForm(connection, Sql.getSignetsTableIds(connection, connectedAccount.getUserHash()), "Signets", connectedAccount.getUserHash());
                            }
                        }
                    }
                    case 2 -> {
                        // Tweet Case
                        postTweetForm(connection, connectedAccount.getUserHash());
                    }
                    case 3 -> {
                        // My TL Case
                        listOfTweetsForm(connection, Sql.getMyTLTweetsIds(connection, connectedAccount.getUserHash()), "My TL", connectedAccount.getUserHash());
                    }
                    case 4 -> {
                        // Messages Case
                        messagesForm(connection, connectedAccount.getUserHash());
                    }
                    case 5 -> {
                        // Search Case
                        searchForm(connection, connectedAccount.getUserHash());
                    }
                    case 0 -> {
                        System.out.println("See you soon !");
                        System.exit(0);
                    }
                }
            }
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

    private static int choiceSelector(String title, List<String> choices) {
        Scanner scanner = new Scanner(System.in);
        System.out.println(title);
        for (int i = 0; i < choices.size(); i++) {
            System.out.printf("─ [%d] %s\n", i + 1, choices.get(i));
        }
        System.out.println("─ [0] Exit\n");
        int choice = scanner.nextInt();
        while (choice < 0 || choice > choices.size()) {
            System.out.println("Invalid choice. Please try again.");
            choice = scanner.nextInt();
        }
        return choice;
    }

    private static int contactSelector(String title, List<String> contacts) {
        Scanner scanner = new Scanner(System.in);
        System.out.println(title);
        for (int i = 0; i < contacts.size(); i++) {
            System.out.printf("─ [%d] %s\n", i + 1, contacts.get(i));
        }
        System.out.println("─ [" + (contacts.size() + 1) + "] Show more");
        System.out.println("─ [" + (contacts.size() + 2) + "] Send a message to a new user");
        System.out.println("─ [0] Exit\n");
        int choice = scanner.nextInt();
        while (choice < 0 || choice > contacts.size() + 2) {
            System.out.println("Invalid choice. Please try again.");
            choice = scanner.nextInt();
        }
        return choice;
    }

    private static int accountSelector(String title, List<String> contacts) {
        Scanner scanner = new Scanner(System.in);
        System.out.println(title);
        for (int i = 0; i < contacts.size(); i++) {
            System.out.printf("─ [%d] %s\n", i + 1, contacts.get(i));
        }
        System.out.println("─ [" + (contacts.size() + 1) + "] Show more");
        System.out.println("─ [0] Exit\n");
        int choice = scanner.nextInt();
        while (choice < 0 || choice > contacts.size() + 1) {
            System.out.println("Invalid choice. Please try again.");
            choice = scanner.nextInt();
        }
        return choice;
    }

    private static void messagesForm(Connection connection, String userHash) throws ExceptionsReader {
        Scanner scanner = new Scanner(System.in);
        List<String> contacts = Sql.getContacts(connection, userHash);
        int startIndex = 0;
        int pageSize = 5;

        while (true) {
            if (contacts.isEmpty()) {
                System.out.println("You don't have any contacts!");
                int choice = choiceSelector("What do you want to do ?", List.of("Add a contact"));
                if (choice == 0) break;
                System.out.println("Type the user you want to send a message to:");
                String user = scanner.nextLine();
                if (!Sql.userExists(connection, user)) {
                    System.out.println("This user doesn't exist.");
                    continue;
                }
                handleMessaging(connection, userHash, user);
                break;
            }

            List<String> displayedContacts = contacts.subList(startIndex, Math.min(startIndex + pageSize, contacts.size()));
            int choicePerson = contactSelector((startIndex == 0 ? "------- Messages -------" : ""), displayedContacts);

            if (choicePerson == 0) {
                break;
            } else if (choicePerson == 6) {
                startIndex += pageSize;
            } else if (choicePerson == 7) {
                String user = getUserInput("Type the user you want to send a message to:", scanner, connection);
                if (user != null) {
                    handleMessaging(connection, userHash, contacts.get(choicePerson - 1));
                    break;
                }
            } else {
                handleMessaging(connection, userHash, contacts.get(choicePerson - 1));
                break;
            }
        }
    }

    private static String getUserInput(String prompt, Scanner scanner, Connection connection) throws ExceptionsReader {
        System.out.println(prompt);
        String user = scanner.nextLine();
        if (!Sql.userExists(connection, user)) {
            System.out.println("This user doesn't exist.");
            return null;
        }
        return user;
    }

    private static void handleMessaging(Connection connection, String senderHash, String receiverHash) throws ExceptionsReader {
        Scanner scanner = new Scanner(System.in);
        int action;
        do {
            Sql.showMessages(connection, senderHash, receiverHash);
            action = choiceSelector("What do you want to do?", List.of("Send a message"));
            if (action == 1) {
                System.out.println("Type your message:");
                Sql.sendAMessage(connection, new Message(senderHash, receiverHash, scanner.nextLine()));
            }
        } while (action != 0);
    }

    private static int tweetSelector(String title, List<Tweet> tweets) {
        Scanner scanner = new Scanner(System.in);
        System.out.println(title);
        for (int i = 0; i < tweets.size(); i++) {
            System.out.println("── [" + (i + 1) + "] ──");
            tweets.get(i).show();
            System.out.println("──────────");
        }
        System.out.println("─ [" + (tweets.size() + 1) + "] Show more");
        System.out.println("─ [0] Exit\n");
        int choice = scanner.nextInt();
        while (choice < 0 || choice > tweets.size() + 1) {
            System.out.println("Invalid choice. Please try again.");
            choice = scanner.nextInt();
        }
        return choice;
    }

    private static Account creatingAccountForm(Connection connection) throws ExceptionsReader {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter your username :");
        String username = scanner.next();
        String userHash = null;
        while (userHash == null) {
            System.out.println("Please enter your userHash :");
            String userHashTemp = scanner.next();
            if (!Sql.userExists(connection, userHashTemp)) {
                userHash = userHashTemp;
            } else {
                System.out.println("UserHash already exist. Please try again.");
            }
        }

        String email = null;
        while (email == null) {
            System.out.println("Please enter your email :");
            String emailTemp = scanner.next();
            if (!Sql.emailExist(connection, emailTemp)) {
                email = emailTemp;
            }
        }

        String phoneNumber = null;
        while (phoneNumber == null) {
            System.out.println("Please enter your phone number :");
            String phoneNumberTemp = scanner.next();
            if (Sql.phoneNumberExist(connection, phoneNumberTemp)) {
                phoneNumber = phoneNumberTemp;
            }
        }

        System.out.println("Please enter your password :");
        String password = scanner.next();

        String birthDate = null;
        while (birthDate == null) {
            System.out.println("Please enter your birth date (dd/MM/yyyy) :");
            String birthDateTemp = scanner.next();
            if (Sql.realBirthDate(birthDateTemp)) {
                birthDate = birthDateTemp;
            } else {
                System.out.println("Birth date is not valid. Please try again.");
            }
        }

        Country country = null;
        while (country == null) {
            System.out.println("Please enter your country :");
            String inputCountry = scanner.next();
            try {
                country = Country.valueOf(inputCountry.substring(0, 1).toUpperCase() + inputCountry.substring(1).toLowerCase());

            } catch (IllegalArgumentException e) {
                System.out.println("Country is not valid. Please try again.");
            }
        }
        Account account = new Account(username, userHash, password, email, phoneNumber, birthDate, Country.France);
        Sql.createAccount(connection, account);
        return account;
    }

    private static Account loginAccountForm(Connection connection) throws ExceptionsReader {
        Scanner scanner = new Scanner(System.in);
        Account account = null;
        while (account == null) {
            String userHash = null;
            while (userHash == null) {
                System.out.println("Please enter your userHash :");
                String userHashTemp = scanner.next();
                if (Sql.userExists(connection, userHashTemp)) {
                    userHash = userHashTemp;
                } else {
                    System.out.println("UserHash doesn't exist. Please try again.");
                }
            }

            System.out.println("Please enter your password :");
            String password = scanner.next();
            while (!connectToAccount(connection, userHash, password)) {
                int choice = choiceSelector("Invalid password.", List.of("retry"));
                if (choice == 0) {
                    break;
                }
                password = scanner.next();
            }

            account = getAccount(connection, userHash);
            }
        return account;
    }

    private static void postTweetForm(Connection connection, String userHash) throws ExceptionsReader {
        Scanner scanner = new Scanner(System.in);
        System.out.print("----- Tweet -----\nTweet something :");
        String message = null;
        while (message == null || message.isBlank()) {
            message = scanner.nextLine();
            if (message.isBlank())
                System.out.print("You can't tweet nothing !\nRetry :");
        }
        int choice = choiceSelector("Choose the visibility of your tweet :", List.of("Public", "Private"));
        if (choice == 0) {
            System.out.println("You cancel the tweet !");
            return;
        }
        boolean isPrivate = (choice == 2);
        postTweet(connection, new Tweet(message, userHash, isPrivate));
    }

    private static void replyTweetForm(Connection connection, String userHash, int tweetId) throws ExceptionsReader {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Tell something :");
        String message = null;
        while (message == null || message.isBlank()) {
            message = scanner.nextLine();
            if (message.isBlank())
                System.out.print("You can't reply nothing !\nRetry :");
        }
        int choice = choiceSelector("Confirm reply :", List.of("Post"));
        if (choice == 0) {
            System.out.println("You cancel the reply !");
            return;
        }
        postTweet(connection, new Tweet(message, userHash, false, tweetId));
    }

    private static void listOfTweetsForm(Connection connection, List<Integer> tweetsIds, String listTitle, String userHash) throws ExceptionsReader {
        int i = 0;
        int choice = -1;
        if (tweetsIds.size() == 0) {
            System.out.println("You don't have any " + listTitle + " tweets.");
            choice = 0;
        }
        while (choice != 0) {
            List<Tweet> tweets = Sql.getTweets(connection, tweetsIds.subList(i, (i+5 >= tweetsIds.size()) ? tweetsIds.size() : i+5));
            choice = tweetSelector((i == 0 ? ("--- " + listTitle + " Tweets ---") : ""), tweets);
            if (choice == 6) {
                i += 5;
                continue;
            }
            if (choice != 0) {
                Tweet tweet = tweets.get(choice - 1);
                tweet.show();
                boolean isRetweet = Sql.isRetweeted(connection, userHash, tweet.getId());
                boolean isLiked = Sql.isLiked(connection, userHash, tweet.getId());
                boolean isSignet = Sql.isSigneted(connection, userHash, tweet.getId());
                int choiceTweet = choiceSelector("What do you want to do ?",
                        List.of(
                                (isLiked ? "Unlike" : "Like"),
                                (isRetweet ? "Unretweet" : "Retweet"),
                                (isSignet ? "Remove from Signets" : "Add to Signets"),
                                "Comment", "Show comments"));
                switch (choiceTweet) {
                    case 1 -> {
                        if (!isLiked) {
                            Sql.setLiked(connection, userHash, tweet.getId());
                        } else {
                            Sql.unsetLiked(connection, userHash, tweet.getId());
                        }
                    }
                    case 2 -> {
                        if (!isRetweet) {
                            Sql.setRetweeted(connection, userHash, tweet.getId());
                        } else {
                            Sql.unsetRetweeted(connection, userHash, tweet.getId());
                        }
                    }
                    case 3 -> {
                        if (!isSignet) {
                            Sql.setSigneted(connection, userHash, tweet.getId());
                        } else {
                            Sql.unsetSigneted(connection, userHash, tweet.getId());
                        }
                    }
                    case 4 -> {
                        replyTweetForm(connection, userHash, tweet.getId());
                    }
                    case 5 -> {
                        // TODO: Actions with comments
                        tweet.show();
                        showReplies(connection, tweet.getId(), 1); // call recursive function to show replies
                    }
                }

            }
        }
    }

    private static void listOfContactsForm(Connection connection, List<String> usersHashs, String listTitle, String userHash) throws ExceptionsReader {
        int i = 0;
        int choice = -1;
        if (usersHashs.size() == 0) {
            System.out.println("You don't have any " + listTitle + " tweets.");
            choice = 0;
        }
        while (choice != 0) {
            choice = accountSelector((i == 0 ? ("--- " + listTitle + " Contacts ---") : ""), usersHashs.subList(i, (i + 5 >= usersHashs.size()) ? usersHashs.size() : i + 5));
            if (choice == 6) {
                i += 5;
                continue;
            }
            if (choice != 0) {
                Account account = getAccount(connection, usersHashs.get(choice - 1));
                boolean isSubscribed = isSubTo(connection, userHash, account.getUserHash());
                int choice2 = choiceSelector("What do you want to do ?",
                        List.of((isSubscribed ? "Unfollow" : "Follow"),
                                "Show profile",
                                "Send message"));
                switch (choice2) {
                    case 1 -> {
                        if (!isSubscribed) {
                            Sql.subTo(connection, userHash, account.getUserHash());
                        } else {
                            Sql.unsubTo(connection, userHash, account.getUserHash());
                        }
                        System.out.println("You " + (isSubscribed ? "unfollowed" : "followed") + " " + account.getUsername() + " !");
                    }
                    case 2 -> {
                        account.show(connection);
                        int choice3 = choiceSelector("What do you want to do ?",
                                List.of("Show tweets"));
                        if (choice3 == 1) {
                            listOfTweetsForm(connection, Sql.getMyTweetsTableIds(connection, account.getUserHash()), (account.getUsername() + "'s"), userHash);
                        }
                    }
                    case 3 -> {
                        handleMessaging(connection, userHash, account.getUserHash());
                    }
                }
            }
        }
    }

    private static void updateAccountForm(Connection connection, Account account) throws ExceptionsReader {
        Scanner scanner = new Scanner(System.in);
        System.out.println("----- Update Account -----\nWhat do you want to update ?");
        int choice = -1;
        while (choice != 0) {
            account.show(connection);
            choice = choiceSelector("What do you want to do ?", List.of("Username", "Password", "Email", "Phone number", "Bio", "Country", "Birth date"));
            if (choice == 0) {
                System.out.println("You cancel the update !");
                break;
            }
            switch (choice) {
                case 1 -> {
                    String newUsername = null;
                    while (newUsername == null) {
                        System.out.println("Please enter your new username :");
                        String newUsernameTemp = scanner.next();
                        if (newUsernameTemp.length() < 1 || newUsernameTemp.length() > 50) {
                            System.err.println("Invalid username. Please try again.");
                        } else {
                            newUsername = newUsernameTemp;
                        }
                    }
                    account.setUsername(newUsername);
                }
                case 2 -> {
                    //TODO: ADD verification with last pass
                    String newPassword = null;
                    while (newPassword == null) {
                        System.out.println("Please enter your new password :");
                        String newPasswordTemp = scanner.next();
                        if (newPasswordTemp.length() < 1 || newPasswordTemp.length() > 50) {
                            System.err.println("Invalid password. Please try again.");
                        } else {
                            newPassword = newPasswordTemp;
                        }
                    }
                    account.setPassword(newPassword);

                }
                case 3 -> {
                    String newEmail = null;
                    while (newEmail == null) {
                        System.out.println("Please enter your new email :");
                        String newEmailTemp = scanner.next();
                        if (!Sql.emailExist(connection, newEmailTemp)) {
                            newEmail = newEmailTemp;
                        }
                    }
                    account.setEmail(newEmail);
                }
                case 4 -> {
                    String newPhoneNumber = null;
                    while (newPhoneNumber == null) {
                        System.out.println("Please enter your new phone number :");
                        String newPhoneNumberTemp = scanner.next();
                        if (Sql.phoneNumberExist(connection, newPhoneNumberTemp)) {
                            newPhoneNumber = newPhoneNumberTemp;
                        }
                    }
                    account.setPhoneNumber(newPhoneNumber);
                }
                case 5 -> {
                    String newBio = null;
                    while (newBio == null) {
                        System.out.println("Please enter your new bio :");
                        String newBioTemp = scanner.nextLine();
                        if (newBioTemp.length() > 160) {
                            System.err.println("Invalid bio. Please try again.");
                        } else {
                            newBio = newBioTemp;
                        }
                    }
                    account.setBio(newBio);
                }
                case 6 -> {
                    Country newCountry = null;
                    while (newCountry == null) {
                        System.out.println("Please enter your country :");
                        String inputCountry = scanner.next();
                        try {
                            newCountry = Country.valueOf(inputCountry.substring(0, 1).toUpperCase() + inputCountry.substring(1).toLowerCase());
                        } catch (IllegalArgumentException e) {
                            System.out.println("Country is not valid. Please try again.");
                        }
                    }
                    account.setCountry(newCountry);
                }
                case 7 -> {
                    String birthDate = null;
                    while (birthDate == null) {
                        System.out.println("Please enter your birth date (dd/MM/yyyy) :");
                        String birthDateTemp = scanner.next();
                        if (Sql.realBirthDate(birthDateTemp)) {
                            birthDate = birthDateTemp;
                        } else {
                            System.out.println("Birth date is not valid. Please try again.");
                        }
                    }
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                        account.setBirthDate(new Date(dateFormat.parse(birthDate).getTime()));
                    } catch (ParseException e) {
                        throw new ExceptionsReader("Invalid date format. Please use the format dd/MM/yyyy.");
                    }
                }
            }
            updateAccount(connection, account);
        }
    }

    private static void searchForm(Connection connection, String userHash) throws ExceptionsReader {
        Scanner scanner = new Scanner(System.in);
        String search = null;
        System.out.println("----- Search -----");
        while (search == null || search.isBlank()) {
            System.out.print("Type here : ");
            search = scanner.nextLine();
            if (search.isBlank())
                System.out.println("You can't search nothing.");
        }
        search = search.replace(' ', '&');
        int choice = choiceSelector("What are you searching for ?", List.of("Tweets", "Accounts"));
        switch (choice) {
            case 1 -> {
                try {
                    listOfTweetsForm(connection, Sql.searchTweets(connection, search), "Searched", userHash);
                } catch (SQLException e) {
                    throw new ExceptionsReader("Error while searching tweets.");
                }
            }
            case 2 -> {
                try {
                    List<String> accounts = Sql.searchAccounts(connection, search);
                    accounts.remove(userHash);
                    listOfContactsForm(connection, accounts, "Searched", userHash);
                } catch (SQLException e) {
                    throw new ExceptionsReader("Error while searching accounts.");
                }
            }
        }
    }

}