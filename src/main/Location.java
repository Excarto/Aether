
public class Location implements Locatable{
	
	private double posX, posY, velX, velY;
	
	public Location(){}
	
	public Location(double posX, double posY, double velX, double velY){
		setCoordinates(posX, posY, velX, velY);
	}
	
	public void setCoordinates(double posX, double posY, double velX, double velY){
		this.posX = posX;
		this.posY = posY;
		this.velX = velY;
		this.velY = velY;
	}
	
	public double getPosX(){
		return posX;
	}
	public double getPosY(){
		return posY;
	}
	public double getVelX(){
		return velX;
	}
	public double getVelY(){
		return velY;
	}
}
