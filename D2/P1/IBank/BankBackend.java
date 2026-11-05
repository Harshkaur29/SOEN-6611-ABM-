/**
 * Purpose: Simulates the central iBank banking backend.
 * Project role: Verifies card/PIN credentials, enforces cash-withdrawal limits,
 * updates account balances, manages note cassettes, and creates dated
 * transaction confirmations. All banking rules live here, not in the GUI.
 */
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BankBackend {

    /** Maximum total cash withdrawal allowed per currency per day. */
    public static final double DAILY_WITHDRAWAL_LIMIT = 1000.00;

    private static final int[] DENOMINATIONS = {100, 50, 20, 10, 5};
    private static final String TECHNICIAN_PIN = "2468";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<Integer, BankCustomer> customers = new HashMap<>();
    private final Map<Integer, Integer> failedAttempts = new HashMap<>();
    private final Set<Integer> lockedCards = new HashSet<>();
    private final Map<String, int[]> cashNotesByCurrency = new HashMap<>();
    private int nextReceiptNumber = 1001;
    private boolean hardwareError;
    private boolean receiptRollAvailable = true;

    /**
     * Creates the backend and loads representative customer and cash data.
     */
    public BankBackend() {
        for (String currency : Account.SUPPORTED_CURRENCIES) {
            cashNotesByCurrency.put(currency, new int[] {20, 30, 100, 100, 100});
        }

        try {
            customers.put(1001, new BankCustomer(
                    1001, "Customer One", "1234", 1500.00, 500.00));
            customers.put(1002, new BankCustomer(
                    1002, "Customer Two", "4321", 800.00, 1200.00));
        } catch (BankException exception) {
            throw new IllegalStateException(
                    "Representative customer data could not be loaded.", exception);
        }
    }

    /**
     * Authenticates a customer and applies the session lockout policy.
     *
     * @param cardNumber card number supplied by the customer
     * @param pin PIN supplied by the customer
     * @return the authenticated customer
     * @throws BankException if credentials are invalid or the card is locked
     */
    public BankCustomer login(int cardNumber, String pin) throws BankException {
        if (lockedCards.contains(cardNumber)) {
            throw new BankException(
                    "This card is locked after three failed attempts for this application session.");
        }

        BankCustomer customer = customers.get(cardNumber);
        if (customer == null || !customer.verifyPin(pin)) {
            int attempts = failedAttempts.getOrDefault(cardNumber, 0) + 1;
            failedAttempts.put(cardNumber, attempts);

            if (attempts >= 3) {
                lockedCards.add(cardNumber);
                throw new BankException(
                        "Three consecutive failures. This card is locked for this application session.");
            }
            throw new BankException(
                    "Card number or PIN is incorrect. Failed attempt "
                            + attempts + " of 3.");
        }

        failedAttempts.remove(cardNumber);
        return customer;
    }

    /**
     * Authenticates an ABM technician.
     *
     * @param enteredPin technician code
     * @throws BankException if the code is incorrect
     */
    public void authenticateTechnician(String enteredPin) throws BankException {
        if (enteredPin == null || !TECHNICIAN_PIN.equals(enteredPin)) {
            throw new BankException("Technician code is incorrect.");
        }
    }

    /** @return a copy of the currencies supported by the ABM. */
    public String[] getSupportedCurrencies() {
        return Account.SUPPORTED_CURRENCIES.clone();
    }

    /**
     * Withdraws CAD cash from a customer's account.
     *
     * @param customer authenticated customer
     * @param accountType source account type
     * @param amount whole-dollar withdrawal amount
     * @return transaction receipt
     * @throws BankException if the transaction violates an ABM or banking rule
     */
    public String withdrawCash(BankCustomer customer, String accountType,
                               double amount) throws BankException {
        return withdrawCash(customer, accountType, Account.CAD, amount);
    }

    /**
     * Withdraws cash in a selected currency from a customer's account.
     *
     * @param customer authenticated customer
     * @param accountType source account type
     * @param currency withdrawal currency
     * @param amount whole-dollar withdrawal amount
     * @return transaction receipt
     * @throws BankException if the transaction violates an ABM or banking rule
     */
    public String withdrawCash(BankCustomer customer, String accountType,
                               String currency, double amount) throws BankException {
        ensureCashMachineReady();
        String normalizedCurrency = Account.normalizeCurrency(currency);
        validateCashWithdrawalAmount(amount);

        if (customer.getDailyWithdrawalTotal(normalizedCurrency) + amount > DAILY_WITHDRAWAL_LIMIT) {
            throw new BankException(
                    "Daily cash withdrawal limit is $1,000.00 " + normalizedCurrency + ". "
                            + "Your remaining limit is "
                            + money(DAILY_WITHDRAWAL_LIMIT - customer.getDailyWithdrawalTotal(normalizedCurrency),
                            normalizedCurrency) + ".");
        }

        Account account = customer.getAccount(accountType, normalizedCurrency);
        if (amount > account.getBalance()) {
            throw new BankException("Insufficient funds in the selected account.");
        }

        int[] notesToDispense = new int[DENOMINATIONS.length];
        int[] availableNotes = cashNotesByCurrency.get(normalizedCurrency);
        int wholeDollars = (int) Math.round(amount);

        if (!findNotes(0, wholeDollars, availableNotes, notesToDispense)) {
            throw new BankException(
                    "The ABM cannot dispense that amount with its current notes in "
                            + normalizedCurrency + ". Try another amount.");
        }

        account.withdraw(amount);
        for (int i = 0; i < availableNotes.length; i++) {
            availableNotes[i] -= notesToDispense[i];
        }
        customer.recordDailyWithdrawal(normalizedCurrency, amount);

        return createReceipt(
                "Cash Withdrawal",
                customer,
                "Currency: " + normalizedCurrency + "\n"
                        + "Selected account: " + accountType + "\n"
                        + "Amount: " + money(amount, normalizedCurrency) + "\n"
                        + "Notes dispensed: " + describeNotes(notesToDispense));
    }

    /**
     * Deposits CAD bills into a customer's account.
     *
     * @param customer authenticated customer
     * @param accountType destination account type
     * @param billCounts note counts in supported-denomination order
     * @return transaction receipt
     * @throws BankException if the deposit is invalid or the ABM is unavailable
     */
    public String depositCash(BankCustomer customer, String accountType,
                              int[] billCounts) throws BankException {
        return depositCash(customer, accountType, Account.CAD, billCounts);
    }

    /**
     * Deposits bills in a selected currency into a customer's account.
     *
     * @param customer authenticated customer
     * @param accountType destination account type
     * @param currency deposit currency
     * @param billCounts note counts in supported-denomination order
     * @return transaction receipt
     * @throws BankException if the deposit is invalid or the ABM is unavailable
     */
    public String depositCash(BankCustomer customer, String accountType,
                              String currency, int[] billCounts) throws BankException {
        ensureCashMachineReady();
        String normalizedCurrency = Account.normalizeCurrency(currency);
        double total = countDepositedBills(billCounts);
        Account account = customer.getAccount(accountType, normalizedCurrency);

        account.deposit(total);
        int[] availableNotes = cashNotesByCurrency.get(normalizedCurrency);
        for (int i = 0; i < availableNotes.length; i++) {
            availableNotes[i] += billCounts[i];
        }

        return createReceipt(
                "Cash Deposit",
                customer,
                "Currency: " + normalizedCurrency + "\n"
                        + "Selected account: " + accountType + "\n"
                        + "Amount accepted: " + money(total, normalizedCurrency) + "\n"
                        + "Bills accepted: " + describeNotes(billCounts));
    }

    /**
     * Creates a receipt containing all supported-currency account balances.
     *
     * @param customer authenticated customer
     * @return balance inquiry receipt
     */
    public String balanceInquiry(BankCustomer customer) {
        StringBuilder detail = new StringBuilder();
        for (String currency : customer.getSupportedCurrencies()) {
            try {
                detail.append(currency).append(" Chequing: ")
                        .append(money(customer.getAccount(Account.CHEQUING, currency).getBalance(), currency))
                        .append("\n");
                detail.append(currency).append(" Savings: ")
                        .append(money(customer.getAccount(Account.SAVINGS, currency).getBalance(), currency))
                        .append("\n");
            } catch (BankException exception) {
                detail.append(currency).append(" account data unavailable: ")
                        .append(exception.getMessage()).append("\n");
            }
        }
        return createReceipt("Balance Inquiry", customer, detail.toString(), false);
    }

    /**
     * Transfers CAD funds between two customer accounts.
     *
     * @param customer authenticated customer
     * @param sourceType source account type
     * @param targetType destination account type
     * @param amount amount to transfer
     * @return transaction receipt
     * @throws BankException if the transfer is invalid
     */
    public String transferFunds(BankCustomer customer, String sourceType,
                                String targetType, double amount)
            throws BankException {
        return transferFunds(customer, sourceType, targetType, Account.CAD, amount);
    }

    /**
     * Transfers funds in a selected currency between customer accounts.
     *
     * @param customer authenticated customer
     * @param sourceType source account type
     * @param targetType destination account type
     * @param currency transfer currency
     * @param amount amount to transfer
     * @return transaction receipt
     * @throws BankException if the transfer is invalid
     */
    public String transferFunds(BankCustomer customer, String sourceType,
                                String targetType, String currency, double amount)
            throws BankException {
        String normalizedCurrency = Account.normalizeCurrency(currency);
        if (amount <= 0) {
            throw new BankException("Transfer amount must be greater than 0.00.");
        }

        Account source = customer.getAccount(sourceType, normalizedCurrency);
        Account target = customer.getAccount(targetType, normalizedCurrency);
        if (source.getAccountNumber() == target.getAccountNumber()) {
            throw new BankException("Choose different source and destination accounts.");
        }

        source.withdraw(amount);
        target.deposit(amount);

        return createReceipt(
                "Fund Transfer",
                customer,
                "Currency: " + normalizedCurrency + "\n"
                        + "From: " + sourceType + "\n"
                        + "To: " + targetType + "\n"
                        + "Amount transferred: " + money(amount, normalizedCurrency));
    }

    /**
     * Changes a customer's PIN and creates a confirmation receipt.
     *
     * @param customer authenticated customer
     * @param oldPin current PIN
     * @param newPin replacement PIN
     * @return confirmation receipt
     * @throws BankException if either PIN is invalid
     */
    public String changePin(BankCustomer customer, String oldPin, String newPin)
            throws BankException {
        customer.changePin(oldPin, newPin);
        return "PIN changed successfully.";
    }

    /** @return a copy of the supported cash denominations. */
    public int[] getSupportedDenominations() {
        return Arrays.copyOf(DENOMINATIONS, DENOMINATIONS.length);
    }

    /** @return a report of ABM hardware, receipt, and cash inventory status. */
    public String runDiagnostics() {
        StringBuilder report = new StringBuilder();
        report.append("iBank ABM Diagnostics\n");
        report.append("Hardware status: ")
                .append(hardwareError ? "ERROR ACTIVE" : "Ready")
                .append("\n");
        report.append("Paper receipt roll: ")
                .append(receiptRollAvailable ? "Available" : "Empty")
                .append("\n");

        for (String currency : Account.SUPPORTED_CURRENCIES) {
            int[] notes = cashNotesByCurrency.get(currency);
            report.append(currency).append(" cash inventory: ")
                    .append(money(getCashInventoryValue(currency), currency)).append("\n");
            for (int i = 0; i < DENOMINATIONS.length; i++) {
                report.append(currency).append(" $").append(DENOMINATIONS[i]).append(" notes: ")
                        .append(notes[i]).append("\n");
            }
        }
        return report.toString();
    }

    /**
     * Adds CAD notes to the cash inventory.
     *
     * @param denomination note denomination
     * @param quantity number of notes to add
     * @return replenishment status message
     * @throws BankException if the denomination or quantity is invalid
     */
    public String replenishCashNotes(int denomination, int quantity)
            throws BankException {
        return replenishCashNotes(Account.CAD, denomination, quantity);
    }

    /**
     * Adds notes in a selected currency to the cash inventory.
     *
     * @param currency inventory currency
     * @param denomination note denomination
     * @param quantity number of notes to add
     * @return replenishment status message
     * @throws BankException if an input is invalid
     */
    public String replenishCashNotes(String currency, int denomination, int quantity)
            throws BankException {
        String normalizedCurrency = Account.normalizeCurrency(currency);
        int index = findDenominationIndex(denomination);
        if (index == -1) {
            throw new BankException("Unsupported note denomination.");
        }
        if (quantity <= 0) {
            throw new BankException("Replenishment quantity must be positive.");
        }

        int[] notes = cashNotesByCurrency.get(normalizedCurrency);
        notes[index] += quantity;
        return "Replenished " + quantity + " x $" + denomination + " notes in "
                + normalizedCurrency + ".\n"
                + "Cash inventory is now " + money(getCashInventoryValue(normalizedCurrency), normalizedCurrency)
                + ".";
    }

    /** @return status message confirming that the receipt roll is available. */
    public String replaceReceiptRoll() {
        receiptRollAvailable = true;
        return "Paper receipt roll replaced. Receipt roll is available.";
    }

    /** Activates a simulated hardware error for demonstrations and tests. */
    public void simulateHardwareErrorForDemo() {
        hardwareError = true;
    }

    /** Marks the simulated receipt roll as empty for demonstrations and tests. */
    public void simulateEmptyReceiptRollForDemo() {
        receiptRollAvailable = false;
    }

    /** @return status message after clearing simulated hardware errors. */
    public String clearHardwareErrors() {
        hardwareError = false;
        return "Hardware errors cleared. Cash operations are available again.";
    }

    private String createReceipt(String type, BankCustomer customer, String details) {
        return createReceipt(type, customer, details, true);
    }

    private String createReceipt(String type, BankCustomer customer, String details,
                                 boolean includeAccountBalances) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("iBank Receipt #").append(nextReceiptNumber++).append("\n");
        receipt.append("Date/Time: ")
                .append(LocalDateTime.now().format(DATE_FORMAT)).append("\n");
        receipt.append("Transaction: ").append(type).append("\n");
        receipt.append("Customer: ").append(customer.getCustomerName()).append("\n");
        receipt.append("Details: ").append(details).append("\n");

        if (includeAccountBalances) {
            for (String currency : customer.getSupportedCurrencies()) {
                try {
                    Account chequing = customer.getAccount(Account.CHEQUING, currency);
                    Account savings = customer.getAccount(Account.SAVINGS, currency);
                    receipt.append(currency).append(" Chequing account ")
                            .append(chequing.getAccountNumber())
                            .append(" balance: ")
                            .append(money(chequing.getBalance(), currency))
                            .append("\n");
                    receipt.append(currency).append(" Savings account ")
                            .append(savings.getAccountNumber())
                            .append(" balance: ")
                            .append(money(savings.getBalance(), currency))
                            .append("\n");
                } catch (BankException exception) {
                    receipt.append(currency).append(" account data unavailable: ")
                            .append(exception.getMessage()).append("\n");
                }
            }
        }
        return receipt.toString();
    }

    private void validateCashWithdrawalAmount(double amount) throws BankException {
        if (amount <= 0) {
            throw new BankException("Withdrawal amount must be greater than 0.00.");
        }
        if (amount != Math.floor(amount) || ((int) Math.round(amount)) % 5 != 0) {
            throw new BankException(
                    "Cash withdrawals must be whole-dollar amounts divisible by $5.00.");
        }
    }

    private boolean findNotes(int index, int amount, int[] available, int[] used) {
        if (amount == 0) {
            return true;
        }
        if (index == DENOMINATIONS.length) {
            return false;
        }

        int maximum = Math.min(available[index], amount / DENOMINATIONS[index]);
        for (int count = maximum; count >= 0; count--) {
            used[index] = count;
            if (findNotes(index + 1,
                    amount - count * DENOMINATIONS[index], available, used)) {
                return true;
            }
        }
        used[index] = 0;
        return false;
    }

    private double countDepositedBills(int[] billCounts) throws BankException {
        if (billCounts == null || billCounts.length != DENOMINATIONS.length) {
            throw new BankException("Deposit bill counts are incomplete.");
        }

        int total = 0;
        boolean hasBills = false;
        for (int i = 0; i < billCounts.length; i++) {
            if (billCounts[i] < 0) {
                throw new BankException("Bill counts cannot be negative.");
            }
            if (billCounts[i] > 0) {
                hasBills = true;
                total += billCounts[i] * DENOMINATIONS[i];
            }
        }
        if (!hasBills) {
            throw new BankException("Enter at least one bill to make a deposit.");
        }
        return total;
    }

    private int findDenominationIndex(int denomination) {
        for (int i = 0; i < DENOMINATIONS.length; i++) {
            if (DENOMINATIONS[i] == denomination) {
                return i;
            }
        }
        return -1;
    }

    private String describeNotes(int[] counts) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                result.append("$").append(DENOMINATIONS[i]).append(" x ").append(counts[i]);
            }
        }
        return result.toString();
    }

    private double getCashInventoryValue(String currency) {
        String normalizedCurrency;
        try {
            normalizedCurrency = Account.normalizeCurrency(currency);
        } catch (BankException exception) {
            normalizedCurrency = Account.CAD;
        }

        int[] notes = cashNotesByCurrency.get(normalizedCurrency);
        double total = 0;
        for (int i = 0; i < notes.length; i++) {
            total += (double) DENOMINATIONS[i] * notes[i];
        }
        return total;
    }

    private void ensureCashMachineReady() throws BankException {
        if (hardwareError) {
            throw new BankException(
                    "The ABM has a hardware error. Please contact an ABM Technician.");
        }
    }

    private String money(double amount) {
        return money(amount, Account.CAD);
    }

    private String money(double amount, String currency) {
        String normalizedCurrency;
        try {
            normalizedCurrency = Account.normalizeCurrency(currency);
        } catch (BankException exception) {
            normalizedCurrency = Account.CAD;
        }

        String symbol;
        switch (normalizedCurrency) {
            case Account.USD:
                symbol = "$";
                break;
            case Account.EUR:
                symbol = "\u20AC";
                break;
            default:
                symbol = "$";
                break;
        }
        return String.format(Locale.CANADA, "%s%.2f %s", symbol, amount, normalizedCurrency);
    }
}
