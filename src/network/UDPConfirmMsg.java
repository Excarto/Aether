import java.io.*;

public class UDPConfirmMsg extends Message {
	public byte getId(){return MsgId.UDP_CONFIRM.id;}
	public Protocol getProtocol(){
		return Protocol.UDP;
	}
	
	public void encode(DataOutputStream stream) throws IOException{}
	public void decode(DataInputStream stream) throws IOException{}
}
