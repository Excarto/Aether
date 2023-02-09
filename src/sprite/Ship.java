import static java.lang.Math.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

// Large units. Ships have sheilds, can store craft, can repair, and can warp into the arena.

public class Ship extends Unit{
	public enum QueueStatus{UNQUEUED, QUEUED, CATAPULT}
	static final double WARP_POS_RADIUS = 1000.0, WARP_VEL_RADIUS = 0.4, WARP_DIST = 1.0E7;
	static final int WARP_TIME = Main.TPS*3;
	static final double WARP_RATE = 5.0/Main.TPS;
	static final int MAX_SHIELD_EFFECTS = 1;
	
	static final Random random = new Random();
	static CraftPanel craftPanel;
	static RepairPanel repairPanel;
	static Sound warpSound;
	static Renderable shieldRenderable;
	
	public final ShipType type;
	
	public final IdList<Craft> crafts;
	public final List<RepairTarget> repairTargets;
	public final LinkedList<RepairQueueItem> repairQueue;
	
	public double frontShield, rearShield;
	public boolean outOfArena;
	public boolean craftDefaultAmmo, craftDefaultRepair, craftDefaultRecharge;
	public double material;
	public int autoRepair;
	
	private Queue<QueuedCraft> toLaunch;
	private List<Effect> effects;
	private int timeToRearm, rearmIndex;
	private double warpPosX, warpPosY;
	private int warpTime;
	
	CraftLaunchMsg launchMsg;
	CraftDockMsg dockMsg;
	WarpInMsg warpMsg;
	
	public Ship(ShipType type){
		super(type);
		this.type = type;
		
		crafts = new IdList<Craft>();
		toLaunch = new ConcurrentLinkedQueue<QueuedCraft>();
		effects = new ArrayList<Effect>(8);
		ammoRatio = 0.75;
		autoRepair = Main.options.defaultAutoRepair;
		outOfArena = true;
		
		repairTargets = new ArrayList<RepairTarget>();
		repairQueue = new LinkedList<RepairQueueItem>();
	}
	
	public void act(){
		if (warpTime > 0){
			warpPosX += getVelX();
			warpPosY += getVelY();
			return;
		}
		
		for (int x = 0; x < effects.size(); x++)
			effects.get(x).act();
		
		if (hull < 0)
			return;
		
		super.act();
		
		if (frontShield < type.frontShield && drainEnergy(
				type.shieldRecharge*Main.config.energyPerShield, "Shields", "Front"))
			frontShield += type.shieldRecharge;
		if (rearShield < type.rearShield && drainEnergy(
				type.shieldRecharge*Main.config.energyPerShield, "Shields", "Rear"))
			rearShield += type.shieldRecharge;
		
		if (!toLaunch.isEmpty() && toLaunch.peek().getReady()){
			QueuedCraft queuedCraft = toLaunch.poll();
			player.launchCraft(this, queuedCraft.craft, queuedCraft.catapult);
		}
		
		if (turnsPassed%50 == 0){
			for (Iterator<RepairQueueItem> i = repairQueue.iterator(); i.hasNext();)
				if (!repairTargets.contains(i.next().target))
					i.remove();
		}
		
		if (turnsPassed%101 == 0)
			refreshRepairQueue();
		repairRearmRecap();
	}
	
	public void postMove(){
		super.postMove();
		
		if (warpTime-- > 0){
			this.place(getPosX()*(1-WARP_RATE) + warpPosX*WARP_RATE,
					getPosY()*(1-WARP_RATE) + warpPosY*WARP_RATE,
					getVelX(), getVelY(), getAngle(), getTurnSpeed());
			return;
		}
	}
	
	public void setId(){
		super.setId();
		for (Craft craft : crafts)
			craft.setId();
	}
	
	public void launchCraft(Craft craft, boolean catapult){
		double velX = (catapult ? type.craftLaunchSpeed : 3.0/Main.TPS)*sin(toRadians(getAngle()));
		double velY = (catapult ? type.craftLaunchSpeed : 3.0/Main.TPS)*cos(toRadians(getAngle()));
		double posX = 50*sin(toRadians(getAngle())), posY = 50*cos(toRadians(getAngle()));
		
		craft.place(getPosX()+posX, getPosY()-posY, getVelX()+velX, getVelY()-velY, getAngle(), 0);
		this.accel(-sqrt(velX*velX+velY*velY)*craft.type.mass/this.type.mass, getAngle());
		this.addMass(-craft.mass);
		
		Main.game.playSound(craft.type.launchSound, this, true);
		
		player.controllables.add(craft);
		crafts.remove(craft);
		for (int x = 0; x < repairTargets.size(); x++){
			if (repairTargets.get(x).unit == craft)
				removeRepairTarget(repairTargets.get(x));
		}
		
		craft.setTarget(this.target);
		craft.setMothership(this);
		for (Weapon weapon : craft.weapons)
			weapon.reload();
	}
	
