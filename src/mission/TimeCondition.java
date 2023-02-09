
// Event condition that some amount of time has passed

public class TimeCondition extends Condition{
	
	public final int turn;
	
	public TimeCondition(double time){
		turn = (int)(time*Main.TPS);
	}
	
	public boolean isSatisfied(Game game){
		return game.activeTurn >= turn;
	}
	
}
