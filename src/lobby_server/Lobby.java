import java.net.*;

public class Lobby{
	String name;
	InetAddress address;
	long creationTime;
	
	public Lobby(InetAddress address, String name){
		this.address = address;
		this.name = name;
		creationTime = java.lang.System.currentTimeMillis();
	}
	
	public int minutesAlive(){
		return (int)((java.lang.System.currentTimeMillis() - creationTime)/(1000*60));
	}
}
