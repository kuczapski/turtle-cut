package edu.kuczapski.turtlecut.scripting;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;

public class FilteredTokenSource implements TokenSource {
	
	private TokenSource tokenSource;
	
	public FilteredTokenSource(TokenSource tokenSource) {
		this.tokenSource = tokenSource;
	}
	

	@Override
	public Token nextToken() {
	
		do {
			Token token = tokenSource.nextToken();
			if(token==null) return null;
			if(token.getType() == TurtleLexer.WS) continue;
			if(token.getType() == TurtleLexer.UNKNOWN) continue;
			return token;
		}while(true);
	}

	@Override
	public int getLine() {
		return tokenSource.getLine();
	}

	@Override
	public int getCharPositionInLine() {
		return tokenSource.getCharPositionInLine();
	}

	@Override
	public CharStream getInputStream() {
		return tokenSource.getInputStream();
	}

	@Override
	public String getSourceName() {
		return tokenSource.getSourceName();
	}

	@Override
	public void setTokenFactory(TokenFactory<?> factory) {
		tokenSource.setTokenFactory(factory);
	}

	@Override
	public TokenFactory<?> getTokenFactory() {
		// TODO Auto-generated method stub
		return null;
	}

}
