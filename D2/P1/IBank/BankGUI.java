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

    /**
     * Creates the main application window.
     * Input: the simulated bank backend.
     * Output: a BankGUI object.
     * Why: Connects the Swing interface to the backend in one place.
     */
    public BankGUI(BankBackend bank) {
        this.bank = bank;
        frame = new JFrame("iBank - Canadian Retail ABM");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(520, 520);
        frame.setLocationRelativeTo(null);
    }

    /**
     * Opens the application at the login screen.
     * Input: none.
     * Output: no return value; the window becomes visible.
     * Why: Gives IBank a single method to start the user interface.
     */
    public void start() {
        showLoginScreen();
        frame.setVisible(true);
    }

    /**
     * Displays customer login controls and technician access.
     * Input: none.
     * Output: no return value; the login screen is placed in the frame.
     * Why: Customers authenticate before transactions, while technicians use a separate access path.
     */
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
        panel.add(new JLabel("All monetary amounts are in CAD."));

        frame.setContentPane(panel);
        frame.revalidate();
        frame.repaint();
    }

    /**
     * Authenticates the customer using the values in the login form.
     * Input: card-number text field and PIN field.
     * Output: no return value; shows the menu or an error message.
     * Why: Converts GUI input to a backend login request and handles input errors.
     */
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

    /**
     * Shows the logged-in customer their linked accounts and transaction options.
     * Input: none; the authenticated customer is stored in loggedInCustomer.
     * Output: no return value; displays the banking menu.
     * Why: Provides direct access to all retail customer use cases.
     */
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
        panel.add(new JLabel("Choose the account separately for each transaction."));
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

    /**
     * Requests a withdrawal amount and asks the backend to dispense banknotes.
     * Input: amount from the user and the selected Chequing/Savings account.
     * Output: an on-screen receipt or a helpful error message.
     * Why: Connects UC-02 to the bank's validation and cash-cassette logic.
     */
    private void withdrawCash() {
        try {
            String accountType = askForAccountType("Withdraw cash from which account?");
            if (accountType == null) {
                return;
            }

            Double amount = askForAmount(
                    "Withdrawal amount from " + accountType + " (CAD):");
            if (amount == null) {
                return;
            }
            showMessage(bank.withdrawCash(
                    loggedInCustomer, accountType, amount));
        } catch (NumberFormatException exception) {
            showError("Enter a valid numeric amount.");
        } catch (BankException exception) {
            showError(exception.getMessage());
        }
    }

    /**
     * Asks how many bills of each supported denomination are being deposited.
     * Input: user-entered non-negative counts for each note denomination.
     * Output: an on-screen deposit receipt or a helpful error message.
     * Why: Simulates the ABM counting paper currency instead of accepting only a total.
     */
    private void depositCash() {
        String accountType = askForAccountType("Deposit cash into which account?");
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
                frame, form, "Deposit CAD Bills", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            int[] counts = new int[denominations.length];
            for (int i = 0; i < fields.length; i++) {
                counts[i] = Integer.parseInt(fields[i].getText().trim());
            }
            showMessage(bank.depositCash(
                    loggedInCustomer, accountType, counts));
        } catch (NumberFormatException exception) {
            showError("Each bill count must be a whole number.");
        } catch (BankException exception) {
            showError(exception.getMessage());
        }
    }

    /**
     * Displays a receipt containing the balances of both linked accounts.
     * Input: none.
     * Output: an on-screen balance inquiry receipt.
     * Why: Implements UC-05 without requiring the user to select only one account.
     */
    private void showBalance() {
        showMessage(bank.balanceInquiry(loggedInCustomer));
    }

    /**
     * Transfers money between the customer's linked Chequing and Savings accounts.
     * Input: separately selected source and destination accounts, plus a CAD amount.
     * Output: a transfer receipt or an error message.
     * Why: Makes both transfer directions explicit and lets the backend reject
     * choosing the same account as both source and destination.
     */
    private void transferFunds() {
        try {
            String source = askForAccountType("Transfer funds FROM which account?");
            if (source == null) {
                return;
            }

            String target = askForAccountType("Transfer funds TO which account?");
            if (target == null) {
                return;
            }

            Double amount = askForAmount(
                    "Transfer from " + source + " to " + target + " (CAD):");
            if (amount == null) {
                return;
            }
            showMessage(bank.transferFunds(
                    loggedInCustomer, source, target, amount));
        } catch (NumberFormatException exception) {
            showError("Enter a valid numeric amount.");
        } catch (BankException exception) {
            showError(exception.getMessage());
        }
    }

    /**
     * Collects the old and new PINs and calls the backend to change the PIN.
     * Input: values entered in two password fields.
     * Output: a confirmation receipt or an error message.
     * Why: Implements UC-06 without exposing the stored PIN to the GUI.
     */
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

    /**
     * Opens the technician login dialog and validates the technician code.
     * Input: technician code entered in a password field.
     * Output: the technician menu or an error dialog.
     * Why: Restricts the maintenance screen to the simulated technician actor.
     */
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

    /**
     * Shows diagnostics, replenishment, receipt-roll, and error-clearing actions.
     * Input: none.
     * Output: no return value; displays the technician maintenance screen.
     * Why: Implements UC-08 for the ABM Technician actor.
     */
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

    /**
     * Lets the technician choose a bill denomination and number of notes to add.
     * Input: denomination and positive quantity entered on the maintenance screen.
     * Output: replenishment confirmation or an error message.
     * Why: Exposes cash-vault replenishment through the technician GUI.
     */
    private void replenishCashNotes() {
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

        try {
            int denomination = (Integer) denominationChoice.getSelectedItem();
            int quantity = Integer.parseInt(quantityField.getText().trim());
            showMessage(bank.replenishCashNotes(denomination, quantity));
        } catch (NumberFormatException exception) {
            showError("Quantity must be a whole number.");
        } catch (BankException exception) {
            showError(exception.getMessage());
        }
    }

    /**
     * Asks the customer which linked account to use for a transaction.
     * Input: prompt - explains whether the customer is choosing a source or destination.
     * Output: Chequing, Savings, or null if the customer cancels.
     * Why: Makes account selection explicit for withdrawals, deposits, and transfers.
     */
    private String askForAccountType(String prompt) {
        String[] choices = {Account.CHEQUING, Account.SAVINGS};
        Object selected = JOptionPane.showInputDialog(
                frame,
                prompt,
                "Select Account",
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                Account.CHEQUING);

        return selected == null ? null : selected.toString();
    }

    /**
     * Prompts for a monetary amount.
     * Input: a prompt displayed to the customer.
     * Output: the parsed amount, or null if the customer cancels.
     * Why: Reuses the same input behavior for withdrawal and transfer.
     */
    private Double askForAmount(String prompt) {
        String value = JOptionPane.showInputDialog(frame, prompt);
        if (value == null) {
            return null;
        }
        return Double.parseDouble(value.trim());
    }

    /**
     * Logs out the customer and returns to the login screen.
     * Input: none.
     * Output: no return value; the customer session is cleared from the GUI.
     * Why: Stops the next user from continuing the visible customer's session.
     */
    private void logout() {
        loggedInCustomer = null;
        showLoginScreen();
    }

    /**
     * Displays a successful operation or receipt.
     * Input: message to display.
     * Output: no return value.
     * Why: Keeps successful GUI feedback consistent.
     */
    private void showMessage(String message) {
        JOptionPane.showMessageDialog(
                frame, message, "iBank", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Displays a helpful error message.
     * Input: explanation of the error.
     * Output: no return value.
     * Why: Problem 4 requires meaningful messages for exceptional behavior.
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(
                frame, message, "iBank Error", JOptionPane.ERROR_MESSAGE);
    }
}
