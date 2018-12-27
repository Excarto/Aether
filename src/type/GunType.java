import static java.lang.Math.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

public class GunType extends WeaponType{
	
	public final Renderable projectileRenderable;
	
	public final int explosiveDamage;
	public final int EMDamage;
	public final double kineticMultiplier;
	public final double kineticExponent;
	public final int projectilesPerShot;
	public final double velocity;
	public final int lifespan;
	public final double rotateSpeed;
	public final double projectileMass;
	public final int defaultAutoRange;
	public final double impactExplosionForce;
	public final boolean goThroughMissiles;
	
	public GunType(String type){
		super(type);
		
		velocity = getDouble("velocity")/Main.TPS;
		explosiveDamage = getInt("explosive_damage");
		EMDamage = getInt("em_damage");
		kineticExponent = getDouble("kinetic_damage_exponent");
		//kineticMultiplier = getDouble("kinetic_damage_multiplier")*pow(Main.TPS, kineticExponent);
		kineticMultiplier = getDouble("kinetic_damage")/pow(getEstimatedVelocity(), kineticExponent);
		projectilesPerShot = getInt("projectiles_per_shot") != 0 ? getInt("projectiles_per_shot") : 1;
		lifespan = getDouble("lifespan") == 0 ? Integer.MAX_VALUE : (int)ceil(getDouble("lifespan")*Main.TPS);
		rotateSpeed = getDouble("rotate_speed")/Main.TPS;
		projectileMass = getDouble("projectile_mass");
		defaultAutoRange = (int)(getDouble("default_range") == 0 ? 5*Main.TPS : getDouble("default_range")*Main.TPS);
		impactExplosionForce = explosiveDamage*Main.impactImpulsePerDamage;//getDouble("impact_explosion_force")/Main.TPS;
		goThroughMissiles = getInt("go_through_missiles") != 0;
		
		projectileRenderable = new Renderable(getInt("min_render_size"), Main.renderAnglesMultiplier*getInt("num_render_angles"));
	}
	
	public void load(){
		super.load();
		
		BufferedImage temp = null;
		File file = new File("data/weapons/" + name + "/projectile.png");
		try{
			temp = ImageIO.read(file);
		}catch(IOException e){
			Main.crash(file.getPath());
		}
		projectileRenderable.load(new BufferedImage[]{temp}, projectileScale);
	}
	
	protected void genSpecs(){
		int rateOfFire = (int)(Main.TPS/reloadTime);
		specs = new String[][] {
				{"Projectile Velocity",		String.valueOf(velocity*Main.TPS)},
				{"Track Rate (Degrees/Sec)",String.valueOf((int)(Main.TPS*trackRate))},
				{"Inaccuracy (+/- Degrees)",String.valueOf(-inaccuracy)},
					{"Damage", "CATEGORY"},
				{"Explosive Damage",		String.valueOf(explosiveDamage)},
				{"Approx. Kinetic Damage",	String.valueOf(kineticMultiplier*pow(velocity, kineticExponent))},
				{"EM Damage",				String.valueOf(EMDamage)},
				{"Rate of Fire (Shots/Min)",String.valueOf(60.0*rateOfFire*projectilesPerShot)},
					{"Resources", "CATEGORY"},
				{"Energy Use (Per Second)",	String.valueOf(-getAveragePowerUse()*Main.TPS/1000)},
				{"Ammo Use (Mass/Min)",		String.valueOf(ammoType != -1 ? -Main.ammoMass[ammoType]*60.0*rateOfFire : 0)},
				{"Unturreted Mass",			String.valueOf(-mass)},
				{"Turreted Mass",			String.valueOf(getMass(360) < 1000 ? -getMass(360) : "N/A")}
		};
	}
	
	protected double getEstimatedVelocity(){
		return getDouble("velocity")/Main.TPS;
	}
}
