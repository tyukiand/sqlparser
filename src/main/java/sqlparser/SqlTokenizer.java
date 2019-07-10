package sqlparser;

import io.vavr.control.Either;
import java.util.*;

import static sqlparser.Constants.*;
import static sqlparser.Token.Type.*;
import static sqlparser.Token.Type;

/**
 * Hand-crafted tokenizer for a tiny subset of SQL.
 *
 * Does basic panic-like error recovery.
 */
public final class SqlTokenizer {

    /**
     * States of a simple DFA that is run during the <code>tokenize</code>
     * method invocation.
     */
    private static enum State {
        DEFAULT,
        SCAN_ID,
        SCAN_NUM,
        SCAN_STR,
        SCAN_STR_ESCAPE,
        SCAN_OP
    }

    /** Source file path (for emitting better error-messages). */
    private final String sourceFilePath;

    /** The entire input that is to be tokenized. */
    private final String input;

    /** Start of the currently scanned token. */
    private int tokenStart = 0;

    /** Current position of the tokenizer. */
    private int pos = 0;

    /** Keeping track of the current line for better error messages. */
    private int lineIdx = 0;

    /** Position of last line break, for columns in error messages. */
    private int lastLineBreakPos = -1;

    /** State of the DFA. */
    private State state = State.DEFAULT;

    /** Scanned tokens. */
    private ArrayList<Token> result = new ArrayList<>();

    /** Occurred errors. */
    private LinkedList<ErrorMessage> errors = new LinkedList<>();

    /**
     * Sets up a fresh DFA and result / error buffers.
     *
     * @param sourceFilePath path of the input file (used for error messages).
     * @param input the input to be parsed.
     */
    private SqlTokenizer(String sourceFilePath, String input) {
        this.sourceFilePath = sourceFilePath;
        this.input = input;
    }

    private Position currentPosition() {
        return Position.of(sourceFilePath, 1 + lineIdx, pos - lastLineBreakPos);
    }

    private Position currentTokenStartPosition() {
        return Position.of(
            sourceFilePath,
            1 + lineIdx,
            tokenStart - lastLineBreakPos
        );
    }

    /**
     * Pushes an identifier-like token to the result list, sets next token
     * start to current position.
     *
     * Recognizes keywords, converts them into special keyword-tokens.
     * The exact string values of the keywords are not preserved, keywords
     * are all converted into upper case.
     */
    private void addIdentifierLikeToken() {
        String str = input.substring(tokenStart, pos);
        String strUpper = str.toUpperCase();
        Token tok =
            RESERVED_KEYWORDS.contains(strUpper) ?
            Token.of(KEYWORD, strUpper, currentTokenStartPosition()) :
            Token.of(IDENTIFIER, str, currentTokenStartPosition());
        result.add(tok);
        tokenStart = pos;
    }

    /**
     * Checks whether the currently scanned token is a valid operator,
     * adds an operator token in case of success, or an error message in the
     * case that the scanned substring is not a valid operator.
     */
    private void addOperatorToken() {
        String str = input.substring(tokenStart, pos);
        if (ALL_OPS.contains(str)) {
            result.add(Token.of(OPERATOR, str, currentTokenStartPosition()));
        } else {
            addErrorMessageAtToken("Invalid operator: `" + str + "`");
        }
    }

    /**
     * Attempts to add a number token.
     *
     * Emits error if the number is too large.
     */
    private void addNumberToken() {
        String str = input.substring(tokenStart, pos);
        try {
            long value = Long.parseLong(str);
            result.add(Token.of(NUMBER, str, currentTokenStartPosition()));
        } catch (NumberFormatException e) {
            addErrorMessageAtToken("Number too long: `" + str + "`");
        }
    }

    /**
     * Appends token of specified type to the result list, moves next token
     * start to current index.
     */
    private void addTokenOfType(Type t) {
        String str = input.substring(tokenStart, pos);
        result.add(Token.of(t, str, currentTokenStartPosition()));
        tokenStart = pos;
    }

    /** Emits error message positioned at the start of current token. */
    private void addErrorMessageAtToken(String msg) {
        errors.add(new ErrorMessage(currentTokenStartPosition(), msg));
    }

    /** Emits error message positioned at the current character. */
    private void addErrorMessageAtChar(String msg) {
        errors.add(new ErrorMessage(currentPosition(), msg));
    }

