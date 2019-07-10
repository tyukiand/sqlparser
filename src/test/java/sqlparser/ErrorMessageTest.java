package sqlparser;

import static org.junit.Assert.*;

import org.junit.Test;


/**
 * Check that the error messages are really printed in the same format as
 * in Maven.
 */
public class ErrorMessageTest {

    @Test
    public void shouldProduceSameFormatAsMaven() {
        ErrorMessage e = new ErrorMessage(
            Position.of("somewhere/someFile", 10, 20),
            "something bad happened"
        );

        String regex =
            "^\\[ERROR\\] ([a-zA-Z0-9_./-]+):\\[([0-9]+),([0-9]+)\\] (.*)$";

        assertTrue(e.formatMavenStyle().matches(regex));
    }

}