	private void refreshRepairQueue(){
		// For auto-repair, remove ship to make room for possible higher-priority items
		if (repairQueue.size() == 1 && repairQueue.peek().repairable instanceof Ship && repairQueue.peek().isAuto)
			repairQueue.poll();
		
		// For auto-repair, prioritize repairing weapons over systems
		if (repairQueue.size() > 1 && repairQueue.peek().repairable instanceof System && repairQueue.peek().isAuto
				&& repairQueue.get(1).repairable instanceof Weapon)
			repairQueue.poll();
		
		// Finish repairing
		for (RepairTarget target : repairTargets){
			if (material <= 0){
				target.doneRepair = true;
			}else{
				target.doneRepair = target.unit.getHull() == target.unit.type.hull;
				for (Component component : target.unit)
					target.doneRepair = target.doneRepair && component.getHull() >= component.type.hull;
			}
		}
		
		// Add auto repair items
		if (repairQueue.isEmpty()){
			
			// Find unit to repair, including self, or crafts in bay
			int maxPriority = 0;
			RepairTarget priorityTarget = null;
			for (RepairTarget target : repairTargets){
				
				if (target.repair){
					int priority = 0;
					if (target.unit == this){
						priority = 1;
					}else if (target.isInCraftBay){
						double totalMaterial = (target.unit.type.hull-target.unit.hull)/target.unit.type.hullPerMaterial;
						for (Component component : target.unit)
							totalMaterial += max(0, (component.type.hull*autoRepair/100.0-component.getHull()))/component.type.hullPerMaterial;
						if (totalMaterial > 0)
							priority = (int)(1000000/totalMaterial);
					}else
						priority = Integer.MAX_VALUE;
					
					if (priority > maxPriority){
						priorityTarget = target;
						maxPriority = priority;
					}
				}
			}
			
			// If no units found to repair, check for Components
			if (priorityTarget != null){
				if (autoRepair > 0){
					boolean searching = true;
					while (searching){
						Component mostDamaged = null;
						int minHealth = Integer.MAX_VALUE;
						for (Component component : priorityTarget.unit){
							if (component.isEngaged()){
								boolean inQueue = false;
								for (RepairQueueItem queueItem : repairQueue){
									if (queueItem.repairable == component)
										inQueue = true;
								}
								
								if (!inQueue){
									int health = (int)(component.getHull()*100.0/component.type.hull);
									if (health < minHealth && health < autoRepair){
										minHealth = health;
										mostDamaged = component;
									}
								}
							}
						}
						if (mostDamaged != null){
							repairQueue.add(new RepairQueueItem(mostDamaged, priorityTarget,
										autoRepair/100.0*mostDamaged.type.hull, priorityTarget.repairRate,
										false, true));
						}else
							searching = false;
					}
				}
				
				if (priorityTarget.unit.hull < priorityTarget.unit.type.hull){
					repairQueue.add(new RepairQueueItem(priorityTarget.unit, priorityTarget,
									priorityTarget.unit.type.hull, priorityTarget.repairRate,
									false, true));
				}
			}
		}
		
	}
	
