package sqlparser;

import java.util.*;
import java.util.function.*;

import static sqlparser.Ast.*;

class DotRenderer {

    private final LinkedList<String> nodes = new LinkedList<>();
    private final LinkedList<String> edges = new LinkedList<>();
    private final Supplier<String> idSupp = new Supplier<String>() {
        private long id = 0;
        public String get() {
            id++;
            return "n" + id;
        }
    };

    /**
     * Instances of this class act only as closures for the
     * hash sets and node-id suppliers, they should be created only
     * during the invocation of the static <code>render</code> method.
     */
    private DotRenderer() {
        /* Nothing to do. Start with empty data structures. */
    }

    private String renderStatements(List<Statement> statements) {

        nodes.add("root [style=invisible];");
        StringBuilder invisibleConnections = new StringBuilder("root");
        for (Statement s: statements) {
            String stmtNodeId = renderStatement(s);
            invisibleConnections.append(" -> ");
            invisibleConnections.append(stmtNodeId);
        }
        invisibleConnections.append(" [style=invis];");



        StringBuilder bldr = new StringBuilder();
        bldr.append(
            "/* DOT vizgraph file with an AST-forest" +
            " of the parsed SQL statements. */\n"
        );
        bldr.append("/* To generate a .PNG image of the AST, run: */\n");
        bldr.append("/* dot -Tpng <thisFile> -o <outputFile.png> */\n");
        bldr.append("\n");
        bldr.append("digraph {\n  rankdir=LR;\n");
        for (String nodeDecl: nodes) {
            bldr.append("  ");
            bldr.append(nodeDecl);
            bldr.append("\n");
        }
        for (String edgeDecl: edges) {
            bldr.append("  ");
            bldr.append(edgeDecl);
            bldr.append("\n");
        }

        bldr.append("  {\n    rank = same;\n");
        bldr.append("    ");
        bldr.append(invisibleConnections.toString());
        bldr.append("\n    rankdir=TB;\n  }\n");

        bldr.append("}\n");

        return bldr.toString();
    }

    /**
     * Performs basic escaping of special symbols in labels.
     *
     * @param label node or edge label, possibly with backslashes and quotes.
     * @return escaped string that can be enclosed in quotes and used as DOT
     *         string literal.
     */
    private static String escape(String label) {
        return label
            .replaceAll("\\\\", "\\\\\\\\")
            .replaceAll("\"", "\\\\\"")
            .replaceAll("\n", "\\\\n")
            .replaceAll("\r", "\\\\r")
            .replaceAll("\f", "\\\\f")
            .replaceAll("\b", "\\\\b");
    }

    /**
     * Generates an edge statement.
     *
     * @param nodeIdA source node.
     * @param nodeIdB target node.
     * @param label edge label.
     * @return semicolon-terminated DOT statement that generates the edge.
     */
    private static String edge(String nodeIdA, String nodeIdB, String label) {
        String escaped = escape(label);
        return nodeIdA + " -> " + nodeIdB + "[label=\"" + escaped + "\"];";
    }

    private static String node(String nodeId, String shape, String label) {
        String escaped = escape(label);
        return String.format(
            "%s [shape=%s,label=\"%s\"];",
            nodeId,
            shape,
            escape(label)
        );
    }

    private static String boxNode(String nodeId, String label) {
        return node(nodeId, "box", label);
    }

    private static String ellipseNode(String nodeId, String label) {
        return node(nodeId, "ellipse", label);
    }

    private static String circleNode(String nodeId, String label) {
        return node(nodeId, "circle", label);
    }

