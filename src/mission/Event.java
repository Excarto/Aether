
public abstract class Event{
	
	public final Condition condition;
	public final int repetition;
	
	private int timesExecuted;
	
	public Event(Condition condition, int repetition){
		this.condition = condition;
		this.repetition = repetition;
		timesExecuted = 0;
	}
	
	public void act(Game game){
		if (timesExecuted < repetition && condition.isSatisfied(game)){
			execute(game);
			onExecute();
			timesExecuted++;
		}
	}
	
	public abstract void execute(Game game);
	public void onExecute(){}
}
