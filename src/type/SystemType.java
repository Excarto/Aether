
public abstract class SystemType extends ComponentType{
	
	public final int radius;
	
	public SystemType(String type){
		super(type, "systems");
		
		radius = getInt("range");
	}
	
	public String powerCategory(){
		return "Systems";
	}
}
