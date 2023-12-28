package edu.kuczapski.turtlecut.scripting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

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
import edu.kuczapski.turtlecut.scripting.TurtleParser.SetCanvasContext;
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
	private double spentTimeBudgetSec = 0;
	private long startNanos;
	
	private double drawingSpeed = 20;
	private double turningSpeed = 4;
	private double maxCanvasWidthMM;
	private double maxCanvasHeightMM;
	private double minCanvasWidthMM;
	private double minCanvasHeightMM;
	
	public Cutter(double canvasWidthMM, double canvasHeightMM, double pixelSizeMM) {
		this.canvasWidthMM = canvasWidthMM;
		this.canvasHeightMM = canvasHeightMM;
		this.maxCanvasWidthMM = canvasWidthMM;
		this.maxCanvasHeightMM = canvasHeightMM;
		this.minCanvasWidthMM = canvasWidthMM / 5;
		this.minCanvasHeightMM = canvasHeightMM / 5;
		
		
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
	
	public void setCanvasSize(double canvasWidthMM, double canvasHeightMM) {

		boolean wasAnimating = animateDrawing;
		animateDrawing = false;

		try {
			if(canvasWidthMM>maxCanvasWidthMM)  canvasWidthMM = maxCanvasWidthMM;
			if(canvasHeightMM>maxCanvasHeightMM)  canvasHeightMM = maxCanvasHeightMM;
			if(canvasWidthMM<minCanvasWidthMM)  canvasWidthMM = minCanvasWidthMM;
			if(canvasHeightMM<minCanvasHeightMM)  canvasHeightMM = minCanvasHeightMM;

			this.canvasWidthMM = canvasWidthMM;
			this.canvasHeightMM = canvasHeightMM;
			clearCanvas();
		}finally {	
			animateDrawing = wasAnimating;
		}

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
	
	public void drawTurtle(Graphics2D graphics, Vector2D position, double angle) {
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
		this.drawingSpeed = speedMMPS;
		
		CharStream input = CharStreams.fromString(program);    
        TurtleLexer lexer = new TurtleLexer(input);
        lexer.addErrorListener(new ConsoleErrorListener());
        
        CommonTokenStream tokens = new CommonTokenStream(new FilteredTokenSource(lexer));
        
        TurtleParser parser = new TurtleParser(tokens);
        parser.addErrorListener(new ConsoleErrorListener());  
      
        
        TurtleParser.ProgramContext tree = parser.program();

        visitProgram(tree);

        if(!stopExecution) {
        	notifyDrawingListeners();
        }
        
     
	}
	
	
	private void notifyDrawingListeners() {
		synchronized (image) {
			BufferedImage image = deepCopy(this.image);
			drawTurtle((Graphics2D)image.getGraphics(),curPos, curAngle);
			if(drawingListener!=null) {
				drawingListener.accept(image);
			}
		}
	}

	private void clearCanvas() {
		
		image = new BufferedImage((int)(canvasWidthMM / pixelSizeMM), (int)(canvasHeightMM/pixelSizeMM), BufferedImage.TYPE_4BYTE_ABGR);
		
		this.graphics =  (Graphics2D) image.getGraphics();
		
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
		animateDrawing = false;
		clearCanvas();
		animateDrawing = drawingSpeed>0;
		startNanos = System.nanoTime();
		spentTimeBudgetSec = 0;
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
			
			turnTo(newAngle);
			//curAngle = newAngle;
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

	private void turnTo(double newAngle) {	
		
		double dif = newAngle-curAngle;
		
		if(dif>Math.PI) dif = dif - 2*Math.PI;
		if(dif<-Math.PI) dif = dif + 2*Math.PI;
		
		double _dif = dif;
		
		double prevAngle = curAngle;
		animate( Math.abs(dif)/turningSpeed, progress->{
			curAngle = prevAngle + _dif*progress;
		});
		
		curAngle = newAngle;
	}

	private void drawLine(Vector2D p1, Vector2D p2, Color color, Stroke stroke) {
		drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY(), color, stroke);
	}
	private void drawLine(double p1x, double p1y, double p2x, double p2y, Color color, Stroke stroke) {
		
		double timeNeeded = Math.sqrt((p2x-p1x)*(p2x-p1x) + (p2y-p1y)*(p2y-p1y)) / drawingSpeed;
		
		Stroke prevStroke = graphics.getStroke();
		Color prevColor = graphics.getColor();

		
		try {
			if(color!=null)	graphics.setColor(color);
			if(stroke!=null) graphics.setStroke(stroke);
			
			animate(timeNeeded, progress->{
				double p3x = p1x + (p2x - p1x) * progress;
				double p3y = p1y + (p2y - p1y) * progress;

				curPos = new Vector2D(p3x, p3y);

				graphics.drawLine( 
						(int)Math.round(p1x / pixelSizeMM), 
						(int)Math.round((canvasHeightMM - p1y) / pixelSizeMM),
						(int)Math.round(p3x / pixelSizeMM),
						(int)Math.round((canvasHeightMM - p3y) / pixelSizeMM)
						);

				notifyDrawingListeners();

				restoreCurrentState();
			});
			
			
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
	
		
		if(animateDrawing) notifyDrawingListeners();
	}
	private void animate(double timeNeeded, DoubleConsumer drawer) {
		if(animateDrawing) {
			double progress = 0;
			do {
				if(stopExecution) return;
				double timeElapsedSec = (System.nanoTime() - startNanos) / 1e9; 
				double elapsedLocalTimeBudget = timeElapsedSec - spentTimeBudgetSec;
				progress = elapsedLocalTimeBudget / timeNeeded;
				if(progress<0) progress = 0;
			
				if(progress<1.0) {
					storeCurrentState();
					
					drawer.accept(progress);
					
					notifyDrawingListeners();
					
					restoreCurrentState();
					
				}
				
			}while(progress<1.0);
			
			spentTimeBudgetSec += timeNeeded;
		}
	}
	private void restoreCurrentState() {
		// TODO Auto-generated method stub
		
		
	}

	private void storeCurrentState() {
		// TODO Auto-generated method stub
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
	
	@Override
	public Object visitSetCanvas(SetCanvasContext ctx) {
		if(ctx.length().size()==2) {
			setCanvasSize(visitLength(ctx.length(0)), visitLength(ctx.length(1)));
		}
		return null;
	}
	
	public static BufferedImage deepCopy(BufferedImage bi) {
		  ColorModel cm = bi.getColorModel();
		  boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		  WritableRaster raster = bi.copyData(bi.getRaster().createCompatibleWritableRaster());
		  return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
	
}
