import java.io.*;

public abstract class System extends Component{
	
	public final SystemType type;
	boolean engaged;
	
	public System(SystemType type, Hardpoint hardpoint, Unit unit){
		super(type, hardpoint, unit);
		this.type = type;
	}
	
	public void act(){
	}
	
	public boolean setEngaged(boolean engaged){
		this.engaged = engaged && (this.engaged || unit.drainEnergy(type.engageEnergy, "Systems", type.name));
		return this.engaged;
	}
	
	public boolean isEngaged(){
		return engaged;
	}
	
	public int getCost(){
		return type.cost;
	}
	
	public int getMass(){
		return type.mass;
	}
	
	public static void read(BufferedReader in, Unit unit, Hardpoint hardpoint){
		try{
			String typeName = in.readLine();
			if (!typeName.equals("null")){
				SystemType type = null;
				for (SystemType systemType : Main.systemTypes){
					if (typeName.equals(systemType.name))
						type = systemType;
				}
				if (type == null || type.mass > hardpoint.mass)
					return;
				unit.setSystem(type, hardpoint);
			}
		}catch(IOException|NumberFormatException e){
			return;
		}
	}
}
