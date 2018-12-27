import java.io.*;

public class GameEndMsg extends Message{
	public byte getId(){return MsgId.GAME_END.id;}
	
	public int victoryTeam;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeInt(victoryTeam);
	}
	public void decode(DataInputStream stream) throws IOException{
		victoryTeam = stream.readInt();
	}
	
	public void confirmed(){
		Main.game.endGame(victoryTeam);
	}
}