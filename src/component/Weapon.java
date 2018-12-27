import static java.lang.Math.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.awt.*;

public abstract class Weapon extends Component{
	public final static int ARC_INCREMENT = 5, MIN_ARC = 1;
	public final static double AUTO_ANGLE = Double.POSITIVE_INFINITY;
	public enum FireMode{DISABLED, MANUAL, AUTOMATIC, AUTONOMOUS};
	private final static int TURNS_CALC_ANGLE = 1;
	
	static WeaponPanel weaponPanel;
	static int weaponCounter = 0;
	static final Random random = new Random();
	
	public final WeaponType type;
	public final WeaponHardpoint hardpoint;
	
	public boolean autoMissiles, autoCraft, autoShips;
	public int timePassed;
	
	public FireMode mode;
	
	private int arc, mountAngle;
	private double mountAngleFrac;
	private double angle;
	private int timeToLoaded;
	private double targetAngle, targetDist;
	private boolean hasTarget;
	private boolean manualAim;
	private Target autoTarget, overrideTarget;
	private Component subTarget;
	private boolean isReady, isInRange, triggerPulled;
	private FireWeaponMsg fireMsg;
	private int lastAngleUpdateTime;
	
	protected abstract double calcTargetAngle();
	protected abstract double approxTime(Locatable locatable);
	protected abstract double approxAngle(Locatable sprite, double time);
	protected abstract boolean inRange(Locatable target);
	protected abstract boolean inManualRange(double dist);
	
	public Weapon(WeaponType type, WeaponHardpoint hardpoint, Unit unit, int arc, double mountAngleFrac){
		super(type, hardpoint, unit);
		this.type = type;
		this.hardpoint = hardpoint;
		setArc(arc);
		setMountAngle(mountAngleFrac);
		angle = mountAngle;
		
		mode = type.defaultMode;
		autoMissiles = type.defaultAutoMissiles;
		autoCraft = type.defaultAutoCraft;
		autoShips = type.defaultAutoShips;
		
		timePassed = weaponCounter++;//(int)(random()*100);
	}
	
	public void act(){
		timePassed++;
		
		if (mode == FireMode.DISABLED)
			return;
		
		boolean newManualAim = unit.player.manualAim(unit) && selected;
		if (manualAim != newManualAim)
			isInRange = false;
		manualAim = newManualAim;
		hasTarget = findTarget();
		
		boolean firePressed = false;
		if (unit.player instanceof HumanPlayer && selected){
			GameWindow gameWindow = ((HumanPlayer)unit.player).getWindow();
			firePressed = gameWindow.isSelected(unit) && (
					gameWindow.getInputHandler().controlPressed(Control.FIRE) ||
					(manualAim && gameWindow.getInputHandler().isMousePressed()));
		}
		
		if (hasTarget){
			if (timePassed%(isInRange ? TURNS_CALC_ANGLE : 2*TURNS_CALC_ANGLE) == 0){
				if (manualAim){
					GameWindow window = ((HumanPlayer)unit.player).getWindow();
					double dx = getPosX()-window.getMousePosX();
					double dy = getPosY()-window.getMousePosY();
					targetAngle = Game.fixAngle(-unit.getAngle()+toDegrees(atan2(dy, dx))-90);
					targetDist = sqrt(dx*dx+dy*dy);
					isInRange = inManualRange(targetDist);
				}else{
					targetAngle = calcTargetAngle();
					double dx = getPosX()-getTarget().getPosX();
					double dy = getPosY()-getTarget().getPosY();
					targetDist = sqrt(dx*dx+dy*dy);
					isInRange = firePressed ? inManualRange(targetDist) : inRange(getTarget());
				}
			}
		}else{
			targetAngle = Double.NaN;
			isInRange = false;
		}
		
		if (!Double.isNaN(targetAngle) && getHull() > 0)
			trackTarget();
		
		isReady = determineIfReady();
		//triggerPulled = determineIfTriggerPulled(firePressed);
		triggerPulled = hasTarget && !Double.isNaN(targetAngle) && isInRange &&
				(firePressed || !manualAim);
		if (isReady && triggerPulled && isAimed())
			unit.player.fireWeapon(this);
		
		if (timeToLoaded > 0)
			timeToLoaded--;
	}
	
