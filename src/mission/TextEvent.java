
// Event that writes text to the screen

public class TextEvent extends Event{
	int lifetime;
	String text;
	
	public TextEvent(Condition condition, String text, double lifetime){
		super(condition, 1);
		this.text = text;
		this.lifetime = (int)(lifetime*Main.TPS);
	}
	
	public void execute(Game game){
		HumanPlayer human = game.getHuman();
		human.getWindow().receiveMessage(text, 0, lifetime);
	}
	
}
