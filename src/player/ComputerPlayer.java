import static java.lang.Math.*;
import java.util.*;

// Local computer controlled player. All AI behavior is implemented in this class.
// A list of available actions is maintained , e.g. attack a target or capture an objective.
// Each unit periodically computes a priority for each action in the list and chooses the bese one.
// Most complexity is in the calculation of action priorities. Priority values are in units of time,
// with larger values being lower priority

public class ComputerPlayer extends Player{
	
	static final int ACTION_REFRESH_PERIOD = 200;
	
	private static int actionCounter = 0;
	
	private final List<Action> actions;
	private final double boosterThreshold, captureCaution;
	private double timeToWin, timeToLose;
	private double objectiveFracCapturing;
	private boolean attackDisabled, captureDisabled;
	
	public ComputerPlayer(String name, int team, List<Ship> ships, int budget, Arena arena){
		super(name, team, ships, budget, arena);
		
		actions = new ArrayList<Action>();
		captureDisabled = false;
		attackDisabled = false;
		
		setFireModes();
		
		boosterThreshold = 0.4 + 0.4*random();
		
		// Randomly generate parameter dictating overall strategy, with larger value causing the fleet
		// to stick together more and smaller value causing more spreading out to capture objectives quickly
		captureCaution = (0.40 + 0.60*random())*30000*Main.TPS;
	}
	
	// Use combination of heuristics and randomness to set weapon fire modes at start of game
	private void setFireModes(){
		for (Ship ship : this.ships){
			for (Component component : ship){
				if (component instanceof Weapon){
					if (attackDisabled){
						((Weapon)component).setFireMode(Weapon.FireMode.DISABLED);
					}else{
						Weapon weapon = (Weapon)component;
						weapon.setFireMode(Weapon.FireMode.AUTONOMOUS);
						if (weapon.type.reloadTime > Main.TPS){
							weapon.setFireMode(Weapon.FireMode.AUTOMATIC);
							weapon.autoMissiles = false;
							weapon.autoCraft = false;
							if (random() < 0.5 && weapon instanceof Launcher && ((Launcher)weapon).type.deltaV > 14.0){
								((Launcher)weapon).volleySize = 1+(int)(3*random());
							}else if (random() < 0.5 && weapon instanceof Gun && ((Gun)weapon).type.velocity*Main.TPS > 700){
								weapon.autoCraft = true;
							}
						}
					}
				}
			}
		}
	}
	
	public void start(int position){
		super.start(position);
		
		for (Arena.Objective objective : Main.game.arena.objectives){
			actions.add(new CaptureAction(objective));
			actions.add(new DefendObjectiveAction(objective));
		}
		
		for (Ship ship : ships)
			actions.add(new EscortAction(ship));
		
	}
	
	public void contact(Projectile projectile, Controllable controllable){
		if (projectile.contact(controllable))
			controllable.explode();
	}
	
	public void contact(Beam beam, Controllable controllable, double posX, double posY){
		if (beam.contact(controllable, posX, posY))
			controllable.explode();
	}
	
	public boolean isMaster(){
		return true;
	}
	
