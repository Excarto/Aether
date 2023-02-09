import static java.lang.Math.*;
import java.util.*;
import java.awt.Graphics2D;
import java.awt.Color;

// Some enemy unit that has been detected by a sensor. Each SensorTarget contains at least one SensorSighting,
// with more SensorSightings on the same unit giving a more accurave estimate of the true unit posision.
// A unit can only have one associated SensorTarget for a given team, with different sensors aggregating to the same unit.

public class SensorTarget implements Locatable{
	
	public final Sprite sprite;
	private final List<SensorSighting> sightings;
	private double strength, posX, posY, velX, velY;
	private double renderPosX, renderPosY;
	
	public SensorTarget(Sprite sprite){
		this.sprite = sprite;
		sightings = new ArrayList<SensorSighting>();
	}
	
	public void act(){
		strength = posX = posY = velX = velY = 0.0;
		double weightSum = 0.0;
		for (int x = 0; x < sightings.size(); x++){
			SensorSighting sighting = sightings.get(x);
			sighting.act();
			if (sighting.strength <= 0){
				sightings.remove(sighting);
			}else{
				strength += sighting.strength;
				double weight = 0.5 + sighting.strength;
				weightSum += weight;
				posX += weight*sighting.posX;
				posY += weight*sighting.posY;
				velX += weight*sighting.velX;
				velY += weight*sighting.velY;
			}
		}
		posX /= weightSum;
		posY /= weightSum;
		velX /= weightSum;
		velY /= weightSum;
	}
	
	public void recordPos(){
		renderPosX = posX;
		renderPosY = posY;
	}
	
	public void draw(Graphics2D g, GameWindow window){
		g.setColor(Color.ORANGE);
		int posX = (int)window.posXOnScreen(renderPosX), posY = (int)window.posYOnScreen(renderPosY);
		int blipSize = 3 + (int)(650.0*window.getRenderZoom()*log1p(strength)/log(2));
		g.drawOval(posX-blipSize/2, posY-blipSize/2, blipSize, blipSize);
		window.drawPointerLine(g, posX, posY, null);
	}
	
	public void addSighting(SensorSighting sighting){
		SensorSighting weakest = null;
		int count = 0;
		for (SensorSighting existing : sightings){
			if (existing.source == sighting.source){
				count++;
				if (weakest == null || weakest.strength > existing.strength)
					weakest = existing;
			}
		}
		if (count >= 2)
			sightings.remove(weakest);
		sightings.add(sighting);
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
	
	public double getStrength(){
		return strength;
	}
	
}
