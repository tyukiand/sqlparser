package sqlparser;

import static org.junit.Assert.*;

import org.junit.Test;
import io.vavr.control.Either;
import java.util.*;
import static sqlparser.Ast.*;
import static sqlparser.ExampleStatements.*;
import java.util.stream.Collectors;

/**
 * Unit tests for the parser.
 *
 * Checks that it doesn't crash and burn on valid input, and that it emits
 * error messages for the syntactic errors at the expected positions.
 * It also checks that the panic-mode recovery works as expected, and that
 * error messages are emitted for separate statements independently.
 * Checks only the positions, doesn't
 * attempt to look into the textual descriptions of the error messages.
 */
public class SqlParserTest {

    @Test
    public void shouldAcceptValidStatements() {
        for (String q: VALID_STATEMENTS) {
            Either<List<ErrorMessage>, List<Statement>> res =
                SqlTokenizer.tokenize("", q).flatMap(SqlParser::parse);
            assertTrue("Should accept: `" + q + "`", res.isRight());
        }
    }

    /**
     * Helper method that compares reported syntax errors with the manually
     * marked syntax errors.
     */
    private void shouldDetectSyntaxErrors(String badCode) {
        ErrorExample ex = extractExample(badCode);
        Either<List<ErrorMessage>, List<Statement>> res =
            SqlTokenizer
            .tokenize("", ex.getCode())
            .flatMap(SqlParser::parse);
        assertTrue("Should reject: `" + ex.getCode() + "`", res.isLeft());
        List<Position> reportedErrors =
            res
            .getLeft()
            .stream()
            .map(ErrorMessage::getPosition)
            .collect(Collectors.toList());
        assertEquals(
            "Ex: `" + ex.getCode() + "`;\nReported: " + res.getLeft() + "\n",
            ex.getErrorPositions(),
            reportedErrors
        );
    }

    @Test
    public void shouldDetectErrorsInSingleStatements() {
        for (String e: STATEMENTS_WITH_SYNTAX_ERRORS) {
           shouldDetectSyntaxErrors(e);
        }
    }

    @Test
    public void shouldDetectMultipleErrorsUsingPanicModeRecovery() {
        for (String e: STATEMENTS_WITH_SYNTAX_ERRORS) {
            for (String j: VALID_STATEMENTS) {
                for (String k: STATEMENTS_WITH_SYNTAX_ERRORS) {
                    shouldDetectSyntaxErrors(e + "\n" + j + "\n" + k);
                }
            }
        }
    }

    @Test
    public void shouldExitProperlyIfThereIsNoSemicolonAfterSyntaxError() {
        shouldDetectSyntaxErrors("SELECT yes!!!>>>)");
    }
}
