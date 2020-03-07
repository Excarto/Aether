import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ClientJoinWindow extends Window{
	static final int MAX_GAMES = 10;
	
	boolean isLAN;
	Connection connection;
	JTextField addressField;
	JLabel errorLabel;
	JList<AvailableServer> serverList;
	JButton refreshButton;
	DefaultListModel<AvailableServer> serverListModel;
	boolean[] running;
	String username;
	
	public ClientJoinWindow(boolean isLAN){
		super(Size.NORMAL);
		this.isLAN = isLAN;
		
		username = Main.options.username;
		
		serverListModel = new DefaultListModel<AvailableServer>();
		serverList = new JList<AvailableServer>(serverListModel);
		serverList.setPreferredSize(new Dimension(500, 300));
		serverList.setBorder(BorderFactory.createEtchedBorder());
		serverList.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				if (!serverList.isSelectionEmpty())
					addressField.setText(serverList.getSelectedValue().getAddressString());
			}
		});
		
		addressField = new JTextField();
		addressField.setPreferredSize(new Dimension(200, 20));
		addressField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				join();
		}});
		
		JButton joinButton = new JButton("Join");
		joinButton.setPreferredSize(new Dimension(100, 30));
		joinButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				join();
		}});
		
		if (!isLAN){
			refreshButton = new JButton("Refresh");
			refreshButton.setPreferredSize(new Dimension(100, 30));
			refreshButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					queryLobbyServer();
			}});
		}
		
		JPanel joinPanel = new JPanel();
		joinPanel.setPreferredSize(new Dimension(1000, 50));
		joinPanel.setOpaque(false);
		joinPanel.add(new JLabel("IP Address: "));
		joinPanel.add(addressField);
		joinPanel.add(joinButton);
		if (!isLAN)
			joinPanel.add(refreshButton);
		
		final JTextField usernameField = new JTextField(username);
		usernameField.setPreferredSize(new Dimension(120, 22));
		usernameField.getDocument().addDocumentListener(new DocumentListener(){
			public void changedUpdate(DocumentEvent e){
				username = usernameField.getText();
			}
			public void insertUpdate(DocumentEvent e){
				username = usernameField.getText();
			}
			public void removeUpdate(DocumentEvent e){
				username = usernameField.getText();
			}
		});
		
		JPanel namePanel = new JPanel();
		namePanel.setPreferredSize(new Dimension(1000, 50));
		namePanel.setOpaque(false);
		namePanel.add(new JLabel("Player Name:"));
		namePanel.add(usernameField);
		
		JButton backButton = new JButton("Back");
		backButton.setPreferredSize(new Dimension(120, 35));
		backButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Main.removeWindow();
		}});
		JPanel backPanel = new JPanel();
		backPanel.setPreferredSize(new Dimension(1000, 100));
		backPanel.setOpaque(false);
		backPanel.add(backButton);
		
		errorLabel = new JLabel();
		errorLabel.setFont(Main.getDefaultFont(13));
		JPanel errorPanel = new JPanel();
		errorPanel.setPreferredSize(new Dimension(1000, 50));
		errorPanel.setOpaque(false);
		errorPanel.add(errorLabel);
		
		this.add(createSpacer(1000, 100));
		this.add(new Title(isLAN ? "Join LAN Game" : "Join Internet Game", 1000, 40));
		this.add(serverList);
		this.add(joinPanel);
		this.add(errorPanel);
		this.add(namePanel);
		this.add(backPanel);
		
		if (isLAN){
			startListenThreads();
		}else
			queryLobbyServer();
	}
	
	private void listenForServers(boolean[] running){
		DatagramSocket socket = null;
		try{
			socket = new DatagramSocket(Main.options.clientBroadcastPort);
			byte[] buffer = new byte[Connection.PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buffer, Connection.PACKET_SIZE);
			BroadcastMsg broadcastListener = new BroadcastMsg(){
				public void confirmed(){
					if (address != null && gameName.length() > 1){
						for (int x = 0; x < serverListModel.getSize(); x++){
							AvailableServer server = serverListModel.getElementAt(x);
							if (server.update(this)){
								serverListModel.setElementAt(server, x);
								return;
							}
						}
						serverListModel.addElement(new AvailableServer(this));
					}
					serverList.repaint();
			}};
			while (running[0]){
				socket.receive(packet);
				if (buffer[0] == broadcastListener.getId())
					broadcastListener.read(buffer);
			}
		}catch(IOException e){
			displayMessage("Unable to listen for server broadcast");
		}
		if (socket != null)
			socket.close();
	}
	
	private void cleanupServerList(boolean[] running){
		while (running[0]){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e){
				e.printStackTrace();
			}
			
			for (int x = 0; x < serverListModel.getSize(); x++){
				AvailableServer server = serverListModel.getElementAt(x);
				if (java.lang.System.currentTimeMillis()-server.updateTime > 3*BroadcastMsg.BROADCAST_INTERVAL){
					serverListModel.remove(x);
					serverList.clearSelection();
				}
			}
		}
	}
	
	private void startListenThreads(){
		if (running != null)
			running[0] = false;
		running = new boolean[]{true};
		new Thread("ServerListenThread"){
			public void run(){
				listenForServers(running);
		}}.start();
		new Thread("CleanupServerListThread"){
			public void run(){
				cleanupServerList(running);
		}}.start();
	}
	
	private void queryLobbyServer(){
		refreshButton.setEnabled(false);
		serverListModel.clear();
		final Connection lobbyServer;
		try{
			Socket lobbyServerSocket = new Socket();
			lobbyServerSocket.setSoTimeout(2000);
			lobbyServerSocket.bind(new InetSocketAddress(Main.options.clientPort));
			lobbyServerSocket.connect(new InetSocketAddress(Main.options.lobbyServer, Main.options.lobbyClientPort), 4000);
			lobbyServer = new Connection(lobbyServerSocket, true);
			lobbyServer.addListener(new BroadcastMsg(){
				public void confirmed(){
					serverListModel.addElement(new AvailableServer(this));
					serverList.repaint();
			}});
		}catch (IOException ex){
			displayMessage("Cannot connect to lobby server");
		}
		
		new Thread("RefreshEnableThread"){
			public void run(){
				try{
					Thread.sleep(5000);
				}catch (InterruptedException ex){}
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						refreshButton.setEnabled(true);
				}});
		}}.start();
	}
	
	public void resume(){
		if (isLAN){
			startListenThreads();
		}else
			queryLobbyServer();
	}
	
	public void suspend(){
		if (running != null)
			running[0] = false;
	}
	
	private void join(){
		try{
			Socket socket = new Socket();
			socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress(Main.options.clientPort));
			
			/*if (Main.UPnPEnabled){
				Main.UPnPHandler.enableUPnP(socket.getLocalPort(), Protocol.TCP);
				if (!Main.UPnPHandler.isSuccessful())
					errorMessage("UPnP configuration failed");
			}*/
			
			socket.connect(new InetSocketAddress(addressField.getText(), Main.options.serverPort), 4000);
			connection = new Connection(socket, Main.options.forceTCP);
			final boolean[] responseReceived = new boolean[]{false};
			
			connection.addSwingListener(new ClientAcceptedMsg(){
				public void confirmed(){
					responseReceived[0] = true;
					errorLabel.setText("");
					errorLabel.setBorder(null);
					Main.addWindow(new SetupWindowNetClient(connection, username));
			}});
			
			connection.addSwingListener(new ClientRejectedMsg(){
				public void confirmed(){
					responseReceived[0] = true;
					displayMessage(message);
					connection.close();
			}});
			
			JoinMsg joinMsg = new JoinMsg();
			joinMsg.name = username;
			joinMsg.dataHash = Main.dataHash;
			joinMsg.version = Main.VERSION;
			joinMsg.playerType = SetupWindow.PlayerType.NET_HOST;
			connection.send(joinMsg);
			
			new Thread("WaitForServerResponseThread"){
				public void run(){
					try{
						Thread.sleep(1500);
					}catch (InterruptedException e){}
					if (!responseReceived[0])
						displayMessage("Host is not responding");
				}
			}.start();
			
		}catch (UnknownHostException e){
			displayMessage("Could not connect to host");
		}catch (IOException e){
			displayMessage(e.getMessage());
		}
	}
	
	private void displayMessage(String message){
		Main.errorSound.play();
		errorLabel.setText(message);
		errorLabel.setBorder(BorderFactory.createEtchedBorder());
		Main.print(message);
	}
	
	private class AvailableServer{
		InetAddress address;
		String gameName;
		long updateTime;
		int numClients;
		
		public AvailableServer(BroadcastMsg msg){
			address = msg.address;
			gameName = msg.gameName;
			numClients = msg.numClients;
			updateTime = java.lang.System.currentTimeMillis();
		}
		
		public boolean update(BroadcastMsg msg){
			if (msg.address.equals(address)){
				updateTime = java.lang.System.currentTimeMillis();
				gameName = msg.gameName;
				numClients = msg.numClients;
				return true;
			}
			return false;
		}
		
		public String getAddressString(){
			return address.getHostAddress();
		}
		
		public String toString(){
			return String.format("%1$-" + Server.MAX_GAME_NAME_LENGTH + "s", gameName)
					+ "       " + numClients + " player(s)"
					+ "       " + address.getHostAddress();
		}
	}
}
