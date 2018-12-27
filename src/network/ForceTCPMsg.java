import java.io.*;

public class ForceTCPMsg extends Message {
	public byte getId(){return MsgId.FORCE_TCP.id;}
	
	public void encode(DataOutputStream stream) throws IOException{}
	public void decode(DataInputStream stream) throws IOException{}
}
