
public abstract class InGameMsg extends Message{
	
	protected abstract boolean isGood();
	
	public void read(byte[] data){
		if (data.length == Connection.PACKET_SIZE){
			Main.game.queue(this, data);
		}else
			super.read(data);
	}
	
	protected void received(){
		if (isGood()){
			confirmed();
			if (Main.server != null)
				Main.game.sendMsg(this);
		}
	}
	
}
