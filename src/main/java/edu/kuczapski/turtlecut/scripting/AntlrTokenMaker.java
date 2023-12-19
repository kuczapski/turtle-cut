package edu.kuczapski.turtlecut.scripting;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.text.Segment;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenImpl;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerBase;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

public class AntlrTokenMaker extends TokenMakerBase {

	public static final TokenMakerFactory FACTORY = new TokenMakerFactory() {

		@Override
		public Set<String> keySet() {
			return new LinkedHashSet<>(Arrays.asList("Turtle Cut"));
		}

		@Override
		protected TokenMaker getTokenMakerImpl(String key) {
			return new AntlrTokenMaker();
		}
	};



	public Token toList(Segment text, int startOffset, List<org.antlr.v4.runtime.Token> antlrTokens) {
		if (antlrTokens.isEmpty()) {
			return null;
		} else {
			org.antlr.v4.runtime.Token at = antlrTokens.get(0);
			TokenImpl t = new TokenImpl(text, text.offset + at.getStartIndex(),
					text.offset + at.getStartIndex() + at.getText().length() - 1,
					startOffset + at.getStartIndex(), getType(at), 0);
			t.setNextToken( toList(text, startOffset, antlrTokens.subList(1, antlrTokens.size())));
			return t;
		}
	}

	private int getType(org.antlr.v4.runtime.Token at) {
		//return at.getType();
		switch(at.getType()) {
			case org.antlr.v4.runtime.Token.EOF: return -1; 
		
			case TurtleLexer.UNKNOWN: return Token.ERROR_CHAR;
			case TurtleLexer.NUM: return Token.LITERAL_NUMBER_FLOAT;
			case TurtleLexer.INT: return Token.LITERAL_NUMBER_DECIMAL_INT;
			
			case TurtleLexer.LINEKEY: 
			case TurtleLexer.CIRCLEKEY:
			case TurtleLexer.MOVETOKEY:
				   return Token.IDENTIFIER;
			
			case TurtleLexer.CUTKEY: 
			case TurtleLexer.DRAWKEY:
				   return Token.FUNCTION;
				   
			case TurtleLexer.REPEATKEY:
					return Token.RESERVED_WORD;	
			
			default:return  Token.COMMENT_KEYWORD;  // at.getType(); 
		}
		
		
	}

	@Override
	public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
		if (text == null) {
			throw new IllegalArgumentException();
		}

		CharStream input = CharStreams.fromString(text.toString());
		TurtleLexer lexer = new TurtleLexer(input);

		List<org.antlr.v4.runtime.Token> tokens = new LinkedList<>();
		while (!lexer._hitEOF) {
			tokens.add(lexer.nextToken());
		}

		return toList(text, startOffset, tokens);
	}
}

/*

class AntlrTokenMaker(val antlrLexerFactory : AntlrLexerFactory) : TokenMakerBase() {
    fun toList(text: Segment, startOffset: Int, antlrTokens:List<org.antlr.v4.runtime.Token>) : Token?{
        if (antlrTokens.isEmpty()) {
            return null
        } else {
            val at = antlrTokens[0]
            val t = TokenImpl(text, text.offset + at.startIndex, text.offset + at.startIndex + at.text.length - 1, startOffset + at.startIndex, at.type, 0)
            t.nextToken = toList(text, startOffset, antlrTokens.subList(1, antlrTokens.size))
            return t
        }
    }
    override fun getTokenList(text: Segment?, initialTokenType: Int, startOffset: Int): Token {
        if (text == null) {
            throw IllegalArgumentException()
        }
        val lexer = antlrLexerFactory.create(text.toString())
        val tokens = LinkedList<org.antlr.v4.runtime.Token>()
        while (!lexer._hitEOF) {
            tokens.add(lexer.nextToken())
        }
        return toList(text, startOffset, tokens) as Token
    }
}
 */