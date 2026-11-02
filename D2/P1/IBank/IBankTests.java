/*
 * IBankTests.java
 *
 * Purpose:
 * Tests the iBank application using representative data.
 *
 * Project role:
 * Problem 4(b) - functional, negative, boundary, security,
 * transaction-integrity, currency, and maintenance testing.
 *
 * Framework:
 * Standalone Java test runner. 
 *
 * Important:
 * Run against a fresh BankBackend instance for independent tests.
 */

public class IBankTests {

    private static int passed = 0;
    private static int failed = 0;

    @FunctionalInterface
    interface TestCase {
        void execute() throws Exception;
    }

    @FunctionalInterface
    interface BankAction {
        void execute() throws Exception;
    }

    private static void runTest(String id, String description, TestCase test) {
        try {
            test.execute();
            passed++;
            System.out.println("[PASS] " + id + " - " + description);
        } catch (Throwable error) {
            failed++;
            System.out.println("[FAIL] " + id + " - " + description);
            System.out.println("       Reason: " + error.getMessage());
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void checkMoney(double actual, double expected,
                                   String message) {
        if (Math.abs(actual - expected) > 0.001) {
            throw new AssertionError(
                message + " Expected: " + expected + ", Actual: " + actual
            );
        }
    }

    private static void expectBankException(BankAction action, String message)
            throws Exception {
        try {
            action.execute();
        } catch (BankException expected) {
            return;
        }

        throw new AssertionError(message);
    }

    private static BankCustomer login(BankBackend bank,
                                      int cardNumber,
                                      String pin) throws Exception {
        return bank.login(cardNumber, pin);
    }

    private static double balance(BankCustomer customer,
                                 String accountType,
                                 String currency) throws BankException {
        return customer.getAccount(accountType, currency).getBalance();
    }

    public static void main(String[] args) {

        // =========================================================
        // AUTHENTICATION AND SECURITY
        // =========================================================

        runTest("TC-01", "Valid customer login", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            check(customer != null, "Valid login should return a customer.");
        });

        runTest("TC-02", "Incorrect PIN is rejected", () -> {
            BankBackend bank = new BankBackend();

            expectBankException(
                () -> login(bank, 1001, "9999"),
                "Incorrect PIN should be rejected."
            );
        });

        runTest("TC-03", "Unknown card number is rejected", () -> {
            BankBackend bank = new BankBackend();

            expectBankException(
                () -> login(bank, 9999, "1234"),
                "Unknown card should be rejected."
            );
        });

        runTest("TC-04", "Empty PIN is rejected", () -> {
            BankBackend bank = new BankBackend();

            expectBankException(
                () -> login(bank, 1001, ""),
                "Empty PIN should be rejected."
            );
        });

        runTest("TC-05", "Three incorrect PIN attempts lock the card",
                () -> {
            BankBackend bank = new BankBackend();

            for (int i = 0; i < 3; i++) {
                try {
                    login(bank, 1001, "9999");
                } catch (BankException expected) {
                    // Expected failed attempt.
                }
            }

            expectBankException(
                () -> login(bank, 1001, "1234"),
                "Correct PIN should not bypass the card lockout."
            );
        });

        runTest("TC-06", "One customer's failed attempts do not lock another",
                () -> {
            BankBackend bank = new BankBackend();

            for (int i = 0; i < 3; i++) {
                try {
                    login(bank, 1001, "9999");
                } catch (BankException expected) {
                    // Expected failed attempt.
                }
            }

            BankCustomer otherCustomer = login(bank, 1002, "4321");

            check(otherCustomer != null,
                  "The second customer's valid login should succeed.");
        });

        // =========================================================
        // BALANCE INQUIRY
        // =========================================================

        runTest("TC-07", "Balance inquiry returns a result", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            String result = bank.balanceInquiry(customer);

            check(result != null && !result.trim().isEmpty(),
                  "Balance inquiry should return a result.");
        });

        runTest("TC-08", "Balance inquiry does not change balances", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            double beforeChequing =
                balance(customer, Account.CHEQUING, Account.CAD);
            double beforeSavings =
                balance(customer, Account.SAVINGS, Account.CAD);

            bank.balanceInquiry(customer);

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                beforeChequing,
                "Chequing balance changed."
            );

            checkMoney(
                balance(customer, Account.SAVINGS, Account.CAD),
                beforeSavings,
                "Savings balance changed."
            );
        });

        // =========================================================
        // WITHDRAWALS
        // =========================================================

        runTest("TC-09", "Successful CAD withdrawal", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            bank.withdrawCash(customer, Account.CHEQUING, 200);

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                1300,
                "Incorrect balance after withdrawal."
            );
        });

        runTest("TC-10", "Minimum supported withdrawal", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            bank.withdrawCash(customer, Account.CHEQUING, 5);

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                1495,
                "Incorrect balance after $5 withdrawal."
            );
        });

        runTest("TC-11", "Zero withdrawal is rejected", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.withdrawCash(customer, Account.CHEQUING, 0),
                "Zero withdrawal should be rejected."
            );
        });

        runTest("TC-12", "Negative withdrawal is rejected", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.withdrawCash(customer, Account.CHEQUING, -50),
                "Negative withdrawal should be rejected."
            );
        });

        runTest("TC-13", "Fractional withdrawal is rejected", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.withdrawCash(customer, Account.CHEQUING, 100.50),
                "Fractional cash withdrawal should be rejected."
            );
        });

        runTest("TC-14", "Withdrawal exceeding balance is rejected", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.withdrawCash(customer, Account.CHEQUING, 1600),
                "Withdrawal exceeding the available balance should fail."
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                1500,
                "Balance changed after rejected withdrawal."
            );
        });

        runTest("TC-15", "Withdrawal exceeding daily limit is rejected",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.withdrawCash(customer, Account.CHEQUING, 1005),
                "Withdrawal above the daily limit should fail."
            );
        });

        runTest("TC-16", "Withdrawal at the daily limit succeeds",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            bank.withdrawCash(customer, Account.CHEQUING, 1000);

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                500,
                "Incorrect balance after a $1,000 withdrawal."
            );
        });

        runTest("TC-17", "Cumulative withdrawals cannot exceed daily limit",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            bank.withdrawCash(customer, Account.CHEQUING, 600);

            expectBankException(
                () -> bank.withdrawCash(customer, Account.CHEQUING, 405),
                "Cumulative withdrawals above $1,000 should fail."
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                900,
                "Balance changed after rejected second withdrawal."
            );
        });

        runTest("TC-18", "Hardware error prevents withdrawal", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            double before =
                balance(customer, Account.CHEQUING, Account.CAD);

            bank.simulateHardwareErrorForDemo();

            expectBankException(
                () -> bank.withdrawCash(customer, Account.CHEQUING, 100),
                "Withdrawal should fail when a hardware error is active."
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                before,
                "Balance changed despite the hardware error."
            );
        });

        // =========================================================
        // DEPOSITS
        // =========================================================

        runTest("TC-19", "Deposit two $50 notes", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            bank.depositCash(
                customer,
                Account.CHEQUING,
                new int[] {0, 2, 0, 0, 0}
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                1600,
                "Incorrect balance after depositing $100."
            );
        });

        runTest("TC-20", "Deposit one $100 note and two $20 notes", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            bank.depositCash(
                customer,
                Account.CHEQUING,
                new int[] {1, 0, 2, 0, 0}
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                1640,
                "Incorrect balance after depositing $140."
            );
        });

        runTest("TC-21", "Deposit with all zero note counts", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.depositCash(
                    customer,
                    Account.CHEQUING,
                    new int[] {0, 0, 0, 0, 0}
                ),
                "A zero-value deposit should be rejected."
            );
        });

        runTest("TC-22", "Negative note count is rejected", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.depositCash(
                    customer,
                    Account.CHEQUING,
                    new int[] {1, -1, 0, 0, 0}
                ),
                "Negative note count should be rejected."
            );
        });

        runTest("TC-23", "Incorrect deposit array length is rejected",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.depositCash(
                    customer,
                    Account.CHEQUING,
                    new int[] {1, 0, 0}
                ),
                "Invalid note-count array length should be rejected."
            );
        });

        runTest("TC-24", "Deposit does not change savings balance",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            double savingsBefore =
                balance(customer, Account.SAVINGS, Account.CAD);

            bank.depositCash(
                customer,
                Account.CHEQUING,
                new int[] {0, 1, 0, 0, 0}
            );

            checkMoney(
                balance(customer, Account.SAVINGS, Account.CAD),
                savingsBefore,
                "Chequing deposit incorrectly changed savings."
            );
        });

        runTest("TC-25", "Deposit to savings updates the correct account",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            bank.depositCash(
                customer,
                Account.SAVINGS,
                new int[] {0, 0, 1, 0, 0}
            );

            checkMoney(
                balance(customer, Account.SAVINGS, Account.CAD),
                520,
                "Savings balance should increase by $20."
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                1500,
                "Chequing balance should remain unchanged."
            );
        });

        // =========================================================
        // TRANSFERS
        // =========================================================

        runTest("TC-26", "Successful transfer between CAD accounts",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            bank.transferFunds(
                customer,
                Account.CHEQUING,
                Account.SAVINGS,
                300
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                1200,
                "Incorrect source balance."
            );

            checkMoney(
                balance(customer, Account.SAVINGS, Account.CAD),
                800,
                "Incorrect destination balance."
            );
        });

        runTest("TC-27", "Transfer between the same account is rejected",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.transferFunds(
                    customer,
                    Account.CHEQUING,
                    Account.CHEQUING,
                    100
                ),
                "Transfer to the same account should be rejected."
            );
        });

        runTest("TC-28", "Transfer exceeding source balance is rejected",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.transferFunds(
                    customer,
                    Account.CHEQUING,
                    Account.SAVINGS,
                    1600
                ),
                "Transfer exceeding the source balance should fail."
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                1500,
                "Source balance changed after rejected transfer."
            );

            checkMoney(
                balance(customer, Account.SAVINGS, Account.CAD),
                500,
                "Destination balance changed after rejected transfer."
            );
        });

        runTest("TC-29", "Zero-value transfer is rejected", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.transferFunds(
                    customer,
                    Account.CHEQUING,
                    Account.SAVINGS,
                    0
                ),
                "Zero-value transfer should be rejected."
            );
        });

        runTest("TC-30", "Negative transfer is rejected", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.transferFunds(
                    customer,
                    Account.CHEQUING,
                    Account.SAVINGS,
                    -100
                ),
                "Negative transfer should be rejected."
            );
        });

        runTest("TC-31", "Transfer with excessive decimal precision is rejected",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.transferFunds(
                    customer,
                    Account.CHEQUING,
                    Account.SAVINGS,
                    100.123
                ),
                "Transfer with more than two decimal places should fail."
            );
        });

        runTest("TC-32", "Total funds are conserved after transfer",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            double before =
                balance(customer, Account.CHEQUING, Account.CAD)
                + balance(customer, Account.SAVINGS, Account.CAD);

            bank.transferFunds(
                customer,
                Account.CHEQUING,
                Account.SAVINGS,
                250
            );

            double after =
                balance(customer, Account.CHEQUING, Account.CAD)
                + balance(customer, Account.SAVINGS, Account.CAD);

            checkMoney(after, before,
                       "Total funds changed after the transfer.");
        });

        // =========================================================
        // PIN MANAGEMENT
        // =========================================================

        runTest("TC-33", "Successful PIN change", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            bank.changePin(customer, "1234", "5678");

            BankCustomer loggedInAgain = login(bank, 1001, "5678");

            check(loggedInAgain != null,
                  "Login with the new PIN should succeed.");
        });

        runTest("TC-34", "Incorrect current PIN cannot change the PIN",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.changePin(customer, "9999", "5678"),
                "Incorrect current PIN should be rejected."
            );
        });

        runTest("TC-35", "PIN shorter than four digits is rejected",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.changePin(customer, "1234", "567"),
                "A three-digit PIN should be rejected."
            );
        });

        runTest("TC-36", "Non-numeric PIN is rejected", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.changePin(customer, "1234", "ABCD"),
                "A non-numeric PIN should be rejected."
            );
        });

        // =========================================================
        // MULTI-CURRENCY TRANSACTIONS
        // =========================================================

        runTest("TC-37", "USD withdrawal updates the USD account", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            bank.withdrawCash(
                customer,
                Account.CHEQUING,
                Account.USD,
                100
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.USD),
                1400,
                "Incorrect USD balance after withdrawal."
            );
        });

        runTest("TC-38", "EUR deposit updates the EUR account", () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            bank.depositCash(
                customer,
                Account.CHEQUING,
                Account.EUR,
                new int[] {0, 2, 0, 0, 0}
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.EUR),
                1600,
                "Incorrect EUR balance after deposit."
            );
        });

        runTest("TC-39", "USD transaction leaves CAD balance unchanged",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            double cadBefore =
                balance(customer, Account.CHEQUING, Account.CAD);

            bank.withdrawCash(
                customer,
                Account.CHEQUING,
                Account.USD,
                100
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                cadBefore,
                "USD withdrawal incorrectly changed CAD balance."
            );
        });

        // =========================================================
        // TECHNICIAN AND MAINTENANCE OPERATIONS
        // =========================================================

        runTest("TC-40", "Valid technician PIN is accepted", () -> {
            BankBackend bank = new BankBackend();

            bank.authenticateTechnician("2468");
        });

        runTest("TC-41", "Invalid technician PIN is rejected", () -> {
            BankBackend bank = new BankBackend();

            expectBankException(
                () -> bank.authenticateTechnician("0000"),
                "Invalid technician PIN should be rejected."
            );
        });

        runTest("TC-42", "Cash replenishment with a positive quantity",
                () -> {
            BankBackend bank = new BankBackend();

            bank.replenishCashNotes(Account.CAD, 20, 10);
        });

        runTest("TC-43", "Negative replenishment quantity is rejected",
                () -> {
            BankBackend bank = new BankBackend();

            expectBankException(
                () -> bank.replenishCashNotes(Account.CAD, 20, -10),
                "Negative replenishment quantity should be rejected."
            );
        });

        runTest("TC-44", "Diagnostics return a result", () -> {
            BankBackend bank = new BankBackend();

            Object result = bank.runDiagnostics();

            check(result != null,
                  "Diagnostics should return a result.");
        });

        // =========================================================
        // ADDITIONAL TESTS - DISTINCT EDGE CASES
        // =========================================================

        runTest("TC-45", "Withdrawal amount not divisible by $5 is rejected",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.withdrawCash(customer, Account.CHEQUING, 12),
                "A $12 withdrawal should be rejected if only $5 multiples are supported."
            );
        });

        runTest("TC-46", "Withdrawal of the second customer's full balance",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1002, "4321");

            bank.withdrawCash(customer, Account.CHEQUING, 800);

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                0,
                "The account should have a zero balance after withdrawal."
            );
        });

        runTest("TC-47", "Withdrawal affects only the selected account",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            double savingsBefore =
                balance(customer, Account.SAVINGS, Account.CAD);

            bank.withdrawCash(customer, Account.CHEQUING, 100);

            checkMoney(
                balance(customer, Account.SAVINGS, Account.CAD),
                savingsBefore,
                "Withdrawal from chequing changed savings."
            );
        });

        runTest("TC-48", "Transfer of the second customer's full balance",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1002, "4321");

            bank.transferFunds(
                customer,
                Account.CHEQUING,
                Account.SAVINGS,
                800
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                0,
                "Source account should have a zero balance."
            );

            checkMoney(
                balance(customer, Account.SAVINGS, Account.CAD),
                2000,
                "Destination account should contain the transferred funds."
            );
        });

        runTest("TC-49", "Transfer affects only the selected currency",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            double cadBefore =
                balance(customer, Account.CHEQUING, Account.CAD);

            bank.transferFunds(
                customer,
                Account.CHEQUING,
                Account.SAVINGS,
                Account.USD,
                100
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                cadBefore,
                "USD transfer incorrectly changed the CAD account."
            );
        });

        runTest("TC-50", "Old PIN is rejected after a successful PIN change",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            bank.changePin(customer, "1234", "5678");

            expectBankException(
                () -> login(bank, 1001, "1234"),
                "The old PIN should no longer be accepted."
            );
        });

        runTest("TC-51", "Unsupported currency withdrawal is rejected",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.withdrawCash(
                    customer,
                    Account.CHEQUING,
                    "GBP",
                    100
                ),
                "Unsupported currency should be rejected."
            );
        });

        runTest("TC-52", "Unsupported currency deposit is rejected",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.depositCash(
                    customer,
                    Account.CHEQUING,
                    "GBP",
                    new int[] {0, 2, 0, 0, 0}
                ),
                "Unsupported currency should be rejected."
            );
        });

        runTest("TC-53", "Unsupported currency transfer is rejected",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            expectBankException(
                () -> bank.transferFunds(
                    customer,
                    Account.CHEQUING,
                    Account.SAVINGS,
                    "GBP",
                    100
                ),
                "Unsupported currency should be rejected."
            );
        });

        runTest("TC-54", "Unsupported cash denomination is rejected",
                () -> {
            BankBackend bank = new BankBackend();

            expectBankException(
                () -> bank.replenishCashNotes(Account.CAD, 25, 10),
                "Unsupported denomination should be rejected."
            );
        });

        runTest("TC-55", "Zero-quantity cash replenishment is rejected",
                () -> {
            BankBackend bank = new BankBackend();

            expectBankException(
                () -> bank.replenishCashNotes(Account.CAD, 20, 0),
                "Zero replenishment quantity should be rejected."
            );
        });

        runTest("TC-56", "Clearing a hardware error restores withdrawals",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            bank.simulateHardwareErrorForDemo();
            bank.clearHardwareErrors();

            bank.withdrawCash(customer, Account.CHEQUING, 100);

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                1400,
                "Withdrawal should succeed after clearing the error."
            );
        });

        runTest("TC-57", "Failed deposit does not partially update balance",
                () -> {
            BankBackend bank = new BankBackend();
            BankCustomer customer = login(bank, 1001, "1234");

            double before =
                balance(customer, Account.CHEQUING, Account.CAD);

            expectBankException(
                () -> bank.depositCash(
                    customer,
                    Account.CHEQUING,
                    new int[] {1, -1, 0, 0, 0}
                ),
                "Invalid deposit should be rejected."
            );

            checkMoney(
                balance(customer, Account.CHEQUING, Account.CAD),
                before,
                "Balance changed after the rejected deposit."
            );
        });

        System.out.println();
        System.out.println("========== AUTOMATED TEST SUMMARY ==========");
        System.out.println("Tests passed: " + passed);
        System.out.println("Tests failed: " + failed);
        System.out.println("Total tests executed: " + (passed + failed));
        System.out.println("============================================");
    
    }
}