    private String renderStatement(Statement s) {
        final String thisNodeId = idSupp.get();
        matchStatement(
            s,
            (Use use) -> {
                nodes.add(boxNode(thisNodeId, "USE"));
                edges.add(edge(
                    thisNodeId,
                    renderString(use.getDatabaseName()),
                    "databaseName"
                ));
                return (Void) null;
            },
            (Select sel) -> {
                nodes.add(boxNode(thisNodeId, "SELECT"));

                edges.add(edge(
                    thisNodeId,
                    renderExpressionList(sel.getSelectList()),
                    "selectList"
                ));

                sel.getFromTableId().ifPresent((TableId tid) -> {
                    edges.add(edge(thisNodeId, renderTableId(tid), "from"));
                });

                sel.getWhereCondition().ifPresent((Expression w) -> {
                    edges.add(edge(thisNodeId, renderExpression(w), "where"));
                });

                sel.getOrderBy().ifPresent((Expression o) -> {
                    edges.add(edge(thisNodeId, renderExpression(o), "orderBy"));
                });

                return (Void) null;
            },
            (Insert ins) -> {
                nodes.add(boxNode(thisNodeId, "INSERT"));
                edges.add(edge(
                    thisNodeId,
                    renderTableId(ins.getIntoTable()),
                    "into"
                ));
                edges.add(edge(
                    thisNodeId,
                    renderStringList(ins.getColumnList()),
                    "columns"
                ));
                edges.add(edge(
                    thisNodeId,
                    renderExpressionList(ins.getValues()),
                    "values"
                ));

                return (Void) null;
            },
            (Delete del) -> {
                nodes.add(boxNode(thisNodeId, "DELETE"));
                edges.add(edge(
                    thisNodeId,
                    renderTableId(del.getFromTable()),
                    "from"
                ));
                edges.add(edge(
                    thisNodeId,
                    renderExpression(del.getWhereCondition()),
                    "where"
                ));

                return (Void) null;
            }
        );

        return thisNodeId;
    }

    private String renderExpression(Expression expr) {
        final String thisNodeId = idSupp.get();
        matchExpression(
            expr,
            (Identifier id) -> {
                nodes.add(boxNode(thisNodeId, id.getValue()));
                return (Void) null;
            },
            (NumConstant num) -> {
                nodes.add(ellipseNode(thisNodeId, "" + num.getValue()));
                return (Void) null;
            },
            (StringConstant str) -> {
                nodes.add(boxNode(thisNodeId, str.getValue()));
                return (Void) null;
            },
            (FunctionApplication funApp) -> {
                nodes.add(ellipseNode(thisNodeId, funApp.getFunctionId()));
                String argsNodeId = renderExpressionList(funApp.getArguments());
                edges.add(edge(thisNodeId, argsNodeId, "args"));
                return (Void) null;
            },
            (BinOp binOp) -> {
                String leftNode = renderExpression(binOp.getLeftOperand());
                String rightNode = renderExpression(binOp.getRightOperand());
                nodes.add(circleNode(thisNodeId, binOp.getOperator()));
                edges.add(edge(thisNodeId, leftNode, "first"));
                edges.add(edge(thisNodeId, rightNode, "second"));
                return (Void) null;
            },
            (UnOp unOp) -> {
                String opNode = renderExpression(unOp.getOperand());
                nodes.add(circleNode(thisNodeId, unOp.getOperator()));
                edges.add(edge(thisNodeId, opNode, ""));
                return (Void) null;
            },
            (IsNullCheck inc) -> {
                String opNode = renderExpression(inc.getOperand());
                nodes.add(ellipseNode(
                    thisNodeId,
                    inc.isNot() ? "IS NOT NULL" : "IS NULL"
                ));
                edges.add(edge(thisNodeId, opNode, ""));
                return (Void) null;
            }
        );
        return thisNodeId;
    }

    private String renderExpressionList(List<Expression> expressions) {
        final String listNodeId = idSupp.get();
        nodes.add(boxNode(listNodeId, "[]"));
        int idx = 0;
        for (Expression e: expressions) {
            edges.add(edge(listNodeId, renderExpression(e), "" + idx));
            idx++;
        }
        return listNodeId;
    }

    private String renderString(String str) {
        final String nodeId = idSupp.get();
        nodes.add(boxNode(nodeId, str));
        return nodeId;
    }

    private String renderStringList(List<String> strings) {
        final String listNodeId = idSupp.get();
        nodes.add(boxNode(listNodeId, "[]"));
        int idx = 0;
        for (String s: strings) {
            edges.add(edge(listNodeId, renderString(s), "" + idx));
            idx++;
        }
        return listNodeId;
    }

    private String renderTableId(TableId tableId) {
        final String thisNodeId = idSupp.get();
        nodes.add(boxNode(thisNodeId, "TableId"));
        tableId.getDatabaseName().ifPresent((String n) -> {
            edges.add(edge(thisNodeId, renderString(n), "database"));
        });
        edges.add(edge(
            thisNodeId,
            renderString(tableId.getTableName()),
            "table"
        ));
        return thisNodeId;
    }

    /** Renders ASTs in DOT graph format. */
    public static String render(List<Statement> statements) {
        return new DotRenderer().renderStatements(statements);
    }
}
