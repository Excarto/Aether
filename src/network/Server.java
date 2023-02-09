import static java.lang.Math.*;

import java.net.*;
import java.io.*;
import java.util.*;

// Handles networking for hosted game. Maintains a list of connected Clients. Also maintains a Client
// object for the localhost player as well as Clients for any AI players. The AI Clients do not maintain Connections.

public class Server{
	public static final int MAX_GAME_NAME_LENGTH = 40;
	public static final String DEFAULT_GAME_NAME = "BATTLE in SPACE";
	
	List<Client> clients; // Contains only network players, not host or computers
	Client localhost;
	List<Client> computerPlayers;
	ServerSocket listenSocket;
	Connection lobbyServer; // Connection to remote global LobbyServer for disseminating hosted game info
	final boolean isLAN;
	boolean inProgress;
	boolean running;
	
	Iterable<Client> humans;
	Iterable<Client> players;
	
	Arena arena;
	int budget;
	int currentName;
	double gameSpeed;
	String gameName;
	
	TextMsg textMsg;
	ClientRejectedMsg rejectMsg;
	ClientAcceptedMsg acceptMsg;
	GameSettingsMsg settingsMsg;
	GameStartMsg startMsg;
	LeaveMsg leaveMsg;
	
	public Server(boolean isLAN){
		this.isLAN = isLAN;
		gameName = DEFAULT_GAME_NAME;
		
		clients = new ArrayList<Client>();
		computerPlayers = new ArrayList<Client>();
		
		// Convenience iterator object, does not contain data
		humans = new Iterable<Client>(){
			public Iterator<Client> iterator(){
				return new Iterator<Client>(){
					Iterator<Client> clientIterator;
					public boolean hasNext(){
						return localhost != null && (clientIterator == null || clientIterator.hasNext());
					}
					public Client next(){
						if (clientIterator == null){
							clientIterator = clients.iterator();
							return localhost;
						}else
							return clientIterator.next();
					}
					public void remove(){}
				};
			}
		};
		
		// Convenience iterator object, does not contain data
		players = new Iterable<Client>(){
			public Iterator<Client> iterator(){
				return new Iterator<Client>(){
					Iterator<Client> humanIterator = humans.iterator();
					Iterator<Client> compIterator = computerPlayers.iterator();
					public boolean hasNext(){
						return humanIterator.hasNext() || compIterator.hasNext();
					}
					public Client next(){
						if (humanIterator.hasNext()){
							return humanIterator.next();
						}else
							return compIterator.next();
					}
					public void remove(){}
				};
			}
		};
		
		textMsg = new TextMsg();
		rejectMsg = new ClientRejectedMsg();
		acceptMsg = new ClientAcceptedMsg();
		settingsMsg = new GameSettingsMsg();
		startMsg = new GameStartMsg();
		leaveMsg = new LeaveMsg();
	}
	
	private void listenForClients(){
		try{
			Connection localhostConnection = new Connection(listenSocket.accept(), Main.options.forceTCP);
			localhost = new Client(false);
			localhost.connection = localhostConnection;
			
			while (running){
				final Connection connection = new Connection(listenSocket.accept(), Main.options.forceTCP);
				connection.addListener(new JoinMsg(){
					public void confirmed(){
						if (clients.size()+computerPlayers.size()+1 >= Arena.MAX_PLAYERS){
							textMsg.message = "Game full - player " + name + " rejected";
							for (Client client : humans)
								client.connection.send(textMsg);
							rejectMsg.message = "Game is full";
							connection.send(rejectMsg);
							connection.close();
							
						}else if (version < Main.VERSION){
							rejectMsg.message = "The host has a newer game version";
							connection.send(rejectMsg);
							connection.close();
							
						}else if (version > Main.VERSION){
							rejectMsg.message = "The host has an older game version";
							connection.send(rejectMsg);
							connection.close();
							
						}else if (!dataHash.equals(Main.dataHash)){
							rejectMsg.message = "Incompatible game data";
							connection.send(rejectMsg);
							connection.close();
							
						}else if (inProgress){
							rejectMsg.message = "Game is started";
							connection.send(rejectMsg);
							connection.close();
							
						}else if (getClient(name) != null || name.length() < 1 || name.startsWith("AI ")){
							rejectMsg.message = "Username already in use";
							connection.send(rejectMsg);
							connection.close();
							
						}else{
							// New player successfully joined. Initialize the client, send info about the new client to existing players,
							// and send updated information to the LobbyServer
							
							connection.send(acceptMsg);
							
							final Client newClient = new Client(false);
							newClient.connection = connection;
							newClient.name = name;
							clients.add(newClient);
							
							while (getClient(name) == null){
								try{
									Thread.sleep(30);
								}catch(InterruptedException ex){}
							}
							
							sendInfoToLobbyServer();
							
							for (Client player : humans){
								if (player != newClient)
									sendInfo(player.connection, newClient);
							}
							
							addListeners(newClient);
							connection.setCloseListener(connection.new CloseListener(){
								public void closed(){
									clients.remove(newClient);
									leaveMsg.name = newClient.name;
									for (Client player : humans)
										player.connection.send(leaveMsg);
									if (newClient.inGame){
										if (Main.game != null)
											Main.game.playerLeft(newClient.name);
										tryEndGame();
									}
									sendInfoToLobbyServer();
							}});
						}
				}});
			} // End while(running)
		}catch (IOException e){
			if (running){
				close();
				e.printStackTrace();
			}
		}
	}
	
