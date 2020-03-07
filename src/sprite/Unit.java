import static java.lang.Math.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;
import java.io.*;

public abstract class Unit extends Sprite implements Controllable, Repairable, Iterable<Component>{
	public final static int ESCORT_SLACK = 6*Main.TPS;
	public final static int RES = 0, CAP = 1, OFF = 2;
	final static int CONTROLS_HEIGHT = 640;
	final static double THRUST_AVG_FACTOR = 1/(0.25*Main.TPS);
	final static char[] VALID_NAME_CHARS =
			" abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_+-[]".toCharArray();
	final static Color PLAYER_COLOR = Color.GREEN, ALLY_COLOR = new Color(0, 210, 130), ENEMY_COLOR = Color.RED;
	final static Random RANDOM = new Random();
	final static Color STATUS_OUTLINE_COLOR = new Color(80, 80, 80);
	static Font statusFont;
	static Font iconFont;
	
	static int unitCounter = 0;
	static SidePanel menu;
	static JLabel nameLabel;
	static JTabbedPane controls;
	static PowerPanel powerPanel;
	static ComponentsPanel weaponsPanel;
	static ComponentsPanel systemsPanel;
	static TargetPanel targetPanel;
	static SidePanel selectedTab;
	
	public final UnitType type;
	
	public boolean manualAmmo;
	public final double[] ammoRatios;
	public int lastUnitUpdateTime;
	public String debug;
	public double ammoRatio;
	
	Player player;
	IdList<Weapon> weapons;
	IdList<System> systems;
	OrderQueue orders;
	final int[] ammo;
	double mass;
	double hull;
	Target target;
	int radarRange, cloakRange, scannerRange;
	double radarSize, visionSize;
	int accelLinearTime, accelRightTime, accelLeftTime, accelForwardTime;
	double forwardThrustAvg, leftThrustAvg, rightThrustAvg;
	int turnsPassed;
	Thruster[][] activeThrusters;
	
	Map<String, Map<String, double[]>> capacitor;
	double[] unreservedCap;
	double totalCap;
	double overflowPower;
	
	private String name;
	private Location futureTarget;
	
	public Unit(UnitType type){
		super(type.renderable);
		
		this.type = type;
		weapons = new IdList<Weapon>(type.weaponHardpoints.length);
		systems = new IdList<System>(type.systemHardpoints.length);
		ammo = new int[Main.ammoMass.length];
		ammoRatios = new double[ammo.length];
		ammoRatio = 1.0;
		setName(type.name);
		turnsPassed = unitCounter++;
		activeThrusters = new Thruster[3][];
	}
	
	public void act(){
		turnsPassed++;
		
		addEnergy(type.power);
		
		if (orders != null)
			orders.act();
		
		if (netThrustTime[Thruster.FORWARD] > 0){
			netThrustTime[Thruster.FORWARD]--;
			accelForward();
		}
		if (netThrustTime[Thruster.LEFT] > 0){
			netThrustTime[Thruster.LEFT]--;
			accelTurn(false);
		}
		if (netThrustTime[Thruster.RIGHT] > 0){
			netThrustTime[Thruster.RIGHT]--;
			accelTurn(true);
		}
		
		for (Component component : this)
			component.move();
		
		if (turnsPassed%31 == 0)
			processRadar();
		
		if (turnsPassed%16 == 0 && orders != null){
			if (player instanceof HumanPlayer && ((HumanPlayer)player).getWindow().isSelected(this)
					&& !((HumanPlayer)player).getWindow().isStrategic()){
				if (orders.getDefault() != null)
					orders.setDefault(null);
			}else{
				if (orders.getDefault() == null)
					orders.setDefault(new FaceWeapons());
			}
		}
		
		if (turnsPassed%37 == 0 && Main.game.isLocal()){
			if (getTurnSpeed()*Main.TPS > 2*360)
				explode();
		}
	}
	
	public void postMove(){
		//durationThrustingForward = accelForwardTime == Main.game.turn ? durationThrustingForward+1 : 0;
		//durationThrustingLeft = accelLeftTime == Main.game.turn ? durationThrustingLeft+1 : 0;
		//durationThrustingRight = accelRightTime == Main.game.turn ? durationThrustingRight+1 : 0;
		forwardThrustAvg = (1.0-THRUST_AVG_FACTOR)*forwardThrustAvg + THRUST_AVG_FACTOR*(accelForwardTime == Main.game.turn ? 1.0 : 0.0);
		leftThrustAvg = (1.0-THRUST_AVG_FACTOR)*leftThrustAvg + THRUST_AVG_FACTOR*(accelLeftTime == Main.game.turn ? 1.0 : 0.0);
		rightThrustAvg = (1.0-THRUST_AVG_FACTOR)*rightThrustAvg + THRUST_AVG_FACTOR*(accelRightTime == Main.game.turn ? 1.0 : 0.0);
	}
	
	public void recordPos(){
		super.recordPos();
		if (orders != null)
			orders.recordPos();
		for (Component component : this)
			component.recordPos();
	}
	
