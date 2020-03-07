import static java.lang.Math.*;
import java.awt.*;

public class Target implements Locatable{
	public static Font unitLabelFont;
	
	private double posX, posY;
	private double velX, velY;
	private boolean targetVisible, targetScanned;
	private int lastTimeSighted;
	
	public final Controllable target;
	public final Player player;
	
	public double renderPosX, renderPosY;
	
	public Target(Controllable target, Player player){
		this.target = target;
		this.player = player;
		targetVisible = true;
	}
	
	public void update(){
		if (targetVisible){
			posX = ((Sprite)target).getPosX();
			posY = ((Sprite)target).getPosY();
			velX = ((Sprite)target).getVelX();
			velY = ((Sprite)target).getVelY();
		}else{
			posX += velX;
			posY += velY;
		}
	}
	
	public void recordPos(){
		renderPosX = posX;
		renderPosY = posY;
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
	
	public boolean isVisible(){
		return targetVisible;
	}
	
	public boolean isScanned(){
		return targetScanned;
	}
	
	public void updateVisible(){
		boolean wasVisible = targetVisible;
		targetVisible = player.visibleControllables.contains(target);
		if (wasVisible && !targetVisible)
			lastTimeSighted = Main.game.turn;
	}
	
	public void updateScanned(){
		if (targetVisible && target instanceof Unit){
			for (Controllable controllable : player.controllables){
				if (controllable instanceof Unit){
					for (System system : ((Unit)controllable).systems){
						if (system instanceof Scanner && ((Scanner)system).isScanned((Unit)target)){
							targetScanned = true;
							return;
						}
					}
				}
			}
		}
		targetScanned = false;
	}
	
	public int getHUDSize(double zoom){
		return 4+((Sprite)target).getRenderSize(zoom)*5/16;
	}
	
	public void draw(Graphics2D g, GameWindow window){
		if (target instanceof Unit){
			int size = getHUDSize(window.getRenderZoom());
			int length;
			if (targetVisible){
				length = max(1, size/10);
				g.setColor(new Color(255, 0, 0, 150));
			}else{
				int age = getAge();
				if (age >= Main.options.targetFadeTime)
					return;
				length = size;
				g.setColor(new Color(255, 0, 0, 255-250*age/Main.options.targetFadeTime));
			}
			
			int posX = window.posXOnScreen(renderPosX);
			int posY = window.posYOnScreen(renderPosY);
			g.drawLine(posX-size-length, posY-size+length, posX-size+length, posY-size-length);
			g.drawLine(posX+size-length+1, posY-size-length, posX+size+length+1, posY-size+length);
			g.drawLine(posX+size+length+1, posY+size-length+1, posX+size-length+1, posY+size+length+1);
			g.drawLine(posX-size+length, posY+size+length+1, posX-size-length, posY+size-length+1);
			
			if (!targetVisible){
				g.setFont(unitLabelFont);
				UnitType type = ((Unit)target).type;
				g.drawString(type.iconLabel, posX+1-type.iconLabelWidth/2, posY+5);
			}
		}
	}
	
	public int getAge(){
		return targetVisible ? 0 : Main.game.turn-lastTimeSighted;
	}
	
	public boolean equals(Object other){
		return target.equals(other) || super.equals(other);
	}
	
}
