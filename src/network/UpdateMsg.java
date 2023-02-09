import java.io.*;

// Used for periodic in-game update messages

public abstract class UpdateMsg extends InGameMsg{
	public static final int TIME_UPDATE_FACTOR = 32;
	public Protocol getProtocol(){
		return Protocol.UDP;
	}
	
	public int time;
	public Player player;
	public double latency;
	
	protected void received(){
		super.received();
		if (Main.server == null){
			//Main.game.time = (3*Main.game.time + time+latency)/4;
			Main.game.updateTime(time+latency);
		}else{
			//Main.game.sendMsg(this);
			((NetPlayerHost)this.player).updateLatency(Main.game.turn-time);
			//player.latency = (3*player.latency + (Main.game.time-time))/4.0;
		}
	}
	
	protected void encode(DataOutputStream stream) throws IOException{
		if (Main.server != null)
			stream.writeFloat((float)latency);
		stream.writeInt(Main.game.turn);
		stream.writeByte(Main.game.players.indexOf(player));
	}
	protected void decode(DataInputStream stream) throws IOException{
		if (Main.server == null)
			latency = stream.readFloat();
		time = stream.readInt();
		player = Main.game.players.get(stream.readByte());
	}
}
