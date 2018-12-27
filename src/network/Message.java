import java.io.*;

public abstract class Message{
	
	public enum MsgId{
		BEAM_CONTACT(1),
		BROADCAST(2),
		CAP_SETTINGS(3),
		CAPTURE(4),
		CLIENT_ACCEPTED(5),
		CLIENT_REJECTED(6),
		CLOSE_CONNECTION(7),
		CONTACT(8),
		CRAFT_DOCK(9),
		CRAFT_LAUNCH(10),
		FIRE_WEAPON(11),
		FORCE_TCP(12),
		GAME_END(13),
		GAME_SETTINGS(14),
		GAME_START(15),
		JOIN(16),
		LEAVE(17),
		RETURN_TO_LOBBY(18),
		SET_BUDGET(19),
		SET_READY(20),
		SET_TEAM(21),
		SPRITE_STATUS(22),
		TEXT(23),
		UDP_CONFIRM(24),
		UNIT_DESCRIPTION(25),
		UNIT_STATUS(26),
		WARP_IN(27),
		WEAPON_ANGLE(28),
		REPAIR(29);
		
		public final byte id;
		MsgId(int id){
			this.id = (byte)id;
		}
	}
	
	private DataInputStream inStream;
	private DataOutputStream outStream;
	private byte[] data;
	private int dataIndex;
	
	public abstract byte getId();
	protected abstract void encode(DataOutputStream stream) throws IOException;
	protected abstract void decode(DataInputStream stream) throws IOException;
	
	public Message(){
		data = new byte[4];
		outStream = new DataOutputStream(new OutputStream(){
			public void write(int dataByte){
				data[dataIndex++] = (byte)dataByte;
		}});
		dataIndex = 0;
		inStream = new DataInputStream(new InputStream(){
			public int read(){
				return data[dataIndex++] & 0xFF;
		}});
		data = null;
	}
	
	public int write(byte[] data){
		this.data = data;
		dataIndex = 2;
		try{
			encode(outStream);
		}catch(IOException e){
			e.printStackTrace();
		}
		data[0] = getId();
		data[1] = (byte)((dataIndex+1)/2);
		return 2*(data[1] & 0xFF);
	}
	
	public void read(byte[] data){
		this.data = data;
		dataIndex = 2;
		try{
			decode(inStream);
		}catch(IOException e){
			e.printStackTrace();
		}
		received();
	}
	
	public Protocol getProtocol(){
		return Protocol.TCP;
	}
	protected void received(){
		confirmed();
	}
	public void confirmed(){}
	
	static byte currentId = 0;
	static byte generateId(){
		return currentId++;
	}
}