	public void fire(double angle, int time){
		this.angle = angle;
		fire(time);
	}
	public void fire(int time){
		unit.drainEnergy(type.energyPerShot, "Weapons", type.name);
		unit.changeAmmo(type.ammoType, -1);
		Main.game.playSound(type.fireSound, unit, true);
	}
	
	public void reload(){
		timeToLoaded = type.reloadTime;
	}
	
	private boolean findTarget(){
		boolean targetFound = false;
		if (mode != FireMode.DISABLED && manualAim)
			targetFound = true;
		else{
			Target manualTarget = overrideTarget != null ? overrideTarget : unit.getTarget();
			
			if (mode == FireMode.MANUAL){
				if (manualTarget != null)
					targetFound = manualTarget.isVisible();
			}
			
			if (mode == FireMode.AUTOMATIC){
				if (manualTarget != null && inRange(manualTarget) && manualTarget.isVisible()){
					this.autoTarget = null;
					targetFound = true;
				}else{
					if (timePassed%11 == 0)
						findAutoTarget();
					targetFound = this.autoTarget != null;
				}
			}
			
			if (mode == FireMode.AUTONOMOUS){
				if (timePassed%11 == 0)
					findAutoTarget();
				targetFound = this.autoTarget != null;
			}
		}
		return targetFound;
	}
	
	private void findAutoTarget(){
		this.autoTarget = null;
		double lowestPriority = Double.MAX_VALUE;
		for (Controllable controllable : unit.player.visibleControllables){
			if (controllable.getPlayer().team != unit.player.team &&
					((controllable instanceof Missile && autoMissiles) ||
					(controllable instanceof Craft && autoCraft) ||
					(controllable instanceof Ship && autoShips))){
				
				double time = approxTime(controllable);
				double angle = approxAngle(controllable, time);
				time += abs(Game.fixAngle(this.angle+unit.getAngle()-angle))/(type.trackRate*2*(type instanceof MissileType ? 4 : 1));
				
				double priority = time;
				if (controllable instanceof Missile){
					priority *= 1.0 - 0.5*((Missile)controllable).type.overallDamage/MissileType.highestDamage;
					if (unit.getTarget() != null && unit.getTarget().target == controllable)
						priority *= 0.5;
					priority *= 1.0 + 0.5*unit.getWeaponTargetCount((Missile)controllable);
				}
				double arcViolation = getArcViolation(angle);//max(0, max(angle-rightBound, leftBound-angle));
				if (arcViolation > 0){
					if (type instanceof MissileType){
						priority *= 1+log1p(arcViolation/800.0);
					}else
						priority *= 1+log1p(arcViolation/1.0);
				}
				
				if (priority <= lowestPriority){
					this.autoTarget = unit.player.getTarget(controllable);
					lowestPriority = priority;
				}
			}
		}
	}
	
	private void trackTarget(){
		double deltaAngle = Game.fixAngle(targetAngle-angle);
		if (this.arc >= 180){
			if (abs(deltaAngle) > type.trackRate){
				this.angle += (deltaAngle > 0 ? 1 : -1)*type.trackRate;
			}else
				angle = targetAngle;
		}else{
			angle = Game.centerAbout(angle, mountAngle);
			targetAngle = Game.centerAbout(targetAngle, mountAngle);
			
			boolean rotateRight;
			double leftBound = mountAngle-this.arc, rightBound = mountAngle+this.arc;
			if (targetAngle < rightBound && targetAngle > leftBound)
				rotateRight = angle < targetAngle;
			else{
				if (abs(unit.getTurnSpeed()) < 0.01){
					return;
				}else
					rotateRight = unit.getTurnSpeed() > 0;
			}
			
			if (abs(angle-targetAngle) < type.trackRate){
				angle = targetAngle;
			}else
				angle += (rotateRight ? 1 : -1)*type.trackRate;
			
			if (angle > rightBound){
				angle = rightBound;
			}else if (angle < leftBound)
				angle = leftBound;
		}
		angle = Game.fixAngle(angle);
		targetAngle = Game.fixAngle(targetAngle);
	}
	
