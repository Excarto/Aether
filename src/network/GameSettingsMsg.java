import java.io.*;

public class GameSettingsMsg extends Message {
	public byte getId(){return MsgId.GAME_SETTINGS.id;}
	
	public int budget;
	public int arenaIndex;
	public double gameSpeed;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeLong(budget);
		stream.writeInt(arenaIndex);
		stream.writeDouble(gameSpeed);
	}
	public void decode(DataInputStream stream) throws IOException{
		budget = (int)stream.readLong();
		arenaIndex = stream.readInt();
		gameSpeed = stream.readDouble();
	}
}
