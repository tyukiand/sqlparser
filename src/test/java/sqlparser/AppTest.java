package sqlparser;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void basicSelectShouldNotCrash() {
        String query = "SELECT stuff FROM table WHERE x > 10;";

        assertTrue(
            SqlTokenizer.tokenize("", query).flatMap(SqlParser::parse).isRight()
        );
    }
}
