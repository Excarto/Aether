import java.awt.*;

public class Explosion extends Sprite{
	
	public final ExplosionType type;
	private int time;
	
	public Explosion(ExplosionType type, 
			double posX, double posY, double velX, double velY){
		super(null);
		this.place(posX, posY, velX, velY, 0, 0);
		
		this.type = type;
		time = -2*type.preloadFrames*Main.TPS/Main.framesPerSec;
		
		for (int x = 0; x < type.preloadFrames; x++)
			type.queueLoadImg(x);
	}
	
	public void act(){
		time++;
		if (time*Main.framesPerSec >= type.getNumFrames()*Main.TPS)
			Main.game.removeGraphic(this);
	}
	
	public Image getImage(double zoom, double renderTime){
		if (time < 0)
			return null;
		int frame = time*Main.framesPerSec/Main.TPS;
		
		Image img = type.getImg(zoom, frame, renderTime < 0.6);
		if (frame < type.getNumFrames()-type.preloadFrames)
			type.queueLoadImg(frame+type.preloadFrames);
		return img;
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
