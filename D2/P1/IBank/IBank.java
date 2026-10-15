/*
 * Purpose: Application entry point for iBank.
 * Project role: Creates the simulated backend and starts the required Swing GUI.
 * This class contains no banking rules, keeping startup separate from the product logic.
 */
import javax.swing.SwingUtilities;

public class IBank {

    /**
     * Starts iBank on Swing's event-dispatch thread.
     * Input: command-line arguments are not used.
     * Output: no return value; opens the iBank GUI.
     * Why: Provides a standard Java entry point and starts Swing safely.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BankBackend bank = new BankBackend();
            BankGUI gui = new BankGUI(bank);
            gui.start();
        });
    }
}