	private void repairRearmRecap(){
		RepairQueueItem repairItem = repairQueue.peek();
		
		// Repair single target
		if (repairItem != null && repairItem.target.inRange){
			if (repairItem.scrap){
				double materialScrapped = min(repairItem.repairRate, repairItem.repairable.getHull()-repairItem.repairTo);
				player.repair(repairItem.repairable, materialScrapped, true);
				material += materialScrapped*Main.config.scrapReturn;
				if (repairItem.repairable.getHull() <= repairItem.repairTo)
					repairQueue.poll();
			}else{
				double materialUsed = min(repairItem.repairTo-repairItem.repairable.getHull(), min(material, repairItem.repairRate));
				player.repair(repairItem.repairable, materialUsed, false);
				material -= materialUsed;
				if (repairItem.repairable.getHull() >= repairItem.repairTo)
					repairQueue.poll();
			}
		}
		
		// Recharge crafts in bay that aren't being repaired
		for (RepairTarget target : repairTargets){
			if (target.isInCraftBay && target.unit.getEnergy() < target.unit.type.capacitor){
				target.doneRecharge = false;
				boolean busy = repairItem != null && repairItem.target == target;
				if (!busy){
					double addEnergy = target.unit.type.power;
					if (target.recharge && this.drainEnergy(type.craftRechargeRate, "Craft", target.unit.type.name))
						addEnergy += type.craftRechargeRate;
					target.unit.addEnergy(addEnergy);
				}
			}else
				target.doneRecharge = true;
		}
		
		// Rearm next unit in cycle, skip crafts being repaired or recharged
		if (--timeToRearm <= 0){
			rearmIndex++;
			for (int end = rearmIndex+repairTargets.size(); rearmIndex < end; rearmIndex++){
				RepairTarget target = repairTargets.get(rearmIndex%repairTargets.size());
				Unit unit = target.unit;
				
				target.doneRearm = true;
				if (target.rearm){
					for (int a = 0; a < unit.ammo.length; a++){
						int targetAmmoCount = (int)((this.ammo[a] + unit.ammo[a])*unit.getWeaponCount(a)/(unit.getWeaponCount(a) + this.getWeaponCount(a)));
						targetAmmoCount = min(targetAmmoCount, unit.getMaxAmmo(a));
						if (this.ammo[a] > 0 && unit.ammo[a]+1 <= targetAmmoCount){
							target.doneRearm = false;
							boolean busy = (repairItem != null && repairItem.target == target) || (target.recharge && !target.doneRecharge);
							if (target.inRange && (!target.isInCraftBay || !busy)){
								unit.changeAmmo(a, 1);
								this.changeAmmo(a, -1);
								timeToRearm = (int)round(Main.ammoMass[a]*type.ammoTransferTimePerMass);
								return;
							}
						}
					}
				}
				
			}
		}
		
	}
	
	public void takeHit(double posX, double posY,
			Component subTarget, double direction, boolean continuous,
			double explosiveDamage, double kineticDamage, double EMPDamage){
		
		double shieldStrength, shieldDamage;
		if (abs(direction) < 90){
			shieldStrength = this.frontShield/type.frontShield;
			
			double explosiveShieldDamage = this.frontShield/type.frontShield*explosiveDamage;
			explosiveDamage -= explosiveShieldDamage;
			this.frontShield -= explosiveShieldDamage*Main.config.explosiveShieldDamage;
			
			double kineticShieldDamage = this.frontShield/type.frontShield*kineticDamage;
			kineticDamage -= kineticShieldDamage;
			this.frontShield -= kineticShieldDamage*Main.config.kineticShieldDamage;
			
			EMPDamage = EMPDamage*(this.frontShield/type.frontShield);
			
			shieldDamage = (kineticShieldDamage + explosiveShieldDamage)/type.frontShield;
		}else{
			shieldStrength = this.rearShield/type.rearShield;
			
			double explosiveShieldDamage = this.rearShield/type.rearShield*explosiveDamage;
			explosiveDamage -= explosiveShieldDamage;
			this.rearShield -= explosiveShieldDamage*Main.config.explosiveShieldDamage;
			
			double kineticShieldDamage = this.rearShield/type.rearShield*kineticDamage;
			kineticDamage -= kineticShieldDamage;
			this.rearShield -= kineticShieldDamage*Main.config.kineticShieldDamage;
			
			EMPDamage = EMPDamage*(this.rearShield/type.rearShield);
			
			shieldDamage = (kineticShieldDamage + explosiveShieldDamage)/type.rearShield;
		}
		
		double oldHull = hull;
		super.takeHit(posX, posY, subTarget, direction, continuous, explosiveDamage, kineticDamage, EMPDamage);
		
		double hitDamage = (oldHull - hull)/type.hull;
		double totalDamage = (type.hull - hull)/type.hull;
		for (int x = 1; x <= 3; x++){
			if (RANDOM.nextDouble() < 4.5*hitDamage*(0.2 + totalDamage)){
				if (RANDOM.nextDouble() < 0.3){
					effects.add(new VentEffect());
				}else
					effects.add(new ArcEffect());
			}
		}
		
		if (continuous)
			shieldDamage *= sqrt(Main.TPS);
		double shieldEffectStrength = sqrt(shieldStrength*shieldDamage);
		if (shieldEffectStrength > 0.1){
			int numShieldEffects = 0;
			for (int x = 0; x < effects.size(); x++){
				if (effects.get(x) instanceof ShieldEffect)
					numShieldEffects++;
			}
			int index = 0;
			while (numShieldEffects > MAX_SHIELD_EFFECTS){
				if (effects.get(index) instanceof ShieldEffect){
					effects.remove(index);
					numShieldEffects--;
				}else
					index++;
			}
			effects.add(new ShieldEffect(posX, posY, shieldEffectStrength));
		}
	}
	