	public void act(){
		super.act();
		
		if (turnsPassed%303 == 0){
			
			timeToLose = Main.game.getTimeToLose(team);
			timeToWin = Main.game.getTimeToWin(team);
			
			// Compute proportion of map objectives currently being captured, used to compute priority of capture action
			objectiveFracCapturing = 0.0;
			for (Unit unit : units){
				if (unit.orders().getOrder() instanceof Capture)
					objectiveFracCapturing += ((Capture)unit.orders().getOrder()).objective.value;
			}
			objectiveFracCapturing /= Main.game.arena.totalObjectiveValue;
			
			// Check if can afford the next ship in order
			for (Ship ship : ships){
				if (ship.outOfArena){
					if (ship.getCost() <= getBudget())
						warpIn(ship);
					break;
				}
			}
			
			// Check to launch craft, or enable or disable systems
			for (Controllable controllable : controllables){
				if (controllable instanceof Ship){
					Ship ship = (Ship)controllable;
					for (Craft craft : ship.crafts){
						if ((craft.type.captureRate > 0 || craft.type.weaponHardpoints.length == 0) && ship.isReady(craft))
							ship.setQueueStatus(craft, Ship.QueueStatus.QUEUED);
					}
					
					for (System system : ship.systems){
						if (system instanceof Cloak){
							if (ship.getEnergy() < ship.type.capacitor/2){
								system.setEngaged(false);
							}else if (ship.getEnergy() > ship.type.capacitor*4/5)
								system.setEngaged(true);
						}else if (system instanceof Booster){
							if (ship.getHull() < boosterThreshold*ship.type.hull)
								system.setEngaged(true);
						}
					}
				}
			}
		}
		
		// Find and perform the best action for each unit. The best attack action is used to
		// set the unit's target, even in the case that a non-attack action is performed
		if (turnsPassed%ACTION_REFRESH_PERIOD == 0){
			for (Controllable controllable : controllables){
				if (controllable instanceof Unit){
					Unit unit = (Unit)controllable;
					
					double bestPriority = Double.MAX_VALUE;
					Action bestAction = null;
					double bestAttackPriority = Double.MAX_VALUE;
					AttackAction bestAttackAction = null;
					for (Action action : actions){
						double priority = action.getPriority(unit).value;
						
						if (priority < bestPriority){
							bestAction = action;
							bestPriority = priority;
						}
						if (action instanceof AttackAction){
							if (priority <= bestAttackPriority){
								bestAttackAction = (AttackAction)action;
								bestAttackPriority = priority;
							}
						}
					}
					
					if (bestAction != null){
						bestAction.perform(unit);
						if (Main.DEBUG)
							unit.debug = bestAction.getClass().getSimpleName()+" "+Main.game.turn;
					}
					
					if (bestAttackAction != null && bestAttackAction != bestAction)
						unit.setTarget(bestAttackAction.target);
				}
			}
		} // End action refresh
		
		// Compute and store priorities for each action for each unit at staggered intervals.
		for (int x = 0; x < actions.size(); x++){
			Action action = actions.get(x);
			if ((turnsPassed + action.updateTime)%ACTION_REFRESH_PERIOD == 0){
				if (!action.isValid()){
					actions.remove(x);
					x--;
				}else
					action.setPriority();
			}
		}
		
	}
	
	public boolean addSensorSighting(SensorSighting sighting, Sprite sprite){
		if (super.addSensorSighting(sighting, sprite)){
			actions.add(new ScoutAction(getSensorTarget(sprite)));
			return true;
		}else
			return false;
	}
	
	public boolean spotTarget(Controllable controllable){
		if (super.spotTarget(controllable)){
			if (controllable instanceof Unit)
				actions.add(new AttackAction(getTarget(controllable)));
			return true;
		}else
			return false;
	}
	
	public void setCaptureDisabled(boolean captureDisabled){
		this.captureDisabled = captureDisabled;
	}
	
	public void setAttackDisabled(boolean attackDisabled){
		this.attackDisabled = attackDisabled;
		setFireModes();
	}
	
	
	private double threatAt(Locatable location){
		double threat = 0.0;
		for (Player player : Main.game.players){
			if (player.team != team){
				for (Controllable enemyUnit : player.controllables){
					if (enemyUnit instanceof Unit)
						threat += unitKnowledge((Unit)enemyUnit)*unitPresence((Unit)enemyUnit, location);
				}
			}
		}
		return threat;
	}
	
	// Heuristic for how well the AI knows the location of a given unit
	private double unitKnowledge(Unit unit){
		if (unit.getPlayer().team == team)
			return 1.0;
		Target target = getTarget(unit);
		if (target != null && target.isVisible())
			return 1.0;
		double knowledge = 0.0;
		if (target != null && target.getAge() < 25*Main.TPS)
			knowledge = 1.0 - target.getAge()/(25.0*Main.TPS);
		if (getSensorTarget(unit) != null)
			knowledge += 0.6*(1 - knowledge);
		return knowledge;
	}
	
	// Heuristic for how powerful the AI judges an enemy unit, in units of money
	private static double unitStrength(Unit unit){
		int craftMass = 0;
		double health = unit.getHull();
		double maxHealth = unit.type.hull;
		if (unit instanceof Ship){
			Ship ship = (Ship)unit;
			craftMass = ship.type.totalCraftMass;
			health += 0.5*(ship.frontShield + ship.rearShield);
			maxHealth += 0.5*(ship.type.frontShield + ship.type.rearShield);
		}
		return unit.getCost()*(health/maxHealth)*
				unit.getWeaponReadiness()/max(1.0, 0.8 + craftMass*2.0/unit.type.mass);
	}
	
	// Heuristic for how much power a given unit is able to influence a given point in space with, in units of money
	private static double unitPresence(Unit unit, Locatable location){
		double time = Utility.approxTime(unit, location);
		return unitStrength(unit)/(1+pow(max(0, time-0.8*unitRange(unit))/(25*Main.TPS), 1.4));
	}
	
