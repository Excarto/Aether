import static java.lang.Math.*;
import java.io.*;

public class TextMsg extends Message{
	public byte getId(){return MsgId.TEXT.id;}
	
	public static final int MAX_LENGTH = 130;
	
	public String message;
	public int team;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeInt(team);
		int length = min(MAX_LENGTH, message.length());
		stream.writeUTF(message.substring(0, length));
	}
	public void decode(DataInputStream stream) throws IOException{
		team = stream.readInt();
		message = stream.readUTF();
	}
}