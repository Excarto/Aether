import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class SetupWindowNetHost extends SetupWindowHost{
	
	Connection connection;
	boolean disableTeamListener;
	ChatPanel chatPanel;
	
	public SetupWindowNetHost(boolean isLAN){
		titleLabel.setPreferredSize(new Dimension(550, 40));
		Main.server = new Server(isLAN);
		setGameSettings();
		connection = Main.server.start(Main.options.username, selectedPlayer.ships);
		
		connection.addSwingListener(new JoinMsg(){ 
			public void confirmed(){
				NetPlayerPanel panel = new NetPlayerPanel(playerType, name);
				Main.server.setShips(name, panel.ships);
				if (inGame)
					panel.setBackground(IN_GAME_COLOR);
				playersPanel.add(panel);
				playersPanel.revalidate();
				playersPanel.repaint();
				displayMessage(name+" has joined the game", 0);
		}});
		
		connection.addSwingListener(new SetTeamMsg(){
			public void confirmed(){
				PlayerPanel panel = getPanel(name);
				if (panel != null){
					disableTeamListener = true;
					panel.teamBox.setSelectedIndex(team-1);
					panel.teamBox.repaint();
					disableTeamListener = false;
				}
		}});
		
		connection.addSwingListener(new LeaveMsg(){
			public void confirmed(){
				PlayerPanel panel = getPanel(name);
				if (panel != null){
					playersPanel.remove(panel);
					playersPanel.revalidate();
					playersPanel.repaint();
				}
				displayMessage(name+" has left the game", 0);
		}});
		
		connection.addSwingListener(new ReturnToLobbyMsg(){
			public void confirmed(){
				PlayerPanel panel = getPanel(name);
				if (panel != null)
					panel.setBackground(playersPanel.getBackground());
				displayMessage(name+" has returned to game lobby", 0);
		}});
		
		connection.addSwingListener(new TextMsg(){
			public void confirmed(){
				displayMessage(message, team);
		}});
		
		chatPanel = new ChatPanel(connection);
		this.add(chatPanel, this.getComponentCount()-1);
		
		gameSpeedSlider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				setGameSettings();
		}});
			
		addCompButton.removeActionListener(addCompButton.getActionListeners()[0]);
		addCompButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Main.server.addAI();
		}});
		
		backButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Main.server.close();
				connection.close();
				Main.server = null;
		}});
		
		final JTextField nameField = new JTextField(Server.DEFAULT_GAME_NAME);
		nameField.setPreferredSize(new Dimension(140, 20));
		nameField.getDocument().addDocumentListener(new DocumentListener(){
			public void changedUpdate(DocumentEvent e){
				Main.server.setGameName(nameField.getText());
			}
			public void insertUpdate(DocumentEvent e){
				Main.server.setGameName(nameField.getText());
			}
			public void removeUpdate(DocumentEvent e){
				Main.server.setGameName(nameField.getText());
			}
		});
		
		JPanel gameNamePanel = new JPanel(DEFAULT_LAYOUT);
		gameNamePanel.setPreferredSize(new Dimension(142, 45));
		gameNamePanel.add(new JLabel("Game Name:"));
		gameNamePanel.add(nameField);
		optionPanel.add(gameNamePanel);
		optionPanel.revalidate();
		
		if (!isLAN && Main.options.UPnPEnabled){
			if (Main.UPnPHandler.isSuccessful()){
				chatPanel.print("UPnP enabled");
				chatPanel.print("Your external IP address is " + Main.UPnPHandler.getExternalIP());
			}else
				chatPanel.print("UPnP configuration failed");
		}
	}
	
	protected HostPlayerPanel getHostPanel(){
		return new NetPlayerPanel(PlayerType.HOST, Main.options.username);
	}
	
	protected void addDefaultComps(){}
	
	protected void updateUnivBudget(){
		super.updateUnivBudget();
		setGameSettings();
	}
	
	private void setGameSettings(){
		int budget;
		try{
			budget = Integer.parseInt(univBudgetField.getText());
		}catch(NumberFormatException e){
			return;
		}
		Main.server.setGameSettings(arena, gameSpeedSlider.getValue()/100.0, budget);
	}
	
	private void displayMessage(String message, int team){
		Window window = Main.getCurrentWindow();
		if (window instanceof GameWindow){
			((GameWindow)window).receiveChat(message, team);
		}else
			chatPanel.print(message);
	}
	
	public void returnValue(Object type){
		super.returnValue(type);
		if (type instanceof Arena)
			setGameSettings();
	}
	
	protected void start(int randomSeed){
		if (readyToStart() && Main.server.checkReady()){
			super.start(randomSeed);
			Main.server.startGame(randomSeed);
			for (int x = 0; x < playersPanel.getComponentCount(); x++){
				PlayerPanel playerPanel = (PlayerPanel)playersPanel.getComponent(x);
				if (playerPanel.playerType == PlayerType.NET_HOST)
					playerPanel.setBackground(IN_GAME_COLOR);
			}
		}
	}
	
	public void resume(){
		super.resume();
		connection.send(new ReturnToLobbyMsg());
	}
	
	protected class NetPlayerPanel extends HostPlayerPanel{
		
		public NetPlayerPanel(PlayerType playerType, String name){
			super(playerType, name);
			
			teamBox.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (!disableTeamListener)
						Main.server.setTeam(getName(), getTeam());
			}});
		}
		
		public void updateBudget(){
			super.updateBudget();
			Main.server.setBudget(getName(), Integer.parseInt(budgetField.getText()));
		}
		
		public Player getPlayer(){
			if (playerType == PlayerType.NET_HOST){
				return new NetPlayerHost(getName(), getTeam(), getCopiedShips(), getMaxBudget(), arena, Main.server.getConnection(getName()));
			}else if (playerType == PlayerType.HOST){
				return new HostPlayer(getName(), getTeam(), getCopiedShips(), getMaxBudget(), arena, connection);
			}else if (playerType == PlayerType.AI){
				return new NetComputerPlayer(getName(), getTeam(), getCopiedShips(), getMaxBudget(), arena);
			}else
				return super.getPlayer();
		}
		
		protected void kickPlayer(){
			super.kickPlayer();
			
			Main.server.kick(getName());
		}
	}
}
