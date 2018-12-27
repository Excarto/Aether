import static java.lang.Math.*;
import java.awt.*;

public class Missile extends Projectile implements Controllable{
	
	public final MissileType type;
	static SidePanel menu;
	static MissilePanel missilePanel;
	
	int turnsPassed;
	int afterFuelTime;
	final OrderQueue orders;
	double hull;
	double capacitor;
	private int accelForwardTime, accelTurnTime;
	private Thruster[][] activeThrusters;
	
	boolean autoShips, autoCraft, autoMissiles;
	Weapon.FireMode mode;
	
	public Missile(Gun gun){
		super(gun, gun.unit.player, gun.getSubTarget());
		
		turnsPassed = 0;//(int)(random()*100);
		this.type = (MissileType)gun.type;
		capacitor = type.capacitor;
		hull = type.missileHull;
		afterFuelTime = type.fuelLifespan;
		activeThrusters = new Thruster[][]{NO_THRUSTERS, NO_THRUSTERS, NO_THRUSTERS};
		orders = player.isMaster() ? new OrderQueue(this) : null;
		
		Target target = gun.getTarget();
		if (!gun.isManualAim() && target != null && target.target instanceof Missile)
			hitMissileOnlyTime = Main.TPS*3;
		
		mode = gun.getFireMode();
		autoShips = gun.autoShips;
		autoCraft = gun.autoCraft;
		autoMissiles = gun.autoMissiles;
	}
	
	public void act(){
		super.act();
		turnsPassed++;
		
		if (orders != null)
			orders.move();
		
		if (netThrustTime[Thruster.FORWARD] > 0){
			netThrustTime[Thruster.FORWARD]--;
			accelForward();
		}
		
		if (capacitor <= 0 && afterFuelTime-- == 0)
			Main.game.removeControllable(this);
		
		if (orders != null){
			if (mode == Weapon.FireMode.AUTONOMOUS && orders.isEmpty() && turnsPassed%16 == 0)
				findTarget(autoShips, autoCraft, autoMissiles);
		}
	}
	
	public void postMove(){}
	
	public void recordPos(){
		super.recordPos();
		if (orders != null)
			orders.recordPos();
	}
	
	public void accelForward(){
		if (capacitor > 0 && accelForwardTime < Main.game.turn){
			accel(type.thrust/type.projectileMass, getAngle());
			capacitor -= type.thrust*Main.energyPerThrust;
			accelForwardTime = Main.game.turn;
		}
	}
	
	public void accelTurn(boolean direction){
		if (capacitor > 0 && accelTurnTime < Main.game.turn){
			accelTurn((direction ? 1 : -1)*type.turnThrust/type.projectileMass);
			capacitor -= type.turnThrust*Main.energyPerTurnThrust;
			accelTurnTime = Main.game.turn;
		}
	}
	
	public void stopTurn(){
		if (abs(getTurnSpeed()) > 0.0001)
			accelTurn(getTurnSpeed() < 0);
	}
	
	public void takeHit(double posX, double posY,
			Component subTarget, double direction, boolean continuous,
			double explosiveDamage, double kineticDamage, double EMPDamage){
		this.hull -= explosiveDamage+kineticDamage;
	}
	
	public void findTarget(boolean findShip, boolean findCraft, boolean findMissile){
		Target minTarget = null;
		double minTime = Double.MAX_VALUE;
		for (Target target : player.getTargets()){
			if (target.isVisible() && (target.target instanceof Ship && findShip ||
					target.target instanceof Craft && findCraft || target.target instanceof Missile && findMissile)){
				double time = type.approxTime(target.getPosX()-getPosX(), target.getPosY()-getPosY(),
						target.getVelX()-getVelX(), target.getVelY()-getVelY(),
						bearing(target), capacitor, 0);
				if (time < minTime){
					minTime = time;
					minTarget = target;
				}
			}
		}
		
		if (minTime < type.defaultAutoRange){
			//target = minTarget;
			orders.clear();
			orders.queueOrder(new Impact(minTarget));
		}
	}
	
	public void explode(){
		Main.game.removeControllable(this);
		Main.game.addGraphic(new Explosion(type.deathExplosion, getPosX(), getPosY(), getVelX(), getVelY()));
	}
	
	public void remove(){
		Main.game.removeControllable(this);
	}
	
	public void removeControllable(Controllable target){
		if (orders != null)
			orders.removeTarget(target);
		/*if (orders != null){
			Order order = orders.getOrder();
			orders.removeTarget(target);
			if (order instanceof Impact)
				Test.p(target+" "+((Impact)order).target.target+" "+((Impact)order).target.equals(target)+" "+order+" "+orders.getOrder());
			if ((mode == Weapon.FireMode.AUTOMATIC || mode == Weapon.FireMode.AUTONOMOUS) &&
					order instanceof Impact && ((Impact)order).target.equals(target) && orders.isEmpty())
				findTarget(true, true, true);
		}*/
	}
	
	public double getAccel(){
		return type.thrust/type.projectileMass;
	}
	public double getTurnAccel(){
		return type.turnThrust/type.projectileMass;
	}
	
	public double getEnergy(){
		return capacitor;
	}
	
	public double getHull(){
		return hull;
	}
	
	public boolean isVisible(Sprite sprite){
		return distance(sprite) < type.visionRange;
	}
	
	public BuyType getType(){
		return type;
	}
	
	public double getRadarSize(){
		return type.radarSize;
	}
	
	public double getVisionSize(){
		return type.radarSize;
	}
	
	public boolean[][] getContactMap(){
		return type.contactMap[Renderable.getAngleIndex(getAngle(), MissileType.NUM_CONTACT_MAPS)];
	}
	
	public Player getPlayer(){
		return player;
	}
	
	public double getMass(){
		return type.mass;
	}
	
	protected Thruster[][] getThrusters(){
		if (Main.game.turn-accelForwardTime < 1){
			activeThrusters[Thruster.FORWARD] = type.thruster;
		}else
			activeThrusters[Thruster.FORWARD] = NO_THRUSTERS;
		return activeThrusters;
	}
	
	public OrderQueue orders(){
		return orders;
	}
	
	public void drawVision(Graphics2D g, GameWindow window){
		int posX = (int)window.posXOnScreen(renderPosX), posY = (int)window.posYOnScreen(renderPosY);
		int visionRange = (int)(window.getZoom()*type.visionRange);
		g.setColor(new Color(0, 50, 250, 100));
		g.drawOval(posX-visionRange, posY-visionRange, visionRange*2, visionRange*2);
	}
	
	public static void createMenu(){
		menu = new SidePanel(){
			public void refresh(){
				missilePanel.refresh();
			}
		};
		missilePanel = new MissilePanel();
		menu.add(missilePanel);
	}
	public SidePanel getMenu(){
		missilePanel.setMissile(this);
		//menu.start();
		return menu;
	}
	public void restoreMenuState(){}
	
}