	// Approximate weapon range of a unit
	private static double unitRange(Unit unit){
		double sum = 0.0;
		int count = 0;
		for (Weapon weapon : unit.weapons){
			if (weapon.isOperational()){
				count++;
				double range = 0.0;
				if (weapon instanceof Beam){
					range = ((Beam)weapon).type.length;
				}else if (weapon instanceof Launcher){
					range = ((Launcher)weapon).autoRange*((Launcher)weapon).type.deltaV/2;
				}else if (weapon instanceof Gun)
					range = ((Gun)weapon).autoRange*((Gun)weapon).type.velocity/2;
				sum += sqrt(2*range/unit.getAccel());
			}
		}
		return count > 0 ? sum/count : 0.0;
	}
	
	// How far away a unit is from any objectives, used to avoid chasing an enemy pointlessly into empty space
	private static double timeToObjective(Unit unit, Locatable location){
		if (Main.game.arena.objectives.length == 0){
			return 0;
		}else{
			double time = Double.MAX_VALUE;
			for (Arena.Objective objective : Main.game.arena.objectives)
				time = min(time, Utility.approxTime(objective, location, unit.getAccel()));
			return time;
		}
	}
	
	// Heuristic for how well suited one unit is for fighting another, based on the loadouts
	private static double unitEffectiveness(Unit unit, Unit target){
		double pointDefense = 0.0;
		double defenseAngle = target.heading(unit);
		for (Weapon weapon : target.weapons){
			if (weapon.isOperational())
				pointDefense += weapon.getPointDefenseEffect()/(1+0.4*log1p(weapon.getArcViolation(defenseAngle)/1.0));
		}
		
		int sumCost = 0;
		double sumEffect = 0.0;
		for (Weapon weapon : unit.weapons){
			if (weapon.isOperational()){
				double effect = weaponEffectiveness(weapon.type, target.type, pointDefense);
				sumEffect += weapon.type.cost*effect;
				sumCost += weapon.type.cost;
			}
		}
		if (unit instanceof Ship){
			for (Craft craft : ((Ship)unit).crafts){
				for (Weapon weapon : craft.weapons){
					if (weapon.isOperational()){
						double effect = weaponEffectiveness(weapon.type, target.type, pointDefense);
						sumEffect += weapon.type.cost*effect;
						sumCost += weapon.type.cost;
					}
				}
			}
		}
		
		return sumCost == 0 ? 0.0 : sumEffect/sumCost;
	}
	
	// Huristic to compute how effective a given weapon type is against a given target
	private static double weaponEffectiveness(WeaponType weapon, UnitType target, double pointDefense){
		double baseExplosiveDamage = 0.0, baseKineticDamage = 0.0, EMDamage = 0.0, armor = 0.0;
		if (weapon instanceof GunType){
			GunType type = (GunType)weapon;
			baseExplosiveDamage = type.explosiveDamage;
			baseKineticDamage = type.kineticMultiplier*pow(type.velocity, type.kineticExponent);
			EMDamage = type.EMDamage;
			armor = target.armor;
		}else if (weapon instanceof BeamType){
			BeamType type = (BeamType)weapon;
			baseExplosiveDamage = type.explosiveDamagePerTurn;
			EMDamage = type.EMDamagePerTurn;
			armor = target.armor/Main.TPS;
		}
		double kineticDamage = Unit.damage(baseKineticDamage, armor);
		double explosiveDamage = Unit.damage(baseExplosiveDamage, armor);
		double fracDamage = (10*EMDamage + kineticDamage + explosiveDamage)/
				(10*EMDamage + baseKineticDamage + baseExplosiveDamage);
		
		double pointDefenseFactor = 1.0;
		if (weapon instanceof MissileType)
			pointDefenseFactor /= 0.6 + 1.4*pointDefense/(0.5 + 0.5*((GunType)weapon).projectilesPerShot);
		
		return fracDamage*pointDefenseFactor;
	}
	
	private abstract class Action{
		private final Map<Unit,Priority> priority;
		int updateTime;
		
		Action(){
			priority = new HashMap<Unit,Priority>();
			updateTime = actionCounter++;
		}
		
		void setPriority(){
			update();
			
			for (Controllable controllable : controllables){
				if (controllable instanceof Unit)
					getPriority((Unit)controllable).value = computePriority((Unit)controllable);
			}
		}
		
