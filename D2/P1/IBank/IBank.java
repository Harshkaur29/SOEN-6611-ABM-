/**
 * Purpose: Application entry point for iBank.
 * Project role: Creates the simulated backend and starts the required Swing GUI.
 * This class contains no banking rules, keeping startup separate from the product logic.
 */
import javax.swing.SwingUtilities;

public class IBank {

    /**
     * Starts iBank on Swing's event-dispatch thread.
     *
     * @param args command-line arguments, which are not used
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BankBackend bank = new BankBackend();
            BankGUI gui = new BankGUI(bank);
            gui.start();
        });
    }
}
