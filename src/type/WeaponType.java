import static java.lang.Math.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.*;

public abstract class WeaponType extends ComponentType{
	
	public Renderable renderable;
	public final Sound fireSound;
	public final Sound impactSound;
	
	public final ExplosionType impactExplosion;
	public final double projectileScale, weaponScale;
	public final double inaccuracy;
	public final int reloadTime;
	public final double energyPerShot;
	public final double trackRate;
	public final int ammoType;
	public final double radarSize;
	public final double arcResourceBase;
	public final double arcResourceExponent;
	public final double pointDefenseEffect;
	public final Weapon.FireMode defaultMode;
	public final boolean defaultAutoShips, defaultAutoCraft, defaultAutoMissiles;
	
	public WeaponType(String type){
		super(type, "weapons");
		
		impactExplosion = Main.getExplosionType(getString("impact_explosion"));
		
		projectileScale = getDouble("projectile_scale");
		weaponScale = getDouble("weapon_scale");
		//iconSize = getInt("icon_size");
		inaccuracy = getDouble("inaccuracy");
		reloadTime = (int)(getDouble("reload_time")*Main.TPS);
		energyPerShot = getDouble("energy_per_shot");
		trackRate = getDouble("tracking_rate")/Main.TPS;
		ammoType = getInt("ammo_type") > 0 ? Main.mapAmmo(getInt("ammo_type"), getDouble("ammo_mass")) : -1;
		radarSize = getDouble("radar_signature");
		arcResourceBase = getDouble("arc_resource_base");
		arcResourceExponent = getDouble("arc_resource_exponent");
		defaultMode = Weapon.FireMode.valueOf(getString("default_fire_mode").toUpperCase());
		defaultAutoShips = getBoolean("default_auto_ships");
		defaultAutoCraft = getBoolean("default_auto_craft");
		defaultAutoMissiles = getBoolean("default_auto_missiles");
		
		double rofFactor = reloadTime > 2*Main.TPS ? 0.0 : 1.0/(1.0 + reloadTime/(double)Main.TPS);
		double accuracyFactor = 1.0/(1.0 + 0.3*inaccuracy);
		double trackRateFactor = max(1.0, 0.2 + trackRate*Main.TPS/120);
		pointDefenseEffect = rofFactor*accuracyFactor*trackRateFactor;
		
		fireSound = new Sound(new File("data/weapons/" + type + "/fire.wav"));
		impactSound = new Sound(new File("data/weapons/" + type + "/impact.wav"));
	}
	
	public int getMass(int arc){
		return (int)round(mass*arcMultiplier(arc));
	}
	
	public int getCost(int arc){
		return (int)round(cost*arcMultiplier(arc));
	}
	
	public double getAveragePowerUse(){
		return energyPerShot/reloadTime + super.getAveragePowerUse();
	}
	
	private double arcMultiplier(int arc){
		return 1.0 + arcResourceBase*pow(arc/Weapon.ARC_INCREMENT, arcResourceExponent);
	}
	
	public String powerCategory(){
		return "Weapons";
	}
	
	public void load(){
		super.load();
		
		fireSound.load();
		impactSound.load();
		
		File file = new File("data/weapons/" + name + "/weapon.png");
		if (file.exists()){
			try{
				BufferedImage render = ImageIO.read(file);
				renderable = new Renderable(2, UnitType.maxNumRenderAngles);
				renderable.load(new BufferedImage[]{render}, weaponScale);
			}catch(IOException e){}
		}
	}
}
