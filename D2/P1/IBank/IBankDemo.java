/*
 * Purpose: Runs representative backend tests for Problem 4(b).
 * Project role: Demonstrates customer transactions, exception handling, lockout,
 * diagnostics, cash replenishment, receipt-roll replacement, and error clearing.
 * This file is test/demo code and must be excluded from Problem 5's SLOC count.
 */
public class IBankDemo {

    /**
     * Runs representative success and failure scenarios.
     * Input: none.
     * Output: receipts, maintenance messages, and expected errors on the console.
     * Why: Verifies the banking backend without requiring manual GUI interaction.
     */
    public static void main(String[] args) {
        BankBackend bank = new BankBackend();

        try {
            BankCustomer customer = bank.login(1001, "1234");

            System.out.println(bank.balanceInquiry(customer));
            System.out.println();

            System.out.println(bank.withdrawCash(
                    customer, Account.CHEQUING, 200.00));
            System.out.println();

            // Denomination order is $100, $50, $20, $10, $5; two $50 bills total $100.
            System.out.println(bank.depositCash(
                    customer, Account.CHEQUING, new int[] {0, 2, 0, 0, 0}));
            System.out.println();

            System.out.println(bank.transferFunds(
                    customer, Account.CHEQUING, Account.SAVINGS, 300.00));
            System.out.println();

            System.out.println(bank.changePin(customer, "1234", "5678"));
            System.out.println();

            // $805 plus the earlier $200 exceeds the assumed $1,000 daily limit.
            try {
                bank.withdrawCash(customer, Account.CHEQUING, 805.00);
            } catch (BankException exception) {
                System.out.println("Expected withdrawal error: " + exception.getMessage());
            }

            System.out.println();
            bank.simulateHardwareErrorForDemo();
            try {
                bank.withdrawCash(customer, Account.CHEQUING, 20.00);
            } catch (BankException exception) {
                System.out.println("Expected hardware error: " + exception.getMessage());
            }
            System.out.println(bank.clearHardwareErrors());
            System.out.println(bank.replenishCashNotes(20, 10));
            bank.simulateEmptyReceiptRollForDemo();
            System.out.println(bank.runDiagnostics());
            System.out.println(bank.replaceReceiptRoll());

        } catch (BankException exception) {
            System.out.println("Unexpected test error: " + exception.getMessage());
        }

        testThreeFailureLockout();
    }

    /**
     * Confirms that three consecutive invalid PINs lock the card for this run.
     * Input: none; this method creates a separate backend for an isolated test.
     * Output: three expected error messages, including the lockout message.
     * Why: Tests UC-01 without locking the customer used by the transaction demo.
     */
    private static void testThreeFailureLockout() {
        BankBackend testBank = new BankBackend();
        System.out.println("\nAuthentication lockout test:");

        for (int i = 1; i <= 3; i++) {
            try {
                testBank.login(1001, "9999");
            } catch (BankException exception) {
                System.out.println(exception.getMessage());
            }
        }

        try {
            testBank.login(1001, "1234");
        } catch (BankException exception) {
            System.out.println("Expected locked-card result: " + exception.getMessage());
        }
    }
}
