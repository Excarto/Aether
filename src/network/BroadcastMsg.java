import java.io.*;
import java.net.*;

// Used for both broadcasting hosted server on LAN, or to communicate hosted internet server to the LobbyServer

public class BroadcastMsg extends Message{
	public byte getId(){return MsgId.BROADCAST.id;}
	
	public static final int BROADCAST_INTERVAL = 2000;
	private static final char[] VALID_NAME_CHARS =
			" abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_+-".toCharArray();
	
	InetAddress address;
	String gameName;
	int numClients;
	
	protected void encode(DataOutputStream stream) throws IOException{
		stream.writeUTF(address == null ? "" : address.getHostAddress());
		stream.writeUTF(gameName);
		stream.writeInt(numClients);
	}
	
	protected void decode(DataInputStream stream) throws IOException{
		try{
			address = InetAddress.getByName(stream.readUTF());
		}catch (IOException ex){
			address = null;
		}
		gameName = Utility.filter(stream.readUTF(), VALID_NAME_CHARS);
		numClients = stream.readInt();
	}
	
}