	private boolean determineIfReady(){
		return unit.player.isMaster() &&
				timeToLoaded == 0 && getHull() > 0 && (type.ammoType == -1 || unit.ammo[type.ammoType] > 0) &&
				unit.getEnergy("Weapons", type.name) > type.energyPerShot;
	}
	
	/*private boolean determineIfTriggerPulled(boolean firePressed){
		if (!hasTarget || Double.isNaN(targetAngle) || !isInRange)
			return false;
		//if (unit.player instanceof HumanPlayer && selected){
			//GameWindow gameWindow = ((HumanPlayer)unit.player).getWindow();
			//boolean firePressed = gameWindow.isSelected(unit) && (
			//		gameWindow.getInputHandler().controlPressed(Control.FIRE) ||
			//		(manualAim && gameWindow.getInputHandler().isMousePressed()));
			if (firePressed)
				return true;
		//}
		if ((mode == FireMode.AUTOMATIC || mode == FireMode.AUTONOMOUS) && !manualAim)
			return true;
		return false;
	}*/
	
	public boolean isAimed(){
		if (Double.isNaN(targetAngle))
			return false;
		if (type instanceof MissileType)
			return true;
		
		double targetArc;
		Target target = getTarget();
		if (target != null){
			targetArc = max(0.05, toDegrees(atan2(target.target.getSize()/6, 200+unit.distance(target))));
		}else
			targetArc = arc > 1 ? 0.05 : 2.0;
		return abs(targetAngle-angle) < targetArc;
	}
	
	protected double getDx(){
		return getTarget().getPosX()-getPosX();
	}
	protected double getDy(){
		return getTarget().getPosY()-getPosY();
	}
	
	public double getArcViolation(double angle){
		double leftBound = mountAngle-this.arc, rightBound = mountAngle+this.arc;
		angle = Game.centerAbout(angle-unit.getAngle(), mountAngle);
		return max(0, max(angle-rightBound, leftBound-angle));
	}
	
	public boolean isInRange(){
		return isInRange;
	}
	
	public boolean isReady(){
		return isReady;
	}
	
	public boolean isTriggerPulled(){
		return triggerPulled;
	}
	
	public boolean isManualAim(){
		return manualAim;
	}
	
	public int timeToLoaded(){
		return timeToLoaded;
	}
	
	public FireMode getFireMode(){
		return mode;
	}
	
	public void setFireMode(FireMode mode){
		if (this.mode == FireMode.AUTONOMOUS || this.mode == FireMode.AUTOMATIC)
			autoTarget = null;
		this.mode = mode;
		isInRange = false;
		targetAngle = Double.NaN;
	}
	
	public Target getTarget(){
		return autoTarget != null ? autoTarget : overrideTarget != null ? overrideTarget : unit.getTarget();
	}
	public void setOverrideTarget(Target target){
		this.overrideTarget = target;
		targetAngle = Double.NaN;
		isInRange = false;
		setSubTarget(null);
	}
	
	public double getTargetAngle(){
		return targetAngle;
	}
	
	public Component getSubTarget(){
		return subTarget;
	}
	public void setSubTarget(Component subTarget){
		this.subTarget = subTarget;
	}
	
	public void removeControllable(Controllable controllable){
		if (this.overrideTarget != null && this.overrideTarget.target == controllable)
			this.overrideTarget = null;
		if (this.autoTarget != null && this.autoTarget.target == controllable)
			this.autoTarget = null;
		if (subTarget != null && subTarget.unit == controllable)
			subTarget = null;
	}
	
