package banking;

import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class Main {

    private static String enterCardNumber;
    private static String enterPIN;
    private static boolean loggedIn = false;
    private static boolean isDoing = true;

    private static final CardGeneratorNumber cardGeneratorNumber = new CardGeneratorNumber();
    private static final Scanner scanner = new Scanner(System.in);
    private static final SQLiteDataSource dataSource = new SQLiteDataSource();
    private static Connection con = null;
    private static Statement statement = null;
    private static ResultSet rs = null;

    public static void main(String[] args) {
        String url;
        if (args[0].equals("-fileName")) {
            url = "jdbc:sqlite:" + args[1];
        } else {
            url = "jdbc:sqlite:card.s3db";
        }

        dataSource.setUrl(url);

        create();
        createMenu();
    }

    private static void create() {
        try {
            con = dataSource.getConnection();
            System.out.println("Connection is valid.");
            statement = con.createStatement();
            // Statement execution
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS card(" +
                    "id INTEGER PRIMARY KEY," +
                    "number TEXT," +
                    "pin TEXT," +
                    "balance INTEGER DEFAULT 0)");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            check();
        }
    }

    private static void check() {
        try {
            if (rs != null) {
                rs.close();
            }
            if (con != null) {
                con.close();
            }
            if (statement != null) {
                statement.close();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createMenu() {
        while (!loggedIn && isDoing) {
            System.out.println("1. Create an account\n" +
                    "2. Log into account\n" +
                    "0. Exit");
            switch (scanner.nextInt()) {
                case 1:
                    generate();
                    break;
                case 2:
                    login();
                    break;
                case 0:
                    System.out.println("\nBye!");
                    isDoing = false;
                    break;
            }
        }
    }

    private static void loggedMenu() {
        while (loggedIn && isDoing) {
            System.out.println("1. Balance\n" +
                    "2. Add income\n" +
                    "3. Do transfer\n" +
                    "4. Close account\n" +
                    "5. Log out\n" +
                    "0. Exit\n");
            Scanner scanner = new Scanner(System.in);
            switch (scanner.nextInt()) {
                case 1:
                    getBalance();
                    break;
                case 2:
                    addIncome();
                    break;
                case 3:
                    doTransfer();
                    break;
                case 4:
                    closeAccount();
                    loggedIn = false;
                    break;
                case 5:
                    System.out.println("\nYou have successfully logged out!\n");
                    loggedIn = false;
                    break;
                case 0:
                    System.out.println("\nBye!");
                    isDoing = false;
            }
        }
    }

    private static void closeAccount() {


        try {
            check();


            con = dataSource.getConnection();

            statement = con.createStatement();
            String update = "DELETE FROM card WHERE number = '"+enterCardNumber+"'";
            statement.executeUpdate(update);
            System.out.println("\nThe account has been closed!\n");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            check();
        }
    }


    private static void login() {
        System.out.println("Enter your card number:");
        String enterCardNumberScan = scanner.next();
        System.out.println("Enter your PIN:");
        String enterPINScan = scanner.next();
        try {
            check();
            con = dataSource.getConnection();
            statement = con.createStatement();
            rs = statement.executeQuery(
                    "SELECT * FROM card WHERE number = '" + enterCardNumberScan + "' AND pin = '" + enterPINScan + "'");
            if (rs.next()) {
                do {
                    enterCardNumber = rs.getString("number");
                    enterPIN = rs.getString("pin");
                    System.out.println("\nYou have successfully logged in!\n");
                    loggedIn = true;
                    loggedMenu();
                }
                while (rs.next());
            } else {
                System.out.println("\nWrong card number or PIN!\n");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            check();
        }


    }


    private static void generate() {
        String cardPIN = getFormat();
        String str = cardGeneratorNumber.generate("400000");

        try {
            check();

            con = dataSource.getConnection();
            statement = con.createStatement();
            int i = statement.executeUpdate("INSERT INTO card VALUES " +
                    "(NULL, '" + str + "', '" + cardPIN + "', 0)");
            System.out.println("Your card has been created");
            System.out.println("Your card number:");
            System.out.println(str);
            System.out.println("Your card PIN:");
            System.out.println(cardPIN);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            check();
        }


    }

    private static void getDataFromDB() throws SQLException {
        try {
            check();

            rs = statement.executeQuery("SELECT * FROM card");
            while (rs.next()) {
                // Retrieve column values
                int id = rs.getInt("id");
                String number = rs.getString("number");
                String pin = rs.getString("pin");
                int balance = rs.getInt("balance");

                System.out.printf("Card %d%n", id);
                System.out.printf("\tNumber: %s%n", number);
                System.out.printf("\tPIN Code: %s%n", pin);
                System.out.printf("\tBalance: %d%n", balance);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            check();
        }
    }

    private static String getFormat() {
        Random random = new Random();
        return String.format("%04d", random.nextInt(9999));
    }


    private static void doTransfer() {
        System.out.println("Transfer");
        System.out.println("Enter card number:");
        String cardTransfer = scanner.next();


        if (cardTransfer.length() == 16 && !CardGeneratorNumber.checkLuhn(cardTransfer)) {
            System.out.println("Probably you made a mistake in the card number. Please try again!");
        } else if (cardTransfer.equals(enterCardNumber)) {
            System.out.println("You can't transfer money to the same account!");
        } else {
            try {
                check();

                con = dataSource.getConnection();
                statement = con.createStatement();
                rs = statement.executeQuery(
                        "SELECT * FROM card WHERE number = '" + cardTransfer + "'");
                if (rs.next()) {
                    System.out.println("Enter how much money you want to transfer:");
                    int moneySend = scanner.nextInt();
                    check();
                    if (moneySend > getBalanceMethod()) {
                        System.out.println("Not enough money!");
                    } else {
                        check();
                        transfer(cardTransfer, moneySend);
                        System.out.println("Success!");
                    }
                } else {
                    System.out.println("\nSuch a card does not exist.\n");
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            } finally {
                check();
            }
        }


    }

    private static void transfer(String cardTransfer, int moneySend) {
        try {
            check();


            con = dataSource.getConnection();

            statement = con.createStatement();
            String update = "UPDATE card " +
                    "SET balance = balance - " + moneySend + " " +
                    "WHERE number = '" + enterCardNumber + "' AND pin = '" + enterPIN + "'";
            statement.executeUpdate(update);

            con = dataSource.getConnection();
            statement = con.createStatement();
            String update2 = "UPDATE card " +
                    "SET balance = balance + " + moneySend + " " +
                    "WHERE number = '" + cardTransfer + "'";
            statement.executeUpdate(update2);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            check();
        }
    }

    private static void addIncome() {
        System.out.println("Enter income:");
        int income = scanner.nextInt();
        try {
            check();

            con = dataSource.getConnection();
            statement = con.createStatement();
            String update = "UPDATE card " +
                    "SET balance = balance + " + income + " " +
                    "WHERE number = '" + enterCardNumber + "' AND pin = '" + enterPIN + "'";
            statement.executeUpdate(update);
            System.out.println("Income was added!\n");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            check();
        }

    }


    private static void getBalance() {

        try {
            check();

            con = dataSource.getConnection();
            statement = con.createStatement();
            rs = statement.executeQuery("SELECT * FROM card WHERE number = '" + enterCardNumber + "' AND pin = '" + enterPIN + "'");
            while (rs.next()) {
                int balance = rs.getInt("balance");
                System.out.printf("Balance: %d%n", balance);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            check();
        }
    }

    private static int getBalanceMethod() {

        try {
            check();

            con = dataSource.getConnection();
            statement = con.createStatement();
            rs = statement.executeQuery("SELECT * FROM card WHERE number = '" + enterCardNumber + "' AND pin = '" + enterPIN + "'");
            while (rs.next()) {
                return rs.getInt("balance");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            check();
        }
        return 5;
    }
}