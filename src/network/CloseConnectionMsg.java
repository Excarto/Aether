import java.io.*;

public class CloseConnectionMsg extends Message {
	public byte getId(){return MsgId.CLOSE_CONNECTION.id;}
	
	public void encode(DataOutputStream stream) throws IOException{}
	public void decode(DataInputStream stream) throws IOException{}
}