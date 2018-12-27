import java.io.*;

public class SetBudgetMsg extends Message {
	public byte getId(){return MsgId.SET_BUDGET.id;}
	
	public String name;
	public int budget;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeUTF(name);
		stream.writeLong(budget);
	}
	public void decode(DataInputStream stream) throws IOException{
		name = stream.readUTF();
		budget = (int)stream.readLong();
	}
}