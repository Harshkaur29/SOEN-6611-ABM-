/**
 * Purpose: Represents one linked Chequing or Savings account.
 * Project role: Stores an account number, account type, currency, and balance.
 * The backend uses this class to update balances without the GUI changing
 * account data directly.
 */
import java.text.NumberFormat;
import java.util.Locale;
public class Account {

    /** Canadian dollar currency code. */
    public static final String CAD = "CAD";
    /** United States dollar currency code. */
    public static final String USD = "USD";
    /** Euro currency code. */
    public static final String EUR = "EUR";
    /** Currency codes supported by the application. */
    public static final String[] SUPPORTED_CURRENCIES = {CAD, USD, EUR};

    /** Chequing account type. */
    public static final String CHEQUING = "Chequing";
    /** Savings account type. */
    public static final String SAVINGS = "Savings";

    private final long accountNumber;
    private final String accountType;
    private final String currency;
    private double balance;

    /**
     * Creates one bank account in CAD.
     *
     * @param accountNumber account number
     * @param accountType account type
     * @param startingBalance initial balance
     * @throws BankException if an input is invalid
     */
    public Account(long accountNumber, String accountType, double startingBalance)
            throws BankException {
        this(accountNumber, accountType, CAD, startingBalance);
    }

    /**
     * Creates one bank account in the given currency.
     *
     * @param accountNumber account number
     * @param accountType account type
     * @param currency account currency
     * @param startingBalance initial balance
     * @throws BankException if an input is invalid
     */
    public Account(long accountNumber, String accountType, String currency,
                  double startingBalance) throws BankException {
        if (accountNumber <= 0) {
            throw new BankException("Account number must be positive.");
        }
        if (!CHEQUING.equals(accountType) && !SAVINGS.equals(accountType)) {
            throw new BankException("Account type must be Chequing or Savings.");
        }
        this.currency = normalizeCurrency(currency);
        if (!Double.isFinite(startingBalance) || startingBalance < 0) {
            throw new BankException("Starting balance must be a valid non-negative amount.");
        }

        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = roundToCents(startingBalance);
    }

    /**
     * Adds money to this account.
     *
     * @param amount amount in the account's currency
     * @throws BankException if the amount is invalid
     */
    public void deposit(double amount) throws BankException {
        validatePositiveAmount(amount);
        balance = roundToCents(balance + amount);
    }

    /**
     * Removes money when the account has enough funds.
     *
     * @param amount amount in the account's currency
     * @throws BankException if the amount is invalid or funds are insufficient
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
     *
     * @return this account's number
     */
    public long getAccountNumber() {
        return accountNumber;
    }

    /**
     * Returns the account type.
     *
     * @return {@link #CHEQUING} or {@link #SAVINGS}
     */
    public String getAccountType() {
        return accountType;
    }

    /**
     * Returns the account currency.
     *
     * @return the supported currency code
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Returns the current balance.
     *
     * @return the balance rounded to cents
     */
    public double getBalance() {
        return balance;
    }

    /**
     * Normalizes a currency code and validates that it is supported.
     *
     * @param currency entered currency code
     * @return the canonical supported currency code
     * @throws BankException if the currency is missing or unsupported
     */
    public static String normalizeCurrency(String currency) throws BankException {
        if (currency == null) {
            throw new BankException("Currency is required.");
        }

        String normalized = currency.trim().toUpperCase();
        if ("EURO".equals(normalized) || "EUR".equals(normalized) || "€".equals(normalized)) {
            return EUR;
        }
        if ("USD".equals(normalized) || "US$".equals(normalized) || "$".equals(normalized)) {
            return USD;
        }
        if ("CAD".equals(normalized) || "C$".equals(normalized) || "CAN".equals(normalized)) {
            return CAD;
        }
        if ("CAD".equals(normalized) || "USD".equals(normalized) || "EUR".equals(normalized)) {
            return normalized;
        }
        throw new BankException("Currency must be one of: CAD, USD, EUR.");
    }

    /**
     * Checks that an operation uses a finite positive amount with at most two decimals.
     *
     * @param amount amount supplied to a deposit or withdrawal
     * @throws BankException if the amount is invalid
     */
    private void validatePositiveAmount(double amount) throws BankException {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new BankException("Amount must be a valid value greater than 0.00.");
        }
        double cents = amount * 100.0;
        if (Math.abs(cents - Math.round(cents)) > 0.00001) {
            throw new BankException("Amount cannot contain fractions of a cent.");
        }
    }

    /**
     * Rounds a value to two decimal places.
     *
     * @param amount monetary amount
     * @return amount rounded to cents
     */
    private double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
