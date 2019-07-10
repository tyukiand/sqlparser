package sqlparser;

import lombok.Data;

@Data
public final class ErrorMessage {
    public final Position position;
    public final String message;

    /**
     * Generates an error message with file path and coordinates, so it can
     * be easily picked up by text editors that can show the error messages
     * in the code.
     */
    public String formatMavenStyle() {
        return String.format(
            "[ERROR] %s:[%d,%d] %s",
            position.getSource(),
            position.getLine(),
            position.getColumn(),
            message
        );
    }
}
