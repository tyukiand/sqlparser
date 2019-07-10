package sqlparser;

import lombok.Value;

@Value(staticConstructor = "of")
public final class Position {
    String source;
    int line;
    int column;
}