	public void setId(){
		super.setId();
		for (Component component : this)
			component.setId();
	}
	
	public void addEnergy(double input){
		totalCap = unreservedCap[1];
		overflowPower = 0;
		double totalEmpty = 0;
		LinkedList<double[]> nonFull = new LinkedList<double[]>();
		
		for (String key : capacitor.keySet()){
			Map<String, double[]> specific = capacitor.get(key);
			double[] specificUnresCap = specific.get("Unreserved");
			
			for (String specificKey : specific.keySet()){
				double[] val = specific.get(specificKey);
				
				val[CAP] += val[RES]*input;
				totalCap += val[CAP];
				
				if (specificKey != "Unreserved")
					if (val[CAP] > val[RES]*type.capacitor){
						specificUnresCap[CAP] += val[CAP]-val[RES]*type.capacitor;
						val[CAP] = val[RES]*type.capacitor;
						
						if (specificUnresCap[CAP] > specificUnresCap[RES]*type.capacitor)
							overflowPower += val[RES];
					}
					else if (val[CAP] < val[RES]*type.capacitor){
						totalEmpty += val[RES]*type.capacitor-val[CAP];
						nonFull.add(val);
					}
			}
			if (specificUnresCap[CAP] > specificUnresCap[RES]*type.capacitor){
				unreservedCap[CAP] += specificUnresCap[CAP]-specificUnresCap[RES]*type.capacitor;
				specificUnresCap[CAP] = specificUnresCap[RES]*type.capacitor;
				overflowPower += specificUnresCap[RES];
			}else if (specificUnresCap[CAP] < specificUnresCap[RES]*type.capacitor){
				totalEmpty += specificUnresCap[RES]*type.capacitor-specificUnresCap[CAP];
				nonFull.add(specificUnresCap);
			}
		}
		unreservedCap[CAP] += unreservedCap[RES]*input;
		if (unreservedCap[CAP] > unreservedCap[RES]*type.capacitor){
			if (totalEmpty > 0){
				double capToRecycle = min(totalEmpty, unreservedCap[CAP]-unreservedCap[RES]*type.capacitor);
				while(!nonFull.isEmpty())
					nonFull.peek()[CAP] += (nonFull.peek()[RES]*type.capacitor-nonFull.remove()[CAP])/totalEmpty*capToRecycle;
			}
			unreservedCap[CAP] = unreservedCap[RES]*type.capacitor;
		}
	}
	
	public boolean drainEnergy(double energy, String category, String specific){
		double[] specificCap = capacitor.get(category).get(specific);
		double[] specificUnresCap = capacitor.get(category).get("Unreserved");
		
		if (specificCap[OFF] > 0 || specificUnresCap[OFF] > 0)
			return false;
		
		if (specificCap[CAP] + specificUnresCap[CAP] + unreservedCap[CAP] < energy){
			return false;
		}else{
			if (specificCap[CAP] >= energy){
				specificCap[CAP] -= energy;
			}else{
				if (specificCap[CAP]+specificUnresCap[CAP] >= energy){
					specificUnresCap[CAP] -= energy-specificCap[CAP];
					specificCap[CAP] = 0;
				}else{
					unreservedCap[CAP] -= energy-specificUnresCap[CAP]-specificCap[CAP];
					specificUnresCap[CAP] = 0;
					specificCap[CAP] = 0;
				}
			}
			totalCap -= energy;
			return true;
		}
	}
	
	public double getEnergy(){
		return totalCap;
	}
	public double getEnergy(String category, String specific){
		return capacitor.get(category).get(specific)[CAP]+
				capacitor.get(category).get("Unreserved")[CAP]+unreservedCap[CAP];
	}
	
	private void processRadar(){
		radarRange = 0;
		cloakRange = 0;
		scannerRange = 0;
		for (System system : systems){
			if (system.type instanceof SensorType && system.isActive())
				radarRange = max(radarRange, system.type.radius);
			if (system.type instanceof CloakType && system.isActive())
				cloakRange = max(cloakRange, system.type.radius);
			if (system.type instanceof ScannerType && system.isActive())
				scannerRange = max(scannerRange, system.type.radius);
		}
		
		radarSize = type.radarSize;
		for (Player player : Main.game.players)
			if (player.team == this.player.team)
				for (Controllable controllable : player.controllables)
					if (controllable instanceof Unit)
						for (System system : ((Unit)controllable).systems)
							if (system instanceof Cloak && system.isActive() &&
									(controllable == this || distance((Sprite)controllable) < system.type.radius))
								radarSize *= 1.0-((Cloak)system).type.effect;
	}
	
	public void scannedBy(Sensor sensor){
		for (System system : systems)
			if (system instanceof Detector){
				SensorSighting sighting = ((Detector)system).scannedBy(sensor);
				if (sighting != null)
					player.addSensorSighting(sighting, sensor.unit);
			}
	}
	
