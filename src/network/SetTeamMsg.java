import java.io.*;

public class SetTeamMsg extends Message {
	public byte getId(){return MsgId.SET_TEAM.id;}
	
	public String name;
	public int team;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeUTF(name);
		stream.writeInt(team);
	}
	public void decode(DataInputStream stream) throws IOException{
		name = stream.readUTF();
		team = stream.readInt();
	}
}