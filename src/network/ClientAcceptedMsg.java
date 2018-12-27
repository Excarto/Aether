import java.io.*;

public class ClientAcceptedMsg extends Message{
	public byte getId(){return MsgId.CLIENT_ACCEPTED.id;}
	
	public void encode(DataOutputStream stream) throws IOException{}
	public void decode(DataInputStream stream) throws IOException{}
}
