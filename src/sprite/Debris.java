import static java.lang.Math.*;

public class Debris extends Sprite{
	
	final DebrisType type;
	int timeToLive;
	
	public Debris(DebrisType type){
		super(type.renderable);
		this.type = type;
		timeToLive = (int)(10*random()*Main.TPS*pow(type.size, 0.5));
	}
	
	public void act(){
		if (timeToLive-- < 0){
			if (timeToLive%Main.TPS == 0){
				for (Player player : Main.game.players)
					if (player instanceof HumanPlayer && ((HumanPlayer)player).visibleGraphics.contains(this))
						return;
				Main.game.removeGraphic(this);
			}
		}
	}
	
	public int getIconSize(){
		return 0;
	}
	
	public double getVisionSize(){
		return type.visionSize;
	}
	
	public double getRadarSize(){
		return 0.0;
	}
}
