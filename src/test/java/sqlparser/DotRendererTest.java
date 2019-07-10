package sqlparser;

import static org.junit.Assert.*;

import org.junit.Test;
import java.util.*;
import static sqlparser.Ast.*;
import static sqlparser.ExampleStatements.*;

/**
 * Rudimentary smoke test on the <code>DotRenderer</code>.
 *
 * Simply checks that something is rendered without errors as long as the
 * input is correct. Does not attempt to evaluate the aesthetic properties
 * of the resulting graph. Does not even attempt to check whether the DOT
 * syntax is valid.
 *
 * Useful anyway, because it checks that there are no
 * <code>IllegalFormatConversionException</code> during formatting, and that
 * all cases are handled in certain pattern matches that simulate ad-hoc
 * polymorphism with lots of <code>instanceof</code>'s.
 */
public class DotRendererTest {

    @Test
    public void shouldNotCrashOnValidStatements() {
        for (String q: VALID_STATEMENTS) {
            for (String p: VALID_STATEMENTS) {
                List<Statement> statements =
                    SqlTokenizer
                    .tokenize("", q + p)
                    .flatMap(SqlParser::parse)
                    .get();

                DotRenderer.render(statements);
            }
        }
    }

}