	public int getCost(){
		int cost = super.getCost();
		for (Craft craft : crafts)
			cost += craft.getCost();
		return cost;
	}
	
	public int totalCraftMass(){
		int sum = 0;
		for (Craft craft : crafts)
			sum += craft.type.mass;
		return sum;
	}
	
	public void setQueueStatus(Craft craft, QueueStatus status){
		for (Iterator<QueuedCraft> i = toLaunch.iterator(); i.hasNext();){
			QueuedCraft queuedCraft = i.next();
			if (queuedCraft.craft == craft){
				if (status == QueueStatus.QUEUED){
					queuedCraft.catapult = false;
				}else if (status == QueueStatus.UNQUEUED){
					i.remove();
				}else if (status == QueueStatus.CATAPULT)
					queuedCraft.catapult = true;
				return;
			}
		}
		if (crafts.contains(craft) && (status == QueueStatus.QUEUED || status == QueueStatus.CATAPULT))
			toLaunch.add(new QueuedCraft(craft, status == QueueStatus.CATAPULT));
	}
	
	public QueueStatus getQueueStatus(Craft craft){
		for (Iterator<QueuedCraft> i = toLaunch.iterator(); i.hasNext();){
			QueuedCraft queuedCraft = i.next();
			if (queuedCraft.craft == craft)
				return queuedCraft.catapult ? QueueStatus.CATAPULT : QueueStatus.QUEUED;
		}
		return QueueStatus.UNQUEUED;
	}
	
	public void retrieveCraft(Craft craft){
		this.accel(-speed(craft)*craft.type.mass/this.type.mass, velBearing(craft)+getAngle());
		this.addMass(craft.mass);
		
		craft.dock();
		Main.game.removeControllable(craft);
		crafts.add(craft);
		
		RepairTarget target = new RepairTarget(craft, type.craftRepairRate, true);
		target.repair = craftDefaultRepair;
		target.recharge = craftDefaultRecharge;
		target.rearm = craftDefaultAmmo;
		repairTargets.add(target);
	}
	
	public void warpIn(){
		warpTime = WARP_TIME;
		
		double[] playerVel = Main.game.arena.teamVel[player.position];
		double velRad = random.nextDouble()*WARP_VEL_RADIUS, velAngle = random.nextDouble()*360.0;
		double warpVelX = velRad*cos(toRadians(velAngle)) + playerVel[0];
		double warpVelY = velRad*sin(toRadians(velAngle)) + playerVel[1];
		
		int[] playerPos = Main.game.arena.teamPos[player.position];
		double warpAngle = toDegrees(atan2(-playerPos[0], playerPos[1]));
		
		this.place(-WARP_DIST*sin(toRadians(warpAngle)), WARP_DIST*cos(toRadians(warpAngle)),
				warpVelX, warpVelY, warpAngle, 0);
		
		double posRad = random.nextDouble()*WARP_POS_RADIUS, posAngle = random.nextDouble()*360.0;
		warpPosX = playerPos[0] + posRad*cos(toRadians(posAngle));
		warpPosY = playerPos[1] + posRad*sin(toRadians(posAngle));
		
		player.controllables.add(this);
		for (Player otherPlayer : Main.game.players)
			otherPlayer.setVisibleSprites();
		Main.game.playSound(warpSound, this, false);
	}
	
	public boolean isReady(Craft craft){
		if (!crafts.contains(craft))
			return false;
		for (RepairTarget target : repairTargets){
			if (target.unit == craft)
				return target.doneRearm && target.doneRecharge && target.doneRepair;
		}
		return false;
	}
	
	public void setPlayer(Player player){
		this.player = player;
		for (Craft craft : crafts)
			craft.player = player;
	}
	
	public void initializeAmmoAndMass(){
		super.initializeAmmoAndMass();
		material = type.storageSpace*(1.0-ammoRatio)/Main.config.massPerMaterial;
		if (crafts != null){
			for (Craft craft : crafts){
				craft.initializeAmmoAndMass();
				mass += craft.mass;
			}
		}
		mass += material*Main.config.massPerMaterial;
	}
	
	public void removeControllable(Controllable controllable){
		super.removeControllable(controllable);
		for (int x = 0; x < repairTargets.size(); x++){
			RepairTarget target = repairTargets.get(x);
			if (target.unit == controllable && !crafts.contains(target.unit))
				removeRepairTarget(target);
		}
	}
	