	// For LAN games, attempt to periodically broadcast hosted server info
	private void broadcast(){
		DatagramSocket broadcastSocket = null;
		try{
			InetAddress broadcastAddress = getBroadcastAddress();
			broadcastSocket = new DatagramSocket(Main.options.serverBroadcastPort);
			broadcastSocket.setBroadcast(true);
			byte[] broadcastBuffer = new byte[Connection.PACKET_SIZE];
			BroadcastMsg broadcastMsg = new BroadcastMsg();
			while(running){
				try {
					Thread.sleep(BroadcastMsg.BROADCAST_INTERVAL);
				} catch (InterruptedException e){
					e.printStackTrace();
				}
				broadcastMsg.address = getLocalAddress();
				broadcastMsg.gameName = gameName;
				broadcastMsg.numClients = 1+clients.size();
				int length = broadcastMsg.write(broadcastBuffer);
				DatagramPacket packet = new DatagramPacket(broadcastBuffer, length,
							broadcastAddress, Main.options.clientBroadcastPort);
				broadcastSocket.send(packet);
			}
		}catch (IOException e){
			textMsg.message = "Server broadcast failed";
			localhost.connection.send(textMsg);
			java.lang.System.err.println(textMsg.message);
			e.printStackTrace();
		}
		if (broadcastSocket != null)
			broadcastSocket.close();
	}
	
	// For internet games, attempt to send information about hosted server to the global LobbyServer
	private void connectToLobbyServer(){
		Socket lobbyServerSocket = null;
		try{
			lobbyServerSocket = new Socket();
			lobbyServerSocket.setReuseAddress(true);
			lobbyServerSocket.bind(new InetSocketAddress(Main.options.clientPort));
			lobbyServerSocket.connect(new InetSocketAddress(Main.options.lobbyServer, Main.options.lobbyHostPort), 4000);
			lobbyServer = new Connection(lobbyServerSocket, true);
			sendInfoToLobbyServer();
		}catch (IOException ex){
			lobbyServer = null;
			try{
				lobbyServerSocket.close();
			}catch (IOException e){}
			textMsg.message = "Cannot connect to game lobby server";
			localhost.connection.send(textMsg);
			java.lang.System.err.println(textMsg.message);
			ex.printStackTrace();
		}
	}
	
	public Connection start(String name, List<Ship> ships){
		Connection localClient = null;
		try{
			listenSocket = new ServerSocket(Main.options.serverPort);
			listenSocket.setReuseAddress(true);
			
			if (!isLAN)
				Main.UPnPHandler.enableUPnP(Main.options.serverPort, Protocol.TCP);
			
			running = true;
			new Thread("ServerListenThread"){
				public void run(){
					listenForClients();
			}}.start();
			
			Socket localClientSocket = new Socket();
			localClientSocket.setReuseAddress(true);
			localClientSocket.bind(new InetSocketAddress(Main.options.clientPort));
			localClientSocket.connect(new InetSocketAddress("localhost", Main.options.serverPort));
			
			while (localhost == null){
				try{
					Thread.sleep(20);
				}catch (InterruptedException e){}
			}
			localClient = new Connection(localClientSocket, true);
			
			localhost.name = name;
			localhost.team = 1;
			localhost.ships = ships;
			addListeners(localhost);
			
			new Thread("ServerAdvertiseThread"){
				public void run(){
					if (isLAN){
						broadcast();
					}else
						connectToLobbyServer();
			}}.start();
			
		}catch (IOException e){
			e.printStackTrace();
			Main.crash("Could not initialize server: " + e.getMessage());
		}
		return localClient;
	}
	
	private void sendInfoToLobbyServer(){
		if (lobbyServer != null){
			BroadcastMsg msg = new BroadcastMsg();
			msg.gameName = gameName;
			msg.numClients = 1+clients.size();
			lobbyServer.send(msg);
		}
	}
	