	public void changeAmmo(int ammoType, int numShots){
		if (ammoType != -1){
			ammo[ammoType] += numShots;
			mass += numShots*Main.ammoMass[ammoType];
		}
	}
	
	public void accelForward(){
		double thrust = type.thrust;
		for (System system : systems){
			if (system instanceof Booster)
				thrust += ((Booster)system).getThrust();
		}
		if (accelLinear(getAngle(), thrust))
			accelForwardTime = Main.game.turn;
	}
	
	public void accelManeuver(double angle){
		accelLinear(getAngle()+angle, Main.config.maneuverThrust*type.thrust);
	}
	
	private boolean accelLinear(double angle, double thrust){
		if (accelLinearTime < Main.game.turn){
			if (drainEnergy(thrust*Main.config.energyPerThrust, "Engines", "Forward")){
				accel(thrust/mass, angle);
				accelLinearTime = Main.game.turn;
				return true;
			}
		}
		return false;
	}
	
	public void accelTurn(boolean direction){
		if (accelRightTime < Main.game.turn && accelLeftTime < Main.game.turn){
			if (drainEnergy(type.turnThrust*Main.config.energyPerTurnThrust, "Engines", "Turn")){
				if (abs(getTurnSpeed()) >= type.turnThrust/type.mass/2 || abs(getTurnSpeed()) < 0.0001){
					accelTurn((direction ? 1 : -1)*type.turnThrust/type.mass);
				}else
					accelTurn(-getTurnSpeed());
				if (direction){
					accelRightTime = Main.game.turn;
				}else
					accelLeftTime = Main.game.turn;
			}
		}
	}
	
	public void stopTurn(){
		if (abs(getTurnSpeed()) > 0.0001)
			accelTurn(getTurnSpeed() < 0);
	}
	
	public void takeHit(double posX, double posY,
			Component subTarget, double direction, boolean continuous,
			double explosiveDamage, double kineticDamage, double EMDamage){
		
		double armor = continuous ? type.armor/Main.TPS : type.armor;
		boolean hasSubtarget = subTarget != null && (weapons.contains(subTarget) || systems.contains(subTarget));
		boolean componentHit = false;
		
		if (explosiveDamage > 0){
			if (!componentHit && hasSubtarget && RANDOM.nextDouble() < Main.config.explosiveSubtargetChance){
				double explosiveTargetDamage = (RANDOM.nextDouble()*Main.config.explosiveComponentDamage)*explosiveDamage;
				explosiveDamage -= explosiveTargetDamage;
				subTarget.takeHit(explosiveTargetDamage);
			}
			for (Component component : weapons){
				if (!componentHit && RANDOM.nextDouble() < Main.config.explosiveComponentChance){
					double weaponExplosiveDamage = (RANDOM.nextDouble()*Main.config.explosiveComponentDamage)*explosiveDamage;
					component.takeHit(weaponExplosiveDamage);
					explosiveDamage -= weaponExplosiveDamage;
				}
			}
			for (Component component : systems){
				if (!componentHit && RANDOM.nextDouble() < Main.config.explosiveComponentChance){
					double systemExplosiveDamage = (RANDOM.nextDouble()*Main.config.explosiveComponentDamage)*explosiveDamage;
					component.takeHit(systemExplosiveDamage);
					explosiveDamage -= systemExplosiveDamage;
				}
			}
			this.hull -= damage(explosiveDamage, armor*Main.config.armorExplosiveEffectiveness);
		}
		
		if (kineticDamage > 0){
			if (!componentHit && hasSubtarget && RANDOM.nextDouble() < Main.config.kineticSubtargetChance){
				subTarget.takeHit(Main.config.kineticComponentDamage*kineticDamage);
				kineticDamage -= Main.config.kineticComponentDamage*kineticDamage;
			}else if (!componentHit && weapons.size() > 0 && RANDOM.nextDouble() < Main.config.kineticComponentChance){
				weapons.get((int)(RANDOM.nextDouble()*weapons.size())).takeHit(Main.config.kineticComponentDamage*kineticDamage);
				kineticDamage -= Main.config.kineticComponentDamage*kineticDamage;
			}else if (!componentHit && systems.size() > 0 && RANDOM.nextDouble() < Main.config.kineticComponentChance){
				systems.get((int)(RANDOM.nextDouble()*systems.size())).takeHit(Main.config.kineticComponentDamage*kineticDamage);
				kineticDamage -= Main.config.kineticComponentDamage*kineticDamage;
			}
			this.hull -= damage(kineticDamage, armor*Main.config.armorKineticEffectiveness);
		}
		
		if (EMDamage > 0){
			for (Component component : this){
				if (RANDOM.nextDouble() > Main.config.EMComponentChance)
					component.takeHit(EMDamage*RANDOM.nextDouble());
			}
		}
	}
	
	public void explode(){
		Main.game.removeControllable(this);
		//Main.game.createDeathExplosion(type, getPosX(), getPosY(), getVelX(), getVelY());
		for (Weapon weapon : weapons)
			player.firingBeams.remove(weapon);
	}
	
