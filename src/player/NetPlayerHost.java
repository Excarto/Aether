import static java.lang.Math.*;
import java.util.*;

public class NetPlayerHost extends Player{
	
	public final Connection connection;
	
	SpriteStatusMsg spriteMsg;
	UnitStatusMsg unitMsg;
	WeaponAngleMsg angleMsg;
	double latency;
	boolean running;
	
	public NetPlayerHost(String name, int team, List<Ship> ships, int budget, Arena arena, Connection connection){
		super(name, team, ships, budget, arena);
		this.connection = connection;
		spriteMsg = new SpriteStatusMsg();
		unitMsg = new UnitStatusMsg();
		angleMsg = new WeaponAngleMsg();
		
		connection.addListener(new SpriteStatusMsg());
		connection.addListener(new UnitStatusMsg());
		connection.addListener(new WeaponAngleMsg());
		connection.addListener(new FireWeaponMsg());
		connection.addListener(new CraftLaunchMsg());
		connection.addListener(new CraftDockMsg());
		connection.addListener(new CaptureMsg());
		connection.addListener(new ContactMsg());
		connection.addListener(new BeamContactMsg());
		connection.addListener(new WarpInMsg());
		connection.addListener(new RepairMsg());
	}
	
	public void act(){
		super.act();
		
		spriteMsg.latency = unitMsg.latency = angleMsg.latency = latency;
		for (Player player : Main.game.players){
			if (player != this){
				spriteMsg.player = unitMsg.player = angleMsg.player = player;
				
				if (turnsPassed%SpriteStatusMsg.UPDATE_INTERVAL == 0){
					for (int x = 0; x < player.firingBeams.size(); x += WeaponAngleMsg.NUM_WEAPONS){
						angleMsg.numWeapons = min(WeaponAngleMsg.NUM_WEAPONS, player.firingBeams.size()-x);
						for (int y = 0; y < angleMsg.numWeapons; y++)
							angleMsg.weapons[y] = player.firingBeams.get(x+y);
						connection.send(angleMsg);
					}
				}
				
				if (turnsPassed%SpriteStatusMsg.UPDATE_INTERVAL == 0){
					for (int x = 0; x < player.controllables.size(); x += SpriteStatusMsg.NUM_SPRITES){
						spriteMsg.numSprites = min(SpriteStatusMsg.NUM_SPRITES, player.controllables.size()-x);
						for (int y = 0; y < spriteMsg.numSprites; y++)
							spriteMsg.sprites[y] = (Sprite)player.controllables.get(x+y);
						connection.send(spriteMsg);
					}
				}
				
				if (turnsPassed%UnitStatusMsg.UPDATE_INTERVAL == 0){
					unitMsg.numUnits = 0;
					for (int x = 0; x < player.controllables.size(); x++){
						Controllable controllable = player.controllables.get(x);
						if (controllable instanceof Unit){
							unitMsg.units[unitMsg.numUnits++] = (Unit)controllable;
							if (unitMsg.numUnits == UnitStatusMsg.NUM_UNITS){
								connection.send(unitMsg);
								unitMsg.numUnits = 0;
							}
						}
					}
					if (unitMsg.numUnits > 0)
						connection.send(unitMsg);
				}
			}
		}
	}
	
	public void contact(Projectile projectile, Controllable controllable){
	/*	ContactMsg msg = projectile.getContactMsg(controllable);
		projectile.contact(controllable);
		if (controllable.getHull() <= 0){
			controllable.explode();
			msg.explodes = true;
		}
		Main.game.sendMsg(msg);*/
	}
	public void contact(Beam beam, Controllable controllable, double posX, double posY){
	/*	BeamContactMsg msg = beam.getContactMsg(controllable, posX, posY);
		beam.contact(controllable, posX, posY);
		if (controllable.getHull() <= 0){
			controllable.explode();
			msg.explodes = true;
		}
		Main.game.sendMsg(msg);*/
	}
	
	public void launchCraft(Ship ship, Craft craft, boolean catapult){}
	public void retrieveCraft(Ship ship, Craft craft){}
	
	public void warpIn(Ship ship){}
	
	public void gameEnd(int victoryTeam){
		super.gameEnd(victoryTeam);
		GameEndMsg msg = new GameEndMsg();
		msg.victoryTeam = victoryTeam;
		connection.send(msg);
	}
	
	public void updateLatency(double latency){
		this.latency = ((UpdateMsg.TIME_UPDATE_FACTOR-1)*this.latency + latency)/UpdateMsg.TIME_UPDATE_FACTOR;
	}
}
