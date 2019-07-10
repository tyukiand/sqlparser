package sqlparser;

import lombok.Value;
import java.util.*;

class ExampleStatements {

    public static final String[] VALID_STATEMENTS = new String[] {
        "USE database1;",
        "USE some_database;",
        "USE _the_database;",
        "SELECT id, name, address FROM users \n" +
        "    WHERE is_customer IS NOT NULL ORDER BY created;",
        "INSERT INTO user_notes (id, user_id, note, created)" +
        "    VALUES (1, 1, \"Note 1\", NOW());",
        "DELETE FROM database2.logs WHERE id < 1000;",
        "USE myDatabase;",
        "USE\ndatabase\n;",
        "SELECT 1,2;",
        "SELECT a FROM b.c ORDER BY c - a;",
        "SELECT stuff\nFROM table WHERE x > 10\n  ;",
        "SELECT a,\"yes\" , col FROM tbl\nWHERE a > 10 * 10 ORDER BY -a;",
        "SELECT x FROM y WHERE z IS NULL;",
        "SELECT x FROM y WHERE z IS NOT NULL AND NOT z < 10;",
        "INSERT INTO a.b(x,y,z)VALUES(1,-2,\"print\\\"hello\\\"\");",
        "DELETE FROM z WHERE a + b * c - d < a * a + b * b OR a > b;",
        "DELETE FROM x.y WHERE NOT NOT NOT 2 + + + + 2 > - - 5;",
        "INSERT INTO x.y (a, b) VALUES ((1), (2 + (2 + + 2)));",
        "DELETE FROM t WHERE x IS NULL;",
        "DELETE FROM t WHERE x IS NOT NULL;",
        "DELETE FROM t WHERE NOT x IS NULL;"
    };

    @Value public static final class ErrorExample {
        String code;
        List<Position> errorPositions;
    }

    private static final String ERROR_MARKER = "!!!>>>";
    public static final String[] STATEMENTS_WITH_SYNTAX_ERRORS = new String[] {
        "USE !!!>>>)database1;",
        "SELECT id, name,\n(address !!!>>>FROM users \n" +
        "    WHERE is_customer IS NOT NULL ORDER BY created;",
        "INSERT INTO\nuser_notes (id,\nuser_id, note, created)" +
        "    VALUES (1, 1, \"Note 1\", NOW()!!!>>>;",
        "DELETE FROM database2.logs WHERE id + !!!>>><= 1000;",
        "USE myDatabase!!!>>>.somethingElse;",
        "USE\n!!!>>>345678 database\n;",
        "SELECT !!!>>>SELECT 1,2;",
        "SELECT a FROM\nb.c ORDER !!!>>>c - a;",
        "SELECT stuff FROM table !!!>>>WREHE x > 10;",
        "SELECT a,\"yes\" , col FROM tbl\nWHERE a > 10 * 10 !!!>>>BY -a;",
        "SELECT x FROM\ny WHERE\nz IS !!!>>>MAYBE NULL;",
        "SELECT x FROM y \n WHERE z IS NOT NULL\n AND !!!>>>OR NOT z < 10;",
        "INSERT INTO\n a.b\n(x, y, z!!!>>>() VALUES" +
        "(1, -2, \"print\\\"hello\\\"\");",
        "DELETE FROM z\nWHERE a + b * c - d < a(!!!>>>* a + b * b OR a > b;",
        "!!!>>>lets SELECT stuff;",
        "!!!>>>NULL SELECT nothing;"
    };

    public static final String[] STATEMENTS_WITH_LEXICAL_ERRORS = new String[] {
        "USE !!!>>>++database1;",
        "SELECT id, 1234!!!>>>name,\n(address FROM users \n" +
        "    WHERE is_customer IS NOT NULL ORDER BY created;",
        "INSERT INTO\nuser_notes (id,\nuser_id, note, created)" +
        "    VALUES (1, 1, \"Note 1\", NOW();!!!>>>?",
        "DELETE FROM database2.logs WHERE id + !!!>>><=> 1000;",
        "USE myDatabase!!!>>>|somethingElse;",
        "USE\n!!!>>>34567885729759495724959279582479 database\n;",
        "SELECT SELECT 1,2       !!!>>>@here;",
        "SELECT \" FROM!!!>>>\nb.c ORDER c - a;",
        "SELECT \" FROM b.c ORDER c - a; !!!>>>",
        "SELECT stuff FROM table \"this is ok \\!!!>>>a\" x > 10;",
        "emptySpace          !!!>>>~hello",
        "DELETE !!!>>>! invalidOperators 9124",
        "dont end lines with invalid operators !!!>>>!!",
        "too large !!!>>>8573979793679347693874953753957394"
    };

    public static ErrorExample extractExample(String brokenCode) {
        String[] lines = brokenCode.split("\r?\n");
        List<Position> errorPositions = new LinkedList<>();
        for (int i = 0; i < lines.length; i++) {
            int lineIdx = i + 1;
            int col = lines[i].indexOf(ERROR_MARKER);
            if (col >= 0) {
                errorPositions.add(Position.of("", lineIdx, col + 1));
            }
        }
        return new ErrorExample(
            brokenCode.replaceAll(ERROR_MARKER, ""),
            errorPositions
        );
    }
}