	public void repair(double material, boolean isScrap){
		if (isScrap){
			hull -= material*type.hullPerMaterial;
		}else
			hull = min(type.hull, hull+material*type.hullPerMaterial);
	}
	
	public int getCost(){
		int cost = type.cost;
		for (Component component : this)
			cost += component.getCost();
		return cost;
	}
	
	public boolean[][] getContactMap(){
		return type.contactMap[Renderable.getAngleIndex(getAngle(), UnitType.NUM_CONTACT_MAPS)];
	}
	
	public double getAccel(){
		double[] forward = capacitor.get("Engines").get("Forward");
		double[] engines = capacitor.get("Engines").get("Unreserved");
		
		if (engines[OFF] > 0 || forward[OFF] > 0)
			return 0;
		
		if (forward[CAP] + engines[CAP] + unreservedCap[CAP] > 8*Main.TPS*type.thrust*Main.config.energyPerThrust){
			return type.thrust/mass;
		}else{
			return 0.7*type.power*(forward[RES] + engines[RES] +
					unreservedCap[RES]+overflowPower)/(Main.config.energyPerThrust*mass);
		}
	}
	
	public double getTurnAccel(){
		double[] turn = capacitor.get("Engines").get("Turn");
		double[] engines = capacitor.get("Engines").get("Unreserved");
		
		if (engines[OFF] > 0 || turn[OFF] > 0)
			return 0;
		
		if (turn[CAP] + engines[CAP]+unreservedCap[CAP] > 8*Main.TPS*type.turnThrust*Main.config.energyPerTurnThrust){
			return type.turnThrust/mass;
		}else{
			return 0.7*type.power*(turn[RES] + engines[RES] +
					unreservedCap[RES]+overflowPower)/(Main.config.energyPerTurnThrust*mass);
		}
	}
	
	public int getIconSize(){
		return type.iconSize;
	}
	
	public Target getTarget(){
		return target;
	}
	public void setTarget(Target target){
		this.target = target;
		Unit.targetPanel.updateTarget(this);
	}
	
	public void removeControllable(Controllable controllable){
		if (controllable.getPlayer().team != player.team){
			if (this.target != null && this.target.target == controllable)
				setTarget(null);
			for (Weapon weapon : weapons)
				weapon.removeControllable(controllable);
		}
		if (orders != null)
			orders.removeTarget(controllable);
	}
	
	public boolean isVisible(Sprite sprite){
		return distance(sprite) < type.visionRange*sprite.getVisionSize();
	}
	
	public BuyType getType(){
		return type;
	}
	
	public Weapon getWeaponAt(Hardpoint hardpoint){
		for (Weapon weapon : weapons){
			if (weapon.hardpoint == hardpoint)
				return weapon;
		}
		return null;
	}
	public System getSystemAt(Hardpoint hardpoint){
		for (System system : systems){
			if (system.hardpoint == hardpoint)
				return system;
		}
		return null;
	}
	public Component getComponentAt(Hardpoint hardpoint){
		if (getWeaponAt(hardpoint) != null){
			return getWeaponAt(hardpoint);
		}else
			return getSystemAt(hardpoint);
	}
	
	public void setWeapon(WeaponType type, WeaponHardpoint hardpoint, int arc, double mountAngleFrac){
		weapons.remove(getWeaponAt(hardpoint));
		if (type instanceof MissileType){
			weapons.add(weapons.size(), new Launcher((MissileType)type, hardpoint, this, arc, mountAngleFrac));
		}else if (type instanceof GunType){
			weapons.add(weapons.size(), new Gun((GunType)type, hardpoint, this, arc, mountAngleFrac));
		}else if (type instanceof BeamType)
			weapons.add(weapons.size(), new Beam((BeamType)type, hardpoint, this, arc, mountAngleFrac));
	}
	
	public void setSystem(SystemType type, Hardpoint hardpoint){
		systems.remove(getSystemAt(hardpoint));
		if (type instanceof SensorType){
			systems.add(systems.size(), new Sensor((SensorType)type, hardpoint, this));
		}else if (type instanceof DetectorType){
			systems.add(systems.size(), new Detector((DetectorType)type, hardpoint, this));
		}else if (type instanceof CloakType){
			systems.add(systems.size(), new Cloak((CloakType)type, hardpoint, this));
		}else if (type instanceof ScannerType){
			systems.add(systems.size(), new Scanner((ScannerType)type, hardpoint, this));
		}else if (type instanceof BoosterType)
			systems.add(systems.size(), new Booster((BoosterType)type, hardpoint, this));
	}
	
	public Player getPlayer(){
		return player;
	}
	
	public double getHull(){
		return hull;
	}
	
	public double getMass(){
		return mass;
	}
	
	public double getRadarSize(){
		return radarSize;
	}
	
	public double getVisionSize(){
		return type.radarSize;
	}
	
	public BufferedImage getIcon(){
		return type.icon;
	}
	