	// Initialize client with listeners to specify what to do when receiving messages from client
	private void addListeners(final Client newClient){
		newClient.connection.addListener(new GameSettingsMsg(){
			public void confirmed(){
				newClient.connection.send(settingsMsg);
				
				for (Client player : players)
					sendInfo(newClient.connection, player);
		}});
		
		newClient.connection.addListener(new TextMsg(){
			public void confirmed(){
				if (message.equals("ping") && Main.game != null){
					textMsg.team = 0;
					for (Player player : Main.game.players){
						if (player instanceof NetPlayerHost){
							textMsg.message = player.name + " - " + (int)(((NetPlayerHost)player).latency*1000/Main.TPS) + "ms";
							newClient.connection.send(textMsg);
						}
					}
				}else{
					message = newClient.name + ": " + message;
					for (Client player : humans)
						player.connection.send(this);
				}
		}});
		
		newClient.connection.addListener(new SetTeamMsg(){
			public void confirmed(){
				name = newClient.name;
				for (Client player : humans){
					if (newClient != player)
						player.connection.send(this);
				}
		}});
		
		newClient.connection.addListener(new SetReadyMsg(){
			public void confirmed(){
				newClient.ready = ready;
				if (!ready)
					newClient.ships.clear();
		}});
		
		newClient.connection.addListener(new ReturnToLobbyMsg(){
			public void confirmed(){
				if (newClient.inGame){
					name = newClient.name;
					newClient.inGame = false;
					for (Client client : humans){
						if (client != newClient)
							client.connection.send(this);
					}
					if (Main.game != null)
						Main.game.playerLeft(name);
					tryEndGame();
				}
		}});
		
		newClient.connection.addListener(new UnitDescriptionMsg(){
			public void confirmed(){
				if (!newClient.ready){
					if (unit instanceof Ship){
						newClient.ships.add((Ship)unit);
					}else{
						Ship ship = newClient.ships.get(newClient.ships.size()-1);
						ship.crafts.add(ship.crafts.size(), (Craft)unit);
					}
				}
		}});
	}
	
	// Send info about client to connection (may be sending info about itself)
	private void sendInfo(Connection connection, Client client){
		if (connection != client.connection){
			JoinMsg joinMsg = new JoinMsg();
			joinMsg.name = client.name;
			joinMsg.playerType = client.isAI ? SetupWindow.PlayerType.AI : SetupWindow.PlayerType.NET_HOST;
			joinMsg.dataHash = Main.dataHash;
			joinMsg.inGame = client.inGame;
			connection.send(joinMsg);
		}
		
		SetTeamMsg teamMsg = new SetTeamMsg();
		teamMsg.name = client.name;
		teamMsg.team = client.team;
		connection.send(teamMsg);
		
		SetBudgetMsg budgetMsg = new SetBudgetMsg();
		budgetMsg.name = client.name;
		budgetMsg.budget = client.budget;
		connection.send(budgetMsg);
	}
	
	public void setGameSettings(Arena arena, double gameSpeed, int budget){
		for (int x = 0; x < Main.arenas.length; x++)
			if (Main.arenas[x] == arena)
				settingsMsg.arenaIndex = x;
		settingsMsg.gameSpeed = gameSpeed;
		settingsMsg.budget = budget;
		
		for (Client client : clients)
			client.connection.send(settingsMsg);
		
		if (this.budget != budget){
			SetBudgetMsg budgetMsg = new SetBudgetMsg();
			budgetMsg.budget = budget;
			for (Client player : humans){
				player.budget = budget;
				budgetMsg.name = player.name;
				for (Client client : clients)
					client.connection.send(budgetMsg);
			}
		}
		
		this.arena = arena;
		this.budget = budget;
		this.gameSpeed = gameSpeed;
		
		sendInfoToLobbyServer();
	}
	
	public void setTeam(String name, int team){
		getClient(name).team = team;
		
		SetTeamMsg teamMsg = new SetTeamMsg();
		teamMsg.name = name;
		teamMsg.team = team;
		for (Client client : clients)
			client.connection.send(teamMsg);
	}
	
	public void setBudget(String name, int budget){
		getClient(name).budget = budget;
		
		SetBudgetMsg budgetMsg = new SetBudgetMsg();
		budgetMsg.name = name;
		budgetMsg.budget = budget;
		for (Client client : clients)
			client.connection.send(budgetMsg);
	}
	
	public void setGameName(String name){
		gameName = name.substring(0, min(name.length(), MAX_GAME_NAME_LENGTH));
	}
	
	public void kick(String name){
		Client client = getClient(name);
		if (client.isAI){
			computerPlayers.remove(client);
			leaveMsg.name = name;
			for (Client player : humans)
				player.connection.send(leaveMsg);
		}else{
			textMsg.message = client.name + " has been kicked";
			for (Client player : humans)
				player.connection.send(textMsg);
			client.connection.close();
			clients.remove(client);
			if (client.inGame)
				tryEndGame();
		}
	}
	
