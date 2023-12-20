package edu.kuczapski.turtlecut.scripting;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import edu.kuczapski.turtlecut.scripting.TurtleParser.CoordinateContext;
import edu.kuczapski.turtlecut.scripting.TurtleParser.CutContext;
import edu.kuczapski.turtlecut.scripting.TurtleParser.DegreesContext;
import edu.kuczapski.turtlecut.scripting.TurtleParser.DrawContext;
import edu.kuczapski.turtlecut.scripting.TurtleParser.EndCoordinateContext;
import edu.kuczapski.turtlecut.scripting.TurtleParser.LengthContext;
import edu.kuczapski.turtlecut.scripting.TurtleParser.LineContext;
import edu.kuczapski.turtlecut.scripting.TurtleParser.MovetoContext;
import edu.kuczapski.turtlecut.scripting.TurtleParser.ProgramContext;
import edu.kuczapski.turtlecut.scripting.TurtleParser.RepeateContext;
import edu.kuczapski.turtlecut.scripting.TurtleParser.StartCoordinateContext;

public class Cutter extends TurtleBaseVisitor<Object>{
	
	public static enum CursorState{
		IDLE, DRAWING, CUTTING
	}
	
	
	public static final Color WORKSHEET_COLOR = new Color(200, 180, 130); 
	public static final Color CUT_COLOR = new Color(0, 0, 0);
	public static final Color DRAW_COLOR = new Color(50*3, 30*3, 0*3);
	public static final Color MAJOR_GRID_COLOR = new Color(255,255,255,100); //new Color(200 * 2 / 3, 180 * 2 / 3, 130 * 2/3);
	public static final Color MINOR_GRID_COLOR = new Color(255,255,255,50); // new Color(200 * 5 / 6, 180 * 5 / 6, 130 * 6/6);
	
	
	private double canvasWidthMM;
	private double canvasHeightMM;
	private double pixelSizeMM;

	private BufferedImage image;


	private Graphics graphics;
	
	private Vector2D curPos;
	private double curAngle;
	
	private CursorState curState = CursorState.IDLE;

	
	
	public Cutter(double canvasWidthMM, double canvasHeightMM, double pixelSizeMM) {
		this.canvasWidthMM = canvasWidthMM;
		this.canvasHeightMM = canvasHeightMM;
		this.pixelSizeMM = pixelSizeMM;
		
		image = new BufferedImage((int)(canvasWidthMM / pixelSizeMM), (int)(canvasHeightMM/pixelSizeMM), BufferedImage.TYPE_4BYTE_ABGR);
		
		this.graphics =  image.getGraphics();
		
		clearCanvas();

	}
	
	public void requestImage(Consumer<BufferedImage> imgConsumer) {
		synchronized (image) {
			imgConsumer.accept(image);
		}
	}
	
	public void execute(String program, double speedMMPS, int curentLine) {
		
		CharStream input = CharStreams.fromString(program);    
        TurtleLexer lexer = new TurtleLexer(input);
        lexer.addErrorListener(new ConsoleErrorListener());
        
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        
        TurtleParser parser = new TurtleParser(tokens);
        parser.addErrorListener(new ConsoleErrorListener());  
      
        
        TurtleParser.ProgramContext tree = parser.program();
        
        System.out.println(tree.toStringTree());

        visitProgram(tree);
	}
	private void clearCanvas() {
		this.graphics.setColor(WORKSHEET_COLOR);
		this.graphics.fillRect(0, 0, image.getWidth(), image.getHeight());	
		this.curPos = new Vector2D(canvasWidthMM / 2,  canvasHeightMM / 2);
		
		
		for(int x=0;x<canvasWidthMM;x+=1) {
			graphics.setColor( x%10 == 0 ? MAJOR_GRID_COLOR : MINOR_GRID_COLOR);
			int px= (int) Math.round(x/pixelSizeMM);
			graphics.drawLine( px, 0, px, image.getHeight());
		}
		
		for(int y=0;y<canvasHeightMM;y+=1) {
			graphics.setColor( y%10 == 0 ? MAJOR_GRID_COLOR : MINOR_GRID_COLOR);
			int py= (int) Math.round(y/pixelSizeMM);
			graphics.drawLine( 0, py, image.getWidth(), py);
		}
	}
	
	@Override
	public Object visitProgram(ProgramContext ctx) {
		clearCanvas();
		return super.visitProgram(ctx);
	}


