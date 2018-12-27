import java.io.*;

public class WarpInMsg extends InGameMsg{
	public byte getId(){return MsgId.WARP_IN.id;}
	
	public Ship ship;
	public int randomSeed;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeByte((byte)Main.game.players.indexOf(ship.player));
		stream.writeShort(ship.getId());
		stream.writeInt(randomSeed);
	}
	public void decode(DataInputStream stream) throws IOException{
		Player player = Main.game.players.get(stream.readByte());
		ship = player.ships.getById(stream.readShort());
		randomSeed = stream.readInt();
	}
	
	protected boolean isGood(){
		return ship != null && ship.outOfArena;
	}
	
	public void confirmed(){
		Ship.random.setSeed(randomSeed);
		ship.player.confirmWarp(ship);
	}
}
