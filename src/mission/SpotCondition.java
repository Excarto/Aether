
public class SpotCondition extends Condition{
	
	public final Unit unit;
	public final boolean isSensor, isTarget;
	
	public SpotCondition(Unit unit, boolean isSensor, boolean isTarget){
		this.isSensor = isSensor;
		this.isTarget = isTarget;
		this.unit = unit;
	}
	
	public boolean isSatisfied(Game game){
		Player player = Main.game.getHuman();
		if (isSensor && player.getSensorTarget(unit) != null)
			return true;
		if (isTarget && player.getTarget(unit) != null)
			return true;
		return false;
	}
	
}
