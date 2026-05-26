import java.util.List;

/**
 * Represents the outcome of validating course input data.
 * Contains a pass/fail flag and a list of error messages explaining
 * any problems found. If valid is true, the errors list is empty.
 */
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;

    /**
     * Creates a validation result.
     *
     * @param valid  true if all checks passed, false otherwise
     * @param errors list of error messages (empty when valid is true)
     */
    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = List.copyOf(errors);
    }

    /** Returns true if the input passed all validation checks. */
    public boolean isValid() {
        return valid;
    }

    /** Returns an unmodifiable list of error messages. Empty when valid. */
    public List<String> getErrors() {
        return errors;
    }
}
