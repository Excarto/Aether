
// Event condition that a given unit is destroyed

public class DestroyCondition extends Condition{
	
	public final Unit unit;
	
	public DestroyCondition(Unit unit){
		this.unit = unit;
	}
	
	public boolean isSatisfied(Game game){
		return unit.getHull() <= 0;
	}
	
}
