package edu.kuczapski.turtlecut.scripting;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class GramarTest {
    public static void main(String[] args) throws Exception {
        // Create a CharStream from a string or file
        //CharStream input = CharStreams.fromString("3 + 4 * (5 - 2)");
    	CharStream input = CharStreams.fromFileName("examples/first.cut");

        // Create a lexer that feeds off the input CharStream
        
        TurtleLexer lexer = new TurtleLexer(input);

        // Create a buffer of tokens pulled from the lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Create a parser that feeds off the tokens buffer
        TurtleParser parser = new TurtleParser(tokens);

        // Begin parsing at the expression rule
        TurtleParser.ProgramContext tree = parser.program();

        // Print the parse tree
        System.out.println(tree.toStringTree(parser));
    }
}