	public void setShips(String name, List<Ship> ships){
		getClient(name).ships = ships;
	}
	
	public Connection getConnection(String name){
		return getClient(name).connection;
	}
	
	public void addAI(){
		if (clients.size()+computerPlayers.size()+1 < Arena.MAX_PLAYERS){
			Client comp = new Client(true);
			comp.name = Main.names[currentName++%Main.names.length];
			while (getClient(comp.name) != null)
				comp.name = comp.name + (1+(int)(random()*9));
			computerPlayers.add(comp);
			
			JoinMsg joinMsg = new JoinMsg();
			joinMsg.name = comp.name;
			joinMsg.playerType = SetupWindow.PlayerType.AI;
			joinMsg.dataHash = Main.dataHash;
			SetTeamMsg teamMsg = new SetTeamMsg();
			teamMsg.name = comp.name;
			teamMsg.team = comp.team;
			for (Client player : humans){
				player.connection.send(joinMsg);
				player.connection.send(teamMsg);
			}
		}
	}
	
	public boolean checkReady(){
		boolean ready = true;
		for (Client client : clients){
			if (!client.ready){
				ready = false;
				textMsg.message = "Cannot start: " + client.name + " is not ready";
				for (Client player : humans)
					player.connection.send(textMsg);
			}
		}
		return ready;
	}
	
	public void startGame(int randomSeed){
		for (Client client : humans)
			client.inGame = true;
		
		startMsg.randomSeed = randomSeed;
		
		UnitDescriptionMsg msg = new UnitDescriptionMsg();
		for (Client client : clients){
			// Communicate units of all players to each other before starting
			for (Client player : players){
				if (client != player){
					msg.player = player.name;
					for (Ship ship : player.ships){
						msg.unit = ship;
						client.connection.send(msg);
						for (Craft craft : ship.crafts){
							msg.unit = craft;
							client.connection.send(msg);
						}
					}
				}
			}
			client.connection.send(startMsg);
		}
	}
	
	public void tryEndGame(){
		boolean allInLobby = true;
		for (Client client : humans)
			allInLobby = allInLobby && !client.inGame;
		if (allInLobby){
			inProgress = false;
			for (Client player : clients)
				player.ships.clear();
		}
	}
	
	private Client getClient(String name){
		for (Client player : players)
			if (player.name.equals(name))
				return player;
		return null;
	}
	
	public void close(){
		running = false;
		while (!clients.isEmpty())
			clients.get(0).connection.close();
		localhost.connection.close();
		try{
			listenSocket.close();
		}catch(IOException e){}
		if (lobbyServer != null)
			lobbyServer.close();
		Connection.closeUDP();
	}
	
	private static InetAddress getLocalAddress(){
	    Enumeration<NetworkInterface> en;
		try{
			en = NetworkInterface.getNetworkInterfaces();
		}catch (SocketException e){
			e.printStackTrace();
			return null;
		}
	    while (en.hasMoreElements()){
	        NetworkInterface i = en.nextElement();
	        for (Enumeration<InetAddress> en2 = i.getInetAddresses(); en2.hasMoreElements();) {
	        	InetAddress addr = en2.nextElement();
	        	if (!addr.isLoopbackAddress() && addr instanceof Inet4Address)
	            	return addr;
	        }
	    }
	    return null;
	}
	
	private static InetAddress getBroadcastAddress() throws IOException{
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()){
			NetworkInterface networkInterface = interfaces.nextElement();
			if (networkInterface.isUp() && !networkInterface.isLoopback()){
				for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()){
					InetAddress broadcastAddress = interfaceAddress.getBroadcast();
					if (broadcastAddress != null)
						return broadcastAddress;
				}
			}
		}
		return InetAddress.getByName("255.255.255.255");
	}
	
	public class Client{
		Connection connection;
		String name;
		int team;
		int budget;
		List<Ship> ships;
		boolean ready, inGame;
		final boolean isAI;
		
		public Client(boolean isAI){
			this.isAI = isAI;
			budget = Server.this.budget;
			int lowestTeam = 1, lowestNumPlayers = Integer.MAX_VALUE;
			for (int x = 1; x <= arena.teamPos.length; x++){
				int numPlayers = 0;
				for (Client player : players)
					if (player.team == x)
						numPlayers++;
				if (numPlayers < lowestNumPlayers){
					lowestNumPlayers = numPlayers;
					lowestTeam = x;
				}
			}
			team = lowestTeam;
			ready = false;
			inGame = false;
		}
	}
}
