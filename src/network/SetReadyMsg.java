import java.io.*;

public class SetReadyMsg extends Message {
	public byte getId(){return MsgId.SET_READY.id;}
	
	public boolean ready;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeBoolean(ready);
	}
	public void decode(DataInputStream stream) throws IOException{
		ready = stream.readBoolean();
	}
}