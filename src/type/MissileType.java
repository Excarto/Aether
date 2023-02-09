import static java.lang.Math.*;

public class MissileType extends GunType{
	static final int NUM_CONTACT_MAPS = 12;
	
	public static double highestDamage = 0.0, leastDamage = Double.MAX_VALUE;
	
	public final boolean[][][] contactMap;
	
	public final double thrust;
	public final double turnThrust;
	public final int missileHull;
	public final int capacitor;
	public final int visionRange;
	public final double closingFuel;
	public final int fuelLifespan;
	public final double contactScale;
	public final double overallDamage;
	public final double deltaV;
	public final Thruster[] thruster;
	
	public MissileType(String type){
		super(type);
		
		thrust = getDouble("forward_thrust")/Main.TPS/Main.TPS;
		turnThrust = getDouble("turn_thrust")/Main.TPS/Main.TPS;
		missileHull = getInt("missile_hull");
		capacitor = (int)(Main.config.energyPerThrust*getDouble("projectile_mass")*getDouble("delta_v")/Main.TPS);
		deltaV = capacitor/Main.config.energyPerThrust/projectileMass;
		visionRange = getInt("vision_size");
		//cruiseSpeed = getDouble("cruise_speed")/Main.TPS;
		closingFuel = getDouble("closing_fuel") == 0 ? 1.0 : getDouble("closing_fuel");
		fuelLifespan = (int)(getDouble("lifespan_after_fuel")*Main.TPS);
		contactScale = getDouble("contact_scale") > 0 ? getDouble("contact_scale") : 1.0;
		
		if (getInt("thruster_type") > 0){
			thruster = new Thruster[1];
			thruster[0] = new Thruster(getInt("thruster_type")-1,
					getDouble("thruster_x_pos"), -getDouble("thruster_y_pos"), getDouble("thruster_z_pos"),
					0.0, 0.0, Thruster.FORWARD, getInt("thruster_z_order"), thrust, 0);
		}else
			thruster = Sprite.NO_THRUSTERS;
		
		contactMap = new boolean[NUM_CONTACT_MAPS][][];
		
		overallDamage = 0.6*getDouble("kinetic_damage")+explosiveDamage+2.0*EMDamage;
		highestDamage = max(overallDamage, highestDamage);
		leastDamage = min(overallDamage, leastDamage);
	}
	
	protected void genSpecs(){
		int rateOfFire = (int)(60.0*Main.TPS/reloadTime);
		specs =  new String[][]{
				{"Hull",					String.valueOf(missileHull)},
				{"Launch Speed",			String.valueOf(velocity*Main.TPS)},
					{"Damage", "CATEGORY"},
				{"Explosive Damage",		String.valueOf(explosiveDamage),
						"Explosive damage is effective against shields and hull, and does some damage to subsystems"},
				{"Approx. Kinetic Damage",	String.valueOf(kineticMultiplier*
						pow(0.6*capacitor/Main.config.energyPerThrust/projectileMass, kineticExponent)),
						"Kinetic damage is dependent on the speed of impact and deals reduced damage to shields"},
				{"EM Damage",				String.valueOf(EMDamage),
						"EM damage is very effective against subsystems, but does nothing to a unit's hull"},
				{"Rate of Fire (Shots/Min)",String.valueOf(projectilesPerShot*rateOfFire)},
					{"Motor", "CATEGORY"},
				{"Forward Acceleration",	String.valueOf(Main.TPS*Main.TPS*thrust/projectileMass)},
				{"Turn Acceleration",		String.valueOf(Main.TPS*Main.TPS*turnThrust/projectileMass)},
				{"Fuel (s)",				String.valueOf(capacitor/(thrust*Main.config.energyPerThrust)/Main.TPS)},
				{"Maximum Speed",			String.valueOf(deltaV*Main.TPS)},
					{"Resources", "CATEGORY"},
				{"Ammo Use (Mass/Min)",		String.valueOf(ammoType != -1 ? -Main.ammoMass[ammoType]*rateOfFire : 0)},
				{"Unturreted Mass",			String.valueOf(-mass)},
				{"Turreted Mass",			String.valueOf(getMass(360) < 1000 ? -getMass(360) : "N/A")}
		};
	}
	
	public void load(){
		super.load();
		for (int x = 0; x < contactMap.length; x++)
			contactMap[x] = projectileRenderable.getContactMap(x*360.0/NUM_CONTACT_MAPS, contactScale);
		
		if (thruster.length > 0)
			thruster[0].setHostSize(projectileRenderable.size);
	}
	
	protected double getEstimatedVelocity(){
		return getDouble("delta_v")/Main.TPS;
	}
	
	
	// Approximate time for missile of this type to reach a target
	public double approxTime(double dx, double dy, double vx, double vy, double bearing, double capacitor, double v0){
		
		double accel = thrust/projectileMass;
		double launchSpeed = v0*(cos(toRadians(bearing)) - pow(sin(toRadians(abs(bearing)+inaccuracy)),2));
		double turnTime = 0.2*Main.TPS + 1.2*sqrt(4*abs(bearing)*projectileMass/turnThrust);
		double thrustTime = max(0, 0.85*(capacitor-turnTime*turnThrust*Main.config.energyPerTurnThrust)/(thrust*Main.config.energyPerThrust));
		
		dx += vx*turnTime;
		dy += vy*turnTime;
		double distanceSq = dx*dx+dy*dy;
		double distance = sqrt(distanceSq);
		double velSq = vx*vx+vy*vy;
		double dDotV =  dx*vx+dy*vy;
		double closingSpeed = -dDotV/distance;
		closingSpeed -= 1.2*sqrt(velSq - closingSpeed*closingSpeed);
		
		double initialSpeed = closingSpeed+launchSpeed;
		if (initialSpeed <= -accel*thrustTime)
			return Double.MAX_VALUE;
		double accelToTargetTime = (sqrt(initialSpeed*initialSpeed+2*accel*distance) - initialSpeed)/accel;
		if (accelToTargetTime < thrustTime)
			return turnTime + accelToTargetTime;
		return turnTime + (2*distance + accel*thrustTime*thrustTime)/(2*initialSpeed + 2*accel*thrustTime);
		
		/*double vAvg;
		if (accelToTargetTime < thrustTime){
			vAvg = initialSpeed + accelToTargetTime*accel/2;
		}else
			vAvg = distance*(initialSpeed+accel*thrustTime)/(distance+accel*thrustTime*thrustTime/2);
		//vAvg *= 0.62;
		
		//Test.p(launchSpeed+" "+v0+" "+closingSpeed+" "+vAvg+" "+thrustTime+" "+accelToTargetTime+" "+turnTime+" "+accel+" "+(vx*vx+vy*vy)+" "+dDotV);
		//Test.p(capacitor+" "+turnTime+" "+turnThrust+" "+Main.energyPerTurnThrust+" "+Main.energyPerThrust);
		
		double a = velSq-vAvg*vAvg;
		double b = 2*dDotV;
		double c = distanceSq;
		double arg = b*b-4*a*c;
		if (arg < 0){
			return Double.MAX_VALUE;
		}else{
			double root1 = (-b-sqrt(arg))/(2*a);
			double root2 = (-b+sqrt(arg))/(2*a);
			return turnTime + min(root1 > 0 ? root1 : Double.MAX_VALUE, root2 > 0 ? root2 : Double.MAX_VALUE);
		}*/
	}
}
