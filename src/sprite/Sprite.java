import static java.lang.Math.*;
import java.util.*;
import java.util.List;
import java.awt.image.*;
import java.awt.*;

// An object that obeys Newtonian physics in-game. Also contains some mehtods common to all Controllables,
// such as thrusters and drawing the interface

public abstract class Sprite implements Id, Locatable{
	public static final Thruster[] NO_THRUSTERS = new Thruster[]{};
	public static final Thruster[][] NO_THRUSTERS_ALL = new Thruster[][]{NO_THRUSTERS, NO_THRUSTERS, NO_THRUSTERS};
	public static final List<Effect> NO_EFFECTS = new ArrayList<Effect>(0);
	
	private static short currentId;
	private static VolatileImage effectBuffer;
	
	protected final Renderable renderable;
	protected double renderPosX, renderPosY, renderAngle;
	
	private short id;
	private double posX, posY;
	private double velX, velY;
	private double angle;
	private double turnSpeed;
	
	private double adjustRateX, adjustRateY, adjustRateTurn;
	private int adjustTimeLeft;
	public int lastUpdate;
	public int[] netThrustTime;
	
	public Sprite(Renderable renderable){
		this.renderable = renderable;
		renderPosX = 10000.0;
		renderPosY = 10000.0;
		netThrustTime = new int[3];
	}
	
	public abstract double getRadarSize();
	public abstract double getVisionSize();
	public abstract int getIconSize();
	protected Thruster[][] getThrusters(){
		return NO_THRUSTERS_ALL;
	}
	protected BufferedImageOp getThrusterOp(){
		return null;
	}
	protected List<? extends Effect> getEffects(){
		return NO_EFFECTS;
	}
	
	public final void move(){
		if (adjustTimeLeft > 0){
			adjustTimeLeft--;
		}else
			adjustRateX = adjustRateY = adjustRateTurn = 0;
		
		posX += velX+adjustRateX;
		posY += velY+adjustRateY;
		angle = Utility.fixAngle(angle+turnSpeed+adjustRateTurn);
		
		//this.act();
	}
	
	public abstract void act();
	
	// Used for sychronizing between rendering thread and main game thread
	public void recordPos(){
		renderPosX = posX;
		renderPosY = posY;
		if (renderable != null)
			renderAngle = renderable.getRenderAngle(angle);
	}
	
	public void setId(){
		id = currentId++;
	}
	public short getId(){
		return id;
	}
	public static void resetId(){
		currentId = 0;
	}
	
	protected Image getImage(double zoom){
		return renderable.getImage(zoom, renderAngle, true, (int)signum(turnSpeed));
	}
	
	public int getRenderSize(double zoom){
		if (renderable == null)
			return 0;
		if (zoom < renderable.minZoom){
			return getIconSize();
		}else
			return (int)(zoom*renderable.size);
	}
	
	protected final void accel(double accel, double angle){
		double angleRads = toRadians(angle);
		posX += accel*sin(angleRads)/2;
		posY += accel*-cos(angleRads)/2;
		velX += accel*sin(angleRads);
		velY += accel*-cos(angleRads);
	}
	
	protected final void accelTurn(double accel){
		angle += accel/2;
		turnSpeed += accel;
	}
	
	public final double getRenderPosX(){
		return renderPosX;
	}
	public final double getRenderPosY(){
		return renderPosY;
	}
	
	public final double getPosX(){
		return posX;
	}
	public final double getPosY(){
		return posY;
	}
	public final double getVelX(){
		return velX;
	}
	public final double getVelY(){
		return velY;
	}
	public final double getAngle(){
		return angle;
	}
	public final double getTurnSpeed(){
		return turnSpeed;
	}
	
