import java.io.*;

public class LeaveMsg extends Message{
	public byte getId(){return MsgId.LEAVE.id;}
	
	public String name;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeUTF(name);
	}
	public void decode(DataInputStream stream) throws IOException{
		name = stream.readUTF();
	}
}
