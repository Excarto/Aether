import java.util.*;

// Host player on server side in networked game. Events performed by the player are
// performed locally and the corresponding network message is sent to all networked clients

public class HostPlayer extends HumanPlayer{
	
	private final Connection connection;
	private final  TextMsg textMsg;
	
	public HostPlayer(String name, int team, List<Ship> ships, int budget, Arena arena, Connection connection){
		super(name, team, ships, budget, arena);
		this.connection = connection;
		textMsg = new TextMsg();
	}
	
	public void fireWeapon(Weapon weapon){
		Main.game.sendMsg(weapon.getFireMsg());
		super.fireWeapon(weapon);
	}
	
	public void contact(Projectile projectile, Controllable controllable){
		ContactMsg msg = projectile.getContactMsg(controllable);
		if (projectile.contact(controllable)){
			controllable.explode();
			msg.explodes = true;
		}
		Main.game.sendMsg(msg);
	}
	
	public void contact(Beam beam, Controllable controllable, double posX, double posY){
		BeamContactMsg msg = beam.getContactMsg(controllable, posX, posY);
		if (beam.contact(controllable, posX, posY)){
			controllable.explode();
			msg.explodes = true;
		}
		Main.game.sendMsg(msg);
	}
	
	public void launchCraft(Ship ship, Craft craft, boolean catapult){
		Main.game.sendMsg(ship.getLaunchMsg(craft, catapult));
		super.launchCraft(ship, craft, catapult);
	}
	
	public void retrieveCraft(Ship ship, Craft craft){
		Main.game.sendMsg(ship.getDockMsg(craft));
		super.retrieveCraft(ship, craft);
	}
	
	public void capture(Arena.Objective objective, int amount){
		Main.game.sendMsg(objective.getCapMsg(this, amount));
		objective.capture(this, amount);
	}
	
	public void repair(Repairable repairable, double material, boolean isScrap){
		repairable.repair(material, isScrap);
		if (!repairable.getPlayer().isMaster()){
			RepairMsg msg = new RepairMsg();
			msg.repairable = repairable;
			msg.material = material;
			msg.isScrap = isScrap;
			Main.game.sendMsg(msg);
		}
	}
	
	public void warpIn(Ship ship){
		Main.game.sendMsg(ship.getWarpMsg());
		confirmWarp(ship);
	}
	
	/*public void sendMessage(String message, boolean toAll){
		textMsg.message = name+": "+message;
		textMsg.team = toAll ? 0 : team;
		Main.game.sendMsg(textMsg);
		getWindow().receiveChat(textMsg.message, textMsg.team);
	}*/
	public void sendMessage(String message, boolean toAll){
		textMsg.message = message;
		textMsg.team = toAll ? 0 : team;
		connection.send(textMsg);
		//getWindow().receiveChat(name+": "+textMsg.message, textMsg.team);
	}
}
