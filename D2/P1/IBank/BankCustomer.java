/*
 * Purpose: Represents a retail customer and their two linked accounts.
 * Project role: The backend keeps customer records here, verifies the PIN,
 * changes the PIN, and tracks the customer's combined daily withdrawals.
 */
import java.time.LocalDate;

public class BankCustomer {

    private final int cardNumber;
    private final String customerName;
    private String pin;
    private final Account chequingAccount;
    private final Account savingsAccount;
    private double dailyWithdrawalTotal;
    private LocalDate withdrawalDate = LocalDate.now();

    /**
     * Creates a customer with linked Chequing and Savings accounts.
     * Input: card number, name, four-digit PIN, and the two starting balances.
     * Output: a new BankCustomer object.
     * Why: Keeps customer credentials and linked account records together.
     */
    public BankCustomer(int cardNumber, String customerName, String pin,
                        double chequingBalance, double savingsBalance)
            throws BankException {
        if (cardNumber <= 0) {
            throw new BankException("Card number must be positive.");
        }
        validatePin(pin);

        this.cardNumber = cardNumber;
        this.customerName = customerName;
        this.pin = pin;
        this.chequingAccount = new Account(
                (long) cardNumber * 10 + 1,
                Account.CHEQUING,
                chequingBalance);
        this.savingsAccount = new Account(
                (long) cardNumber * 10 + 2,
                Account.SAVINGS,
                savingsBalance);
    }

    /**
     * Checks the PIN entered at login.
     * Input: the PIN entered by the customer.
     * Output: true only when it matches the stored PIN.
     * Why: The backend uses this check before opening a customer session.
     */
    public boolean verifyPin(String enteredPin) {
        return enteredPin != null && pin.equals(enteredPin);
    }

    /**
     * Changes the customer's PIN after checking the existing PIN.
     * Input: old PIN and new PIN.
     * Output: no return value; the saved PIN changes.
     * Why: Implements the Change PIN use case while keeping the PIN private.
     */
    public void changePin(String oldPin, String newPin) throws BankException {
        if (!verifyPin(oldPin)) {
            throw new BankException("The current PIN is incorrect.");
        }
        validatePin(newPin);
        pin = newPin;
    }

    /**
     * Returns one of the customer's linked accounts.
     * Input: account type, either Chequing or Savings.
     * Output: the matching Account object.
     * Why: Keeps account selection consistent for all banking operations.
     */
    public Account getAccount(String accountType) throws BankException {
        if (Account.CHEQUING.equalsIgnoreCase(accountType)) {
            return chequingAccount;
        }
        if (Account.SAVINGS.equalsIgnoreCase(accountType)) {
            return savingsAccount;
        }
        throw new BankException("Select Chequing or Savings.");
    }

    /**
     * Returns the card number used for login.
     * Input: none.
     * Output: the customer's card number.
     * Why: The backend uses this value to locate the customer record.
     */
    public int getCardNumber() {
        return cardNumber;
    }

    /**
     * Returns the customer's display name.
     * Input: none.
     * Output: the customer's name.
     * Why: The GUI and receipts identify the logged-in customer.
     */
    public String getCustomerName() {
        return customerName;
    }

    /**
     * Returns the Chequing account.
     * Input: none.
     * Output: the customer's Chequing Account.
     * Why: Lets receipts and balance inquiries display both linked accounts.
     */
    public Account getChequingAccount() {
        return chequingAccount;
    }

    /**
     * Returns the Savings account.
     * Input: none.
     * Output: the customer's Savings Account.
     * Why: Lets receipts and balance inquiries display both linked accounts.
     */
    public Account getSavingsAccount() {
        return savingsAccount;
    }

    /**
     * Returns the total cash withdrawn today across both accounts.
     * Input: none.
     * Output: today's combined withdrawal total in CAD.
     * Why: The daily limit applies to the customer, not separately to each account.
     */
    public double getDailyWithdrawalTotal() {
        resetDailyTotalIfNeeded();
        return dailyWithdrawalTotal;
    }

    /**
     * Records a successful cash withdrawal against the daily limit.
     * Input: the amount successfully withdrawn in CAD.
     * Output: no return value; today's total increases.
     * Why: Prevents a customer from avoiding the daily limit by switching accounts.
     */
    public void recordDailyWithdrawal(double amount) {
        resetDailyTotalIfNeeded();
        dailyWithdrawalTotal = Math.round(
                (dailyWithdrawalTotal + amount) * 100.0) / 100.0;
    }

    /**
     * Resets the withdrawal total when the calendar day changes.
     * Input: none.
     * Output: no return value; the total is zeroed on a new day.
     * Why: Makes the daily withdrawal rule apply to each calendar day.
     */
    private void resetDailyTotalIfNeeded() {
        if (!withdrawalDate.equals(LocalDate.now())) {
            withdrawalDate = LocalDate.now();
            dailyWithdrawalTotal = 0.0;
        }
    }

    /**
     * Checks that a PIN has exactly four digits.
     * Input: the PIN to validate.
     * Output: no return value; invalid PINs cause a BankException.
     * Why: Both initial PINs and replacement PINs follow the same simple rule.
     */
    private void validatePin(String value) throws BankException {
        if (value == null || !value.matches("\\d{4}")) {
            throw new BankException("PIN must contain exactly 4 digits.");
        }
    }
}
