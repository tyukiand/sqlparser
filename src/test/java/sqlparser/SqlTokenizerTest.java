package sqlparser;

import static org.junit.Assert.*;

import org.junit.Test;
import io.vavr.control.Either;
import java.util.*;
import static sqlparser.Ast.*;
import static sqlparser.ExampleStatements.*;
import java.util.stream.Collectors;

/**
 * Unit test for the tokenizer. Cares only about lexical errors, ignores
 * higher level syntactical structure.
 */
public class SqlTokenizerTest {

    @Test
    public void shouldAcceptValidStatements() {
        for (String q: VALID_STATEMENTS) {
            Either<List<ErrorMessage>, ArrayList<Token>> res =
                SqlTokenizer.tokenize("", q);
            assertTrue("Should accept: `" + q + "`", res.isRight());
        }
    }

    /**
     * Helper method that compares reported lexical errors with the manually
     * marked syntax errors.
     *
     * @param badCode code snippet with manually marked error positions.
     */
    private void shouldDetectLexicalErrors(String badCode) {
        ErrorExample ex = extractExample(badCode);
        Either<List<ErrorMessage>, ArrayList<Token>> res =
            SqlTokenizer
            .tokenize("", ex.getCode());
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
        for (String e: STATEMENTS_WITH_LEXICAL_ERRORS) {
           shouldDetectLexicalErrors(e);
        }
    }

    @Test
    public void shouldDetectMultipleErrorsSkippingInvalidTokens() {
        for (String e: STATEMENTS_WITH_LEXICAL_ERRORS) {
            for (String j: VALID_STATEMENTS) {
                for (String k: STATEMENTS_WITH_LEXICAL_ERRORS) {
                    shouldDetectLexicalErrors(e + "\n" + j + "\n" + k);
                }
            }
        }
    }
}
