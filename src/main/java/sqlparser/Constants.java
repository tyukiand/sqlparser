package sqlparser;

import java.util.*;
import static java.util.stream.Collectors.*;

class Constants {
    /** Reserved keywords. */
    public static HashSet<String> RESERVED_KEYWORDS = new HashSet<>(
        Arrays.asList(
            "AND",
            "BY",
            "DELETE",
            "FROM",
            "HAVING",
            "INSERT",
            "INTO",
            "IS",
            "NOT",
            "NULL",
            "OR",
            "ORDER",
            "SELECT",
            "USE",
            "VALUES",
            "WHERE"
        )
    );

    public static final HashSet<String> COMPARISON_OPS = new HashSet<String>(
        Arrays.asList("=,!=,<,>,<=,>=,!<,!>".split(","))
    );

    public static final HashSet<String> ARITH_HIGH_OPS = new HashSet<String>(
        Arrays.asList("/", "*", "%")
    );

    public static final HashSet<String> ARITH_LOW_OPS = new HashSet<String>(
        Arrays.asList("+", "-")
    );

    public static final HashSet<String> ALL_OPS = new HashSet<String>(){{
        addAll(COMPARISON_OPS);
        addAll(ARITH_HIGH_OPS);
        addAll(ARITH_LOW_OPS);
    }};

    /** Set of characters that can occur as parts of operators. */
    public static final Set<Character> OP_CHARS =
        ALL_OPS
        .stream()
        .flatMap((String s) -> s.chars().mapToObj(c -> (char) c))
        .collect(toSet());
}

