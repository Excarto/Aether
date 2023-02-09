import static java.lang.Math.*;
import java.util.*;

// Remote player on the client side of a networked game, whether host, another client, or AI.
// Periodically sends update messages for its own units to the server. Actions performed are
// not performed locally, but rather the corresponding network message is sent to the server
// and the action is performed when the server confirms the legality of it and sends the message back.

public class NetPlayerClient extends HumanPlayer{
	
	Connection connection;
	SpriteStatusMsg spriteMsg;
	UnitStatusMsg unitMsg;
	WeaponAngleMsg angleMsg;
	TextMsg textMsg;
	boolean running;
	
	public NetPlayerClient(String name, int team, List<Ship> ships, int budget, Arena arena, Connection connection){
		super(name, team, ships, budget, arena);
		this.connection = connection;
		spriteMsg = new SpriteStatusMsg();
		unitMsg = new UnitStatusMsg();
		angleMsg = new WeaponAngleMsg();
		textMsg = new TextMsg();
		spriteMsg.player = unitMsg.player = angleMsg.player = this;
		
		connection.addListener(new SpriteStatusMsg());
		connection.addListener(new UnitStatusMsg());
		connection.addListener(new WeaponAngleMsg());
		connection.addListener(new FireWeaponMsg());
		connection.addListener(new CraftLaunchMsg());
		connection.addListener(new CraftDockMsg());
		connection.addListener(new ContactMsg());
		connection.addListener(new BeamContactMsg());
		connection.addListener(new CaptureMsg());
		connection.addListener(new GameEndMsg());
		connection.addListener(new WarpInMsg());
		connection.addListener(new RepairMsg());
	}
	
	public void act(){
		super.act();
		
		if (turnsPassed%SpriteStatusMsg.UPDATE_INTERVAL == 0){
			for (int x = 0; x < firingBeams.size(); x += WeaponAngleMsg.NUM_WEAPONS){
				angleMsg.numWeapons = min(WeaponAngleMsg.NUM_WEAPONS, firingBeams.size()-x);
				for (int y = 0; y < angleMsg.numWeapons; y++)
					angleMsg.weapons[y] = firingBeams.get(x+y);
				connection.send(angleMsg);
			}
		}
		
		if (turnsPassed%SpriteStatusMsg.UPDATE_INTERVAL == 0){
			spriteMsg.time = Main.game.turn;
			for (int x = 0; x < controllables.size(); x += SpriteStatusMsg.NUM_SPRITES){
				spriteMsg.numSprites = min(SpriteStatusMsg.NUM_SPRITES, controllables.size()-x);
				for (int y = 0; y < spriteMsg.numSprites; y++)
					spriteMsg.sprites[y] = (Sprite)controllables.get(x+y);
				connection.send(spriteMsg);
			}
		}
		
		if (turnsPassed%UnitStatusMsg.UPDATE_INTERVAL == 0){
			unitMsg.time = Main.game.turn;
			unitMsg.numUnits = 0;
			for (int x = 0; x < controllables.size(); x++){
				Controllable controllable = controllables.get(x);
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
	
	public void fireWeapon(Weapon weapon){
		weapon.reload();
		connection.send(weapon.getFireMsg());
	}
	
	public void contact(Projectile projectile, Controllable controllable){
		connection.send(projectile.getContactMsg(controllable));
	}
	public void contact(Beam beam, Controllable controllable, double posX, double posY){
		connection.send(beam.getContactMsg(controllable, posX, posY));
	}
	
	public void launchCraft(Ship ship, Craft craft, boolean catapult){
		connection.send(ship.getLaunchMsg(craft, catapult));
	}
	public void retrieveCraft(Ship ship, Craft craft){
		craft.setDockTime();
		connection.send(ship.getDockMsg(craft));
	}
	
	public void capture(Arena.Objective objective, int amount){
		connection.send(objective.getCapMsg(this, amount));
	}
	
	public void repair(Repairable repairable, double material, boolean isScrap){
		if (repairable.getPlayer().isMaster()){
			repairable.repair(material, isScrap);
		}else{
			RepairMsg msg = new RepairMsg();
			msg.repairable = repairable;
			msg.material = material;
			msg.isScrap = isScrap;
			connection.send(msg);
		}
	}
	
	public void warpIn(Ship ship){
		connection.send(ship.getWarpMsg());
	}
	
	public void sendMessage(String message, boolean toAll){
		textMsg.message = message;
		textMsg.team = toAll ? 0 : team;
		connection.send(textMsg);
		//getWindow().receiveChat(name+": "+textMsg.message, textMsg.team);
	}
}
