
public abstract class SystemType extends ComponentType{
	
	public final int radius;
	
	public SystemType(String type){
		super(type, "systems");
		
		radius = (int)(getInt("range")*Main.config.systemRangeMultiplier);
	}
	
	public String powerCategory(){
		return "Systems";
	}
}
