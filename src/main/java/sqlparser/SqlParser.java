package sqlparser;

import io.vavr.control.Either;
import java.util.*;
import java.util.function.Predicate;

import static sqlparser.Ast.*;
import static sqlparser.Constants.*;
import static sqlparser.Token.Type.*;
import static sqlparser.Token.Type;

/** Simple parser for a subset of SQL. */
public final class SqlParser {

    /** The input, presumably generated by the tokenizer. */
    private final ArrayList<Token> tokens;

    /** Index of the currently inspected token. */
    private int currentIndex = 0;

    /**
     * Signals that parsing of a statement has failed. Carries the
     * error message with the cause and the exact coordinates of the
     * problem.
     */
    private static final class ParseException extends Exception {
        private final ErrorMessage errorMessage;
        public ParseException(ErrorMessage e) {
            super(e.toString());
            this.errorMessage = e;
        }
        public ErrorMessage getErrorMessage() {
            return errorMessage;
        }
    }

    private SqlParser(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Basic statement-level recovery that attempts to skip to the end of
     * an invalid statement.
     *
     * Simply discards all tokens until the next semicolon is encountered.
     */
    private void recover() {
        int n = tokens.size();
        while (currentIndex < n) {
            if (tryPeek(SEMICOLON).isPresent()) {
                currentIndex++;
                return;
            }
            currentIndex++;
        }
    }

    /**
     * Attemts to parse the statements in the input.
     *
     * After the invocation of this method, the state of the parser 
     * is completely undefined.
     */
    private Either<List<ErrorMessage>, List<Statement>> parse() {
        LinkedList<Statement> result = new LinkedList<>();
        LinkedList<ErrorMessage> errors = new LinkedList<>();

        int n = tokens.size();
        while (
            currentIndex < n &&
            tokens.get(currentIndex).getTokenType() != EOF
        ) {
            try {
                Statement s = statement();
                result.add(s);
            } catch (ParseException e) {
                errors.add(e.getErrorMessage());
                recover();
            }
        }

        if (errors.isEmpty()) {
            return Either.right(result);
        } else {
            return Either.left(errors);
        }
    }

    /** 
     * Retrieves current token,
     * assumes that the <code>currentIndex</code> is valid
     * (we never parse past the special EOF token).
     */
    private Token peek() {
        return tokens.get(currentIndex);
    }

    /**
     * Attempts to get the current token, throws if it doesn't have the
     * specified type. 
     */
    private Token peek(Type tpe) throws ParseException {
        Token tok = tokens.get(currentIndex);
        if (tok.getTokenType() == tpe) {
            return tok;
        } else {
            return error(
                tok.getPosition(),
                "Expected token of type " + tpe + ", " +
                "but found `" + tok.getString() + "`"
            );
        }
    }

    /**
     * Attempts to retrieve current token, throws if it either doesn't have
     * the specified token type, or if it doesn't have exactly the specified
     * lexeme.
     */
    private Token peek(Type tpe, String str) throws ParseException {
        Token tok = tokens.get(currentIndex);
        if (tpe == tok.getTokenType() && str.equals(tok.getString())) {
            return tok;
        } else {
            return error(
                tok.getPosition(),
                "Expected token `" + str + "` of type " + tpe + ", " +
                "but found: `" + tok.getString() + "`"
            );
        }
    }

    /**
     * Attempts to get the current token, returns <code>None</code> if
     * it doesn't have the specified type.
     */
    private Optional<Token> tryPeek(Type tpe) {
        Token tok = peek();
        if (tpe == tok.getTokenType()) {
            return Optional.of(tok);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Attempts to get the current token, returns <code>None</code> if
     * it doesn't have the specified type, or if it has the string value
     * other than specified.
     */
    private Optional<Token> tryPeek(Type tpe, String str) {
        Token tok = peek();
        if (tpe == tok.getTokenType() && str.equals(tok.getString())) {
            return Optional.of(tok);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Attempts to get the current token, returns <code>None</code> if 
     * it doesn't satisfy the predicate.
     */
    private Optional<Token> tryPeek(Type tpe, Predicate<String> strPred) {
        Token tok = peek();
        if (tpe == tok.getTokenType() && strPred.test(tok.getString())) {
            return Optional.of(tok);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Same as <code>peek</code>,
     * followed by <code>currentIndex</code> increment.
     */
    private Token eat() {
        Token tok = peek();
        currentIndex++;
        return tok;
    }

    /**
     * Consumes a token of the specified type.
     *
     * If the type matches, then this method increments the current token index,
     * and returns the consumed token. Otherwise, it does not increment the
     * current index, and throws a <code>ParseException</code>.
     */
    private Token eat(Type tpe) throws ParseException {
        Token tok = peek(tpe);
        currentIndex++;
        return tok;
    }

    /**
     * Consumes a token of the specified type and with specified string value.
     *
     * If the type and the string part both match,
     * then this method increments the current token index
     * and returns the consumed token. Otherwise, it does not increment the
     * current index, and throws a <code>ParseException</code>.
     */
    private Token eat(Type tpe, String str) throws ParseException {
        Token tok = peek(tpe, str);
        currentIndex++;
        return tok;
    }

    /**
     * Same as <code>tryPeek</code>, but increment the <code>currentIndex</code>
     * in case of success.
     */
    private Optional<Token> tryEat(Type tpe) {
        Optional<Token> optTok = tryPeek(tpe);
        if (optTok.isPresent()) {
            currentIndex++;
        }
        return optTok;
    }

    /**
     * Same as <code>tryPeek</code>, but increment the <code>currentIndex</code>
     * in case of success.
     */
    private Optional<Token> tryEat(Type tpe, String str) {
        Optional<Token> optTok = tryPeek(tpe, str);
        if (optTok.isPresent()) {
            currentIndex++;
        }
        return optTok;
    }

    /**
     * Same as <code>tryPeek</code>, but increment the <code>currentIndex</code>
     * in case of success.
     */
    private Optional<Token> tryEat(Type tpe, Predicate<String> strPred) {
        Optional<Token> optTok = tryPeek(tpe, strPred);
        if (optTok.isPresent()) {
            currentIndex++;
        }
        return optTok;
    }

    /**
     * Emits an error at current position.
     *
     * The universal quantification over the return type <code>A</code> 
     * guarantees that this method always throws a <code>ParseException</code>.
     */
    private <A> A error(Position pos, String message)
    throws ParseException {
        throw new ParseException(new ErrorMessage(pos, message));
    }

    private Statement statement() throws ParseException {
        Token t = peek();
        switch (t.getTokenType()) {
            case KEYWORD:
                switch (t.getString()) {
                    case "USE": return use();
                    case "SELECT": return select();
                    case "INSERT": return insert();
                    case "DELETE": return delete();
                    default: return error(
                        t.getPosition(),
                        "Unexpected keyword at start of a statement: " +
                        t.getString()
                    );
                }
            default: return error(
                t.getPosition(),
                "Invalid start of statement, not even a keyword: `" +
                t.getString() + "`"
            );
        }
    }

    private Use use() throws ParseException {
        eat(KEYWORD, "USE");
        Token t = eat(IDENTIFIER);
        eat(SEMICOLON);
        return new Use(t.getString());
    }

    private Expression expression() throws ParseException {
        LinkedList<Expression> disjuncts = new LinkedList<>();
        disjuncts.add(disjunct());
        while (tryEat(KEYWORD, "OR").isPresent()) {
            disjuncts.add(disjunct());
        }
        Expression res =  disjuncts.stream().reduce(BinOp.partial("OR")).get();
        return res;
    }

    private Expression disjunct() throws ParseException {
        LinkedList<Expression> conjuncts = new LinkedList<>();
        conjuncts.add(conjunct());
        while (tryEat(KEYWORD, "AND").isPresent()) {
            conjuncts.add(conjunct());
        }
        return conjuncts.stream().reduce(BinOp.partial("AND")).get();
    }

    private Expression conjunct() throws ParseException {
        if (tryEat(KEYWORD, "NOT").isPresent()) {
            return new UnOp("NOT", conjunct());
        } else {
            return isNullCheck();
        }
    }

    private Expression isNullCheck() throws ParseException {
        Expression leftPart = comparison();
        if (tryEat(KEYWORD, "IS").isPresent()) {
            boolean not = tryEat(KEYWORD, "NOT").isPresent();
            eat(KEYWORD, "NULL");
            return new IsNullCheck(leftPart, not);
        } else {
            return leftPart;
        }
    }

    private Expression comparison() throws ParseException {
        Expression accum = comparable();
        while (true) {
            Optional<Token> opTok = tryEat(OPERATOR, COMPARISON_OPS::contains);
            if (opTok.isPresent()) {
                Expression right = comparable();
                accum = new BinOp(opTok.get().getString(), accum, right);
            } else {
                return accum;
            }
        }
    }

    private Expression comparable() throws ParseException {
        Expression accum = term();

        while (true) {
            Optional<Token> opToken = tryEat(OPERATOR, ARITH_LOW_OPS::contains);

            if (opToken.isPresent()) {
                Expression right = term();
                accum = new BinOp(opToken.get().getString(), accum, right);
            } else {
                return accum;
            }
        }
    }

    private Expression term() throws ParseException {
        // TODO: duplication with `comparable`
        Expression accum = factor();
        while (true) {
            Optional<Token> opTok = tryEat(OPERATOR, ARITH_HIGH_OPS::contains);
            if (opTok.isPresent()) {
                Expression right = term();
                accum = new BinOp(opTok.get().getString(), accum, right);
            } else {
                return accum;
            }
        }
    }

    private Expression factor() throws ParseException {
        Token t = peek();
        switch (t.getTokenType()) {
            case IDENTIFIER:
                return functionOrColumn();
            case LPAREN:
                return parenthesizedExpression();
            case OPERATOR:
                switch (t.getString()) {
                    case "+":
                        eat(OPERATOR, "+");
                        return new UnOp("+", factor());
                    case "-":
                        eat(OPERATOR, "-");
                        return new UnOp("-", factor());
                    default: return error(
                        t.getPosition(),
                        "Invalid operator at start of a factor: " +
                        "`" + t.getString() + "`"
                    );
                }
            case NUMBER:
                eat();
                return new NumConstant(Long.parseLong(t.getString()));
            case STRING:
                eat();
                return new StringConstant(t.getString());
            default: return error(
                t.getPosition(),
                "Invalid token type at start of a factor: " +
                "`" + t.getString() + "`"
            );
        }
    }

    private Expression functionOrColumn() throws ParseException {
        String id = eat(IDENTIFIER).getString();
        if (tryPeek(LPAREN).isPresent()) {
            // That looks like a function invocation
            List<Expression> args = tuple();
            return new FunctionApplication(id, args);
        } else {
            // That's just an isolated identifier, looks like column name
            return new Identifier(id);
        }
    }

    private List<Expression> tuple() throws ParseException {
        eat(LPAREN);
        if (tryEat(RPAREN).isPresent()) {
            // empty tuple
            return new LinkedList<>();
        } else {
            // must be a nonempty tuple
            List<Expression> components = commaSeparatedExpressions();
            eat(RPAREN);
            return components;
        }
    }

    private List<Expression> commaSeparatedExpressions() throws ParseException {
        LinkedList<Expression> components = new LinkedList<>();
        components.add(expression());
        while (tryEat(COMMA).isPresent()) {
            components.add(expression());
        }
        return components;
    }

    private Expression parenthesizedExpression() throws ParseException {
        eat(LPAREN);
        Expression res = expression();
        eat(RPAREN);
        return res;
    }

    private TableId tableId() throws ParseException {
        String firstPart = eat(IDENTIFIER).getString();
        if (tryEat(PERIOD).isPresent()) {
            // The first part was only the database. There must be a
            // second part with the actual table name
            String secondPart = eat(IDENTIFIER).getString();
            return new TableId(Optional.of(firstPart), secondPart);
        } else {
            // It's only table name, no database prefix
            return new TableId(Optional.empty(), firstPart);
        }
    }

    private Select select() throws ParseException {
        eat(KEYWORD, "SELECT");
        List<Expression> selectList = commaSeparatedExpressions();

        // WONTFIX: <code>map</code> doesn't work because of exceptions.
        // Fall back to ternary operator: it doesn't care whether the
        // operands can throw exceptions or not.
        Optional<TableId> fromTableId =
            tryEat(KEYWORD, "FROM").isPresent() ?
            Optional.of(tableId()) :
            Optional.empty();

        Optional<Expression> whereCondition =
            tryEat(KEYWORD, "WHERE").isPresent() ?
            Optional.of(expression()) :
            Optional.empty();


        Optional<Expression> orderBy = Optional.empty();
        if (tryEat(KEYWORD, "ORDER").isPresent()) {
            eat(KEYWORD, "BY");
            orderBy = Optional.of(expression());
        }

        eat(SEMICOLON);

        return new Select(selectList, fromTableId, whereCondition, orderBy);
    }

    private Insert insert() throws ParseException {
        eat(KEYWORD, "INSERT");
        eat(KEYWORD, "INTO");
        TableId intoTable = tableId();
        eat(LPAREN);
        List<String> columns = columnList();
        eat(RPAREN);
        eat(KEYWORD, "VALUES");
        List<Expression> values = tuple();
        eat(SEMICOLON);

        return new Insert(intoTable, columns, values);
    }

    private List<String> columnList() throws ParseException {
        LinkedList<String> columns = new LinkedList<>();
        columns.add(eat(IDENTIFIER).getString());
        while (tryEat(COMMA).isPresent()) {
            columns.add(eat(IDENTIFIER).getString());
        }
        return columns;
    }

    private Delete delete() throws ParseException {
        eat(KEYWORD, "DELETE");
        eat(KEYWORD, "FROM");
        TableId fromTable = tableId();
        eat(KEYWORD, "WHERE");
        Expression whereCondition = expression();
        eat(SEMICOLON);

        return new Delete(fromTable, whereCondition);
    }

    /**
     * Attempts to build a forest of <code>Statement</code>-ASTs from the
     * tokens.
     *
     * @param tokens tokens generated by the tokenizer, with a special EOF token
     *               in the end.
     * @return a <code>Right</code> with the forest in case of success,
     *         otherwise a <code>Left</code> with a lits of errors.
     */
    public static Either<List<ErrorMessage>, List<Statement>> parse(
        ArrayList<Token> tokens
    ) {
        return new SqlParser(tokens).parse();
    }
}