	public final double distance(Locatable locatable){
		double dx = locatable.getPosX()-this.posX, dy = locatable.getPosY()-this.posY;
		return sqrt(dx*dx+dy*dy);
	}
	public final double speed(Locatable locatable){
		double vx = locatable.getVelX()-this.velX, vy = locatable.getVelY()-this.velY;
		return sqrt(vx*vx+vy*vy);
	}
	public final double heading(Locatable locatable){
		return Utility.fixAngle(90-toDegrees(atan2(this.posY-locatable.getPosY(), locatable.getPosX()-this.posX)));
	}
	public final double bearing(Locatable locatable){
		return Utility.fixAngle(heading(locatable)-getAngle());
	}
	public final double velBearing(Locatable locatable){
		return Utility.fixAngle(90-toDegrees(atan2(locatable.getVelY()-this.velY, this.velX-locatable.getVelX()))-getAngle());
	}
	public final double radVel(Locatable locatable){
		double dx = locatable.getPosX()-this.posX, dy = locatable.getPosY()-this.posY;
		double vx = locatable.getVelX()-this.velX, vy = locatable.getVelY()-this.velY;
		return (dx*vx + dy*vy)/sqrt(dx*dx + dy*dy);
	}
	public final double tanSpeed(Locatable locatable){
		double dx = locatable.getPosX()-this.posX, dy = locatable.getPosY()-this.posY;
		double vx = locatable.getVelX()-this.velX, vy = locatable.getVelY()-this.velY;
		return abs(dx*vy - dy*vx)/sqrt(dx*dx + dy*dy);
	}
	
	public void place(double posX, double posY, double velX, double velY,
			double angle, double turnSpeed){
		this.posX = posX;
		this.posY = posY;
		this.velX = velX;
		this.velY = velY;
		this.angle = angle;
		this.turnSpeed = turnSpeed;
		this.renderPosX = posX;
		this.renderPosY = posY;
		this.renderAngle = angle;
	}
	
	// Used to smoothly correct motion for networked games
	public void adjustTo(double posX, double posY,
			double velX, double velY,
			double angle, double turnSpeed){
		
		adjustRateX = (posX-this.posX)/Main.config.netAdjustTime;
		adjustRateY = (posY-this.posY)/Main.config.netAdjustTime;
		if (this.angle-angle > 180){
			angle += 360;
		}else if (this.angle-angle < -180)
			angle -= 360;
		adjustRateTurn = (angle-this.angle)/Main.config.netAdjustTime;
		adjustTimeLeft = Main.config.netAdjustTime;
		
		this.velX = velX;
		this.velY = velY;
		this.turnSpeed = turnSpeed;
	}
	
	public void setIsNetworkThrusting(int direction, boolean isThrusting){
		netThrustTime[direction] = isThrusting ? SpriteStatusMsg.UPDATE_INTERVAL : 0;
	}
	
	public double getTargetAngle(){
		if (this instanceof Controllable){
			OrderQueue orders = ((Controllable)this).orders();
			if (orders != null && orders.getTopOrder() instanceof TurnTo)
				return ((TurnTo)orders.getTopOrder()).targetAngle;
		}
		return angle;
	}
	
	public int getSize(){
		if (renderable == null)
			return 0;
		return renderable.size;
	}
	
	public void draw(Graphics2D g, GameWindow window){
		int posX = window.posXOnScreen(renderPosX), posY = window.posYOnScreen(renderPosY);
		int size = getRenderSize(window.getRenderZoom());
		
		if (posX > -size && posX < window.windowResX+size && posY > -size && posY < window.windowResY+size){ // Check if on-screen
			Image img = getImage(window.getRenderZoom());
			if (img != null){
				int width = img.getWidth(null), height = img.getHeight(null);
				List<? extends Effect> effects = getEffects();
				
				Thruster[][] thrusters = getThrusters();
				BufferedImageOp operation = getThrusterOp();
				for (int x = 0; x < thrusters.length; x++){
					for (Thruster thruster : thrusters[x])
						thruster.draw(g, window, this, false, operation);
				}
				
				Graphics2D bufferGraphics = null;
				for (int x = 0; x < effects.size(); x++){
					Effect effect = effects.get(x);
					if (effect.drawToUnit()){
						if (bufferGraphics == null){
							bufferGraphics = effectBuffer.createGraphics();
							bufferGraphics.setComposite(AlphaComposite.Src);
							bufferGraphics.drawImage(img, 0, 0, null);
						}
						effect.draw(bufferGraphics, window, width/2, height/2);
					}
				}
				
				if (bufferGraphics != null){
					Rectangle oldClip = g.getClipBounds();
					g.clipRect(posX-width/2, posY-height/2, width, height);
					g.drawImage(effectBuffer, posX-width/2, posY-height/2, null);
					g.setClip(oldClip.x, oldClip.y, oldClip.width, oldClip.height);
					bufferGraphics.dispose();
				}else
					g.drawImage(img, posX-width/2, posY-height/2, null);
				
				for (int x = 0; x < thrusters.length; x++){
					for (Thruster thruster : thrusters[x])
						thruster.draw(g, window, this, true, operation);
				}
				
				for (int x = effects.size()-1; x >= 0; x--){
					if (!effects.get(x).drawToUnit())
						effects.get(x).draw(g, window, 0, 0);
				}
			}else{ // Image is null, meeaning too zoomed out. Draw a box icon instead
				
				size = getIconSize();
				if (size < 5){
					g.fillRect(posX-size/2, posY-size/2, size, size);
				}else{
					g.drawRect(posX-size/2, posY-size/2, size, size);
					for (int x = 2; x <= size/2; x++)
						g.drawRect(posX-size/2+x, posY-size/2+x, size-2*x, size-2*x);
					//g.drawRect(posX-size/2+2, posY-size/2+2, size-4, size-4);
				}
				
			}
		}
	}
	
