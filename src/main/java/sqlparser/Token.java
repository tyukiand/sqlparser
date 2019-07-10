package sqlparser;

import lombok.Value;

@Value public final class Token {
    public static enum Type {
        KEYWORD,
        IDENTIFIER,
        NUMBER,
        STRING,
        COMMA,
        PERIOD,
        SEMICOLON,
        LPAREN,
        RPAREN,
        OPERATOR,
        EOF
    };

    Type tokenType;
    String string;
    Position position;

    public static Token of(Type t, String str, Position pos) {
        return new Token(t, str, pos);
    }
}
