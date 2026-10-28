/*
 * Purpose: Provides the Swing graphical user interface for iBank.
 * Project role: Problem 4 explicitly requires a Java Swing or JavaFX GUI.
 * This class collects customer/technician input and displays backend results
 * while leaving financial validation and balance updates in BankBackend.
 */
import javax.swing.*;
import java.awt.*;

public class BankGUI {

    private final BankBackend bank;
    private final JFrame frame;
    private BankCustomer loggedInCustomer;

    public BankGUI(BankBackend bank) {
        this.bank = bank;
        frame = new JFrame("iBank - Retail ABM");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(560, 620);
        frame.setLocationRelativeTo(null);
    }

    public void start() {
        showLoginScreen();
        frame.setVisible(true);
    }

    private void showLoginScreen() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 45, 30, 45));

        JLabel heading = new JLabel("iBank Secure Login", SwingConstants.CENTER);
        JTextField cardField = new JTextField();
        JPasswordField pinField = new JPasswordField();
        JButton loginButton = new JButton("UC-01: Authenticate User");
        JButton technicianButton = new JButton("UC-08: ABM Technician Access");

        loginButton.addActionListener(event -> login(cardField, pinField));
        technicianButton.addActionListener(event -> technicianLogin());

        panel.add(heading);
        panel.add(new JLabel("Payment card number:"));
        panel.add(cardField);
        panel.add(new JLabel("4-digit PIN:"));
        panel.add(pinField);
        panel.add(loginButton);
        panel.add(technicianButton);
        panel.add(new JLabel("Supported currencies: CAD, USD, EUR."));

        frame.setContentPane(panel);
        frame.revalidate();
        frame.repaint();
    }

    private void login(JTextField cardField, JPasswordField pinField) {
        try {
            int cardNumber = Integer.parseInt(cardField.getText().trim());
            String pin = new String(pinField.getPassword());
            loggedInCustomer = bank.login(cardNumber, pin);
            showMainScreen();
        } catch (NumberFormatException exception) {
            showError("Card number must contain digits only.");
        } catch (BankException exception) {
            showError(exception.getMessage());
        }
    }

    private void showMainScreen() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 35, 18, 35));

        JLabel welcome = new JLabel(
                "Welcome, " + loggedInCustomer.getCustomerName()
                        + " (card " + loggedInCustomer.getCardNumber() + ")",
                SwingConstants.CENTER);
        JButton withdrawButton = new JButton("UC-02: Withdraw Cash");
        JButton depositButton = new JButton("UC-03: Deposit Cash");
        JButton transferButton = new JButton("UC-04: Transfer Between My Accounts");
        JButton balanceButton = new JButton("UC-05: Balance Inquiry");
        JButton pinButton = new JButton("UC-06: Change PIN");
        JButton logoutButton = new JButton("Log Out");

        withdrawButton.addActionListener(event -> withdrawCash());
        depositButton.addActionListener(event -> depositCash());
        transferButton.addActionListener(event -> transferFunds());
        balanceButton.addActionListener(event -> showBalance());
        pinButton.addActionListener(event -> changePin());
        logoutButton.addActionListener(event -> logout());

        panel.add(welcome);
        panel.add(new JLabel("Choose the currency and account for each transaction."));
        panel.add(withdrawButton);
        panel.add(depositButton);
        panel.add(transferButton);
        panel.add(balanceButton);
        panel.add(pinButton);
        panel.add(logoutButton);

        frame.setContentPane(panel);
        frame.revalidate();
        frame.repaint();
    }

    private void withdrawCash() {
        try {
            String currency = askForCurrency("Withdraw cash in which currency?");
            if (currency == null) {
                return;
            }

            String accountType = askForAccountType(
                    "Withdraw cash from which " + currency + " account?", currency);
            if (accountType == null) {
                return;
            }

            Double amount = askForAmount(
                    "Withdrawal amount from " + currency + " " + accountType + ":");
            if (amount == null) {
                return;
            }
            showMessage(bank.withdrawCash(loggedInCustomer, accountType, currency, amount));
        } catch (NumberFormatException exception) {
            showError("Enter a valid numeric amount.");
        } catch (BankException exception) {
            showError(exception.getMessage());
        }
    }

    private void depositCash() {
        try {
            String currency = askForCurrency("Deposit cash in which currency?");
            if (currency == null) {
                return;
            }

            String accountType = askForAccountType(
                    "Deposit cash into which " + currency + " account?", currency);
            if (accountType == null) {
                return;
            }

            int[] denominations = bank.getSupportedDenominations();
            JTextField[] fields = new JTextField[denominations.length];
            Object[] form = new Object[denominations.length * 2];

            for (int i = 0; i < denominations.length; i++) {
                fields[i] = new JTextField("0");
                form[i * 2] = "Number of $" + denominations[i] + " bills:";
                form[i * 2 + 1] = fields[i];
            }

            int choice = JOptionPane.showConfirmDialog(
                    frame, form, "Deposit " + currency + " Bills", JOptionPane.OK_CANCEL_OPTION);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            int[] counts = new int[denominations.length];
            for (int i = 0; i < fields.length; i++) {
                counts[i] = Integer.parseInt(fields[i].getText().trim());
            }
            showMessage(bank.depositCash(loggedInCustomer, accountType, currency, counts));
        } catch (NumberFormatException exception) {
            showError("Each bill count must be a whole number.");
        } catch (BankException exception) {
            showError(exception.getMessage());
        }
    }

    private void showBalance() {
        showMessage(bank.balanceInquiry(loggedInCustomer));
    }

    private void transferFunds() {
        try {
            String currency = askForCurrency("Transfer which currency?");
            if (currency == null) {
                return;
            }

            String source = askForAccountType(
                    "Transfer funds FROM which " + currency + " account?", currency);
            if (source == null) {
                return;
            }

            String target = askForAccountType(
                    "Transfer funds TO which " + currency + " account?", currency);
            if (target == null) {
                return;
            }

            Double amount = askForAmount(
                    "Transfer from " + source + " to " + target + " in " + currency + ":");
            if (amount == null) {
                return;
            }
            showMessage(bank.transferFunds(loggedInCustomer, source, target, currency, amount));
        } catch (NumberFormatException exception) {
            showError("Enter a valid numeric amount.");
        } catch (BankException exception) {
            showError(exception.getMessage());
        }
    }

    private void changePin() {
        JPasswordField oldPinField = new JPasswordField();
        JPasswordField newPinField = new JPasswordField();
        Object[] form = {
                "Current PIN:", oldPinField,
                "New 4-digit PIN:", newPinField
        };

        int choice = JOptionPane.showConfirmDialog(
                frame, form, "Change PIN", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            showMessage(bank.changePin(
                    loggedInCustomer,
                    new String(oldPinField.getPassword()),
                    new String(newPinField.getPassword())));
        } catch (BankException exception) {
            showError(exception.getMessage());
        }
    }

    private void technicianLogin() {
        JPasswordField codeField = new JPasswordField();
        int choice = JOptionPane.showConfirmDialog(
                frame, codeField, "ABM Technician Code", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            bank.authenticateTechnician(new String(codeField.getPassword()));
            showTechnicianMenu();
        } catch (BankException exception) {
            showError(exception.getMessage());
        }
    }

    private void showTechnicianMenu() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        JButton diagnosticsButton = new JButton("Run Hardware Diagnostics");
        JButton replenishButton = new JButton("Replenish Cash Notes");
        JButton replaceRollButton = new JButton("Replace Paper Receipt Roll");
        JButton simulateFaultButton = new JButton("Simulate Hardware Error (demo)");
        JButton simulateRollButton = new JButton("Simulate Empty Receipt Roll (demo)");
        JButton clearErrorButton = new JButton("Clear Hardware Errors");
        JButton backButton = new JButton("Back to Login");

        diagnosticsButton.addActionListener(
                event -> showMessage(bank.runDiagnostics()));
        replenishButton.addActionListener(event -> replenishCashNotes());
        replaceRollButton.addActionListener(event ->
                showMessage(bank.replaceReceiptRoll()));
        simulateFaultButton.addActionListener(event -> {
            bank.simulateHardwareErrorForDemo();
            showMessage("Demo hardware error activated. Cash deposit and withdrawal are disabled.");
        });
        simulateRollButton.addActionListener(event -> {
            bank.simulateEmptyReceiptRollForDemo();
            showMessage("Demo receipt roll marked empty. Run diagnostics, then replace the roll.");
        });
        clearErrorButton.addActionListener(event ->
                showMessage(bank.clearHardwareErrors()));
        backButton.addActionListener(event -> showLoginScreen());

        panel.add(new JLabel("ABM Technician Maintenance", SwingConstants.CENTER));
        panel.add(diagnosticsButton);
        panel.add(replenishButton);
        panel.add(replaceRollButton);
        panel.add(simulateFaultButton);
        panel.add(simulateRollButton);
        panel.add(clearErrorButton);
        panel.add(backButton);

        frame.setContentPane(panel);
        frame.revalidate();
        frame.repaint();
    }

    private void replenishCashNotes() {
        try {
            String currency = askForCurrency("Select the currency to replenish:");
            if (currency == null) {
                return;
            }

            int[] denominations = bank.getSupportedDenominations();
            Integer[] choices = new Integer[denominations.length];
            for (int i = 0; i < denominations.length; i++) {
                choices[i] = denominations[i];
            }

            JComboBox<Integer> denominationChoice = new JComboBox<>(choices);
            JTextField quantityField = new JTextField("10");
            Object[] form = {
                    "Note denomination:", denominationChoice,
                    "Number of notes to add:", quantityField
            };

            int choice = JOptionPane.showConfirmDialog(
                    frame, form, "Replenish Cash Notes", JOptionPane.OK_CANCEL_OPTION);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            int denomination = (Integer) denominationChoice.getSelectedItem();
            int quantity = Integer.parseInt(quantityField.getText().trim());
            showMessage(bank.replenishCashNotes(currency, denomination, quantity));
        } catch (NumberFormatException exception) {
            showError("Quantity must be a whole number.");
        } catch (BankException exception) {
            showError(exception.getMessage());
        }
    }

    private String askForCurrency(String prompt) {
        String[] choices = bank.getSupportedCurrencies();
        Object selected = JOptionPane.showInputDialog(
                frame,
                prompt,
                "Select Currency",
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                choices[0]);

        return selected == null ? null : selected.toString();
    }

    private String askForAccountType(String prompt, String currency) {
        String[] choices = {Account.CHEQUING, Account.SAVINGS};
        Object selected = JOptionPane.showInputDialog(
                frame,
                prompt,
                "Select " + currency + " Account",
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                Account.CHEQUING);

        return selected == null ? null : selected.toString();
    }

    private Double askForAmount(String prompt) {
        String value = JOptionPane.showInputDialog(frame, prompt);
        if (value == null) {
            return null;
        }
        return Double.parseDouble(value.trim());
    }

    private void logout() {
        loggedInCustomer = null;
        showLoginScreen();
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(
                frame, message, "iBank", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                frame, message, "iBank Error", JOptionPane.ERROR_MESSAGE);
    }
}