	// HUD components that are drawn beneath others
	static final Color TURN_COLOR = new Color(0, 255, 0, 150);
	public void drawHudBottom(Graphics2D g, GameWindow window){
		Controllable controllable = (Controllable)this;
		int posX = window.posXOnScreen(renderPosX), posY = window.posYOnScreen(renderPosY);
		if (posX > 0 && posY > 0 && posX < window.windowResX && posY < window.windowResY){
			int size = getRenderSize(window.getRenderZoom());
			g.setColor(TURN_COLOR);
			Order topOrder = controllable.orders().getTopOrder();
			if (topOrder instanceof TurnTo){
				g.setColor(new Color(0, 255, 0, 30));
				g.fillArc(posX-size/2, posY-size/2, size, size, (int)(90-getAngle()), -((TurnTo)topOrder).getArc());
			}
		}
	}
	
	// HUD components that are drawn on top of others
	static final Color HUD_COLOR = new Color(0, 255, 0, 150);
	static final int GAP_SIZE = 40;
	public void drawHudTop(Graphics2D g, GameWindow window){
		int posX = (int)window.posXOnScreen(renderPosX), posY = (int)window.posYOnScreen(renderPosY);
		int renderSize = getRenderSize(window.getRenderZoom());
		int size = renderSize+8;
		if (posX > -size && posY > -size && posX < window.windowResX+size && posY < window.windowResY+size){
			g.setColor(HUD_COLOR);
			if (window.isStrategic()){
				int startAngle = 45 + GAP_SIZE/2;
				for (int x = 1; x <= 4; x++){
					if (x != 3 || !(this instanceof Unit) || renderSize != ((Unit)this).type.iconSize)
						g.drawArc(posX-size/2, posY-size/2, size+1, size+1, startAngle, 90-GAP_SIZE);
					startAngle += 90;
				}
			}else{
				g.drawLine(posX-size/8, posY-size/2, posX+size/8, posY-size/2);
				g.drawLine(posX+(size+1)/2, posY-size/8, posX+(size+1)/2, posY+size/8);
				g.drawLine(posX+size/8, posY+(size+1)/2, posX-size/8, posY+(size+1)/2);
				g.drawLine(posX-size/2, posY+size/8, posX-size/2, posY-size/8);
				//g.drawLine(posX-size/2, posY-size/4, posX-size/4, posY-size/2);
				//g.drawLine(posX+size/2, posY-size/4, posX+size/4, posY-size/2);
				//g.drawLine(posX-size/2, posY+size/4, posX-size/4, posY+size/2);
				//g.drawLine(posX+size/2, posY+size/4, posX+size/4, posY+size/2);
			}
		}
	}
	
	public void drawOrders(Graphics2D g, GameWindow window){
		Controllable controllable = (Controllable)this;
		
		if (controllable.orders().getOrder() != null){
			double lastPosX = renderPosX;
			double lastPosY = renderPosY;
			
			for (Order order : controllable.orders()){
				if (order instanceof LocatableOrder){
					((LocatableOrder)order).draw(g, window, lastPosX, lastPosY);
					lastPosX = order.getRenderPosX();
					lastPosY = order.getRenderPosY();
				}
			}
		}
	}
	
	public static void initBuffer(){
		if (effectBuffer != null)
			effectBuffer.flush();
		effectBuffer = Main.getCompatibleVolatileImage(ShipType.maxShipSize, ShipType.maxShipSize, true);
	}
	
	protected interface Effect{
		public abstract void draw(Graphics2D g, GameWindow window, int originX, int originY);
		public abstract void act();
		public abstract boolean drawToUnit();
	}
}