	@Override
	public Object visitRepeate(RepeateContext ctx) {
		
		TerminalNode countToken = ctx.NUM();
		
		int count = Integer.parseInt(countToken.getText());
		
		for(int i=0;i<count;i++) {
			visitCommandBlock(ctx.commandBlock());
		}
		
		return null;
	}
	
	@Override
	public Object visitCut(CutContext ctx) {
		CursorState prevState = curState;
		try {
			curState = CursorState.CUTTING;
			return super.visitCut(ctx);
		}finally {
			curState = prevState;
		}
	}

	@Override
	public Object visitDraw(DrawContext ctx) {
		CursorState prevState = curState;
		try {
			curState = CursorState.DRAWING;
			return super.visitDraw(ctx);
		}finally {
			curState = prevState;
		}
	}
	
	@Override
	public Object visitLine(LineContext ctx) {
		System.out.println("drawing line");
		
		curPos = visitStartCoordinate(ctx.startCoordinate());
		
		Vector2D endPos = visitEndCoordinate(ctx.endCoordinate());
		
		System.out.println("Drawing line: "+curPos+" - "+endPos);
		
		Vector2D dir = endPos.subtract(curPos);
		double newAngle =  Math.atan2(dir.getY(), dir.getX());
		if(Double.isFinite(newAngle)) {
			curAngle = newAngle;
		}
		
		
		if(curState == CursorState.CUTTING || curState == CursorState.DRAWING) {
			synchronized (image) {

				graphics.setColor(getCurentDrawingColor());
				graphics.drawLine( 
						(int)Math.round(curPos.getX() / pixelSizeMM), 
						(int)Math.round(curPos.getY() / pixelSizeMM),
						(int)Math.round(endPos.getX() / pixelSizeMM),
						(int)Math.round(endPos.getY() / pixelSizeMM)
						);

			}
		}
		
		curPos = endPos;
		
		
		return null;
	}
	
	private Color getCurentDrawingColor() {
		switch (curState) {
			case CUTTING: return CUT_COLOR;
			case DRAWING: return DRAW_COLOR;
	
			default: return new Color(0, true);
		}
	}

	@Override
	public Object visitMoveto(MovetoContext ctx) {
		curPos = visitCoordinate(ctx.coordinate());
		return null;
	}
	
	
	@Override
	public Vector2D visitCoordinate(CoordinateContext ctx) {
		return new Vector2D(
					Double.parseDouble(ctx.NUM(0).getText())*10,
					Double.parseDouble(ctx.NUM(1).getText())*10
				);
	}
	
	@Override
	public Vector2D visitStartCoordinate(StartCoordinateContext ctx) {
		if(ctx!=null &&  ctx.coordinate()!=null) {
			return visitCoordinate(ctx.coordinate());
		}else {
			return curPos;
		}
	}
	
	
	@Override
	public Vector2D visitEndCoordinate(EndCoordinateContext ctx) {
		if( ctx.coordinate()!=null) {
			return visitCoordinate(ctx.coordinate());
		}else {
			double degrees = visitDegrees(ctx.degrees());
			visitLength(ctx.length());
			double length = visitLength(ctx.length());
			
			double dirAngle = curAngle + Math.toRadians(degrees);
			
			return curPos.add(new Vector2D(length * Math.cos(dirAngle), length*Math.sin(dirAngle)));
		}
	}
	
	@Override
	public Double visitLength(LengthContext ctx) {
		  double lenght = Double.parseDouble(ctx.NUM().toString());
		
		  if(ctx.children.size()>1) {
			  ParseTree uom = ctx.children.get(1);
			  switch (uom.getText().toLowerCase()) {
				case "mm": lenght *= 1;
				break;
				case "cm": lenght *= 10;
				break;
				
				default: lenght *= 10;
			  }
		  }
		  
		  return lenght;
	}
	
	@Override
	public Double visitDegrees(DegreesContext ctx) {
		if(ctx.NUM()!=null) {
			return Double.parseDouble(ctx.NUM().getText());
		}else {
			 switch(ctx.getText().toLowerCase()) {
			 	case "stanga": return 90.0;
			 	case "dreapta": return -90.0;
			 	case "inainte": return 0.0;
			 	default: throw new IllegalArgumentException("Unknown direction: "+ctx.start.toString());
			 }
		}
		
	}
	
}