	public double getWeaponReadiness(){
		if (weapons.size() == 0)
			return 1.0;
		int numWeaponsReady = 0;
		for (Weapon weapon : weapons){
			if (weapon.isOperational())
				numWeaponsReady++;
		}
		return (double)numWeaponsReady/weapons.size();
	}
	
	public OrderQueue orders(){
		return orders;
	}
	
	public double getWeaponCount(int ammoType){
		double count = 0.0;
		for (Weapon weapon : weapons){
			if (weapon.type.ammoType == ammoType)
				count += 1.0;
		}
		return count;
	}
	
	public boolean weaponsInRange(Locatable target, double time){
		futureTarget.setCoordinates(target.getPosX() + time*(target.getVelX()-this.getVelX()),
				target.getPosY() + time*(target.getVelY()-this.getVelY()),
				target.getVelX(), target.getVelY());
		
		boolean inRange = true;
		boolean hasWeapons = false;
		for (Weapon weapon : weapons){
			if (weapon.isOperational() && weapon.getFireMode() != Weapon.FireMode.DISABLED){
				hasWeapons = true;
				inRange = inRange && weapon.inRange(futureTarget);
			}
		}
		return hasWeapons && inRange;
	}
	
	public int getWeaponTargetCount(Controllable target){
		int count = 0;
		for (Weapon weapon : weapons){
			if (weapon.getTarget() != null && weapon.getTarget().target == target)
				count++;
		}
		return count;
	}
	
	public void setDefaultAmmoRatios(){
		double totalWeaponCount = 0;
		for (int x = 0; x < ammo.length; x++)
			totalWeaponCount += getWeaponCount(x);
		for (int x = 0; x < ammo.length; x++){
			if (totalWeaponCount > 0){
				ammoRatios[x] = getWeaponCount(x)/totalWeaponCount;
			}else
				ammoRatios[x] = 0.0;
		}
	}
	
	public int getMaxAmmo(int ammoIndex){
		return (int)(ammoRatio*ammoRatios[ammoIndex]*type.storageSpace/Main.ammoMass[ammoIndex]);
	}
	
	public void initializeAmmoAndMass(){
		for (int x = 0; x < ammoRatios.length; x++)
			ammo[x] = getMaxAmmo(x);
		
		mass = type.mass;
		for (Component component : this)
			mass += component.getMass();
		for (int x = 0; x < ammo.length; x++)
			mass += ammo[x]*Main.ammoMass[x];
	}
	
	public void addMass(double amount){
		mass += amount;
	}
	
	public void autoLoadout(){
		weapons.clear();
		for (WeaponHardpoint hardpoint : type.weaponHardpoints){
			WeaponType type = (WeaponType)hardpoint.autoLoadout(this);
			int arc = hardpoint.getMaxArc(type);
			if (type instanceof MissileType && arc < 90)
				arc = 0;
			setWeapon(type, hardpoint, arc, Weapon.AUTO_ANGLE);
		}
		for (Hardpoint hardpoint : type.systemHardpoints)
			setSystem((SystemType)hardpoint.autoLoadout(this), hardpoint);
	}
	
	public boolean setName(String name){
		this.name = Utility.filter(name, VALID_NAME_CHARS);
		return name.equals(this.name);
	}
	
	public String getName(){
		return name;
	}
	
	public void initialize(){
		if (!manualAmmo)
			setDefaultAmmoRatios();
		
		capacitor = new HashMap<String, Map<String, double[]>>();
		if (!weapons.isEmpty()){
			HashMap<String, double[]> weaponsCap = new HashMap<String, double[]>(Main.weaponTypes.length);
			weaponsCap.put("Unreserved", new double[3]);
			for (Weapon weapon : weapons)
				if (!weaponsCap.containsKey(weapon.type.name))
					weaponsCap.put(weapon.type.name, new double[3]);
			capacitor.put("Weapons", weaponsCap);
		}
		if (!systems.isEmpty()){
			HashMap<String, double[]> systemsCap = new HashMap<String, double[]>(Main.systemTypes.length);
			systemsCap.put("Unreserved", new double[3]);
			for (System system : systems)
				if (!systemsCap.containsKey(system.type.name))
					systemsCap.put(system.type.name, new double[3]);
			capacitor.put("Systems", systemsCap);
		}
		HashMap<String, double[]> enginesCap = new HashMap<String, double[]>(2);
		enginesCap.put("Unreserved", new double[3]);
		enginesCap.put("Forward", new double[3]);
		enginesCap.put("Turn", new double[3]);
		capacitor.put("Engines", enginesCap);
		
		double initialEnginePower = 0.1;//min(0.3, 0.30*type.thrust*Main.energyPerThrust/type.power);
		enginesCap.get("Unreserved")[RES] = initialEnginePower/2;
		enginesCap.get("Turn")[RES] = initialEnginePower/2;
		unreservedCap = new double[2];
		unreservedCap[RES] = 1.0-initialEnginePower;
		
		for (Component component : this)
			component.initialize();
		
		if (player.isMaster()){
			orders = new OrderQueue(this);
			orders.clear();
			orders.setDefault(new FaceWeapons());
		}
		
		hull = type.hull;
		addEnergy(type.capacitor);
		initializeAmmoAndMass();
		futureTarget = new Location();
		turnsPassed = (int)(random()*100);
	}
	