		Priority getPriority(Unit unit){
			Priority unitPriority = priority.get(unit);
			if (unitPriority == null){
				unitPriority = new Priority();
				priority.put(unit, unitPriority);
			}
			return unitPriority;
		}
		
		// Compute how much higher a given unit's priority is for performing this action than that of any other unit.
		// Used to coordinate actions that only need to be done by a single unit
		double getPriorityGap(Unit unit){
			if (unit == null){
				return 0.0;
			}else{
				double gap = Double.MAX_VALUE;
				double thisPriority = priority.get(unit).value;
				for (Action action : actions){
					if (action != this && action.priority.containsKey(unit))
						gap = min(gap, action.getPriority(unit).value - thisPriority);
				}
				return gap;
			}
		}
		
		abstract Locatable getObject();
		abstract void update();
		abstract double computePriority(Unit unit);
		abstract void perform(Unit unit);
		abstract boolean isValid();
	}
	
	// Action to capture a map objective
	private class CaptureAction extends Action{
		final Arena.Objective objective;
		Unit currentUnit;
		double priorityGap;
		double threat;
		int timePerformed;
		
		CaptureAction(Arena.Objective objective){
			this.objective = objective;
		}
		
		Locatable getObject(){
			return objective;
		}
		
		void update(){
			threat = threatAt(objective);
			
			currentUnit = null;
			for (Controllable controllable : controllables){
				if (controllable instanceof Unit){
					Order order = ((Unit)controllable).orders().getOrder();
					if (order instanceof Capture && ((Capture)order).objective == objective){
						currentUnit = (Unit)controllable;
						break;
					}
				}
			}
			priorityGap = getPriorityGap(currentUnit);
		}
		
		double computePriority(Unit unit){
			if (captureDisabled)
				return Double.MAX_VALUE;
			if (unit.orders().getOrder() instanceof Dock)
				return Double.MAX_VALUE;
			if (unit.type.captureRate == 0)
				return Double.MAX_VALUE;
			if (objective.isCaptured && objective.owner == team)
				return Double.MAX_VALUE;
			if (objective.getCapturingTeam() != -1 && objective.getCapturingTeam() != team)
				return Double.MAX_VALUE;
			
			double strength = unitStrength(unit);
			double time = Utility.approxTime(unit, objective);
			double priority = 0.8*time;
			
			if (currentUnit != null && currentUnit != unit)
				priority += priorityGap + 800*Main.TPS;
			
			Order currentOrder = unit.orders().getOrder();
			boolean isCapturing = currentOrder instanceof Capture && ((Capture)currentOrder).objective == objective;
			if (!isCapturing)
				priority += 20*Main.TPS;
			priority += 500*Main.TPS*threat/(strength + 150);
			double capTimeLeft = (objective.isCaptured ? objective.capAmount+objective.capSize : objective.capSize-objective.capAmount)/unit.type.captureRate;
			priority += 0.7*capTimeLeft;
			priority += 8*Main.TPS*unit.weapons.size()*unit.getWeaponReadiness();
			priority += 8*Main.TPS*strength/150.0;
			if (!isCapturing)
				priority += captureCaution*objectiveFracCapturing*objectiveFracCapturing;
			if (!objective.isCaptured && isCapturing && ((Capture)currentOrder).isCapturing())
				priority /= 1.0 + pow(5*Main.TPS/capTimeLeft, 2.0);
			priority /= sqrt(objective.value)*(objective.isCaptured ? 1.5 : 1.0);
			if (timeToLose < timeToWin)
				priority /= 1 + (200*Main.TPS)/timeToLose;
			
			if (unit instanceof Craft)
				priority = priority*0.5 - 200*Main.TPS;
			
			return priority - 15*Main.TPS;
		}
		
		void perform(Unit unit){
			if (timePerformed < Main.game.turn){
				timePerformed = Main.game.turn;
				Order currentOrder = unit.orders().getOrder();
				if (currentOrder instanceof Capture && ((Capture)currentOrder).objective == objective)
					return;
				unit.orders().clear();
				unit.orders().queueOrder(new Capture(objective));
			}
		}
		
		boolean isValid(){
			return true;
		}
	}
	
	// Order to attack an enemy unit
	private class AttackAction extends Action{
		final Target target;
		
		double threat;
		double targetStrength;
		double timeToObjective;
		
		public AttackAction(Target target){
			this.target = target;
		}
		
		Locatable getObject(){
			return target;
		}
		
