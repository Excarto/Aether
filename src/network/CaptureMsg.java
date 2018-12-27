import java.io.*;

public class CaptureMsg extends InGameMsg{
	public byte getId(){return MsgId.CAPTURE.id;}
	
	public Player player;
	public Arena.Objective objective;
	public int amount;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeByte((byte)Main.game.players.indexOf(player));
		for (int x = 0; x < Main.game.arena.objectives.length; x++){
			if (Main.game.arena.objectives[x] == objective)
				stream.writeByte((byte)x);
		}
		stream.writeInt(amount);
	}
	public void decode(DataInputStream stream) throws IOException{
		player = Main.game.players.get(stream.readByte());
		objective = Main.game.arena.objectives[(int)stream.readByte()];
		amount = stream.readInt();
	}
	
	protected boolean isGood(){
		return player != null && objective != null;
	}
	
	public void confirmed(){
		objective.capture(player, amount);
	}
}
