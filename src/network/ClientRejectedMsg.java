import java.io.*;

public class ClientRejectedMsg extends Message {
	public byte getId(){return MsgId.CLIENT_REJECTED.id;}
	
	public String message;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeUTF(message);
	}
	public void decode(DataInputStream stream) throws IOException{
		message = stream.readUTF();
	}
}