    /**
     * Runs the DFA on the input.
     * 
     * After calling this method, the behavior of this DFA is undefined,
     * do not use it again.
     */
    private Either<List<ErrorMessage>, ArrayList<Token>> tokenize() {
        int n = input.length();
        while (pos < n) {
            char c = input.charAt(pos);
            switch (state) {
                case DEFAULT:
                    tokenStart = pos;
                    if (Character.isWhitespace(c)) {
                        if (c == '\n') {
                            lastLineBreakPos = pos;
                            lineIdx++;
                        }
                        pos++;
                        /* Stay in DEFAULT */
                    } else if (Character.isJavaIdentifierStart(c)) {
                        pos++;
                        state = State.SCAN_ID;
                    } else if (Character.isDigit(c)) {
                        pos++;
                        state = State.SCAN_NUM;
                    } else if (c == '"') {
                        pos++;
                        state = State.SCAN_STR;
                    } else if (OP_CHARS.contains(c)) {
                        pos++;
                        state = State.SCAN_OP;
                    } else if (c == '(') {
                        pos++;
                        addTokenOfType(LPAREN);
                        /* Stay in DEFAULT */
                    } else if (c == ')') {
                        pos++;
                        addTokenOfType(RPAREN);
                        /* Stay in DEFAULT */
                    } else if (c == ',') {
                        pos++;
                        addTokenOfType(COMMA);
                        /* Stay in DEFAULT */
                    } else if (c == '.') {
                        pos++;
                        addTokenOfType(PERIOD);
                        /* Stay in DEFAULT */
                    } else if (c == ';') {
                        pos++;
                        addTokenOfType(SEMICOLON);
                        /* Stay in DEFAULT */
                    } else {
                        pos++;
                        addErrorMessageAtToken("Invalid token: `" + c + "`");
                    }
                    break;
                case SCAN_ID:
                    if (Character.isJavaIdentifierPart(c)) {
                        pos++;
                        /* Stay in SCAN_ID */
                    } else {
                        addIdentifierLikeToken();
                        /* Don't advance pos */
                        state = State.DEFAULT;
                    }
                    break;
                case SCAN_NUM:
                    if (Character.isDigit(c)) {
                        pos++;
                    } else if (Character.isJavaIdentifierPart(c)) {
                        addErrorMessageAtChar(
                            "Number lumped together with identifier characters"
                        );
                        state = State.DEFAULT;
                    } else {
                        addNumberToken();
                        /* Don't advance pos */
                        state = State.DEFAULT;
                    }
                    break;
                case SCAN_STR:
                    if (c == '"') {
                        pos++;
                        addTokenOfType(STRING);
                        state = State.DEFAULT;
                    } else if (c == '\\') {
                        pos++;
                        state = State.SCAN_STR_ESCAPE;
                    } else if (c == '\r' || c == '\n') {
                        addErrorMessageAtChar(
                            "Multiline strings not supported"
                        );
                        state = State.DEFAULT;
                    } else {
                        pos++;
                        /* Stay in State.SCAN_STR */
                    }
                    break;
                case SCAN_STR_ESCAPE:
                    if (
                        c == '"' || c == 'n' || c == 'r' ||
                        c == 'b' || c == 'f' || c == '\\'
                    ) {
                        /* valid. proceed. */
                        pos++;
                        state = State.SCAN_STR;
                    } else {
                        addErrorMessageAtChar(
                            "Invalid escape sequence: \\" + c
                        );
                        pos++;
                        state = State.SCAN_STR;
                    }
                    break;
                case SCAN_OP:
                    if (OP_CHARS.contains(c)) {
                        pos++;
                        /* Stay in SCAN_OP */
                    } else {
                        addOperatorToken();
                        state = State.DEFAULT;
                    }
                    break;
            }
        }

        // Now reached EOF
        switch (state) {
            case DEFAULT: /* do nothing. */ break;
            case SCAN_ID: addIdentifierLikeToken(); break;
            case SCAN_NUM: addNumberToken(); break;
            case SCAN_STR: addErrorMessageAtChar("Unterminated string"); break;
            case SCAN_OP: addOperatorToken(); break;
            ///CLOVER:OFF
            default: throw new AssertionError("invalid state: " + state);
            ///CLOVER:ON
        }

        // Attach artificial EOF token (saves line/column for error messages
        // about runaway string literals etc.)
        addTokenOfType(EOF);

        if (errors.isEmpty()) {
            return Either.right(result);
        } else {
            return Either.left(errors);
        }
    }

    /**
     * Attempts to convert the input into a list of tokens.
     *
     * In case of success, returns a <code>Right</code> with the tokens,
     * in case of failure, returns a <code>Left</code> with a list of errors.
     * Emits a special <code>EOF</code> token in the end.
     *
     * @param inputDescription usually path to the file (used only for
     *                         error messages).
     * @param input the input String to be tokenized.
     * @return <code>Right</code> with tokens in case of success, otherwise
     *         <code>Left</code> with a list of error messages.
     */
    public static Either<List<ErrorMessage>, ArrayList<Token>> tokenize(
        String inputDescription,
        String input
    ) {
        return new SqlTokenizer(inputDescription, input).tokenize();
    }
}
