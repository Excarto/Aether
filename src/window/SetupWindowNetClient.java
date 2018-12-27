import static java.lang.Math.*;
import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.*;

public class SetupWindowNetClient extends SetupWindow{
	
	Connection connection;
	boolean disableTeamListener;
	boolean ready;
	String username;
	ChatPanel chatPanel;
	
	JButton readyButton;
	JLabel arenaLabel;
	
	public SetupWindowNetClient(Connection conn, String username){
		this.connection = conn;
		this.username = username;
		disableTeamListener = true;
		titleLabel.setPreferredSize(new Dimension(550, 28));
		
		final ClientPlayerPanel panel = new ClientPlayerPanel(username);
		panel.teamBox.setEnabled(true);
		panel.teamBox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if (!disableTeamListener){
					SetTeamMsg teamMsg = new SetTeamMsg();
					teamMsg.name = SetupWindowNetClient.this.username;
					teamMsg.team = panel.getTeam();
					connection.send(teamMsg);
				}
		}});
		playersPanel.add(panel);
		
		connection.addListener(new JoinMsg(){
			public void confirmed(){
				playerType = playerType == PlayerType.AI ? PlayerType.NET_AI : PlayerType.NET_HOST;
				PlayerPanel panel = new PlayerPanel(playerType, name);
				if (inGame)
					panel.setBackground(IN_GAME_COLOR);
				playersPanel.add(panel);
				playersPanel.revalidate();
				playersPanel.repaint();
				if (ready)
					setReady(false);
				displayMessage(name+" has joined the game", 0);
		}});
		
		connection.addListener(new SetTeamMsg(){
			public void confirmed(){
				PlayerPanel panel = getPanel(name);
				if (panel != null){
					disableTeamListener = true;
					panel.teamBox.setSelectedIndex(team-1);
					panel.teamBox.repaint();
					disableTeamListener = false;
				}
				if (ready)
					setReady(false);
		}});
		
		connection.addListener(new SetBudgetMsg(){
			public void confirmed(){
				getPanel(name).budgetField.setText(String.valueOf(budget));
				getPanel(name).updateBudget();
		}});
		
		connection.addListener(new LeaveMsg(){
			public void confirmed(){
				PlayerPanel panel = getPanel(name);
				if (panel != null){
					playersPanel.remove(panel);
					playersPanel.revalidate();
					playersPanel.repaint();
				}
				if (ready)
					setReady(false);
				displayMessage(name+" has left the game", 0);
		}});
		
		connection.addListener(new ReturnToLobbyMsg(){
			public void confirmed(){
				PlayerPanel panel = getPanel(name);
				if (panel != null)
					panel.setBackground(playersPanel.getBackground());
				displayMessage(name+" has returned to game lobby", 0);
		}});
		
		connection.addListener(new GameSettingsMsg(){
			public void confirmed(){
				setGameSpeed((int)round(gameSpeed*100));
				arena = Main.arenas[arenaIndex];
				arenaLabel.setText(arena.arenaName);
				for (Object component : playersPanel.getComponents())
					((PlayerPanel)component).setTeams();
				univBudgetField.setText(String.valueOf(budget));
				if (ready)
					setReady(false);
		}});
		connection.send(new GameSettingsMsg());
		
		connection.addListener(new GameStartMsg(){
			public void confirmed(){
				start(randomSeed);
				
				for (int x = 0; x < playersPanel.getComponentCount(); x++){
					PlayerPanel playerPanel = (PlayerPanel)playersPanel.getComponent(x);
					if (playerPanel.playerType == PlayerType.NET_HOST)
						playerPanel.setBackground(IN_GAME_COLOR);
				}
		}});
		
		connection.addListener(new UnitDescriptionMsg(){
			public void confirmed(){
				for (Object panel : playersPanel.getComponents()){
					if (((PlayerPanel)panel).getName().equals(player)){
						List<Ship> ships = ((PlayerPanel)panel).ships;
						if (unit instanceof Ship){
							ships.add((Ship)unit);
						}else{
							Ship ship = ships.get(ships.size()-1);
							ship.crafts.add(ship.crafts.size(), (Craft)unit);
						}
					}
				}
		}});
		
		connection.addListener(new TextMsg(){
			public void confirmed(){
				displayMessage(message, team);
		}});
		
		connection.setCloseListener(connection.new CloseListener(){
			public void closed(){
				while (!(Main.getCurrentWindow() instanceof ClientJoinWindow))
					Main.removeWindow();
		}});
		
		chatPanel = new ChatPanel(connection);
		this.add(chatPanel, this.getComponentCount()-1);
		
		univBudgetField.setEditable(false);
		arenaLabel = new JLabel("", JLabel.CENTER);
		arenaLabel.setPreferredSize(new Dimension(125, 18));
		JPanel arenaPanel = new JPanel(DEFAULT_LAYOUT);
		arenaPanel.setPreferredSize(new Dimension(130, 40));
		arenaPanel.add(new JLabel("Arena:"));
		arenaPanel.add(arenaLabel);
		optionPanel.add(arenaPanel);
		
		readyButton = new JButton("Ready");
		readyButton.setPreferredSize(bottomPanel.getComponent(0).getPreferredSize());
		readyButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				setReady(!ready);
		}});
		bottomPanel.add(readyButton);
		
		backButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				connection.setCloseListener(null);
				connection.close();
		}});
	}
	
	private void setReady(boolean ready){
		if (ready && !LoadWindow.isLoaded()){
			chatPanel.print("Cannot ready - loading in progress");
		}else{
			readyButton.setText(ready ? "Un-ready" : "Ready");
			if (ready){
				UnitDescriptionMsg unitMsg = new UnitDescriptionMsg();
				unitMsg.player = Main.username;
				for (Ship ship : selectedPlayer.ships){
					unitMsg.unit = ship;
					connection.send(unitMsg);
					for (Craft craft : ship.crafts){
						unitMsg.unit = craft;
						connection.send(unitMsg);
					}
				}
			}
			
			if (this.ready != ready){
				SetReadyMsg readyMsg = new SetReadyMsg();
				readyMsg.ready = ready;
				connection.send(readyMsg);
			}
			this.ready = ready;
		}
	}
	
	private void displayMessage(String message, int team){
		Window window = Main.getCurrentWindow();
		if (window instanceof GameWindow){
			((GameWindow)window).receiveChat(message, team);
		}else
			chatPanel.print(message);
	}
	
	public void suspend(){
		setReady(false);
		/*if (Main.game == null){
			connection.setCloseListener(null);
			connection.close();
		}*/
	}
	
	public void resume(){
		super.resume();
		connection.send(new ReturnToLobbyMsg());
		
		for (int x = 0; x < playersPanel.getComponentCount(); x++){
			PlayerPanel playerPanel = (PlayerPanel)playersPanel.getComponent(x);
			if (playerPanel.playerType != PlayerType.NET_CLIENT)
				playerPanel.ships.clear();
		}
	}
	
	protected class ClientPlayerPanel extends PlayerPanel{
		
		public ClientPlayerPanel(String name){
			super(PlayerType.NET_CLIENT, name);
		}
		
		public Player getPlayer(){
			return new NetPlayerClient(getName(), getTeam(), ships, getMaxBudget(), arena, connection);
		}
		
		public void updateBudget(){
			super.updateBudget();
			if (ready)
				setReady(false);
			readyButton.setEnabled(readyToStart());
		}
	}
}
