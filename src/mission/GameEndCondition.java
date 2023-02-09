
// Event condition that a team wins

public class GameEndCondition extends Condition{
	
	final int victoryTeam;
	
	public GameEndCondition(int victoryTeam){
		this.victoryTeam = victoryTeam;
	}
	
	public boolean isSatisfied(Game game){
		return game.getVictoryTeam() == victoryTeam;
	}
	
}
