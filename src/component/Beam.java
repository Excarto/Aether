import static java.lang.Math.*;
import java.awt.*;
import java.awt.geom.*;

// Beam-type weapon
public class Beam extends Weapon{
	
	final static int CHECK_INTERVAL = 2;
	
	public final BeamType type;
	
	int firingTimeLeft;
	double shotAngle;
	int contactTime;
	double contactLength;
	boolean hitMissileOnly;
	double renderPosX, renderPosY, renderAngle;
	
	BeamContactMsg contactMsg;
	
	public Beam(BeamType type, WeaponHardpoint hardpoint, Unit unit, int arc, double mountAngleFrac){
		super(type, hardpoint, unit, arc, mountAngleFrac);
		
		this.type = type;
	}
	
	public void move(){
		super.move();
		
		if (firingTimeLeft > 0){
			if (firingTimeLeft == 1)
				unit.player.firingBeams.remove(this);
			firingTimeLeft--;
		}
	}
	
	public void act(){
		super.act();
		
		// Loop through all available targets
		if (firingTimeLeft > 0){
			double angle = toRadians(this.getAngle()+unit.getAngle()+shotAngle);
			double xRate = sin(angle), yRate = -cos(angle);
			for (Player enemy : Main.game.players){
				if (enemy.team != unit.player.team){
					for (Controllable controllable : enemy.controllables){
						if (controllable instanceof Missile || !hitMissileOnly){
							
							// Check along beam length for contact with target
							boolean[][] map = controllable.getContactMap();
							double posX = getPosX()-((Sprite)controllable).getPosX()+0.5*map.length;
							double posY = getPosY()-((Sprite)controllable).getPosY()+0.5*map[0].length;
							if (abs(posX) < type.length && abs(posY) < type.length){
								for (int n = min(type.length, CHECK_INTERVAL); n <= type.length; n += CHECK_INTERVAL){
									posX += xRate*CHECK_INTERVAL;
									posY += yRate*CHECK_INTERVAL;
									int indexX = (int)round(posX), indexY = (int)round(posY);
									if (indexX >= 0 && indexX < map.length && indexY >= 0 && indexY < map[indexX].length && map[indexX][indexY]){
										unit.player.contact(this, controllable, getPosX()+xRate*n, getPosY()+yRate*n);
										return;
									}
								}
							}
							
						}
					}
				}
			}
		}
	}
	
	public void recordPos(){
		renderPosX = getPosX();
		renderPosY = getPosY();
		renderAngle = getAngle()+unit.getAngle()+shotAngle;
	}
	
	// Deal damage and do necessary graphics/sound work
	public boolean contact(Controllable controllable, double posX, double posY){
		boolean alive = controllable.getHull() > 0;
		
		Sprite sprite = (Sprite)controllable;
		controllable.takeHit(posX, posY, getSubTarget(),
				Utility.fixAngle(90-toDegrees(atan2(sprite.getPosY()-getPosY(), getPosX()-sprite.getPosX()))-sprite.getAngle()),
				true, type.explosiveDamagePerTurn, 0, type.EMDamagePerTurn);
		
		if (Main.game.turn%3 != 0)
			Main.game.addGraphic(new Explosion(type.impactExplosion,
					posX, posY, sprite.getVelX(), sprite.getVelY()));
		Main.game.playSound(type.impactSound, controllable, true);
		
		contactTime = Main.game.turn;
		double dx = posX-getPosX(), dy = posY-getPosY();
		contactLength = sqrt(dx*dx+dy*dy)/type.imageLength;
		
		return alive && controllable.getHull() <= 0;
	}
	
	// Initiate beam shot
	public void fire(int time){
		super.fire(time);
		shotAngle = type.inaccuracy*(2*random.nextDouble()-1.0);
		firingTimeLeft = type.duration;
		unit.player.firingBeams.remove(this);
		unit.player.firingBeams.add(this);
		
		Target target = getTarget();
		hitMissileOnly = !isManualAim() && target != null && target.target instanceof Missile;
	}
	
	// Construct network message for beam hit
	public BeamContactMsg getContactMsg(Controllable controllable, double posX, double posY){
		if (contactMsg == null){
			contactMsg = new BeamContactMsg();
			contactMsg.beam = this;
		}
		contactMsg.target = controllable;
		contactMsg.explodes = false;
		contactMsg.posX = posX-((Sprite)controllable).getPosX();
		contactMsg.posY = posY-((Sprite)controllable).getPosY();
		contactMsg.randomSeed = Unit.RANDOM.nextInt();
		Unit.RANDOM.setSeed(contactMsg.randomSeed);
		return contactMsg;
	}
	
	protected double calcTargetAngle(){
		return toDegrees(atan2(-getDy(), -getDx()))-unit.getAngle()-90;
	}
	
	// Override, simple in case of beam
	protected double approxAngle(Locatable locatable, double time){
		return toDegrees(atan2(getPosY()-locatable.getPosY(), getPosX()-locatable.getPosX()))-90;
	}
	
	// Override, simple in case of beam
	protected double approxTime(Locatable locatable){
		return unit.distance(locatable) < type.length ? 0 : Double.MAX_VALUE/8;
	}
	
	// Decide if able to fire
	protected boolean inRange(Locatable target){
		return unit.distance(target) < type.length*7/8 && abs(unit.getTurnSpeed()) < type.trackRate*7/4;
	}
	protected boolean inManualRange(double dist) {
		return dist < type.length*7/8;
	}
	
	// Render beam graphic, accounding for termination at contact point
	public void draw(Graphics2D g, GameWindow window){
		int posX = window.posXOnScreen(renderPosX);
		int posY = window.posYOnScreen(renderPosY);
		int size = (int)(window.getZoom()*type.renderable.size);
		if (posX > -size && posX < window.windowResX+size && posY > -size && posY < window.windowResY+size){
			Image img = type.renderable.getImage(window.getZoom(), 0, false, 0);
			if (img != null){
				
				if (posX > -size && posY > -size && posX < window.windowResX+size && posY < window.windowResY+size){
					int width = img.getWidth(null);
					int height = img.getHeight(null);
					double angle = toRadians(renderAngle);
					AffineTransform origTransform = g.getTransform();
					
					if (Main.game.turn-contactTime < 2){
						
						int contactLength = min(height-1, max(1, (int)(this.contactLength*height)));
						double sina = sin(angle), cosa = cos(angle);
						double xmin = -abs(cosa)*width/2 + min(0, sina)*contactLength;
						double xmax = abs(cosa)*width/2 + max(0, sina)*contactLength;
						double ymin = -abs(sina)*width/2 - max(0, cosa)*contactLength;
						double ymax = abs(sina)*width/2 - min(0, cosa)*contactLength;
						
						Rectangle bounds = g.getClipBounds();
						g.clipRect((int)(posX+xmin), (int)(posY+ymin), (int)(xmax-xmin), (int)(ymax-ymin));
						
						g.rotate(angle, posX, posY);
						g.drawImage(img, posX-width/2, posY-height-2, null);
						g.setTransform(origTransform);
						
						g.setClip(bounds.x, bounds.y, bounds.width, bounds.height);
						//img = img.getSubimage(0, img.getHeight(null)-contactLength, img.getWidth(null), contactLength);
					}else{
						g.rotate(angle, posX, posY);
						g.drawImage(img, posX-width/2, posY-height-2, null);
						g.setTransform(origTransform);
					}
				}
				
			}
		}
	}
	
}