	public void copyComponentsAndAmmoTo(Unit unit){
		for (System system : systems)
			unit.setSystem(system.type, system.hardpoint);
		for (Weapon weapon : weapons)
			unit.setWeapon(weapon.type, weapon.hardpoint, weapon.getArc(), weapon.getMountAngleFrac());
		for (int x = 0; x < ammo.length; x++){
			unit.ammo[x] = ammo[x];
			unit.ammoRatios[x] = ammoRatios[x];
		}
		unit.manualAmmo = manualAmmo;
	}
	
	public String toString(){
		return name;
	}
	
	protected Thruster[][] getThrusters(){
		for (int x = 0; x < activeThrusters.length; x++)
			activeThrusters[x] = NO_THRUSTERS;
		if (Main.game.turn-accelForwardTime <= 1)
			activeThrusters[Thruster.FORWARD] = type.thrusters[Thruster.FORWARD];
		if (leftThrustAvg - rightThrustAvg > 0.3)
			activeThrusters[Thruster.LEFT] = type.thrusters[Thruster.LEFT];
		if (rightThrustAvg - leftThrustAvg > 0.3)
			activeThrusters[Thruster.RIGHT] = type.thrusters[Thruster.RIGHT];
		return activeThrusters;
	}
	protected BufferedImageOp getThrusterOp(){
		for (System system : systems){
			if (system instanceof Booster && system.isActive())
				return ((Booster)system).type.thrusterOp;
		}
		return null;
	}
	
	public Iterator<Component> iterator(){
		return new Iterator<Component>(){
			Iterator<Weapon> weaponIterator = weapons.iterator();
			Iterator<System> systemIterator = systems.iterator();
			public boolean hasNext(){
				return weaponIterator.hasNext() || systemIterator.hasNext();
			}
			public Component next(){
				return weaponIterator.hasNext() ? weaponIterator.next() : systemIterator.next();
			}
			public void remove(){}
		};
	}
	
	public void draw(Graphics2D g, GameWindow window){
		int posX = window.posXOnScreen(renderPosX), posY = window.posYOnScreen(renderPosY);
		int size = getRenderSize(window.getRenderZoom());
		int statSize = Main.options.statusSize;
		
		g.setColor(window.getPlayer() == player ? PLAYER_COLOR :
				(window.getPlayer().team == player.team ? ALLY_COLOR : ENEMY_COLOR));
		window.drawPointerLine(g, posX, posY, type.iconLabel);
		
		if (posX > -size && posX < window.windowResX+size && posY > -size && posY < window.windowResY+size){
			super.draw(g, window);
			
			if (getImage(window.getRenderZoom()) != null){
				
				if (window.getPlayer().knowHealth(this)){
					g.setColor(Utility.getColor(1-(type.hull-getHull())/(0.9*type.hull), 0.0));
					g.fillOval(posX+size*4/11+statSize/4, posY+size*4/11+statSize/4,
								statSize/2, statSize/2);
					g.setColor(STATUS_OUTLINE_COLOR);
					g.drawOval(posX+size*4/11+statSize/4, posY+size*4/11+statSize/4,
							statSize/2, statSize/2);
				}
				
				if (Main.DEBUG){
					g.setFont(statusFont);
					if (debug != null)
						g.drawString(debug, posX-size*4/11-2, posY+size*4/11+statSize+10);
					if (orders.getOrder() != null)
						g.drawString(orders.getOrder().getClass().getName(), posX-size*4/11-2, posY+size*4/11+statSize+2);
				}
				
				if (player.team == window.getPlayer().team){
					g.setColor(STATUS_OUTLINE_COLOR);
					g.setFont(statusFont);
					
					if (orders != null && orders.isActive(FaceWeapons.class))
						g.drawString("F", posX-size*4/11-2, posY+size*4/11+statSize);
					
					g.drawRect(posX-size*4/11-statSize, posY+size*4/11,
							statSize/4, statSize);
					g.drawRect(posX-size*4/11-statSize/2, posY+size*4/11,
							statSize/4, statSize);
					g.drawString("E", posX-size*4/11-statSize, posY+size*4/11+statSize+8);
					g.drawString("A", posX-size*4/11-statSize/2, posY+size*4/11+statSize+8);
					
					
					g.setColor(window.getPlayer() == player ? PLAYER_COLOR : ALLY_COLOR);
					int barHeight = (int)round((statSize-1)*getEnergy()/type.capacitor);
					g.fillRect(posX-size*4/11-statSize+1, posY+size*4/11+1+((statSize-1)-barHeight),
							statSize/4-1, barHeight);
					
					double ammoMass = 0;
					for (int y = 0; y < ammo.length; y++)
						ammoMass += ammo[y]*Main.ammoMass[y];
					barHeight = (int)round((statSize-1)*ammoMass/(type.storageSpace*ammoRatio));
					g.fillRect(posX-size*4/11-statSize/2+1, posY+size*4/11+1+((statSize-1)-barHeight),
							statSize/4-1, barHeight);
				}
				
				for (Weapon weapon : weapons){
					Image weaponImg = weapon.getImage(window.getRenderZoom());
					if (weaponImg != null){
						int weaponPosX = posX + (int)round((weapon.hardpoint.getRotatedPosX(renderAngle)-0.5)*renderable.size*window.getRenderZoom());
						int weaponPosY = posY + (int)round((weapon.hardpoint.getRotatedPosY(renderAngle)-0.5)*renderable.size*window.getRenderZoom());
						g.drawImage(weaponImg, weaponPosX - weaponImg.getWidth(null)/2, weaponPosY - weaponImg.getHeight(null)/2, null);
					}
				}
			}else{
				g.setFont(iconFont);
				g.setColor(Color.LIGHT_GRAY);
				g.drawString(type.iconLabel, posX-type.iconLabel.length()*3, posY+size/2+9);
			}
		}
	}
	
