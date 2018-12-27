import static java.lang.Math.*;
import java.awt.*;

public class Projectile extends Sprite{
	
	public final GunType type;
	public final Player player;
	public final Component subTarget;
	
	private int timeToLive, contactTime;
	int hitMissileOnlyTime;
	
	public Projectile(Gun gun, Player player, Component subTarget){
		super(gun.type.projectileRenderable);
		this.type = gun.type;
		this.player = player;
		this.subTarget = subTarget;
		Target target = gun.getTarget();
		if (!gun.isManualAim() && target != null && target.target instanceof Missile)
			hitMissileOnlyTime = Main.TPS*1;
		
		timeToLive = type.lifespan;
		setId();
	}
	
	public void act(){
		if (Main.game.turn-contactTime > 5){
			for (Player enemy : Main.game.players){
				if (enemy.team != player.team){
					for (Controllable controllable : enemy.controllables){
						if (controllable instanceof Missile || hitMissileOnlyTime <= 0){
							boolean[][] map = controllable.getContactMap();
							int posX = (int)(this.getPosX()-((Sprite)controllable).getPosX()+map.length/2);
							int posY = (int)(this.getPosY()-((Sprite)controllable).getPosY()+map[0].length/2);
							if (posX >= 0 && posX < map.length && posY >= 0 && posY < map[posX].length && map[posX][posY]){
								contactTime = Main.game.turn;
								player.contact(this, controllable);
								return;
							}
						}
					}
				}
			}
		}
		
		hitMissileOnlyTime--;
		if (timeToLive-- == 0)
			this.remove();
	}
	
	public void contact(Controllable controllable){
		Sprite sprite = (Sprite)controllable;
		if (!(controllable instanceof Missile && type.goThroughMissiles))
			this.remove();
		
		double velBearing = sprite.velBearing(this);
		controllable.takeHit(this.getPosX(), this.getPosY(),
				subTarget, velBearing, false,
				type.explosiveDamage,
				type.kineticMultiplier*pow(speed(sprite), type.kineticExponent),
				type.EMDamage);
		sprite.accel(-speed(sprite)*type.projectileMass/controllable.getMass(), velBearing+sprite.getAngle());
		sprite.accel(type.impactExplosionForce/sqrt(controllable.getMass()), bearing(sprite));
		
		Main.game.addGraphic(new Explosion(type.impactExplosion,
				this.getPosX(), this.getPosY(), sprite.getVelX(), sprite.getVelY()));
		
		Main.game.playSound(type.impactSound, controllable, true);
	}
	
	public void remove(){
		player.projectiles.remove(this);
		Main.game.removeGraphic(this);
	}
	
	public ContactMsg getContactMsg(Controllable controllable){
		ContactMsg msg = new ContactMsg();
		msg.projectile = this;
		msg.target = controllable;
		msg.explodes = false;
		Sprite sprite = (Sprite)controllable;
		msg.posX = this.getPosX()-sprite.getPosX();
		msg.posY = this.getPosY()-sprite.getPosY();
		msg.velX = this.getVelX()-sprite.getVelX();
		msg.velY = this.getVelY()-sprite.getVelY();
		msg.randomSeed = Unit.RANDOM.nextInt();
		Unit.RANDOM.setSeed(msg.randomSeed);
		return msg;
	}
	
	public int getIconSize(){
		return 1;
	}
	
	public void draw(Graphics2D g, GameWindow window){
		g.setColor(window.getPlayer().team == player.team ? Color.GREEN : Color.RED);
		super.draw(g, window);
	}
	
	public double getVisionSize(){
		return type.radarSize;
	}
	
	public double getRadarSize(){
		return 0.0;
	}
	
	public int getTimeToLive(){
		return timeToLive;
	}
}