	public void removeRepairTarget(RepairTarget target){
		repairTargets.remove(target);
		for (Iterator<RepairQueueItem> i = repairQueue.iterator(); i.hasNext();){
			if (i.next().target == target)
				i.remove();
		}
	}
	
	public RepairTarget getRepairTarget(Unit unit){
		for (RepairTarget target : repairTargets){
			if (target.unit == unit)
				return target;
		}
		return null;
	}
	
	public void autoLoadout(){
		super.autoLoadout();
		
		crafts.clear();
		if (type.totalCraftMass > CraftType.smallestCraftMass){
			for (int x = 0; x < 100; x++){
				CraftType craftType = type.autoLoadoutCraft();
				if (craftType.mass+totalCraftMass() < type.totalCraftMass){
					Craft craft = new Craft(craftType);
					craft.autoLoadout();
					crafts.add(craft);
				}
			}
		}
	}
	
	public void initialize(){
		super.initialize();
		
		repairTargets.clear();
		repairTargets.add(new RepairTarget(this, type.selfRepairRate, false));
		craftDefaultAmmo = craftDefaultRepair = craftDefaultRecharge = true;
		for (Craft craft : crafts)
			repairTargets.add(new RepairTarget(craft, type.craftRepairRate, true));
		
		Map<String, double[]> shieldsCap = new HashMap<String, double[]>(4);
		shieldsCap.put("Unreserved", new double[3]);
		shieldsCap.put("Front", new double[3]);
		shieldsCap.put("Rear", new double[3]);
		capacitor.put("Shields", shieldsCap);
		
		if (type.totalCraftMass > 0){
			Map<String, double[]> craftsCap = new HashMap<String, double[]>();
			craftsCap.put("Unreserved", new double[3]);
			for (CraftType type : Main.craftTypes)
				craftsCap.put(type.name, new double[3]);
			capacitor.put("Craft", craftsCap);
		}
		
		//initializeAmmoAndMass();
		frontShield = type.frontShield;
		rearShield = type.rearShield;
		
		for (Craft craft : crafts)
			craft.initialize();
		
		effects.clear();
		effects.add(new WarpEffect());
	}
	
	public void explode(){
		super.explode();
		Main.game.startExploding(this);
	}
	
	protected List<? extends Effect> getEffects(){
		return effects;
	}
	
	public Ship copy(){
		Ship newShip = new Ship(type);
		
		newShip.setName(getName());
		newShip.ammoRatio = ammoRatio;	
		copyComponentsAndAmmoTo(newShip);
		newShip.ammoRatio = ammoRatio;
		
		for (Craft craft : crafts){
			Craft newCraft = new Craft(craft.type);
			craft.copyComponentsAndAmmoTo(newCraft);
			newCraft.setName(craft.getName());
			newShip.crafts.add(newCraft);
		}
		
		return newShip;
	}
	
	public double getWeaponCount(int ammoType){
		double count = super.getWeaponCount(ammoType);
		for (Craft craft : crafts){
			for (Weapon weapon : craft.weapons){
				if (weapon.type.ammoType == ammoType)
					count += 0.4;
			}
		}
		return count;
	}
	
	public WarpInMsg getWarpMsg(){
		if (warpMsg == null){
			warpMsg = new WarpInMsg();
			warpMsg.ship = this;
		}
		warpMsg.randomSeed = random.nextInt();
		random.setSeed(warpMsg.randomSeed);
		return warpMsg;
	}
	
	public CraftLaunchMsg getLaunchMsg(Craft craft, boolean catapult){
		if (launchMsg == null){
			launchMsg = new CraftLaunchMsg();
			launchMsg.ship = this;
		}
		launchMsg.catapult = catapult;
		launchMsg.craft = craft;
		return launchMsg;
	}
	
	public CraftDockMsg getDockMsg(Craft craft){
		if (dockMsg == null){
			dockMsg = new CraftDockMsg();
			dockMsg.ship = this;
		}
		dockMsg.craft = craft;
		return dockMsg;
	}
	
