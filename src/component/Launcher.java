import java.util.*;

public class Launcher extends Gun{
	
	final MissileType type;
	int volleySize;
	private final List<Missile> volley;
	
	public Launcher(MissileType type, WeaponHardpoint hardpoint, Unit unit, int arc, double mountAngleFrac){
		super(type, hardpoint, unit, arc, mountAngleFrac);
		this.type = type;
		
		volley = new ArrayList<Missile>();
		volleySize = 1;
	}
	
	public void act(){
		super.act();
		
		if (unit.player.isMaster()){
			Target target = getTarget();
			if (!volley.isEmpty() && target != null){
				if (volley.size() >= volleySize*type.projectilesPerShot || !isTriggerPulled() || unit.ammo[type.ammoType] == 0){
					for (Missile volleyMissile : volley){
						if (volleyMissile.orders().isEmpty())
							volleyMissile.orders.queueOrder(new Impact(target));
					}
					volley.clear();
				}
			}
		}
	}
	
	protected Projectile createProjectile(){
		return new Missile(this);
	}
	
	protected void addProjectile(Projectile projectile){
		unit.player.controllables.add((Missile)projectile);
		if (unit.player.isMaster())
			volley.add((Missile)projectile);
	}
	
	protected double getProjectileVelocity(){
		return 0.5*type.capacitor/Main.config.energyPerThrust/type.projectileMass;
	}
	
	public void removeControllable(Controllable controllable){
		super.removeControllable(controllable);
		volley.remove(controllable);
	}
	
	protected double approxTime(Locatable locatable){
		double dx = locatable.getPosX()-unit.getPosX(), dy = locatable.getPosY()-unit.getPosY();
		double vx = locatable.getVelX()-unit.getVelX(), vy = locatable.getVelY()-unit.getVelY();
		
		return type.approxTime(dx, dy, vx, vy,
				Utility.fixAngle(unit.bearing(locatable)-getAngle()), type.capacitor, type.velocity);
	}
	
}
