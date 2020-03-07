import static java.lang.Math.*;

public class Gun extends Weapon{
	
	public final GunType type;
	public int autoRange;
	private double timeToTarget;
	private VelocityRecord targetVels;
	
	public Gun(GunType type, WeaponHardpoint hardpoint, Unit unit, int arc, double mountAngleFrac){
		super(type, hardpoint, unit, arc, mountAngleFrac);
		
		this.type = type;
		autoRange = type.defaultAutoRange;
		
		targetVels = new VelocityRecord(Main.config.targetAccelTimeframe, 0.85);
	}
	
	public void act(){
		Target target = getTarget();
		
		timeToTarget = target != null ? approxTime(getTarget()) :  Double.MAX_VALUE;
		
		if (target != null){
			targetVels.record(target.getVelX(), target.getVelY());
		}else
			targetVels.clear();
		
		super.act();
	}
	
	public void fire(int time){
		super.fire(time);
		
		for (int x = 0; x < type.projectilesPerShot; x++){
			Projectile projectile = createProjectile();
			double angle = Utility.fixAngle(unit.getAngle()+getAngle()+type.inaccuracy*(2*random.nextDouble()-1.0));
			double velX = unit.getVelX()+type.velocity*sin(toRadians(angle));
			double velY = unit.getVelY()-type.velocity*cos(toRadians(angle));
			int dt = time-Main.game.turn;
			double posX = getPosX() + dt*(unit.getVelX()-velX);
			double posY = getPosY() + dt*(unit.getVelY()-velY);
			if (type.renderable != null){
				posX += sin(toRadians(angle))*(type.renderable.size/2 + 6);
				posY -= cos(toRadians(angle))*(type.renderable.size/2 + 6);
			}
			projectile.place(posX, posY, velX, velY, angle, type.rotateSpeed);
			unit.accel(-projectile.speed(unit)*type.projectileMass/unit.getMass(), angle);
			addProjectile(projectile);
		}
	}
	
	public double getMostRecentTimeToTarget(){
		return timeToTarget;
	}
	
	protected Projectile createProjectile(){
		return new Projectile(this, unit.player, getSubTarget());
	}
	
	protected void addProjectile(Projectile projectile){
		unit.player.projectiles.add(projectile);
		for (Player player : Main.game.players){
			if (player instanceof HumanPlayer && player.isTeamVisible(projectile))
				((HumanPlayer)player).visibleGraphics.add(projectile);
		}
	}
	
	protected double calcTargetAngle(){
		Sprite target = (Sprite)this.getTarget().target;
		
		double Dx = getDx(), Dy = getDy();
    	double Vx = target.getVelX()-unit.getVelX(), Vy = target.getVelY()-unit.getVelY();
    	double Ax = targetVels.getAccel(0),  Ay = targetVels.getAccel(1);
    	double a = 0.0;//type instanceof MissileType ? ((MissileType)type).thrust/((MissileType)type).mass : 0.0;
    	double v = getProjectileVelocity();
    	
    	double time = Utility.getZero(new double[]{
				4*(Dx*Dx+Dy*Dy),
				8*(Dx*Vx+Dy*Vy),
				4*(Dx*Ax+Dy*Ay+Vx*Vx+Vy*Vy-v*v),
				4*(Vx*Ax+Vy*Ay-a*v),
				Ax*Ax+Ay*Ay-a*a
    	});
    	return Utility.fixAngle(toDegrees(atan2(
    			(Dx/time+Vx+Ax*time/2), -(Dy/time+Vy+Ay*time/2)))-unit.getAngle());
	}
	
	protected double getProjectileVelocity(){
		return type.velocity;
	}
	
	protected double approxTime(Locatable locatable){
		double dx = locatable.getPosX()-unit.getPosX(), dy = locatable.getPosY()-unit.getPosY();
		double vx = locatable.getVelX()-unit.getVelX(), vy = locatable.getVelY()-unit.getVelY();
		
		double a = vx*vx+vy*vy-type.velocity*type.velocity;
		double b = 2*dx*vx+2*dy*vy;
		double arg = b*b-4*a*(dx*dx+dy*dy);
		if (arg < 0){
			return Double.MAX_VALUE;
		}else{
			double root1 = (-b-sqrt(arg))/(2*a);
			double root2 = (-b+sqrt(arg))/(2*a);
			return min(root1 > 0 ? root1 : Double.MAX_VALUE, root2 > 0 ? root2 : Double.MAX_VALUE);
		}
	}
	
	protected double approxAngle(Locatable locatable, double time){
		double Dx = locatable.getPosX()-unit.getPosX();
		double Dy = locatable.getPosY()-unit.getPosY();
		double Vx = locatable.getVelX()-unit.getVelX();
		double Vy = locatable.getVelY()-unit.getVelY();
		return Utility.fixAngle(toDegrees(atan2(Dx+Vx*time, -(Dy+Vy*time))));
	}
	
	protected boolean inRange(Locatable target){
		double timeToTarget = target == getTarget() ? this.timeToTarget : approxTime(target);
		return timeToTarget < autoRange && timeToTarget < 0.85*type.lifespan;
	}
	
	protected boolean inManualRange(double dist){
		return true;
	}
	
	private class VelocityRecord{
		double[][] record;
		int index;
		boolean full;
		double convFactor;
		
		public VelocityRecord(int numEntries, double accelMultiplier){
			record = new double[numEntries][2];
			convFactor = accelMultiplier/(numEntries-1);
		}
		
		public void record(double velX, double velY){
			record[index][0] = velX;
			record[index][1] = velY;
			if (index == record.length-1){
				full = true;
				index = 0;
			}else
				index++;
		}
		
		public void clear(){
			index = 0;
			full = false;
		}
		
		public double getAccel(int dimension){
			if (!full)
				return 0.0;
			double oldest = record[index][dimension];
			double newest = index == 0 ? record[record.length-1][dimension] : record[index-1][dimension];
			return convFactor*(newest-oldest);
		}
	}
	
}
