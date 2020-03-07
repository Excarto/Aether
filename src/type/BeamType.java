import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

public class BeamType extends WeaponType{
	
	public final Renderable renderable;
	
	public final double explosiveDamagePerTurn;
	public final double EMDamagePerTurn;
	public final int duration;
	public final int length;
	public int imageLength;
	//public final double contactForce;
	
	public BeamType(String type) {
		super(type);
		
		explosiveDamagePerTurn = getInt("explosive_damage_per_sec")/Main.TPS;
		EMDamagePerTurn = getInt("em_damage_per_sec")/Main.TPS;
		duration = (int)(getDouble("duration")*Main.TPS);
		length = getInt("beam_length");
		//contactForce = getDouble("contact_force")/Main.TPS/Main.TPS;
		
		renderable = new Renderable(1, 1);
	}
	
	public void load(){
		super.load();
		
		File file = new File("data/weapons/" + name + "/beam.png");
		BufferedImage temp = null;
		try{
			temp = ImageIO.read(file);
		}catch(IOException e){
			Main.crash(file.getPath());
		}
		renderable.load(new BufferedImage[] {temp}, projectileScale);
		imageLength = renderable.getImage(1.0, 0.0, false, 0).getHeight(null);
	}
	
	protected void genSpecs(){
		double rateOfFire = (double)Main.TPS/reloadTime;
		specs = new String[][] {
				{"Beam Length",				String.valueOf(length)},
				{"Track Rate (Degrees/Sec)",String.valueOf((int)(Main.TPS*trackRate))},
					{"Damage", "CATEGORY"},
				{"Explosive Damage",		String.valueOf(explosiveDamagePerTurn*Main.TPS),
						"Explosive damage is effective against shields and hull, and does some damage to subsystems"},
				{"EM Damage",				String.valueOf(EMDamagePerTurn*Main.TPS),
						"EM damage is very effective against subsystems, but does nothing to a unit's hull"},
				{"Rate of Fire (Shots/Sec)",String.valueOf(rateOfFire)},
				{"Duration (Sec)",			String.valueOf(duration*Main.TPS)},
					{"Resources", "CATEGORY"},
				{"Energy Use (Per Second)",	String.valueOf(-getAveragePowerUse()*Main.TPS/1000)},
				{"Ammo Use (Mass/Min)",		String.valueOf(ammoType != -1 ? -Main.ammoMass[ammoType]*rateOfFire : 0)},
				{"Unturreted Mass",			String.valueOf(-mass)},
				{"Turreted Mass",			String.valueOf(getMass(360) < 1000 ? -getMass(360) : "N/A")}
		};
	}
}
