import java.io.*;

public class CraftDockMsg extends InGameMsg{
	public byte getId(){return MsgId.CRAFT_DOCK.id;}
	
	public Ship ship;
	public Craft craft;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeByte((byte)Main.game.players.indexOf(ship.player));
		stream.writeShort(ship.getId());
		stream.writeShort(craft.getId());
	}
	public void decode(DataInputStream stream) throws IOException{
		Player player = Main.game.players.get(stream.readByte());
		ship = (Ship)player.controllables.getById(stream.readShort());
		short craftId = stream.readShort();
		craft = ship != null ? (Craft)player.controllables.getById(craftId) : null;
	}
	
	protected boolean isGood(){
		return craft != null;
	}
	
	public void confirmed(){
		ship.retrieveCraft(craft);
	}
}
