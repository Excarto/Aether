import static java.lang.Math.*;
import java.util.*;
import java.util.concurrent.*;

public class Player{
	static final int WARP_DELAY = Main.TPS/3;
	static final int BUDGET_UPDATE_INTERVAL = 10*Main.TPS;
	
	static int playerCounter = 0;
	
	public final String name;
	public final int team;
	public final int totalBudget;
	
	public final IdList<Controllable> controllables;
	public final IdList<Ship> ships;
	public final IdList<Projectile> projectiles;
	public final List<Beam> firingBeams;
	public final Iterable<Unit> units;
	public final Set<Controllable> visibleControllables;
	
	public final Map<Sprite,SensorTarget> sensorTargets;
	public final Map<Controllable,Target> targets;
	private final Queue<Ship> warpQueue;
	
	private int budget, budgetInterval;
	private int timeToWarp;
	
	public int position;
	int turnsPassed;
	
	public Player(String name, int team, List<Ship> ships, int budget, Arena arena){
		this.name = name;
		this.team = team;
		
		controllables = new IdList<Controllable>();
		this.ships = new IdList<Ship>();
		for (int x = 0; x < ships.size(); x++){
			Ship ship = ships.get(x);
			this.ships.add(ship);
			ship.setPlayer(this);
			ship.initialize();
		}
		projectiles = new IdList<Projectile>();
		firingBeams = new ArrayList<Beam>();
		
		turnsPassed = playerCounter++;
		visibleControllables = new TreeSet<Controllable>(new Comparator<Controllable>(){
			public int compare(Controllable controllable1, Controllable controllable2){
				int sizeDiff = controllable2.getSize()-controllable1.getSize();
				if (sizeDiff != 0)
					return sizeDiff;
				return controllable2.hashCode()-controllable1.hashCode();
			}
		});
		sensorTargets = new HashMap<Sprite,SensorTarget>();
		targets = new HashMap<Controllable,Target>();
		warpQueue = new ConcurrentLinkedQueue<Ship>();
		
		units = new Iterable<Unit>(){
			public Iterator<Unit> iterator(){
				return new Iterator<Unit>(){
					boolean isShips = true;
					int index = -1;
					public boolean hasNext(){
						if (index == -1)
							findNext();
						return isShips || index < controllables.size();
					}
					public Unit next(){
						Unit unit = isShips ? Player.this.ships.get(index) : (Unit)controllables.get(index);
						findNext();
						return unit;
					}
					void findNext(){
						index++;
						if (isShips){
							while (index < Player.this.ships.size()){
								if (Player.this.ships.get(index).outOfArena)
									return;
								index++;
							}
							index = 0;
							isShips = false;
						}
						while (index < controllables.size()){
							if (controllables.get(index) instanceof Unit)
								return;
							index++;
						}
					}
				};
			}
		};
		
		this.totalBudget = budget;
		this.budget = (int)round(arena.startBudget*budget);
		if (arena.startBudget < 1.0 && arena.incomeTime > 0){
			this.budgetInterval = BUDGET_UPDATE_INTERVAL*(budget-this.budget)/arena.incomeTime;
		}else
			this.budgetInterval = 0;
	}
	
	public void move(){
		for (int x = 0; x < controllables.size(); x++)
			controllables.get(x).move();
		for (int x = 0; x < projectiles.size(); x++)
			projectiles.get(x).move();
	}
	
	public void act(){
		turnsPassed++;
		
		for (Target target : targets.values())
			target.update();
		
		for (int x = 0; x < controllables.size(); x++)
			controllables.get(x).act();
		for (int x = 0; x < projectiles.size(); x++)
			projectiles.get(x).act();
		
		if (turnsPassed%7 == 0){
			setVisibleSprites();
			
			for (Target target : targets.values()){
				target.updateVisible();
				target.updateScanned();
			}
		}
		
		for (Iterator<SensorTarget> i = sensorTargets.values().iterator(); i.hasNext();){
			SensorTarget target = i.next();
			target.act();
			if (target.getStrength() == 0)
				i.remove();
		}
		
		if (turnsPassed%BUDGET_UPDATE_INTERVAL == 0){
			budget += budgetInterval;
			updateBudget();
		}
	}
	
	public void postMove(){
		for (int x = 0; x < controllables.size(); x++)
			controllables.get(x).postMove();
		
		if (!warpQueue.isEmpty()){
			if (timeToWarp-- <= 0){
				timeToWarp = WARP_DELAY;
				warpQueue.poll().warpIn();
			}
		}
	}
	
	public void start(int position){
		for (Ship ship : ships)
			ship.setId();
		
		this.position = position;
		if (Main.game.arena.startBudget >= 1.0 || totalBudget <= 0){
			for (Ship ship : ships)
				warpIn(ship);
		}
	}
	
	public void gameEnd(int victoryTeam){}
	
	public void setVisibleSprites(){
		visibleControllables.clear();
		for (Player player : Main.game.players){
			for (int x = 0; x < player.controllables.size(); x++){
				Controllable controllable = player.controllables.get(x);
				if (player.team == team || isTeamVisible((Sprite)controllable)){
					visibleControllables.add(controllable);
					if (player.team != team)
						spotTarget(controllable);
				}
			}
		}
	}
	
