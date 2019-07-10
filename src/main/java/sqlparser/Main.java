package sqlparser;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static sqlparser.Ast.Statement;

/**
 * Parses a file with code written in a very
 * restricted subset of SQL.
 */
public final class Main {

    /** Hidden constructor. */
    private Main() { /* nothing to do. */ }

    /**
     * Expects single argument (file path),
     * parses the content of the file,
     * prints the AST or an error message.
     *
     * @param args command line arguments (single
     *             file path expected).
     */
    public static void main(final String[] args) {
        boolean useDotRenderer = false;
        if (args.length != 2) {
            System.out.println("Wrong number of arguments: " + args.length);
            System.out.println("Expected exactly 2.");
            printHelp();
            System.exit(1);
        } else if (args[0].equals("-dot")) {
            useDotRenderer = true;
        } else if (args[0].equals("-text")) {
            useDotRenderer = false;
        } else if (args[0].equals("-help") || args[0].equals("--help")) {
            printHelp();
            System.exit(0);
        } else {
            System.out.println("Unknown output format: " + args[0]);
            printHelp();
            System.exit(2);
        }

        final Function<List<Statement>, String> renderer =
            useDotRenderer ?
            DotRenderer::render :
            (List<Statement> stmts) ->
                stmts
                .stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\n"));

        Path path = Paths.get(args[1]);
        if (Files.exists(path) && Files.isRegularFile(path)) {
            try {
                String input = new String(Files.readAllBytes(path), UTF_8);
                int exitCode =
                    SqlTokenizer
                    .tokenize(args[1], input)
                    .flatMap(SqlParser::parse)
                    .fold(
                        (List<ErrorMessage> errors) -> {
                            for (ErrorMessage e: errors) {
                                System.out.println(e.formatMavenStyle());
                            }
                            System.out.printf(
                                "There were %d errors\n",
                                errors.size()
                            );
                            return 101;
                        },
                        (List<Statement> statements) -> {
                            System.out.println(renderer.apply(statements));
                            return 0;
                        }
                    );
                System.exit(exitCode);
            } catch (IOException e) {
                System.out.println("Error occurred while reading input file: ");
                System.out.println(e.getMessage());
                System.exit(4);
            }
        } else {
            System.out.println("Input file does not exist: `" + args[1] + "`");
            System.exit(3);
        }
    }
    
    /** Prints help. */
    private static void printHelp() {
        System.out.println(
            "An SQL parser in vacuum. Parses SQL-subset, generates ASTs. \n" +
            "Usage:\n" +
            "\n" +
            "    java -cp <PATH_TO_JAR> sqlparser.Main" +
            " <OUTPUT_FORMAT> <INPUT_FILE>\n" +
            "\n" +
            "where <OUTPUT_FORMAT> is one of:\n" +
            "\n" +
            "    -dot          (for generating DOT graph code)\n" +
            "    -text         (for raw text representation of the ASTs) \n" +
            "\n" +
            "and <INPUT_FILE> is the path to input file.\n"
        );
    }
}