	public double getAngle(){
		return angle;
	}
	public void setAngle(double angle){
		this.angle = angle;
	}
	
	public void updateAngle(double angle, int time){
		if (lastAngleUpdateTime < time){
			this.angle = angle;
			lastAngleUpdateTime = time;
		}
	}
	
	public void setArc(int arc){
		this.arc = min(hardpoint.arc, max(MIN_ARC, arc));
		setMountAngle(mountAngleFrac);
	}
	
	public void setMountAngle(double mountAngleFrac){
		int range = hardpoint.arc;
		if (hardpoint.arc < 180)
			range -= arc;
		
		if (mountAngleFrac == AUTO_ANGLE){
			if (abs(range) == 0 || abs(hardpoint.angle) > 120){
				mountAngleFrac = 0.0;
			}else
				mountAngleFrac = min(1.0, max(-1.0, -(double)hardpoint.angle/range));
		}
		mountAngle = hardpoint.angle + (int)(mountAngleFrac*range);
		this.mountAngleFrac = mountAngleFrac;
	}
	
	public double getMountAngleFrac(){
		return mountAngleFrac;
	}
	
	public int getArc(){
		return arc;
	}
	public int getMountAngle(){
		return mountAngle;
	}
	
	public int getMass(){
		return type.getMass(arc);
	}
	
	public int getCost(){
		/*int output = type.cost;
		for (int x = 0; x < arc/Weapon.ARC_INCREMENT; x++)
			output += type.cost*type.arcResourceBase*pow(type.arcResourceExponent, x);
		return output;*/
		return type.getCost(arc);
	}
	
	public boolean isEngaged(){
		return mode != FireMode.DISABLED;
	}
	
	public boolean isOperational(){
		return getHull() > 0 && (type.ammoType == -1 || unit.ammo[type.ammoType] > 0);
	}
	
	public double getPointDefenseEffect(){
		return type.pointDefenseEffect*max(1.0, 0.3 + 0.7*arc/180.0);
	}
	
	public Image getImage(double zoom){
		if (type.renderable == null)
			return null;
		return type.renderable.getImage(zoom, angle + unit.getAngle(), false, 0);
	}
	
	public FireWeaponMsg getFireMsg(){
		if (fireMsg == null){
			fireMsg = new FireWeaponMsg();
			fireMsg.weapon = this;
		}
		fireMsg.angle = angle;
		fireMsg.randomSeed = random.nextInt();
		random.setSeed(fireMsg.randomSeed);
		fireMsg.time = Main.game.turn;
		return fireMsg;
	}
	
	public static void createMenu(){
		weaponPanel = new WeaponPanel();
	}
	public JPanel getMenu(){
		weaponPanel.select(this);
		return weaponPanel;
	}
	
	public void write(BufferedWriter out) throws IOException{
		super.write(out);
		out.write(arc+"\n");
		out.write(mountAngleFrac+"\n");
	}
	
	public static void read(BufferedReader in, Unit unit, WeaponHardpoint hardpoint){
		try{
			String typeName = in.readLine();
			if (!typeName.equals("null")){
				WeaponType type = null;
				for (WeaponType weaponType : Main.weaponTypes){
					if (typeName.equals(weaponType.name))
						type = weaponType;
				}
				if (type == null)
					return;
				int arc = Integer.valueOf(in.readLine());
				double mountAngleFrac = Double.valueOf(in.readLine());
				if (arc > hardpoint.arc || abs(mountAngleFrac) > 1.0)
					return;
				unit.setWeapon(type, hardpoint, arc, mountAngleFrac);
				if (unit.getWeaponAt(hardpoint).getMass() > hardpoint.mass)
					unit.setWeapon(null, hardpoint, 0, 0);
			}
		}catch(IOException|NumberFormatException e){
			return;
		}
	}
}
