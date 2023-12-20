package edu.kuczapski.turtlecut.scripting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
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
	
	private static final BasicStroke HIGHLIGHT_STROKE = new BasicStroke(4.0f);
	private static final BasicStroke DEFAULT_STROKE = new BasicStroke(0);
	
	private BufferedImage turtleImage;

	public enum CursorState{
		IDLE, DRAWING, CUTTING
	}
	
	public static final Color WORKSHEET_COLOR = new Color(200, 180, 130); 
	public static final Color CUT_COLOR = new Color(0, 0, 0);
	public static final Color DRAW_COLOR = new Color(50*3, 30*3, 0*3);
	public static final Color MAJOR_GRID_COLOR = new Color(255,255,255,100); 
	public static final Color MINOR_GRID_COLOR = new Color(255,255,255,50); 
	
	private double canvasWidthMM;
	private double canvasHeightMM;
	private double pixelSizeMM;

	private BufferedImage image;

	private Graphics2D graphics;
	
	private Vector2D curPos;
	private double curAngle;
	
	private CursorState curState = CursorState.IDLE;
	
	private Consumer<BufferedImage> drawingListener = null;

	private boolean stopExecution = false;
	private int currentLine;
	
	private boolean animateDrawing = false;
	private double drawnLenght = 0;
	private long startNanos;
	private long drawingSpeed;
	
	public Cutter(double canvasWidthMM, double canvasHeightMM, double pixelSizeMM) {
		this.canvasWidthMM = canvasWidthMM;
		this.canvasHeightMM = canvasHeightMM;
		this.pixelSizeMM = pixelSizeMM;
		
		try {
			this.turtleImage = ImageIO.read(new File("turtle.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		image = new BufferedImage((int)(canvasWidthMM / pixelSizeMM), (int)(canvasHeightMM/pixelSizeMM), BufferedImage.TYPE_4BYTE_ABGR);
		
		this.graphics =  (Graphics2D) image.getGraphics();
		
		clearCanvas();

	}
	
	public void setStopExecution() {
		this.stopExecution = true;
	}
	
	public void setDrawingListener(Consumer<BufferedImage> drawingListener) {
		this.drawingListener = drawingListener;
	}
	
	public void requestImage(Consumer<BufferedImage> imgConsumer) {
		synchronized (image) {
			imgConsumer.accept(image);
		}
	}
	
	public void drawTurtle(Vector2D position, double angle) {
		AffineTransform prevTransform = graphics.getTransform();
		try {
			int px = (int) (position.getX() / pixelSizeMM);
			int py = (int) ((canvasHeightMM - position.getY()) / pixelSizeMM);
			AffineTransform affineTransform = new  AffineTransform();
			affineTransform.translate(px, py);
			affineTransform.rotate(-(angle - Math.PI / 2));
			graphics.setTransform(affineTransform);
			graphics.drawImage(turtleImage, -turtleImage.getWidth()/2, -turtleImage.getHeight()/2, null);
		}finally {
			graphics.setTransform(prevTransform);
		}
		
	}
	
	public void execute(String program, double speedMMPS, int currentLine) {
		
		this.stopExecution = false;
		this.currentLine = currentLine;
		
		CharStream input = CharStreams.fromString(program);    
        TurtleLexer lexer = new TurtleLexer(input);
        lexer.addErrorListener(new ConsoleErrorListener());
        
        CommonTokenStream tokens = new CommonTokenStream(new FilteredTokenSource(lexer));
        
        TurtleParser parser = new TurtleParser(tokens);
        parser.addErrorListener(new ConsoleErrorListener());  
      
        
        TurtleParser.ProgramContext tree = parser.program();

        visitProgram(tree);

        drawTurtle(curPos, curAngle);
        
        //drawTurtle(graphics, 100, 100, 0, 0);
        notifyDrawingListeners();
        
     
	}
	
	
	private void notifyDrawingListeners() {
		synchronized (image) {
			if(drawingListener!=null) {
				drawingListener.accept(image);
			}
		}
	}

	private void clearCanvas() {
		this.graphics.setColor(WORKSHEET_COLOR);
		this.graphics.fillRect(0, 0, image.getWidth(), image.getHeight());	
		this.curPos = new Vector2D(canvasWidthMM / 2,  canvasHeightMM / 2);
		this.curAngle = Math.PI / 2;
		
		
		for(int x=0;x<canvasWidthMM;x+=1) {
			drawLine(x, 0, x, canvasHeightMM, x%10 == 0 ? MAJOR_GRID_COLOR : MINOR_GRID_COLOR, null);
		}
		
		for(int y=0;y<canvasHeightMM;y+=1) {
			drawLine( 0, y, canvasWidthMM, y , y%10 == 0 ? MAJOR_GRID_COLOR : MINOR_GRID_COLOR, null);
		}
	}
	
	@Override
	public Object visitProgram(ProgramContext ctx) {
		if(stopExecution) return null;
		
		clearCanvas();
		return super.visitProgram(ctx);
	}

	@Override
	protected boolean shouldVisitNextChild(RuleNode node, Object currentResult) {
		if(stopExecution) return false;
		return super.shouldVisitNextChild(node, currentResult);
	}

	@Override
	public Object visitRepeate(RepeateContext ctx) {
		if(stopExecution) return null;
		
		TerminalNode countToken = ctx.NUM();
		
		int count = Integer.parseInt(countToken.getText());
		
		for(int i=0;i<count;i++) {
			visitCommandBlock(ctx.commandBlock());
		}
		
		return null;
	}
	
	@Override
	public Object visitCut(CutContext ctx) {
		if(stopExecution) return null;
		
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
		if(stopExecution) return null;
		
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
		if(stopExecution) return null;
		
		curPos = visitStartCoordinate(ctx.startCoordinate());
		
		Vector2D endPos = visitEndCoordinate(ctx.endCoordinate());
		
		Vector2D dir = endPos.subtract(curPos);
		double newAngle =  Math.atan2(dir.getY(), dir.getX());
		if(Double.isFinite(newAngle)) {
			curAngle = newAngle;
		}
			
		if(curState == CursorState.CUTTING || curState == CursorState.DRAWING) {
			synchronized (image) {
				if(ctx.getStart().getLine()<=currentLine && ctx.getStop().getLine()>=currentLine) {
					drawLine(curPos, endPos, getCurentDrawingColor(), HIGHLIGHT_STROKE);
				}else {
					drawLine(curPos, endPos, getCurentDrawingColor(), DEFAULT_STROKE);
				}
			}
		}
		
		curPos = endPos;
			
		return null;
	}

	private void drawLine(Vector2D p1, Vector2D p2, Color color, Stroke stroke) {
		drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY(), color, stroke);
	}
	private void drawLine(double p1x, double p1y, double p2x, double p2y, Color color, Stroke stroke) {
		
		
		
		Stroke prevStroke = graphics.getStroke();
		Color prevColor = graphics.getColor();
		
		
		try {
			if(color!=null)	graphics.setColor(color);
			if(stroke!=null) graphics.setStroke(stroke);
			
			graphics.drawLine( 
					(int)Math.round(p1x / pixelSizeMM), 
					(int)Math.round((canvasHeightMM - p1y) / pixelSizeMM),
					(int)Math.round(p2x / pixelSizeMM),
					(int)Math.round((canvasHeightMM - p2y) / pixelSizeMM)
			);
		}finally {
			graphics.setColor(prevColor);
			graphics.setStroke(prevStroke);
		}
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
