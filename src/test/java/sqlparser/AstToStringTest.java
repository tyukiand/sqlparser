package sqlparser;

import static org.junit.Assert.*;

import org.junit.Test;
import java.util.*;
import static sqlparser.Ast.*;
import static sqlparser.ExampleStatements.*;

/**
 * Just parses bunch of example queries and then writes out all
 * AST elements.
 *
 * Ensures there are no malformed string format specifiers.
 */
public class AstToStringTest {

    /**
     * Checks that AST's <code>toString</code> methods don't go up in flames
     * on invocation.
     */
    @Test
    public void shouldNotCrashOnValidStatements() {
        for (String q: VALID_STATEMENTS) {
            List<Statement> statements =
                SqlTokenizer
                .tokenize("", q)
                .flatMap(SqlParser::parse)
                .get();

            for (Statement s: statements) {
                s.toString();
            }
        }
    }

}
