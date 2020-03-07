import java.awt.*;

public abstract class Order{		
	protected static final int TRANSLUCENT_ALPHA = 75, OPAQUE_ALPHA = 170;
	
	protected Sprite host;
	protected int runTime;
	
	public void setHost(Controllable host){
		this.host = (Sprite)host;
		runTime = 0;
	}
	public void finish(){}
	
	public abstract void act();
	
	public void move(){
		runTime++;
	}
	
	public void recordPos(){}
	public double getRenderPosX(){
		return Double.NaN;
	}
	public double getRenderPosY(){
		return Double.NaN;
	}
	
	public Color getColor(){return null;}
}
