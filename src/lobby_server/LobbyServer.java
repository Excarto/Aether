import java.net.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class LobbyServer{
	
	static final int CLIENT_PORT = 22886, SERVER_PORT = 22887;
	static final String SERVER_HOSTNAME = "aetherlobby.hopto.org";
	static final int PACKET_SIZE = 512;
	static final int LOBBY_MINUTES_TO_LIVE = 60*10;
	static final int MAX_NLOBBIES = 20;
	static final Lobby NO_INFO = new Lobby(null, null);
	
	ServerSocket clientListenSocket, hostListenSocket;
	boolean running;
	Map<Connection,Lobby> lobbies;
	
	LobbyServer(){
		log("Initializing lobby server");
		lobbies = new ConcurrentHashMap<Connection,Lobby>();
		
		try{
			running = true;
			clientListenSocket = new ServerSocket(CLIENT_PORT);
			hostListenSocket = new ServerSocket(SERVER_PORT);
			
			new Thread("ClientListenThread"){
				public void run(){
					listenForClients();
			}}.start();
			new Thread("HostListenThread"){
				public void run(){
					listenForHosts();
			}}.start();
			new Thread("LobbyCleanupThread"){
				public void run(){
					cleanupLobbies();
			}}.start();
		}catch (IOException ex){
			ex.printStackTrace();
		}
	}
	
	void stop(){
		running = false;
		try{
			clientListenSocket.close();
		}catch (IOException ex){}
		try{
			hostListenSocket.close();
		}catch (IOException ex){}
	}
	
	private void listenForClients(){
		BroadcastMsg msg = new BroadcastMsg();
		while (running){
			final Socket socket;
			try{
				socket = clientListenSocket.accept();
				socket.setSoTimeout(2000);
				socket.setSoLinger(true, 2);
			}catch (IOException ex){
				ex.printStackTrace();
				stop();
				return;
			}
			
			try{
				log("Client connected: "+socket.getInetAddress());
				final Connection connection = new Connection(socket, true);
				try{
					Thread.sleep(100);
				}catch (InterruptedException ex){}
				for (Lobby lobby : lobbies.values()){
					if (lobby != NO_INFO){
						msg.gameName = lobby.name;
						msg.address = lobby.address;
						connection.send(msg);
						//java.lang.System.out.println("Lobby info sent: "+lobby.address+" "+lobby.name);
					}
				}
				connection.close();
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
	}
	
	private void listenForHosts(){
		while (running){
			while (lobbies.size() >= MAX_NLOBBIES){
				try{
					Thread.sleep(1000);
				}catch (InterruptedException ex){}
			}
			final Socket socket;
			try{
				socket = hostListenSocket.accept();
			}catch (IOException ex){
				ex.printStackTrace();
				stop();
				return;
			}
			
			try{
				final Connection connection = new Connection(socket, true);
				
				boolean exists = false;
				for (Connection existing : lobbies.keySet())
					exists = exists || existing.remoteAddress.equals(connection.remoteAddress);
				if (!exists){
					lobbies.put(connection, NO_INFO);
					connection.addListener(new BroadcastMsg(){
						public void confirmed(){
							byte[] address = connection.remoteAddress.getAddress();
							InetAddress hostAddress;
							if (address[0] == (byte)10 && address[1] == (byte)0 && address[2] == (byte)0){
								try{
									hostAddress = InetAddress.getByName(SERVER_HOSTNAME);
								}catch (UnknownHostException e){
									hostAddress = connection.remoteAddress;
								}
							}else
								hostAddress = connection.remoteAddress;
							lobbies.put(connection, new Lobby(hostAddress, this.gameName));
							log("Host connected: "+lobbies.get(connection).address+" "+this.gameName);
					}});
					connection.setCloseListener(connection.new CloseListener(){
						public void closed(){
							lobbies.remove(connection);
							log("Host disconnected: "+connection.remoteAddress);
					}});
				}
				
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
	}
	
	private void cleanupLobbies(){
		while (running){
			try{
				Thread.sleep(1000*30);
			}catch (InterruptedException ex){}
			
			try{
				Connection toKick = null;
				for (Connection connection : lobbies.keySet()){
					Lobby lobby = lobbies.get(connection);
					int timeout = lobby == NO_INFO ? 10 : 60*LOBBY_MINUTES_TO_LIVE;
					if (lobby.secondsAlive() > timeout)
						toKick = connection;
				}
				if (toKick != null){
					lobbies.remove(toKick);
					toKick.close();
					log("Host kicked: "+toKick.remoteAddress);
				}
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
	}
	
	static final SimpleDateFormat dateFormat = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ");
	private void log(String string){
		java.lang.System.out.println(dateFormat.format(new Date()) + string);
	}
	
	public static void main(String[] args){
		new LobbyServer();
	}
	
}
