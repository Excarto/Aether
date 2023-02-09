import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

// Game lobby window used for both single player and for the host of multiplayer games

public class SetupWindowHost extends SetupWindow{
	
	static final Random random = new Random();
	
	int currentName;
	
	JSlider gameSpeedSlider;
	JButton addCompButton;
	
	public SetupWindowHost(){
		for (Arena arena : Main.arenas){
			if (arena.isDefault)
				this.arena = arena;
		}
		univBudgetField.setText(String.valueOf(arena.defaultBudget));
		
		currentName = 0;
		
		univBudgetField.getDocument().addDocumentListener(new DocumentListener(){
			public void changedUpdate(DocumentEvent e){
				updateUnivBudget();
			}
			public void insertUpdate(DocumentEvent e){
				updateUnivBudget();
			}
			public void removeUpdate(DocumentEvent e){
				updateUnivBudget();
			}
		});
		
		gameSpeedSlider = new JSlider(20, 200, 100);
		gameSpeedSlider.setPreferredSize(new Dimension(110, 20));
		gameSpeedSlider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				setGameSpeed(gameSpeedSlider.getValue());
		}});
		gameSpeedPanel.add(gameSpeedSlider);
		
		addCompButton = new JButton("Add AI");
		addCompButton.setPreferredSize(new Dimension(75, 30));
		addCompButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				addComp();
		}});
		JButton map = new JButton("Select Map");
		map.setPreferredSize(new Dimension(100, 30));
		map.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				Main.addWindow(new ArenaWindow(SetupWindowHost.this));
		}});
		optionPanel.add(addCompButton, 0);
		optionPanel.add(map, 1);
		
		JButton start = new JButton("Start");
		start.setPreferredSize(new Dimension(120, 35));
		start.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				start(random.nextInt());
				//start(-1064939365);
		}});
		bottomPanel.add(start);
		
		playersPanel.add(getHostPanel());
		addDefaultComps();
	}
	
	protected HostPlayerPanel getHostPanel(){
		return new HostPlayerPanel(PlayerType.HOST, Main.options.username);
	}
	
	protected void addDefaultComps(){
		addComp();
	}
	
	protected void updateUnivBudget(){
		if (INT_VERIFIER.verify(univBudgetField)){
			for (Object component : playersPanel.getComponents())
				((PlayerPanel)component).setToUnivBudget();
		}
	}
	
	public void returnValue(Object type){
		if (type != null && type instanceof Arena){
			arena = (Arena)type;
			//maxPlayers = min(Arena.MAX_PLAYERS, arena.teamPositions.length);
			univBudgetField.setText(String.valueOf(arena.defaultBudget));
			for (Object component : playersPanel.getComponents())
				((PlayerPanel)component).setTeams();
		}else
			super.returnValue(type);
		
		updateShipList();
		selectedPlayer.updateBudget();
	}
	
	public void addComp(){
		if (playersPanel.getComponentCount() < Arena.MAX_PLAYERS){
			PlayerPanel player = new HostPlayerPanel(PlayerType.AI,
					Main.names[currentName++%Main.names.length]);
			playersPanel.add(player);
			player.revalidate();
		}
	}
	
	protected class HostPlayerPanel extends PlayerPanel{
		public HostPlayerPanel(PlayerType playerType, String name) {
			super(playerType, name);
			
			teamBox.setEnabled(true);
			autoSetTeam();
			
			budgetField.setEditable(true);
			budgetField.setInputVerifier(INT_VERIFIER);
			budgetField.getDocument().addDocumentListener(new DocumentListener(){
				public void changedUpdate(DocumentEvent e){
					updateAIBudget();
				}
				public void insertUpdate(DocumentEvent e){
					updateAIBudget();
				}
				public void removeUpdate(DocumentEvent e){
					updateAIBudget();
				}
				private void updateAIBudget(){
					if (INT_VERIFIER.verify(budgetField)){
						if (HostPlayerPanel.this.playerType == PlayerType.AI)
							autoLoadout();
						updateBudget();
					}
				}
			});
			
			if (playerType != PlayerType.HOST){
				JButton kick = new JButton("Kick");
				kick.setPreferredSize(new Dimension(60, 25));
				kick.addActionListener(new ActionListener(){
					public void actionPerformed (ActionEvent e){
						kickPlayer();
				}});
				this.add(new JLabel("   "));
				this.add(kick);
			}
			
			if (playerType == PlayerType.AI){
				setBackground(NEUTRAL_COLOR);
				autoLoadout();
			}
		}
		
		public void autoSetTeam(){
			int lowestTeam = 1, lowestNumPlayers = Integer.MAX_VALUE;
			for (int x = 0; x < arena.teamPos.length; x++){
				int numPlayers = 0;
				for (int y = 0; y < playersPanel.getComponentCount(); y++)
					if (playersPanel.getComponent(y) instanceof PlayerPanel &&
							((PlayerPanel)playersPanel.getComponent(y)).getTeam() == x+1)
						numPlayers++;
				if (numPlayers < lowestNumPlayers){
					lowestNumPlayers = numPlayers;
					lowestTeam = x+1;
				}
			}
			teamBox.setSelectedIndex(lowestTeam-1);
		}
		
		public Player getPlayer(){
			if (playerType == PlayerType.AI){
				return new ComputerPlayer(getName(), getTeam(), getCopiedShips(), getMaxBudget(), arena);
			}else if (playerType == PlayerType.HOST){
				return new HumanPlayer(getName(), getTeam(), getCopiedShips(), getMaxBudget(), arena);
			}else
				return super.getPlayer();
		}
		
		protected void kickPlayer(){
			playersPanel.remove(HostPlayerPanel.this);
			if (selectedPlayer == HostPlayerPanel.this)
				((PlayerPanel)playersPanel.getComponent(0)).select();
			playersPanel.doLayout();
			playersPanel.repaint();
		}
	}
	
}
