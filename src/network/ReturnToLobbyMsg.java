import java.io.*;

public class ReturnToLobbyMsg extends Message{
	public byte getId(){return MsgId.RETURN_TO_LOBBY.id;}
	
	public String name;
	
	public ReturnToLobbyMsg(){
		name = "";
	}
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeUTF(name);
	}
	public void decode(DataInputStream stream) throws IOException{
		name = stream.readUTF();
	}
}
