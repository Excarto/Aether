import static java.lang.Math.*;

public class SensorSighting{
	public double posX, posY, velX, velY;
	public double strength, decayRate;
	public final System source;
	
	public SensorSighting(double strength, System source, Locatable target,
			double minPosAccuracy, double minVelAccuracy, double timeToLive){
		double posInaccuracy = (1-minPosAccuracy)*(1-strength);
		double posAngleRand = (2*random()-1)*posInaccuracy;
		double posAngle = source.unit.heading(target)+posAngleRand*180;
		double posRadiusRand = (2*random()-1)*posInaccuracy;
		double posRadius = source.unit.distance(target)*(1+posRadiusRand)/(1-posRadiusRand);
		//Test.p(posInaccuracy+" "+posAngleRand+" "+posAngle+" "+posRadiusRand+" "+posRadius+" "+source.distance(target)+" "+(1+posRadiusRand)/(1-posRadiusRand));
		double velInaccuracy = (1-minVelAccuracy)*(1-strength);
		double deltaVelAngle = (2*random()-1)*360;
		double deltaVelMagRand = (2*random()-1)*velInaccuracy;
		double deltaVelMag = source.unit.distance(target)*deltaVelMagRand/timeToLive;
		
		this.posX = source.getPosX()+posRadius*sin(toRadians(posAngle));
		this.posY = source.getPosY()-posRadius*cos(toRadians(posAngle));;
		this.velX = target.getVelX()+deltaVelMag*sin(toRadians(deltaVelAngle));
		this.velY = target.getVelY()-deltaVelMag*cos(toRadians(deltaVelAngle));
		this.strength = strength;
		this.decayRate = strength/timeToLive;
		this.source = source;
	}
	
	public void act(){
		posX += velX;
		posY += velY;
		
		strength -= decayRate;
	}
}
