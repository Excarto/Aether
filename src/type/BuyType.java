import static java.lang.Math.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.ImageIO;

public abstract class BuyType extends Type{
	
	abstract void genSpecs();
	String[][] specs;
	
	public static int maxDebrisSize = 0;
	
	public final String name;
	public final BufferedImage perspectiveImg, sideImg;//, frontImg;
	public final String typeClass;
	public final String description;
	public final ExplosionType deathExplosion;
	public final Sound deathSound;
	public final int debrisSize;
	public final int cost;
	public final int mass;
	public final int hull;
	public final double hullPerMaterial;
	public int maxDebrisPiece;
	
	public BuyType(String name, String folder){
		super(folder + "/" + name);
		this.name = name;
		
		typeClass = getString("class");
		description = getString("description");
		
		int numDeathExplosions = 0;
		while (hasValue("death_explosion" + (numDeathExplosions+1)))
			numDeathExplosions++;
		deathExplosion = Main.getExplosionType(getString("death_explosion1"));
		deathSound = new Sound(new File("data/" + folder + "/" + name + "/death.wav"));
		debrisSize = getInt("debris_size");
		
		cost = getInt("cost");
		mass = getInt("mass");
		hull = getInt("hull");
		hullPerMaterial = getDouble("repair_per_material");
		
		BufferedImage temp = null;
		File file;
		file = new File("data/" + folder + "/" + name + "/perspective.png");
		try{
			temp = ImageIO.read(file);
		}catch(IOException e){
			Main.crash(file.getPath());
		}
		perspectiveImg = temp;
		file = new File("data/" + folder + "/" + name + "/side.png");
		try{
			temp = ImageIO.read(file);
		}catch(IOException e){
			Main.crash(file.getPath());
		}
		sideImg = temp;
		//file = new File("data/" + folder + "/" + name + "/front.png");
		//try{
		//	temp = ImageIO.read(file);
		//}catch(IOException e){
		//	Main.crash(file.getPath());
		//}
		//frontImg = temp;
		
		maxDebrisSize = max(maxDebrisSize, debrisSize);
	}
	
	public String[][] getSpecs(){
		if (specs == null)
			genSpecs();
		return specs;
	}
	
	public void load(){
		deathSound.load();
		
		for (int x = 0; x < Main.debrisTypes.length; x++){
			if ((double)Main.debrisTypes[x].size/Main.debrisTypes[Main.debrisTypes.length-1].size
					< 2.0*(debrisSize+maxDebrisSize/10)/maxDebrisSize){
				maxDebrisPiece = x;
			}
		}
	}
	
	public String toString(){
		return name;
	}
}
