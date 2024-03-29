package edu.kuczapski.turtlecut.scripting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class GCodeGenerator {
	public static final String GCODE_HEADER =
			  "; Turtle CUT 1.0\r\n"
			+ "; GRBL device profile, absolute coords\n"
			+ "; Bounds: X{MINX} Y{MINY} to X{MAXX} Y{MAXY}\n"
			+"G00 G17 G40 G21 G54\n"
			+ "G90\n"
			+ "M4\n"
			+ "M8\n"
			+ "G28.2 XY\n"
			+ "G1 F150\n";
	
	public static final String GCODE_FOOTER = "M9\n"
			+ "G1S0\n"
			+ "M5\n"
			+ "G90\n"
			+ "G0 X0.025 Y0.025\n"
			+ "M2\n";
	
	public static final String CUT_CMD_SUFFIX = " S600";
	public static final String BURN_CMD_SUFFIX = " S100";
	
	
	private double maxWidth;
	private double maxHeight;
	
	private double currentWidth;
	private double currentHeight;
	
	private double currentXOffset;
	private double currentYOffset;
	
	
	private List<String> commands;
	
	private double minX;
	private double minY;
	private double maxX;
	private double maxY;
	
	public GCodeGenerator(double maxWidth, double maxHeight) {
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
	}
	
	public void init(double currentWidth, double currentHeight) {
		this.currentWidth = currentWidth;
		this.currentHeight = currentHeight;
		
		this.currentXOffset = (maxWidth - currentWidth)/2; 
		this.currentYOffset = (maxHeight - currentHeight)/2;
		
		minX = Double.POSITIVE_INFINITY;
		minY = Double.POSITIVE_INFINITY;
		maxX = Double.NEGATIVE_INFINITY;
		maxY = Double.NEGATIVE_INFINITY;
		
		commands = new ArrayList<>();
	}
	
	public String generate() {
		StringBuilder sb = new StringBuilder();
		sb.append( replaceVars(GCODE_HEADER));
		for (String command : commands) {
			sb.append(command);
			sb.append("\n");
		}
		sb.append(GCODE_FOOTER);
		return sb.toString();
	}
	
	private String replaceVars(String text) {
		return text.replace("{MINX}", getActualX(minX))
				   .replace("{MINY}", getActualY(minY))
				   .replace("{MAXX}", getActualX(maxX))
				   .replace("{MAXY}", getActualY(maxY));
	}

	public void writeToFiles(String fileName) throws IOException {
		String gcode = generate();
		FileUtils.writeStringToFile(new File(fileName), gcode);
	}
	
	private String getActualX(double x) {
		return format( x + currentXOffset);
	}
	
	private String getActualY(double y) {
		return format( y+ currentYOffset);
	}
	
	//format number with at most 2 digit
	private String format(double number) {
		return String.format("%.2f", number);
	}
	
	public void moveTo(double x, double y) {
		updateMinMax(x, y);
		commands.add("G0 X" + getActualX(x) + " Y" +  getActualY(y) + " S0");
	}
	

	public void cutTo(double x, double y) {
		updateMinMax(x, y);
		commands.add("G1 X" + getActualX(x) + " Y" + getActualY(y) + CUT_CMD_SUFFIX);
	}
	
	public void burnTo(double x, double y) {
		updateMinMax(x, y);
		commands.add("G1 X" + getActualX(x) + " Y" + getActualY(y) + BURN_CMD_SUFFIX);
	}
	
	public void stop() {
		commands.add("G1S0");
	}
	
	public void burnLine(double x1, double y1, double x2, double y2) {
		
		if(x1<0 || x1> currentWidth || x2<0 || x2> currentWidth || y1<0 || y1> currentHeight || y2<0 || y2> currentHeight) return;
		
		moveTo(x1, y1);
		burnTo(x2, y2);
		stop();
	}
	
	public void cutLine(double x1, double y1, double x2, double y2) {
		
		if(x1<0 || x1> currentWidth || x2<0 || x2> currentWidth || y1<0 || y1> currentHeight || y2<0 || y2> currentHeight) return;
		
		moveTo(x1, y1);
		cutTo(x2, y2);
		stop();
	}
	
	public void updateMinMax(double x, double y) {
		if (x < minX)
			minX = x;
		if (x > maxX)
			maxX = x;
		if (y < minY)
			minY = y;
		if (y > maxY)
			maxY = y;
	}
	
}