	public void draw(Graphics2D g, GameWindow window){
		int posX = window.posXOnScreen(renderPosX), posY = window.posYOnScreen(renderPosY);
		int size = getRenderSize(window.getRenderZoom());
		
		if (posX > -size && posX < window.windowResX+size && posY > -size && posY < window.windowResY+size){
			
			// Display shield status circle in bottom corner
			if (getImage(window.getRenderZoom()) != null){
				if (warpTime <= 0){
					if (window.getPlayer().knowHealth(this)){
						g.setColor(Utility.getColor(1-(type.frontShield-frontShield)/(0.8*type.frontShield), 0.0));
						g.fillArc(posX+size*4/11, posY+size*4/11,
								Main.options.statusSize, Main.options.statusSize, -(int)getAngle(), 182);
						g.setColor(Utility.getColor(1-(type.rearShield-rearShield)/(0.8*type.rearShield), 0.0));
						g.fillArc(posX+size*4/11, posY+size*4/11,
								Main.options.statusSize, Main.options.statusSize, -(int)getAngle(), -182);
					}
				}
					
				if (player.team == window.getPlayer().team && type.totalCraftMass > 0){
					g.setFont(statusFont);
					g.setColor(STATUS_OUTLINE_COLOR);
					g.drawString(String.valueOf(crafts.size()), posX-size*4/11-2, posY+size*4/11+8);
				}
			}
		}
		
		super.draw(g, window);
	}
	
	public SidePanel getMenu(){
		craftPanel.setShip(this);
		repairPanel.setShip(this);
		SidePanel menu = super.getMenu();
		if (type.totalCraftMass > 0)
			controls.addTab("CFT", craftPanel);
		controls.addTab("REP", repairPanel);
		return menu;
	}
	
	public static void createMenu(){
		craftPanel = new CraftPanel();
		repairPanel = new RepairPanel();
		warpSound = new Sound(new File("data/warp.wav"));
		warpSound.load();
		BufferedImage shieldImg;
		try{
			shieldImg = Main.convert(ImageIO.read(new File("data/shield.png")));
		}catch(IOException e){
			shieldImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}
		shieldRenderable = new Renderable(4, 1, false);
		shieldRenderable.load(new BufferedImage[]{shieldImg}, 1.0);
	}
	
	// Output ship to file
	public void write(BufferedWriter out) throws IOException{
		super.write(out);
		out.write(ammoRatio+"\n");
		out.write(crafts.size()+"\n");
		for (Craft craft : crafts)
			craft.write(out);
	}
	
	// Read in ship from file, checking for validity of outfits
	public static Ship read(BufferedReader in){
		try{
			Unit unit = Unit.read(in);
			if (!(unit instanceof Ship))
				return null;
			Ship ship = (Ship)unit;
			ship.ammoRatio = Double.parseDouble(in.readLine());
			int numCrafts = Integer.parseInt(in.readLine());
			for (int x = 0; x < numCrafts; x++){
				unit = Unit.read(in);
				if (unit instanceof Craft && unit.type.mass <= ship.type.craftMass &&
						unit.type.mass+ship.totalCraftMass() <= ship.type.totalCraftMass)
					ship.crafts.add((Craft)unit);
			}
			return ship;
		}catch (IOException|NumberFormatException e){
			return null;
		}
	}
	
	private class QueuedCraft{
		int timeToLaunch;
		Craft craft;
		boolean catapult;
		
		public QueuedCraft(Craft craft, boolean catapult){
			this.craft = craft;
			timeToLaunch = (int)(type.craftLaunchTime*craft.type.launchTimeMultiplier);
			this.catapult = catapult;
		}
		
		public boolean getReady(){
			return --timeToLaunch <= 0;
		}
	}
	
	// Electrical arcing damage effect
	static final int ARC_SEGMENT_LENGTH = 4, MIN_ARC_LENGTH = 10, MAX_ARC_LENGTH = 23;
	static final Color ARC_COLOR = new Color(150, 225, 255);
	private class ArcEffect implements Effect{
		int lifetime;
		final double angle1, length1, angle2, length2;
		
		public ArcEffect(){
			lifetime = (int)(Main.TPS*(0.5 + 1.5*RANDOM.nextDouble()));
			
			boolean[][] map = getContactMap();
			int width = map.length, height = map[0].length;
			int startX = 0, startY = 0;
			do{
				startX = RANDOM.nextInt(width);
				startY = RANDOM.nextInt(height);
			}while (!map[startX][startY]);
			int endX = 0, endY = 0;
			do{
				endX = (int)((2*RANDOM.nextDouble() - 1.0)*(MAX_ARC_LENGTH-MIN_ARC_LENGTH));
				endX += startX + MIN_ARC_LENGTH*signum(endX);
				endY = (int)((2*RANDOM.nextDouble() - 1.0)*(MAX_ARC_LENGTH-MIN_ARC_LENGTH));
				endY += startY + MIN_ARC_LENGTH*signum(endY);
			}while (endX < 0 || endX >= width || endY < 0 || endY >= height || !map[endX][endY]);
			
			angle1 = toDegrees(atan2(startX-width/2, height/2-startY)) - renderAngle;
			length1 = hypot(startX-width/2, height/2-startY);
			angle2 = toDegrees(atan2(endX-width/2, height/2-endY)) - renderAngle;
			length2 = hypot(endX-width/2, height/2-endY);
		}
		
