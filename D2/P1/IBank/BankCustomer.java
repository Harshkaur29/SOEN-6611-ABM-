/**
 * Purpose: Represents a retail customer and their linked accounts.
 * Project role: The backend keeps customer records here, verifies the PIN,
 * changes the PIN, and tracks daily withdrawals for each currency.
 */
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BankCustomer {

    private final int cardNumber;
    private final String customerName;
    private String pin;
    private final Map<String, Account> accounts = new HashMap<>();
    private final Account chequingAccount;
    private final Account savingsAccount;
    private final Map<String, Double> dailyWithdrawalTotals = new HashMap<>();
    private final Map<String, LocalDate> withdrawalDates = new HashMap<>();

    /**
     * Creates a customer with CAD chequing and savings accounts and matching
     * accounts for each supported currency.
     *
     * @param cardNumber customer's card number
     * @param customerName customer's display name
     * @param pin customer's four-digit PIN
     * @param chequingBalance initial chequing balance
     * @param savingsBalance initial savings balance
     * @throws BankException if the card, PIN, or balance is invalid
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
        this.chequingAccount = createAccount(Account.CHEQUING, Account.CAD, chequingBalance);
        this.savingsAccount = createAccount(Account.SAVINGS, Account.CAD, savingsBalance);

        for (String currency : Account.SUPPORTED_CURRENCIES) {
            if (!Account.CAD.equals(currency)) {
                createAccount(Account.CHEQUING, currency, chequingBalance);
                createAccount(Account.SAVINGS, currency, savingsBalance);
            }
            dailyWithdrawalTotals.put(currency, 0.0);
            withdrawalDates.put(currency, LocalDate.now().minusDays(1));
        }
    }

    /**
     * Checks whether a supplied PIN matches the customer's PIN.
     *
     * @param enteredPin PIN supplied for authentication
     * @return {@code true} when the PIN matches; otherwise {@code false}
     */
    public boolean verifyPin(String enteredPin) {
        return enteredPin != null && pin.equals(enteredPin);
    }

    /**
     * Replaces the customer's PIN after validating the current PIN and format.
     *
     * @param oldPin current PIN
     * @param newPin replacement four-digit PIN
     * @throws BankException if the current PIN or replacement PIN is invalid
     */
    public void changePin(String oldPin, String newPin) throws BankException {
        if (!verifyPin(oldPin)) {
            throw new BankException("The current PIN is incorrect.");
        }
        validatePin(newPin);
        pin = newPin;
    }

    /**
     * Gets an account in CAD.
     *
     * @param accountType account type to retrieve
     * @return the requested account
     * @throws BankException if the account type is unsupported
     */
    public Account getAccount(String accountType) throws BankException {
        return getAccount(accountType, Account.CAD);
    }

    /**
     * Gets an account for the requested type and currency.
     *
     * @param accountType account type to retrieve
     * @param currency account currency
     * @return the requested account
     * @throws BankException if the currency or account type is unsupported
     */
    public Account getAccount(String accountType, String currency) throws BankException {
        String normalizedCurrency = Account.normalizeCurrency(currency);
        String key = buildKey(accountType, normalizedCurrency);
        Account account = accounts.get(key);
        if (account == null) {
            throw new BankException("Select Chequing or Savings for a supported currency.");
        }
        return account;
    }

    /** @return the customer's card number. */
    public int getCardNumber() {
        return cardNumber;
    }

    /** @return the customer's display name. */
    public String getCustomerName() {
        return customerName;
    }

    /** @return the customer's CAD chequing account. */
    public Account getChequingAccount() {
        return chequingAccount;
    }

    /** @return the customer's CAD savings account. */
    public Account getSavingsAccount() {
        return savingsAccount;
    }

    /**
     * Returns a copy of the currencies supported by the customer's accounts.
     *
     * @return supported currency codes
     */
    public String[] getSupportedCurrencies() {
        return Account.SUPPORTED_CURRENCIES.clone();
    }

    /** @return today's CAD cash-withdrawal total. */
    public double getDailyWithdrawalTotal() {
        return getDailyWithdrawalTotal(Account.CAD);
    }

    /**
     * Returns today's cash-withdrawal total for a currency.
     *
     * @param currency currency to inspect
     * @return today's withdrawal total, or zero for an invalid currency
     */
    public double getDailyWithdrawalTotal(String currency) {
        String normalizedCurrency;
        try {
            normalizedCurrency = Account.normalizeCurrency(currency);
        } catch (BankException exception) {
            return 0.0;
        }
        resetDailyTotalIfNeeded(normalizedCurrency);
        return dailyWithdrawalTotals.getOrDefault(normalizedCurrency, 0.0);
    }

    /**
     * Adds a CAD withdrawal to today's withdrawal total.
     *
     * @param amount withdrawal amount
     */
    public void recordDailyWithdrawal(double amount) {
        recordDailyWithdrawal(Account.CAD, amount);
    }

    /**
     * Adds a withdrawal to today's total for a currency.
     *
     * @param currency withdrawal currency
     * @param amount withdrawal amount
     */
    public void recordDailyWithdrawal(String currency, double amount) {
        String normalizedCurrency;
        try {
            normalizedCurrency = Account.normalizeCurrency(currency);
        } catch (BankException exception) {
            return;
        }
        resetDailyTotalIfNeeded(normalizedCurrency);
        double current = dailyWithdrawalTotals.getOrDefault(normalizedCurrency, 0.0);
        dailyWithdrawalTotals.put(normalizedCurrency, roundToCents(current + amount));
    }

    private void validatePin(String value) throws BankException {
        if (value == null || !value.matches("\\d{4}")) {
            throw new BankException("PIN must contain exactly 4 digits.");
        }
    }

    private Account createAccount(String accountType, String currency, double balance)
            throws BankException {
        String normalizedCurrency = Account.normalizeCurrency(currency);
        String key = buildKey(accountType, normalizedCurrency);
        long accountNumber = (long) cardNumber * 100 + (accounts.size() + 1);
        Account account = new Account(accountNumber, accountType, normalizedCurrency, balance);
        accounts.put(key, account);
        return account;
    }

    private String buildKey(String accountType, String currency) {
        return accountType.toLowerCase(Locale.ROOT) + ":" + currency.toUpperCase(Locale.ROOT);
    }

    private void resetDailyTotalIfNeeded(String currency) {
        LocalDate today = LocalDate.now();
        LocalDate lastReset = withdrawalDates.getOrDefault(currency, LocalDate.now().minusDays(1));
        if (!today.equals(lastReset)) {
            withdrawalDates.put(currency, today);
            dailyWithdrawalTotals.put(currency, 0.0);
        }
    }

    private double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