		void update(){
			threat = threatAt(target);
			targetStrength = unitStrength((Unit)target.target);
			timeToObjective = timeToObjective((Unit)target.target, target);
		}
		
		double computePriority(Unit unit){
			Unit targetUnit = (Unit)target.target;
			
			if (attackDisabled)
				return Double.MAX_VALUE;
			
			if (unit.orders().getOrder() instanceof Dock)
				return Double.MAX_VALUE;
			
			double strength = 1.1*unitStrength(unit)*unitEffectiveness(unit, targetUnit);
			if (strength == 0)
				return Double.MAX_VALUE;
			
			// Determine how much stuff is already attacking this target. Priority is reduced if either too much or too little is already engaged
			double engagedStrength = 0.0;
			for (Player player : Main.game.players){
				if (player.team == team && player.isMaster()){
					for (Controllable controllable : player.controllables){
						if (controllable instanceof Unit && controllable != unit){
							Unit otherUnit = (Unit)controllable;
							double otherUnitEffectiveness = unitEffectiveness(otherUnit, targetUnit);
							strength += (otherUnit.getTarget() != null && otherUnit.getTarget().target == target.target ? 1.2 : 0.6)*
									otherUnitEffectiveness*unitPresence(otherUnit, target);
							Order order = otherUnit.orders().getOrder();
							if (order instanceof TrackOrder && target.equals(((TrackOrder)order).target))
								engagedStrength += otherUnitEffectiveness*unitStrength(otherUnit);
						}
					}
				}
			}
			
			double timeToTarget = Utility.approxTime(unit, target);
			double priority = timeToTarget;
			if (unit instanceof Craft)
				priority += 3*Utility.approxTime(((Craft)unit).getMothership(), target);
			priority += 750*Main.TPS*max(-0.20, threat*sqrt(unitEffectiveness(targetUnit, unit))/strength - 1.0);
			priority += 150*Main.TPS*pow(1 - unit.getHull()/unit.type.hull, 2.0);
			priority += 50*Main.TPS*(targetUnit.getHull()/targetUnit.type.hull - 1);
			if (target == unit.getTarget())
				priority -= 15*Main.TPS;
			double engagedStrengthRatio = engagedStrength/targetStrength;
			priority -= 50*Main.TPS*engagedStrengthRatio*exp(1.0 - engagedStrengthRatio);
			double strengthFactor = pow(strength/targetStrength, 0.35);
			priority += 60*Main.TPS*pow(target.getAge()/(20*Main.TPS), 4.0)*strengthFactor;
			priority += 70*Main.TPS*pow((1.4*timeToObjective + timeToTarget)/(35*Main.TPS), 2.0)*strengthFactor;
			if (targetUnit instanceof Craft && unit instanceof Ship)
				priority += 20*Main.TPS;
			return priority + 30*Main.TPS;
		}
		
		void perform(Unit unit){
			
			// Decide whether to use AttackFast or AttackSlow depending on weapon loadout
			int bombCount = 0, weaponCount = 0;
			for (Weapon weapon : unit.weapons){
				if (weapon.isOperational()){
					weaponCount++;
					if (weapon instanceof Launcher && ((Launcher)weapon).type.deltaV < 8.0)
						bombCount++;
				}
			}
			
			Order order = unit.orders().getOrder();
			Order newOrder = null;
			if (weaponCount > 0 && (double)bombCount/weaponCount >= 0.5 &&
					unit.distance(target) < 10000 && target.isVisible() && target.target instanceof Ship){
				if (!(order instanceof AttackFast) || ((AttackFast)order).target != target)
					newOrder = new AttackFast(target);
			}else{
				if (!(order instanceof AttackSlow) || ((AttackSlow)order).target != target)
					newOrder = new AttackSlow(target);
			}
			
			if (newOrder != null){
				unit.setTarget(target);
				unit.orders().clear();
				unit.orders().queueOrder(newOrder);
				
				if (unit instanceof Ship){
					Ship ship = (Ship)unit;
					for (Craft craft : ship.crafts){
						if (ship.isReady(craft))
							ship.setQueueStatus(craft, Ship.QueueStatus.QUEUED);
					}
				}
			}
		}
		
		boolean isValid(){
			return getTarget(target.target) != null;
		}
	}
	
	// Order to scout a sensor target
	private class ScoutAction extends Action{
		final SensorTarget target;
		
		public ScoutAction(SensorTarget target){
			this.target = target;
		}
		
		Locatable getObject(){
			return target;
		}
		
