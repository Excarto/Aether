import java.net.*;

// Represents an actively hosted game

public class Lobby{
	String name;
	InetAddress address;
	long creationTime;
	
	public Lobby(InetAddress address, String name){
		this.address = address;
		this.name = name;
		creationTime = java.lang.System.currentTimeMillis();
	}
	
	public int secondsAlive(){
		return (int)((java.lang.System.currentTimeMillis() - creationTime)/(1000));
	}
}