		public void act(){
			lifetime--;
			if (lifetime <= 0)
				effects.remove(this);
		}
		
		public void draw(Graphics2D g, GameWindow window, int originX, int originY){
			if (window.getZoom() > 0.12){
				double x1 = renderPosX + length1*sin(toRadians(angle1+renderAngle));
				double y1 = renderPosY - length1*cos(toRadians(angle1+renderAngle));
				double x2 = renderPosX + length2*sin(toRadians(angle2+renderAngle));
				double y2 = renderPosY - length2*cos(toRadians(angle2+renderAngle));
				
				int nSeg = ((int)sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)))/ARC_SEGMENT_LENGTH;
				double deltaLengthX = (x2-x1)/nSeg;
				double deltaLengthY = (y2-y1)/nSeg;
				double deltaOffsetX = deltaLengthY;
				double deltaOffsetY = -deltaLengthX;
				
				g.setColor(ARC_COLOR);
				double offset = 0.0;
				double posX = x1, posY = y1;
				for (int x = 0; x < nSeg; x++){
					double newPosX, newPosY;
					if (x == nSeg-1){
						newPosX = x2;
						newPosY = y2;
					}else{
						double deltaOffset = 2.0*random() - 1.0;
						if ((offset > 1.5 && deltaOffset > 0.0) || (offset < -1.5 && deltaOffset < 0.0))
							deltaOffset *= -1;
						newPosX = posX + deltaLengthX + deltaOffsetX*deltaOffset;
						newPosY = posY + deltaLengthY + deltaOffsetY*deltaOffset;
						offset += deltaOffset;
					}
					g.drawLine(window.posXOnScreen(posX), window.posYOnScreen(posY),
							window.posXOnScreen(newPosX), window.posYOnScreen(newPosY));
					posX = newPosX;
					posY = newPosY;
				}
			}
		}
		
		public boolean drawToUnit(){
			return false;
		}
	}
	
	// Gas venting damage effects
	static final int MIN_VENT_LENGTH = 24, MAX_VENT_LENGTH = 54;
	static final int VENT_START_OPACITY = 120, VENT_END_OPACITY = 1;
	static final Color[][] VENT_COLOR_ARR = new Color[2][MAX_VENT_LENGTH];
	static final double VENT_SPRAY_SIZE = 0.46;
	private class VentEffect implements Effect{
		int lifetime, age;
		final double angle, length, ventAngle;
		final int ventLength;
		final double[][] offset;
		
		public VentEffect(){
			if (VENT_COLOR_ARR[0][0] == null){
				double opacity = VENT_START_OPACITY;
				for (int x = 0; x < MAX_VENT_LENGTH; x++){
					VENT_COLOR_ARR[0][x] = new Color(220, 210, 255, (int)opacity);
					VENT_COLOR_ARR[1][x] = new Color(220, 210, 255, 2*(int)opacity);
					opacity -= (VENT_START_OPACITY - VENT_END_OPACITY)/(MAX_VENT_LENGTH - 1.0);
				}
			}
			
			lifetime = (int)(Main.TPS*(2.5 + 6.0*RANDOM.nextDouble()));
			age = 0;
			
			boolean[][] map = getContactMap();
			int width = map.length, height = map[0].length;
			int posX = 0, posY = 0;
			do{
				posX = RANDOM.nextInt(width);
				posY = RANDOM.nextInt(height);
			}while (abs(posX) < width/3 || abs(posY) < height/3 || !map[posX][posY]);
			
			angle = toDegrees(atan2(posX-width/2, height/2-posY)) - renderAngle;
			length = hypot(posX-width/2, height/2-posY);
			ventAngle = angle + 30.0*RANDOM.nextDouble();
			ventLength = MIN_VENT_LENGTH + RANDOM.nextInt((MAX_VENT_LENGTH - MIN_VENT_LENGTH)*type.renderable.size/ShipType.maxShipSize);
			offset = new double[ventLength][2];
		}
		
		public void act(){
			int index = (age/2)%ventLength;
			int previous = index == 0 ? ventLength-1 : index-1;
			offset[index][0] = Utility.clamp(offset[previous][0] + VENT_SPRAY_SIZE*(2*random()-1)/4, VENT_SPRAY_SIZE);
			offset[index][1] = Utility.clamp(offset[previous][1] + VENT_SPRAY_SIZE*(2*random()-1)/4, VENT_SPRAY_SIZE);
			
			age++;
			lifetime--;
			if (lifetime <= 0)
				effects.remove(this);
		}
		
		public void draw(Graphics2D g, GameWindow window, int originX, int originY){
			if (window.getZoom() > 0.15){
				double posX = renderPosX + length*sin(toRadians(angle+renderAngle));
				double posY = renderPosY - length*cos(toRadians(angle+renderAngle));
				double deltaX = sin(toRadians(ventAngle+renderAngle));
				double deltaY = -cos(toRadians(ventAngle+renderAngle));
				
				int size = 1;
				boolean big = false;
				for (int x = 0; x < ventLength; x++){
					
					if ((big || window.getZoom() > 0.3) && x <= age/2 && ventLength-x < lifetime/2){
						g.setColor(VENT_COLOR_ARR[big ? 0 : 1][min(MAX_VENT_LENGTH, x*MAX_VENT_LENGTH/ventLength+1)]);
						int index = (age/2-x)%ventLength;
						int drawSize = (int)((big ? 1.0 : 0.6)*size*window.getZoom() + 0.5)/2;
						g.fillOval(window.posXOnScreen(posX + offset[index][0]*drawSize)-drawSize/2,
								window.posYOnScreen(posY + offset[index][1]*drawSize)-drawSize/2, drawSize, drawSize);
					}
					
					size += 1;
					posX += deltaX;
					posY += deltaY;
					big = !big;
				}
			}
		}
		
		public boolean drawToUnit(){
			return false;
		}
	}
	
	// Flash effect when shield takes a hit
	static final int MAX_SHIELD_EFFECT_LIFETIME = (int)(0.9*Main.TPS);
	private class ShieldEffect implements Effect{
		int lifetime;
		final double angle, length;
		final double scale, strengthMult;
		
		public ShieldEffect(double posX, double posY, double strength){
			lifetime = (int)(MAX_SHIELD_EFFECT_LIFETIME*min(0.99f, strength));
			scale = 0.65*sqrt(type.renderable.size/(double)ShipType.maxShipSize);
			strengthMult = sqrt(sqrt(strength))*sqrt(1.0/MAX_SHIELD_EFFECT_LIFETIME);
			
			posX -= getPosX();
			posY -= getPosY();
			angle = toDegrees(atan2(posX, posY)) + renderAngle;
			length = hypot(posX, posY);
		}
		
		public void act(){
			lifetime--;
			if (lifetime <= 0)
				effects.remove(this);
		}
		
		public void draw(Graphics2D g, GameWindow window, int originX, int originY){
			double zoom = window.getRenderZoom();
			if (zoom > 0.10){
				int posX = (int)(zoom*length*sin(toRadians(angle-renderAngle)));
				int posY = (int)(zoom*length*cos(toRadians(angle-renderAngle)));
				
				float opacity = (float)min(1.0, strengthMult*sqrt(lifetime));
				if (opacity <= 0)
					return;
				g.setComposite(AlphaComposite.SrcAtop.derive(opacity));
				
				Image shieldImg = shieldRenderable.getImage(scale*window.getRenderZoom(), 0, true, 0);
				int shieldSize = shieldImg.getWidth(null)/2;
				
				g.drawImage(shieldImg, originX-shieldSize+posX, originY-shieldSize+posY, null);
				
				g.setComposite(Window.DEFAULT_COMPOSITE);
			}
		}
		
		public boolean drawToUnit(){
			return true;
		}
	}
	
	// Fade in from white warp-in effect
	static final Color WARP_COLOR = new Color(255, 255, 255);
	private class WarpEffect implements Effect{
		public void act(){
			if (warpTime <= 0)
				effects.remove(this);
		}
		
		public void draw(Graphics2D g, GameWindow window, int originX, int originY){
			if (warpTime > 0){
				int size = getRenderSize(window.getRenderZoom());
				double opacity = min(1.0, warpTime/(1.6*Main.TPS));
				g.setComposite(AlphaComposite.SrcAtop.derive((float)opacity));
				g.setColor(WARP_COLOR);
				g.fillRect(originX-size/2, originY-size/2, size, size);
				g.setComposite(Window.DEFAULT_COMPOSITE);
			}
		}
		
		public boolean drawToUnit(){
			return true;
		}
	}
	
}
