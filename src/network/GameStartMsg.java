import java.io.*;

public class GameStartMsg extends Message {
	public byte getId(){return MsgId.GAME_START.id;}
	
	//int[] playerPositions;
	int randomSeed;
	
	public GameStartMsg(){
		//playerPositions = new int[Arena.MAX_PLAYERS];
	}
	
	public void encode(DataOutputStream stream) throws IOException{
		//for (int x = 0; x < playerPositions.length; x++)
		//	stream.writeByte(playerPositions[x]);
		stream.writeInt(randomSeed);
	}
	public void decode(DataInputStream stream) throws IOException{
		//for (int x = 0; x < playerPositions.length; x++)
		//	playerPositions[x] = stream.readByte();
		randomSeed = stream.readInt();
	}
}
