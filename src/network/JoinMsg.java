import java.io.*;

public class JoinMsg extends Message{
	public byte getId(){return MsgId.JOIN.id;}
	
	public String name;
	public String dataHash;
	public int version;
	public SetupWindow.PlayerType playerType;
	public boolean inGame;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeUTF(name);
		stream.writeUTF(dataHash);
		stream.writeInt(version);
		stream.writeChar(playerType.typeChar);
		stream.writeBoolean(inGame);
	}
	public void decode(DataInputStream stream) throws IOException{
		name = stream.readUTF();
		dataHash = stream.readUTF();
		version = stream.readInt();
		char playerTypeChar = stream.readChar();
		playerType = (playerTypeChar == SetupWindow.PlayerType.AI.typeChar ? SetupWindow.PlayerType.AI
				: (playerTypeChar == SetupWindow.PlayerType.HOST.typeChar ? SetupWindow.PlayerType.HOST
				: SetupWindow.PlayerType.NET_HOST));
		inGame = stream.readBoolean();
	}
}
