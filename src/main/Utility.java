import static java.lang.Math.*;
import java.security.*;
import java.util.*;
import java.io.*;
import java.math.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.text.*;

// Common utility functions

public class Utility{
	
	public static double approxTime(Controllable startLocation, Locatable destination){
		if (startLocation == null)
			return 0.0;
		return approxTime((Sprite)startLocation, destination, startLocation.getAccel());
	}
	public static double approxTime(Sprite startLocation, Locatable destination, double accel){
		if (startLocation == null || destination == null)
			return 0.0;
		
		double d = startLocation.distance(destination);
		if (d <= 0.0)
			return 0;
		
		double v = startLocation.radVel(destination);
		v += 0.75*startLocation.tanSpeed(destination);
		
		double a = accel;
		double stopDist = v*v/(2*a);
		if (v > 0 || stopDist < d)
			a *= -1;
		
		double t0 = (-v*a + abs(a)*sqrt(v*v/2 - d*a))/(a*a);
		double t1 = (v + a*t0)/a;
		return t0 + t1;
	}
	/*public static double approxTime(Sprite startLocation, Locatable destination, double accel){
		if (startLocation == null || destination == null)
			return 0.0;
		
		double dist = startLocation.distance(destination);
		double speed = startLocation.speed(destination);
		double drdt = startLocation.radVel(destination);
		double a = accel;
		
		double ta = 0.75*speed/a;
		double tb = sqrt(8*abs(dist+drdt*ta/2)/a);
		return ta + tb;
	}*/
	
	public static String filter(String string, char[] validChars){
		StringBuilder filteredName = new StringBuilder(string);
		int index = 0;
		while (index < filteredName.length()){
			boolean isValid = false;
			for (int x = 0; x < validChars.length; x++)
				isValid = isValid || filteredName.charAt(index) == validChars[x];
			if (isValid){
				index++;
			}else
				filteredName.deleteCharAt(index);
		}
		return filteredName.toString();
	}
	
	public static String getHash(String input){
		try{
			MessageDigest algorithm = MessageDigest.getInstance("SHA");
			algorithm.update(input.getBytes(), 0, input.length());
		    return new BigInteger(1, algorithm.digest()).toString(16);
		}catch (Exception e){
			return null;
		}
	}
	
	public static Color getColor(double val, double grayness){
		if (val > 1.0)
			val = 1.0;
		else if (val < 0.0)
			val = 0.0;
		int gray = (int)(255*grayness);
		return new Color((int)(min(255, gray+255-510*(val-0.5))),
				(int)(min(255, gray+510*val)), gray);
	}
	
	public static boolean isPrintable(char c){
	    Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
	    return (!Character.isISOControl(c)) &&
	            c != KeyEvent.CHAR_UNDEFINED &&
	            block != null &&
	            block != Character.UnicodeBlock.SPECIALS;
	}
	
	public static Map<String, String> readDataFile(String file) throws IOException{
		Map<String, String> output = new TreeMap<String, String>();
		BufferedReader input = new BufferedReader(new FileReader(file));
		String property;
		while((property = input.readLine()) != null){
			if (!property.trim().isEmpty() && property.charAt(0) != '#'){
				int y = 1;
				while (property.charAt(y) != '='){
					if (y == property.length()-1){
						input.close();
						throw new IOException(file);
					}
					y++;
				}
				output.put(property.substring(0, y).trim().toLowerCase(), property.substring(y+1).trim());
			}
		}
		input.close();
		return output;
	}
	
	public static void drawOutlinedText(Graphics2D g, String text,
			int centerX, int centerY,
			Color centerColor, Color outlineColor){
		int posX = centerX - g.getFontMetrics().stringWidth(text)/2;
		int posY = centerY;// + g.getFontMetrics().getHeight()/5;
		g.setColor(outlineColor);
		for (int offsetX = -1; offsetX <= 1; offsetX++){
			for (int offsetY = -1; offsetY <= 1; offsetY++)
				g.drawString(text, posX+offsetX, posY+offsetY);
		}
		g.setColor(centerColor);
		g.drawString(text, posX, posY);
	}
	
	public static void setLengthLimit(JTextComponent component, int length){
		component.setDocument(new PlainDocument(){
			public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException{
				if (str == null)
					return;
				if ((getLength() + str.length()) <= length)
					super.insertString(offset, str, attr);
			}
		});
	}
	
	public static double fixAngle(double angle){
		return centerAbout(angle, 0);
	}
	
	public static double centerAbout(double angle, double centerAboutAngle){
		while (centerAboutAngle-angle > 180)
			angle += 360;
		while (centerAboutAngle-angle < -180)
			angle -= 360;
		return angle;
	}
	
	public static double clamp(double val, double max){
		if (val > max)
			val = max;
		if (val < -max)
			val = -max;
		return val;
	}
	
	
	// Simple method for finding polynomial zeros. Good enough for this purpose.
	static final double MAX_VAL = Main.TPS*60*4;
	static final double ITERATIONS = 15;
	static final double X_SCALE = Main.TPS;
	static final double INITIAL_INCREMENT = X_SCALE/15.0;
	public static double getZero(double[] coefs){
		double incrementAmount = INITIAL_INCREMENT;
		double x = 0, y = polyval(coefs, x); 
		
		boolean positive = y > 0;
		while (y > 0 == positive){
			incrementAmount += INITIAL_INCREMENT*(X_SCALE + x)/X_SCALE;
			
			x += incrementAmount;
			y = polyval(coefs, x);
			
			if (x > MAX_VAL)
				return Double.NaN;
		}
		
		double top, bottom;
		if (y > 0){
			top = x;
			bottom = x-incrementAmount;
		}else{
			top = x-incrementAmount;
			bottom = x;
		}
		for (int iteration = 0; iteration < ITERATIONS; iteration++){
			double middle = (top+bottom)/2;
			if (polyval(coefs, middle) > 0){
				top = middle;
			}else
				bottom = middle;
		}
		return (top+bottom)/2.0;
	}
	private static double polyval(double[] coefs, double pos){
    	double sum = 0;
    	double power = 1.0;
		for (int x = 0; x < coefs.length; x++){
			sum += coefs[x]*power;
			power *= pos;
		}
		return sum;
    }
	
}