	public void drawHudTop(Graphics2D g, GameWindow window){
		if (target != null){
			int targetPosX = window.posXOnScreen(target.renderPosX);
			int targetPosY = window.posYOnScreen(target.renderPosY);
			if (targetPosX > 0 && targetPosY > 0 && targetPosX < window.windowResX && targetPosY < window.windowResY){
				int size = target.getHUDSize(window.getRenderZoom());	
				g.setColor(Color.RED);
				int length = size/4;
				g.drawLine(targetPosX-size-length, targetPosY-size-length, targetPosX-size+length/2, targetPosY-size+length/2);
				g.drawLine(targetPosX+size+length, targetPosY-size-length, targetPosX+size-length/2, targetPosY-size+length/2);
				g.drawLine(targetPosX-size-length, targetPosY+size+length, targetPosX-size+length/2, targetPosY+size-length/2);
				g.drawLine(targetPosX+size+length, targetPosY+size+length, targetPosX+size-length/2, targetPosY+size-length/2);
			}
		}
		
		super.drawHudTop(g, window);
	}
	
	public void drawVision(Graphics2D g, GameWindow window){
		int posX = window.posXOnScreen(renderPosX), posY = window.posYOnScreen(renderPosY);
		
		Object useAntialiasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		
		int windowDistX = posX - window.windowResX/2, windowDistY = posY - window.windowResY/2;
		int windowDist = (int)sqrt(windowDistX*windowDistX + windowDistY*windowDistY);
		int maxRadius = windowDist + window.inscribeRadius, minRadius = windowDist - window.inscribeRadius;
		
		g.setColor(new Color(0, 50, 250, 55));
		int visionRange = (int)(window.getZoom()*type.visionRange);
		if (visionRange < maxRadius && visionRange > minRadius)
			g.drawOval(posX-visionRange, posY-visionRange, visionRange*2, visionRange*2);
		
		int visionRangeRadar = (int)(window.getZoom()*radarRange);
		if (visionRangeRadar != visionRange && visionRangeRadar < maxRadius && visionRangeRadar > minRadius){
			g.setColor(new Color(0, 100, 200, 55));
			g.drawOval(posX-visionRangeRadar, posY-visionRangeRadar, visionRangeRadar*2, visionRangeRadar*2);
		}
		
		int cloakRadius = (int)(window.getZoom()*this.cloakRange);
		if (cloakRadius > 0 && cloakRadius < maxRadius && cloakRadius > minRadius){
			g.setColor(new Color(200, 0, 200, 50));
			g.drawOval(posX-cloakRadius, posY-cloakRadius, cloakRadius*2, cloakRadius*2);
		}
		
		int scannerRadius = (int)(window.getZoom()*this.scannerRange);
		if (scannerRadius > 0 && scannerRadius < maxRadius && scannerRadius > minRadius){
			g.setColor(new Color(0, 255, 140, 25));
			g.drawOval(posX-scannerRadius, posY-scannerRadius, scannerRadius*2, scannerRadius*2);
		}
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, useAntialiasing);
	}
	
	public SidePanel getMenu(){
		nameLabel.setText(name);
		weaponsPanel.setUnit(this);
		systemsPanel.setUnit(this);
		powerPanel.setUnit(this);
		targetPanel.setOwner(this);
		selectedTab = (SidePanel)controls.getSelectedComponent();
		controls.removeAll();
		if (!weapons.isEmpty())
			controls.addTab("WEP", Unit.weaponsPanel);
		if (!systems.isEmpty())
			controls.addTab("SYS", Unit.systemsPanel);
		controls.addTab("POW", Unit.powerPanel);
		controls.setSelectedIndex(0);
		return menu;
	}
	
	public void restoreMenuState(){
		try{
			controls.setSelectedComponent(selectedTab);
		}catch (IllegalArgumentException e){
			controls.setSelectedIndex(0);
		}
	}
	
	public static void createMenu(){
		weaponsPanel = new ComponentsPanel(true);
		systemsPanel = new ComponentsPanel(false);
		powerPanel = new PowerPanel();
		targetPanel = new TargetPanel(Main.resY > Main.RES_Y_SHORT+100);
		
		menu = new SidePanel(){
			public void refresh(){
				SidePanel panel = (SidePanel)controls.getSelectedComponent();
				if (panel != null)
					panel.refresh();
			}
		};
		menu.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 0));
		
		nameLabel = new JLabel();
		nameLabel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-4, 20));
		nameLabel.setFont(new Font("Courier", Font.BOLD, 15));
		nameLabel.setHorizontalAlignment(JLabel.CENTER);
		
		controls = new JTabbedPane();
		controls.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH, CONTROLS_HEIGHT));
		
		int spacerHeight = Main.resY-CONTROLS_HEIGHT-targetPanel.getPreferredSize().height-25;
		JPanel spacer = null;
		if (spacerHeight > 0){
			spacer = new JPanel(){
				public void paint(Graphics g){
					g.drawImage(SidePanel.spacer, 0, 0, null);
					super.printBorder(g);
				}
			};
			spacer.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-6, spacerHeight));
			spacer.setBorder(BorderFactory.createEtchedBorder());
		}
		
		menu.add(nameLabel);
		menu.add(controls);
		if (spacer != null)
			menu.add(spacer);
		menu.add(Unit.targetPanel);
	}
	
	public void write(BufferedWriter out) throws IOException{
		out.write(type.name+"\n");
		out.write(manualAmmo+"\n");
		out.write(ammoRatios.length+"\n");
		for (double ratio : ammoRatios)
			out.write(ratio+"\n");
		out.write(type.weaponHardpoints.length+"\n");
		for (Hardpoint hardpoint : type.weaponHardpoints){
			if (getWeaponAt(hardpoint) != null){
				getWeaponAt(hardpoint).write(out);
			}else
				out.write("null"+"\n");
		}
		out.write(type.systemHardpoints.length+"\n");
		for (Hardpoint hardpoint : type.systemHardpoints){
			if (getSystemAt(hardpoint) != null){
				getSystemAt(hardpoint).write(out);
			}else
				out.write("null"+"\n");
		}
	}
	
	public static Unit read(BufferedReader in){
		try{
			String typeName = in.readLine();
			Unit unit = null;
			for (ShipType shipType : Main.shipTypes){
				if (shipType.name.equals(typeName))
					unit = new Ship(shipType);
			}
			for (CraftType craftType : Main.craftTypes){
				if (craftType.name.equals(typeName))
					unit = new Craft(craftType);
			}
			if (unit == null)
				return null;
			
			unit.manualAmmo = Boolean.valueOf(in.readLine());
			if (unit.ammoRatios.length != Integer.valueOf(in.readLine()))
				return null;
			double sum = 0.0;
			for (int x = 0; x < unit.ammoRatios.length; x++){
				unit.ammoRatios[x] = Double.valueOf(in.readLine());
				sum += unit.ammoRatios[x];
			}
			if (sum > 1.001)
				return null;
			
			if (unit.type.weaponHardpoints.length != Integer.valueOf(in.readLine()))
				return null;
			for (WeaponHardpoint hardpoint : unit.type.weaponHardpoints)
				Weapon.read(in, unit, hardpoint);
			
			if (unit.type.systemHardpoints.length != Integer.valueOf(in.readLine()))
				return null;
			for (Hardpoint hardpoint : unit.type.systemHardpoints)
				System.read(in, unit, hardpoint);
			
			return unit;
		}catch (IOException|NumberFormatException e){
			return null;
		}
	}
	
	private final static double ARMOR_BETA = 4+0.1, ARMOR_OMEGA0 = sqrt(ARMOR_BETA);
	private final static double ARMOR_CMINUS = (-ARMOR_BETA-sqrt(ARMOR_BETA*ARMOR_BETA-4*ARMOR_OMEGA0*ARMOR_OMEGA0))/2;
	private final static double ARMOR_CPLUS = (-ARMOR_BETA+sqrt(ARMOR_BETA*ARMOR_BETA-4*ARMOR_OMEGA0*ARMOR_OMEGA0))/2;
	private final static double ARMOR_A = (1+ARMOR_CPLUS)/(ARMOR_CPLUS-ARMOR_CMINUS);
	private final static double ARMOR_B = (1+ARMOR_CMINUS)/(ARMOR_CMINUS-ARMOR_CPLUS);
	public static double damage(double damage, double armor){
		double scaledDamage = damage/armor;
		double f = ARMOR_A*exp(ARMOR_CMINUS*scaledDamage) + ARMOR_B*exp(ARMOR_CPLUS*scaledDamage);
		return armor*(scaledDamage-1+f);
	}
	
}
