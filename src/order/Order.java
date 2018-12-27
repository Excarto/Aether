import java.awt.*;

public abstract class Order{		
	protected static final int TRANSPARENT = 75, OPAQUE = 170;
	
	protected Sprite host;
	public int time;
	
	public void setHost(Controllable host){
		this.host = (Sprite)host;
	}
	public void finish(){}
	
	public abstract void act();
	
	public void move(){}
	
	public void recordPos(){}
	public double getRenderPosX(){
		return Double.NaN;
	}
	public double getRenderPosY(){
		return Double.NaN;
	}
	
	public Color getColor(){return null;}
}