	public boolean isTeamVisible(Sprite sprite){
		for (Player player : Main.game.players){
			if (player.team == team){
				for (Controllable controllable : player.controllables){
					if (controllable.isVisible(sprite))
						return true;
				}
			}
		}
		return false;
	}
	
	public void removeEffect(Sprite effect){}
	
	public void removeControllable(Controllable removed){
		if (controllables.contains(removed))
			controllables.remove(removed);
		for (Controllable controllable : controllables)
			controllable.removeControllable(removed);
		visibleControllables.remove(removed);
		targets.remove(removed);
	}
	
	public boolean addSensorSighting(SensorSighting sighting, Sprite sprite){
		if (!visibleControllables.contains((Controllable)sprite)){
			SensorTarget sensorTarget = sensorTargets.get(sprite);
			if (sensorTarget == null){
				sensorTarget = new SensorTarget(sprite);
				sensorTargets.put(sprite, sensorTarget);
			}
			sensorTarget.addSighting(sighting);
			return true;
		}
		return false;
	}
	
	public boolean spotTarget(Controllable controllable){
		if (targets.containsKey(controllable)){
			return false;
		}else{
			targets.put(controllable, new Target(controllable, this));
			return true;
		}
	}
	
	public int getBudget(){
		return budget;
	}
	
	public void updateBudget(){}
	
	public void fireWeapon(Weapon weapon){
		weapon.fire(Main.game.turn);
		weapon.reload();
	}
	
	public void contact(Projectile projectile, Controllable controllable){}
	
	public void contact(Beam beam, Controllable controllable, double posX, double posY){}
	
	public void launchCraft(Ship ship, Craft craft, boolean catapult){
		ship.launchCraft(craft, catapult);
	}
	
	public void retrieveCraft(Ship ship, Craft craft){
		ship.retrieveCraft(craft);
	}
	
	public void capture(Arena.Objective objective, int amount){
		objective.capture(this, amount);
	}
	
	public void repair(Repairable repairable, double material, boolean isScrap){
		repairable.repair(material, isScrap);
	}
	
	public void warpIn(Ship ship){
		confirmWarp(ship);
	}
	
	public void confirmWarp(Ship ship){
		if (ship.outOfArena){
			ship.outOfArena = false;
			budget -= ship.getCost();
			updateBudget();
			warpQueue.offer(ship);
		}
	}
	
	public Collection<SensorTarget> getSensorTargets(){
		return sensorTargets.values();
	}
	
	public SensorTarget getSensorTarget(Sprite sprite){
		return sensorTargets.get(sprite);
	}
	
	public Collection<Target> getTargets(){
		return targets.values();
	}
	
	public Target getTarget(Controllable controllable){
		return targets.get(controllable);
	}
	
	public boolean manualAim(Unit unit){
		return false;
	}
	
	public boolean knowHealth(Unit unit){
		if (unit.player.team == team)
			return true;
		
		Target target = targets.get(unit);
		if (target == null)
			return false;
		
		return target.isScanned();
	}
	
	public boolean isMaster(){
		return false;
	}
	
	public boolean canCapture(){
		for (Ship ship : ships){
			if (ship.getHull() > 0 && ship.type.captureRate > 0)
				return true;
		}
		for (Controllable controllable : controllables){
			if (controllable instanceof Unit && ((Unit)controllable).type.captureRate > 0)
				return true;
		}
		return false;
	}
	
	public boolean controlPressed(Control control){
		return false;
	}
	
	static final double COST_REJECTION_STD = 1.1;
	static final double COST_REJECTION_BIAS = 0.0;
	static final double CARRIER_REJECTION_PROB = 0.35;
	public static void makeFleet(Vector<Ship> ships, int budget, long randomSeed){
		Random rand = randomSeed == 0 ? new Random() : new Random(randomSeed);
		
		int minCost = Integer.MAX_VALUE;
		int maxCost = 0;
		for (ShipType type : Main.shipTypes){
			minCost = min(minCost, type.outfittedCost);
			maxCost = max(maxCost, type.outfittedCost);
		}
		
		for (int x = 0; x < 50; x++){
			ships.clear();
			int fleetCost = 0;
			
			for (int y = 0; y < 200; y++){
				ShipType type = Main.shipTypes[(rand.nextInt(Main.shipTypes.length))];
				
				boolean rejected = false;
				double costFrac = (type.outfittedCost - minCost)/(double)(maxCost - minCost);
				costFrac = 2*(costFrac - 0.5);
				double acceptProb = exp(-pow(((costFrac-COST_REJECTION_BIAS)/COST_REJECTION_STD), 2));
				rejected = rejected || rand.nextDouble() > acceptProb;
				rejected = rejected || (type.totalCraftMass/CraftType.smallestCraftMass > 3 && rand.nextDouble() < CARRIER_REJECTION_PROB);
				
				if (!rejected){
					Ship ship = new Ship(type);	
					ship.autoLoadout();
					if (fleetCost + ship.getCost() <= budget){
						ships.add(ship);
						fleetCost += ship.getCost();
					}
				}
			}
			if (fleetCost > budget-50)
				break;
		}
	}
	
}
