import java.awt.image.*;

public class BoosterType extends SystemType{
	
	public final double thrust;
	public final int lifetime;
	public final boolean cannotReengage;
	public final BufferedImageOp thrusterOp;
	
	public BoosterType(String type){
		super(type);
		
		lifetime = (int)(getDouble("lifetime")*Main.TPS);
		thrust = getDouble("thrust")*Main.config.unitAccelMultiplier/Main.TPS/Main.TPS;
		cannotReengage = getInt("cannot_reengage") != 0;
		thrusterOp = new RescaleOp(
				new float[]{(float)getDouble("red_scale"), (float)getDouble("green_scale"), (float)getDouble("blue_scale"), 1.0f},
				new float[]{0.0f, 0.0f, 0.0f, 0.0f}, Main.inGameHints);
	}
	
	public double getAveragePowerUse(){
		return thrust*Main.config.energyPerThrust + super.getAveragePowerUse();
	}
	
	public void genSpecs(){
		double avgBoost = 0.0;
		for (ShipType type : Main.shipTypes)
			avgBoost += thrust/type.thrust;
		avgBoost /= Main.shipTypes.length;
		
		specs = new String[][]{
				{"Fuel (s)",					String.valueOf((double)lifetime/Main.TPS)},
				{"Average % Accel. Increase",	String.valueOf(avgBoost*100)},
					{"Resources", "CATEGORY"},
				{"Energy Use (Per Second)",		String.valueOf(-getAveragePowerUse()*Main.TPS/1000)},
				{"Energy To Engage",			String.valueOf(-engageEnergy/1000)},
				{"Required Mass",				String.valueOf(-mass)}
		};
	}
}
