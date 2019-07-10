package sqlparser;

import java.util.Optional;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import lombok.Value;

public final class Ast {

    public static abstract class Statement {

    }

    @Value public static class Use extends Statement {
        String databaseName;
        @Override
        public String toString() {
            return String.format("Use(databaseName=%s)", databaseName);
        }
    }

    @Value public static class Select extends Statement {
        List<Expression> selectList;
        Optional<TableId> fromTableId;
        Optional<Expression> whereCondition;
        Optional<Expression> orderBy;

        @Override
        public String toString() {
            return String.format(
                "Select(\n  list=%s\n  from=%s\n  where=%s\n  orderBy=%s\n)",
                selectList,
                fromTableId.map(String::valueOf).orElse("(omitted)"),
                whereCondition.map(String::valueOf).orElse("(omitted)"),
                orderBy.map(String::valueOf).orElse("(omitted)")
            );
        }
    }

    @Value public static class Insert extends Statement {
        TableId intoTable;
        List<String> columnList;
        List<Expression> values;

        @Override
        public String toString() {
            return String.format(
                "Insert(\n  into=%s\n  columns=%s\n  values=%s\n)",
                intoTable,
                columnList,
                values
            );
        }
    }

    @Value public static class Delete extends Statement {
        TableId fromTable;
        Expression whereCondition;

        @Override
        public String toString() {
            return String.format(
                "Delete(\n  from=%s\n  where=%s\n)",
                fromTable,
                whereCondition
            );
        }
    }

    public static <T> T matchStatement(
        Statement s,
        Function<Use, T> caseUse,
        Function<Select, T> caseSelect,
        Function<Insert, T> caseInsert,
        Function<Delete, T> caseDelete
    ) {
        if (s instanceof Use) {
            return caseUse.apply((Use) s);
        } else if (s instanceof Select) {
            return caseSelect.apply((Select) s);
        } else if (s instanceof Insert) {
            return caseInsert.apply((Insert) s);
            ///CLOVER:OFF
        } else if (s instanceof Delete) {
            ///CLOVER:ON
            // the body is tested; the missing else-branch is ignored.
            return caseDelete.apply((Delete) s);
        } else {
            ///CLOVER:OFF
            throw new AssertionError(
                "Statement match not exhaustive: " + s.getClass()
            );
            ///CLOVER:ON
        }
    }

    @Value public static class TableId {
        Optional<String> databaseName;
        String tableName;
    }

    public static abstract class Expression {

    }

    @Value public static class Identifier extends Expression {
        String value;

        @Override
        public String toString() {
            return "Id(" + value + ")";
        }
    }

    @Value public static class NumConstant extends Expression {
        long value;

        @Override
        public String toString() {
            return "Num(" + value + ")";
        }
    }

    @Value public static class StringConstant extends Expression {
        String value;

        @Override
        public String toString() {
            return "Str(" + value + ")";
        }
    }

    @Value public static class FunctionApplication extends Expression {
        String functionId;
        List<Expression> arguments;

        @Override
        public String toString() {
            return String.format(
                "FunctionApplication(fun=%s, args=%s)",
                functionId,
                arguments
            );
        }
    }

    @Value public static class BinOp extends Expression {
        String operator;
        Expression leftOperand;
        Expression rightOperand;

        @Override
        public String toString() {
            return String.format(
                "BinOp(%s,%s,%s)",
                leftOperand,
                operator,
                rightOperand
            );
        }

        public static BinaryOperator<Expression>
        partial(final String operator) {
            return (Expression l, Expression r) -> new BinOp(operator, l, r);
        }
    }

    @Value public static class UnOp extends Expression {
        String operator;
        Expression operand;
    }

    @Value public static class IsNullCheck extends Expression {
        Expression operand;
        boolean not;
    }

    public static <T> T matchExpression(
        Expression e,
        Function<Identifier, T> caseIdentifier,
        Function<NumConstant, T> caseNum,
        Function<StringConstant, T> caseString,
        Function<FunctionApplication, T> caseFunApp,
        Function<BinOp, T> caseBinOp,
        Function<UnOp, T> caseUnOp,
        Function<IsNullCheck, T> caseIsNullCheck
    ) {
        if (e instanceof Identifier) {
            return caseIdentifier.apply((Identifier) e);
        } else if (e instanceof NumConstant) {
            return caseNum.apply((NumConstant) e);
        } else if (e instanceof StringConstant) {
            return caseString.apply((StringConstant) e);
        } else if (e instanceof FunctionApplication) {
            return caseFunApp.apply((FunctionApplication) e);
        } else if (e instanceof BinOp) {
            return caseBinOp.apply((BinOp) e);
        } else if (e instanceof UnOp) {
            return caseUnOp.apply((UnOp) e);
            ///CLOVER:OFF
        } else if (e instanceof IsNullCheck) {
            ///CLOVER:ON
            return caseIsNullCheck.apply((IsNullCheck) e);
        } else {
            ///CLOVER:OFF
            throw new AssertionError(
                "Expression match not exhaustive: " + e.getClass()
            );
            ///CLOVER:ON
        }
    }
}
