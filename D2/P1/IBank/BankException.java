/**
 * Purpose: Defines the exception type used for expected iBank errors.
 * Project role: Problem 4 requires explicit exception handling and useful
 * messages. The backend raises this exception when a banking rule is broken.
 */
public class BankException extends Exception {

    /**
     * Creates an iBank error with a message for the user.
     *
     * @param message explanation of what went wrong
     */
    public BankException(String message) {
        super(message);
    }
}
