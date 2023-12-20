package edu.kuczapski.turtlecut.scripting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

public class GramarTest {
    public static void main(String[] args) throws Exception {
    	
    	String program = new String( Files.readAllBytes(Path.of("examples/first.cut")));
    	
    	Cutter cutter = new Cutter(400, 300, 0.5);
    	
    	cutter.execute(program, 0, 0);
    	
    	
    	cutter.requestImage(img->{
    		try {
				ImageIO.write(img,"png",new File("grammar-test.png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
    	});
    	
    	Thread.sleep(1000);
    	
    	System.out.println("Done.");
    	
//        // Create a CharStream from a string or file
//        //CharStream input = CharStreams.fromString("3 + 4 * (5 - 2)");
//    	CharStream input = CharStreams.fromFileName("examples/first.cut");
//
//        // Create a lexer that feeds off the input CharStream
//        
//        TurtleLexer lexer = new TurtleLexer(input);
//
//        // Create a buffer of tokens pulled from the lexer
//        CommonTokenStream tokens = new CommonTokenStream(lexer);
//
//        // Create a parser that feeds off the tokens buffer
//        TurtleParser parser = new TurtleParser(tokens);
//
//        // Begin parsing at the expression rule
//        TurtleParser.ProgramContext tree = parser.program();
//
//        // Print the parse tree
//        System.out.println(tree.toStringTree(parser));
    }
}