		void update(){}
		
		double computePriority(Unit unit){
			if (unit.orders().getOrder() instanceof Dock)
				return Double.MAX_VALUE;
			
			double priority = Utility.approxTime(unit, target) + 0.5*timeToObjective(unit, target);
			return priority*1.5*(1+2*unit.weapons.size()*unit.getWeaponReadiness()) + 120*Main.TPS;
		}
		
		void perform(Unit unit){
			Order order = unit.orders().getOrder();
			if (!(order instanceof MoveTo) || ((MoveTo)order).target != target){
				unit.orders().clear();
				unit.orders().queueOrder(new MoveTo(target));
			}
		}
		
		boolean isValid(){
			return getSensorTargets().contains(target);
		}
	}
	
	// Order to escort a friendly unit
	private class EscortAction extends Action{
		final Ship target;
		
		double timeToObjective;
		
		public EscortAction(Ship target){
			this.target = target;
		}
		
		Locatable getObject(){
			return target;
		}
		
		void update(){
			timeToObjective = timeToObjective(target, target);
		}
		
		double computePriority(Unit unit){
			if (target.outOfArena)
				return Double.MAX_VALUE;
			if (unit.orders().getOrder() instanceof Dock)
				return Double.MAX_VALUE;
			if (target == unit)
				return Double.MAX_VALUE;
			if (target.orders().getOrder() instanceof Escort)
				return Double.MAX_VALUE;
			if (unit.weapons.size() == 0)
				return Double.MAX_VALUE;
			
			double priority = 1500*Main.TPS + 1.2*Utility.approxTime(unit, target);
			if (unit instanceof Craft)
				priority += 2*Utility.approxTime(((Craft)unit).getMothership(), target);
			priority += 3*timeToObjective;
			return priority;
		}
		
		void perform(Unit unit){
			Order order = unit.orders().getOrder();
			if (!(order instanceof Escort) || ((Escort)order).target != target){
				unit.orders().clear();
				unit.orders().queueOrder(new Escort(target, 2*Unit.ESCORT_SLACK,
						360*random(), (0.1+0.1*random())*unit.type.visionRange));
			}
		}
		
		boolean isValid(){
			return target.getHull() > 0;
		}
	}
	
	// Order to defend an already captured objective
	private class DefendObjectiveAction extends Action{
		final Arena.Objective objective;
		
		double threat;
		int timeUpdated;
		int numDefenders;
		
		public DefendObjectiveAction(Arena.Objective objective){
			this.objective = objective;
		}
		
		Locatable getObject(){
			return objective;
		}
		
		void update(){
			threat = threatAt(objective);
			
			numDefenders = 0;
			for (Controllable controllable : controllables){
				if (controllable instanceof Unit){
					Order order = ((Unit)controllable).orders().getOrder();
					if (order instanceof Orbit && ((Orbit)order).target == objective)
						numDefenders++;
				}
			}
		}
		
		double computePriority(Unit unit){
			if (unit.orders().getOrder() instanceof Dock)
				return Double.MAX_VALUE;
			if (unit.type.captureRate > 0 && unit.weapons.size() < 2)
				return Double.MAX_VALUE;
			
			double priority = Utility.approxTime(unit, objective);
			if (unit instanceof Craft)
				priority += Utility.approxTime(((Craft)unit).getMothership(), objective);
			priority += 600*Main.TPS*threat/(unitStrength(unit) + 200);
			if (unit.weapons.size() == 0){
				Order order = unit.orders().getOrder();
				boolean isOrbiting = order instanceof Orbit && ((Orbit)order).target == objective;
				priority += 400*Main.TPS*(isOrbiting ? numDefenders-1 : numDefenders);
			}
			if (objective.owner != team && objective.getCapturingTeam() != team && unit.weapons.size() > 0)
				priority += 1500*Main.TPS;
			return priority + 450*Main.TPS;
		}
		
		void perform(Unit unit){
			Order order = unit.orders().getOrder();
			if (!(order instanceof Orbit) || ((Orbit)order).target != objective){
				if (timeUpdated < Main.game.turn){
					timeUpdated = Main.game.turn;
					unit.orders().clear();
					unit.orders().queueOrder(new Orbit(objective, unit.type.visionRange/3));
				}
			}
		}
		
		boolean isValid(){
			return true;
		}
	}
	
	// Wrapper class for use with maps
	private class Priority{
		public double value;
		public Priority(){
			this.value = Double.MAX_VALUE;
		}
	}
	
}
