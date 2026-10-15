/*
 * Purpose: Represents one linked Chequing or Savings account.
 * Project role: Stores an account number, account type, and CAD balance.
 * The backend uses this class to update balances without the GUI changing
 * account data directly.
 */
public class Account {

    public static final String CHEQUING = "Chequing";
    public static final String SAVINGS = "Savings";

    private final long accountNumber;
    private final String accountType;
    private double balance;

    /**
     * Creates one bank account.
     * Input: account number, account type, and starting CAD balance.
     * Output: a new Account object.
     * Why: Keeps each linked account's identity and balance together.
     */
    public Account(long accountNumber, String accountType, double startingBalance)
            throws BankException {
        if (accountNumber <= 0) {
            throw new BankException("Account number must be positive.");
        }
        if (!CHEQUING.equals(accountType) && !SAVINGS.equals(accountType)) {
            throw new BankException("Account type must be Chequing or Savings.");
        }
        if (!Double.isFinite(startingBalance) || startingBalance < 0) {
            throw new BankException("Starting balance must be a valid non-negative amount.");
        }

        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = roundToCents(startingBalance);
    }

    /**
     * Adds money to this account.
     * Input: amount in CAD.
     * Output: no return value; the balance is updated.
     * Why: Used for cash deposits and incoming transfers.
     */
    public void deposit(double amount) throws BankException {
        validatePositiveAmount(amount);
        balance = roundToCents(balance + amount);
    }

    /**
     * Removes money when the account has enough funds.
     * Input: amount in CAD.
     * Output: no return value; the balance is updated.
     * Why: Used for withdrawals and outgoing transfers.
     */
    public void withdraw(double amount) throws BankException {
        validatePositiveAmount(amount);
        if (amount > balance) {
            throw new BankException("Insufficient funds in the selected account.");
        }
        balance = roundToCents(balance - amount);
    }

    /**
     * Returns the account number.
     * Input: none.
     * Output: this account's number.
     * Why: Receipts and balance inquiries identify both linked accounts.
     */
    public long getAccountNumber() {
        return accountNumber;
    }

    /**
     * Returns the account type.
     * Input: none.
     * Output: Chequing or Savings.
     * Why: The backend and GUI need to identify the selected account.
     */
    public String getAccountType() {
        return accountType;
    }

    /**
     * Returns the current CAD balance.
     * Input: none.
     * Output: the balance rounded to cents.
     * Why: Used in balance inquiries, receipts, and tests.
     */
    public double getBalance() {
        return balance;
    }

    /**
     * Checks that an operation uses a finite positive amount with at most two decimals.
     * Input: the amount supplied to a deposit or withdrawal.
     * Output: no return value; invalid amounts cause a BankException.
     * Why: Prevents values such as NaN or fractions of a cent from corrupting a balance.
     */
    private void validatePositiveAmount(double amount) throws BankException {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new BankException("Amount must be a valid value greater than $0.00.");
        }
        double cents = amount * 100.0;
        if (Math.abs(cents - Math.round(cents)) > 0.00001) {
            throw new BankException("Amount cannot contain fractions of a cent.");
        }
    }

    /**
     * Rounds a value to two decimal places.
     * Input: a monetary amount.
     * Output: the amount rounded to cents.
     * Why: Keeps displayed balances readable in this small simulation.
     */
    private double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
