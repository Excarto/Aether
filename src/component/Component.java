import static java.lang.Math.*;
import javax.swing.*;
import java.awt.image.*;
import java.io.*;

//Superclass of both Weapon and System

public abstract class Component implements Repairable, Id{
	
	private static short currentId;
	
	public final ComponentType type;
	public final Hardpoint hardpoint;
	public final Unit unit;
	
	private short id;
	private double hull;
	private boolean isActive;
	
	public boolean selected;
	
	public abstract void act();
	public abstract boolean isEngaged();
	public abstract JPanel getMenu();
	public abstract int getCost();
	public abstract int getMass();
	
	public Component(ComponentType type, Hardpoint hardpoint, Unit unit){
		this.type = type;
		this.unit = unit;
		this.hardpoint = hardpoint;
		hull = type.hull;
	}
	
	public void move(){
		if (isEngaged() && hull > 0  && unit.drainEnergy(type.powerUse, type.powerCategory(), type.name)){
			isActive = true;
			act();
		}else
			isActive = false;
	}
	
	public void recordPos(){}
	
	public void setId(){
		id = currentId++;
	}
	public short getId(){
		return id;
	}
	public static void resetId(){
		currentId = 0;
	}
	
	public double getHull(){
		return hull;
	}
	
	public void setHull(double hull){
		this.hull = hull;
	}
	
	public boolean isActive(){
		return isActive;
	}
	
	public BuyType getType(){
		return type;
	}
	
	public Player getPlayer(){
		return unit.player;
	}
	
	public BufferedImage getIcon(){
		return type.icon;
	}
	
	public void takeHit(double damage){
		if (hull > 0 && hull-damage <= 0){
			Main.game.createDeathExplosion(type, getPosX(), getPosY(), unit.getVelX(), unit.getVelY());
			hull = type.hull*Main.config.maxComponentDamage;
		}
		hull = max(hull-damage, type.hull*Main.config.maxComponentDamage);
	}
	
	public double getPosX(){
		return unit.getPosX() + (hardpoint.getRotatedPosX(unit.getAngle())-0.5)*unit.renderable.size;
	}
	public double getPosY(){
		return unit.getPosY() + (hardpoint.getRotatedPosY(unit.getAngle())-0.5)*unit.renderable.size;
	}
	/*public double getRenderPosX(){
		return unit.renderPosX + (hardpoint.getRotatedPosX(unit.renderAngle)-0.5)*unit.getContactMap().length;
	}
	public double getRenderPosY(){
		return unit.renderPosY + (hardpoint.getRotatedPosY(unit.renderAngle)-0.5)*unit.getContactMap()[0].length;
	}*/
	
	public void initialize(){
		hull = type.hull;
		selected = true;
	}
	
	public void repair(double material, boolean isScrap){
		if (isScrap){
			hull -= material*type.hullPerMaterial/Main.config.scrapReturn;
		}else
			hull = min(type.hull, hull+material*type.hullPerMaterial);
	}
	
	public void write(BufferedWriter out) throws IOException{
		out.write(type.name+"\n");
	}
	
}
