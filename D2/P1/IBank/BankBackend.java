/*
 * Purpose: Simulates the central iBank banking backend.
 * Project role: Verifies card/PIN credentials, enforces the cash-withdrawal
 * limit, updates account balances, manages note cassettes, and creates dated
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

    public static final double DAILY_WITHDRAWAL_LIMIT = 1000.00;

    // Descending order lets the cash dispenser try larger bills first.
    private static final int[] DENOMINATIONS = {100, 50, 20, 10, 5};
    private static final String TECHNICIAN_PIN = "2468";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<Integer, BankCustomer> customers = new HashMap<>();
    private final Map<Integer, Integer> failedAttempts = new HashMap<>();
    private final Set<Integer> lockedCards = new HashSet<>();

    // Cassette counts use the same order as DENOMINATIONS.
    private final int[] cashNotes = {20, 30, 100, 100, 100};
    private int nextReceiptNumber = 1001;
    private boolean hardwareError;
    private boolean receiptRollAvailable = true;

    /**
     * Creates the bank backend and loads representative test customers.
     * Input: none.
     * Output: a ready BankBackend object.
     * Why: Provides master customer records without requiring a real bank or database.
     */
    public BankBackend() {
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
     * Validates a customer's card number and PIN, then returns the customer.
     * Input: card number and four-digit PIN.
     * Output: the authenticated BankCustomer, or a helpful BankException.
     * Why: Implements Authenticate User and locks a card after three failures.
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
     * Checks the technician access code.
     * Input: the code entered by the ABM Technician.
     * Output: no return value; an exception is raised when the code is wrong.
     * Why: Separates technician maintenance actions from customer transactions.
     */
    public void authenticateTechnician(String enteredPin) throws BankException {
        if (enteredPin == null || !TECHNICIAN_PIN.equals(enteredPin)) {
            throw new BankException("Technician code is incorrect.");
        }
    }

    /**
     * Withdraws cash from the chosen linked account and note cassettes.
     * Input: customer, Chequing/Savings account type, and CAD amount.
     * Output: a dated receipt with the notes dispensed and updated balances.
     * Why: Implements cash withdrawal, including balance, daily-limit, and cash checks.
     */
    public String withdrawCash(BankCustomer customer, String accountType,
                               double amount) throws BankException {
        ensureCashMachineReady();
        validateCashWithdrawalAmount(amount);

        if (customer.getDailyWithdrawalTotal() + amount > DAILY_WITHDRAWAL_LIMIT) {
            throw new BankException(
                    "Daily cash withdrawal limit is $1,000.00 CAD. "
                            + "Your remaining limit is "
                            + money(DAILY_WITHDRAWAL_LIMIT
                            - customer.getDailyWithdrawalTotal()) + ".");
        }

        Account account = customer.getAccount(accountType);
        if (amount > account.getBalance()) {
            throw new BankException("Insufficient funds in the selected account.");
        }
        int[] notesToDispense = new int[DENOMINATIONS.length];
        int wholeDollars = (int) Math.round(amount);

        if (!findNotes(0, wholeDollars, cashNotes, notesToDispense)) {
            throw new BankException(
                    "The ABM cannot dispense that amount with its current notes. "
                            + "Try another amount.");
        }

        // Only update the account and cassettes after all validations pass.
        account.withdraw(amount);
        for (int i = 0; i < cashNotes.length; i++) {
            cashNotes[i] -= notesToDispense[i];
        }
        customer.recordDailyWithdrawal(amount);

        return createReceipt(
                "Cash Withdrawal",
                customer,
                "Selected account: " + accountType + "\n"
                        + "Amount: " + money(amount) + "\n"
                        + "Notes dispensed: " + describeNotes(notesToDispense));
    }

    /**
     * Counts deposited CAD bills, credits the selected account, and adds bills to inventory.
     * Input: customer, account type, and counts ordered as $100, $50, $20, $10, $5.
     * Output: a dated receipt showing the accepted bills and updated balances.
     * Why: Simulates accepting and counting paper currency at the ABM.
     */
    public String depositCash(BankCustomer customer, String accountType,
                              int[] billCounts) throws BankException {
        ensureCashMachineReady();
        double total = countDepositedBills(billCounts);
        Account account = customer.getAccount(accountType);

        account.deposit(total);
        for (int i = 0; i < cashNotes.length; i++) {
            cashNotes[i] += billCounts[i];
        }

        return createReceipt(
                "Cash Deposit",
                customer,
                "Selected account: " + accountType + "\n"
                        + "Amount accepted: " + money(total) + "\n"
                        + "Bills accepted: " + describeNotes(billCounts));
    }

    /**
     * Shows the current balance of both linked accounts.
     * Input: the authenticated customer.
     * Output: a dated balance inquiry showing Chequing and Savings balances.
     * Why: Implements Balance Inquiry and makes both linked balances visible together.
     */
    public String balanceInquiry(BankCustomer customer) {
        return createReceipt(
                "Balance Inquiry",
                customer,
                "Balances are shown for both linked accounts.");
    }

    /**
     * Moves money between the same customer's Chequing and Savings accounts.
     * Input: customer, source account type, destination account type, and amount.
     * Output: a dated transfer receipt with both updated balances.
     * Why: Implements transfers only between the customer's own linked accounts.
     */
    public String transferFunds(BankCustomer customer, String sourceType,
                                String targetType, double amount)
            throws BankException {
        if (amount <= 0) {
            throw new BankException("Transfer amount must be greater than $0.00.");
        }

        Account source = customer.getAccount(sourceType);
        Account target = customer.getAccount(targetType);

        if (source.getAccountNumber() == target.getAccountNumber()) {
            throw new BankException(
                    "Choose different source and destination accounts.");
        }

        // The source is checked before the target is credited.
        source.withdraw(amount);
        target.deposit(amount);

        return createReceipt(
                "Fund Transfer",
                customer,
                "From: " + sourceType + "\n"
                        + "To: " + targetType + "\n"
                        + "Amount transferred: " + money(amount));
    }

    /**
     * Changes the PIN after the existing PIN and new PIN format are checked.
     * Input: customer, existing PIN, and replacement PIN.
     * Output: a dated confirmation receipt or a BankException.
     * Why: Implements Change PIN and keeps credential checks in the backend.
     */
    public String changePin(BankCustomer customer, String oldPin, String newPin)
            throws BankException {
        customer.changePin(oldPin, newPin);
        return createReceipt(
                "Change PIN",
                customer,
                "PIN changed successfully.");
    }

    /**
     * Returns a copy of the supported note denominations.
     * Input: none.
     * Output: denominations in descending order: $100, $50, $20, $10, $5.
     * Why: Lets the GUI build bill-count fields without maintaining another list.
     */
    public int[] getSupportedDenominations() {
        return Arrays.copyOf(DENOMINATIONS, DENOMINATIONS.length);
    }

    /**
     * Runs a basic diagnostic report on cash cassettes, receipt roll, and hardware status.
     * Input: none.
     * Output: a readable diagnostic report string.
     * Why: Implements the diagnostic part of Maintain & Replenish.
     */
    public String runDiagnostics() {
        StringBuilder report = new StringBuilder();
        report.append("iBank ABM Diagnostics\n");
        report.append("Hardware status: ")
                .append(hardwareError ? "ERROR ACTIVE" : "Ready")
                .append("\n");
        report.append("Paper receipt roll: ")
                .append(receiptRollAvailable ? "Available" : "Empty")
                .append("\n");
        report.append("Cash inventory: ").append(money(getCashInventoryValue()))
                .append("\n");

        for (int i = 0; i < DENOMINATIONS.length; i++) {
            report.append("$" ).append(DENOMINATIONS[i]).append(" notes: ")
                    .append(cashNotes[i]).append("\n");
        }
        return report.toString();
    }

    /**
     * Adds banknotes to one cash cassette.
     * Input: denomination and positive number of notes to add.
     * Output: a confirmation string, or a BankException for invalid input.
     * Why: Implements cash-vault replenishment by the ABM Technician.
     */
    public String replenishCashNotes(int denomination, int quantity)
            throws BankException {
        int index = findDenominationIndex(denomination);
        if (index == -1) {
            throw new BankException("Unsupported note denomination.");
        }
        if (quantity <= 0) {
            throw new BankException("Replenishment quantity must be positive.");
        }

        cashNotes[index] += quantity;
        return "Replenished " + quantity + " x $" + denomination + " notes.\n"
                + "Cash inventory is now " + money(getCashInventoryValue()) + ".";
    }

    /**
     * Replaces the paper receipt roll and marks it available.
     * Input: none.
     * Output: a maintenance confirmation string.
     * Why: Covers the technician's physical receipt-roll replacement task.
     */
    public String replaceReceiptRoll() {
        receiptRollAvailable = true;
        return "Paper receipt roll replaced. Receipt roll is available.";
    }

    /**
     * Simulates a hardware fault so the technician's clear-error action can be tested.
     * Input: none.
     * Output: no return value; cash withdrawal and deposit are temporarily disabled.
     * Why: This demo-only operation makes the maintenance use case testable without hardware.
     */
    public void simulateHardwareErrorForDemo() {
        hardwareError = true;
    }

    /**
     * Simulates an empty paper receipt roll for testing maintenance.
     * Input: none.
     * Output: no return value; diagnostics reports the roll as empty.
     * Why: Allows receipt-roll replacement to be demonstrated without a physical printer.
     */
    public void simulateEmptyReceiptRollForDemo() {
        receiptRollAvailable = false;
    }

    /**
     * Clears the simulated hardware error state.
     * Input: none.
     * Output: a confirmation string.
     * Why: Implements the clear-hardware-errors action for the ABM Technician.
     */
    public String clearHardwareErrors() {
        hardwareError = false;
        return "Hardware errors cleared. Cash operations are available again.";
    }

    /**
     * Creates a dated receipt with the customer's identity and both current balances.
     * Input: transaction type, customer, and transaction-specific details.
     * Output: a formatted on-screen receipt string.
     * Why: Reuses one receipt format for transactions and balance inquiry.
     */
    private String createReceipt(String type, BankCustomer customer, String details) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("iBank Receipt #").append(nextReceiptNumber++).append("\n");
        receipt.append("Date/Time: ")
                .append(LocalDateTime.now().format(DATE_FORMAT)).append("\n");
        receipt.append("Transaction: ").append(type).append("\n");
        receipt.append("Customer: ").append(customer.getCustomerName()).append("\n");
        receipt.append("Details: ").append(details).append("\n");
        receipt.append("Chequing account ")
                .append(customer.getChequingAccount().getAccountNumber())
                .append(" balance: ")
                .append(money(customer.getChequingAccount().getBalance())).append("\n");
        receipt.append("Savings account ")
                .append(customer.getSavingsAccount().getAccountNumber())
                .append(" balance: ")
                .append(money(customer.getSavingsAccount().getBalance())).append("\n");
        receipt.append("Currency: CAD");
        return receipt.toString();
    }

    /**
     * Checks whether the cash amount can be represented by supported whole-dollar notes.
     * Input: requested cash amount in CAD.
     * Output: no return value; invalid amounts cause a BankException.
     * Why: A physical note dispenser cannot dispense cents or an unsupported denomination.
     */
    private void validateCashWithdrawalAmount(double amount) throws BankException {
        if (amount <= 0) {
            throw new BankException("Withdrawal amount must be greater than $0.00.");
        }
        if (amount != Math.floor(amount) || ((int) Math.round(amount)) % 5 != 0) {
            throw new BankException(
                    "Cash withdrawals must be whole-dollar amounts divisible by $5.00.");
        }
    }

    /**
     * Searches the available cassettes for an exact combination of notes.
     * Input: current denomination index, remaining dollars, available counts, and output counts.
     * Output: true when a valid combination is found; output counts record that combination.
     * Why: Tries larger note counts first, but backs up when a combination will not work.
     */
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

    /**
     * Validates and counts the bills presented for a deposit.
     * Input: bill counts ordered as $100, $50, $20, $10, $5.
     * Output: total deposit value in CAD.
     * Why: Prevents negative bill counts and ensures the deposit contains at least one bill.
     */
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

    /**
     * Finds the array position for a supported denomination.
     * Input: denomination in dollars.
     * Output: matching index, or -1 if it is not supported.
     * Why: Replenishment must update the cassette for the selected note value.
     */
    private int findDenominationIndex(int denomination) {
        for (int i = 0; i < DENOMINATIONS.length; i++) {
            if (DENOMINATIONS[i] == denomination) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Formats an array of note counts for a receipt.
     * Input: counts in denomination order.
     * Output: readable text such as "$50 x 2, $20 x 1".
     * Why: Shows what was accepted or dispensed without repeating formatting logic.
     */
    private String describeNotes(int[] counts) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                result.append("$").append(DENOMINATIONS[i])
                        .append(" x ").append(counts[i]);
            }
        }
        return result.toString();
    }

    /**
     * Calculates the total CAD value of all notes currently in the cassettes.
     * Input: none.
     * Output: total cash value in CAD.
     * Why: Diagnostics reports how much cash the ABM can physically dispense.
     */
    private double getCashInventoryValue() {
        double total = 0;
        for (int i = 0; i < cashNotes.length; i++) {
            total += (double) DENOMINATIONS[i] * cashNotes[i];
        }
        return total;
    }

    /**
     * Stops cash operations when a simulated hardware fault is active.
     * Input: none.
     * Output: no return value; a BankException is raised if the machine is not ready.
     * Why: Makes the technician's clear-error use case affect normal operation.
     */
    private void ensureCashMachineReady() throws BankException {
        if (hardwareError) {
            throw new BankException(
                    "The ABM has a hardware error. Please contact an ABM Technician.");
        }
    }

    /**
     * Formats a CAD amount to two decimal places.
     * Input: amount in CAD.
     * Output: a string such as $250.00.
     * Why: Keeps balances, limits, and receipts consistent.
     */
    private String money(double amount) {
        return String.format(Locale.CANADA, "$%.2f", amount);
    }
}
