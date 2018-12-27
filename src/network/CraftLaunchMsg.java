import java.io.*;

public class CraftLaunchMsg extends InGameMsg{
	public byte getId(){return MsgId.CRAFT_LAUNCH.id;}
	
	public Ship ship;
	public Craft craft;
	public boolean catapult;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeByte((byte)Main.game.players.indexOf(ship.player));
		stream.writeShort(ship.getId());
		stream.writeShort(craft.getId());
		stream.writeBoolean(catapult);
	}
	public void decode(DataInputStream stream) throws IOException{
		Player player = Main.game.players.get(stream.readByte());
		ship = (Ship)player.controllables.getById(stream.readShort());
		short craftId = stream.readShort();
		craft = ship != null ? ship.crafts.getById(craftId) : null;
		catapult = stream.readBoolean();
	}
	
	protected boolean isGood(){
		return craft != null;
	}
	
	public void confirmed(){
		ship.launchCraft(craft, catapult);
	}
}
