import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Arrays;
import java.io.*;

// Main game lobby window for both single player and multiplayer

public abstract class SetupWindow extends Window{
	enum PlayerType{
		AI('a'), HOST('h'), NET_HOST('n'), NET_CLIENT('n'), NET_AI('a');
		
		final char typeChar;
		PlayerType(char typeChar){
			this.typeChar = typeChar;
		}
	};
	
	Arena arena;
	PlayerPanel selectedPlayer;
	int gameSpeed;
	
	Title titleLabel;
	JList<Ship> shipList;
	JPanel playersPanel;
	JLabel gameSpeedLabel;
	JTextField univBudgetField;
	JPanel gameSpeedPanel;
	JPanel optionPanel;
	JPanel bottomPanel;
	JButton backButton;
	
	public SetupWindow(){
		super(Size.NORMAL);
		
		// Drop down menu for loading and saving ship loadouts
		final JMenu loadMenu = new JMenu();
		loadMenu.setPreferredSize(new Dimension(0, 0));
		loadMenu.addMenuListener(new MenuListener(){
			public void menuSelected(MenuEvent e){
				loadMenu.removeAll();
				String[] savedShips = new File(Main.saveDir + "/ships").list();
				Arrays.sort(savedShips);
				for (final String savedShip : savedShips){
					final String savedShipName = savedShip.substring(0, savedShip.length()-4);
					JMenu subMenu = new JMenu(savedShipName);
					subMenu.setHorizontalTextPosition(SwingConstants.LEFT);
					subMenu.addMouseListener(new MouseAdapter(){
						public void mouseClicked(MouseEvent e){
							loadShip(savedShipName);
					}});
					JMenuItem deleteItem = new JMenuItem("delete");
					deleteItem.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e){
							new File(Main.saveDir + "/ships/" + savedShip).delete();
					}});
					subMenu.add(deleteItem);
					loadMenu.add(subMenu);
				}
			}
			public void menuDeselected(MenuEvent e){}
			public void menuCanceled(MenuEvent e){}
		});
		JMenuBar loadMenuBar = new JMenuBar();
		loadMenuBar.add(loadMenu);
		
		shipList = new JList<Ship>();
		shipList.setPreferredSize(new Dimension(205, 310));
		JButton addButton = new JButton("Add");
		addButton.setPreferredSize(new Dimension(65, 37));
		addButton.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				Vector<BuyType> shipTypes = new Vector<BuyType>();
				for (BuyType type : Main.shipTypes)
					shipTypes.add(type);
				Main.addWindow(new TypeWindow(ShipType.class, shipTypes, SetupWindow.this));
		}});
		JButton copyButton = new JButton("Copy");
		copyButton.setPreferredSize(new Dimension(65, 37));
		copyButton.setMargin(new Insets(1,3,1,1));
		copyButton.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				if (shipList.getSelectedIndex() != -1){
					selectedPlayer.ships.add(shipList.getSelectedValue().copy());
					selectedPlayer.updateBudget();
					updateShipList();
				}
		}});
		JButton loadButton = new JButton("Load");
		loadButton.setPreferredSize(new Dimension(65, 37));
		loadButton.setMargin(new Insets(1,3,1,1));
		loadButton.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				loadMenu.doClick();
		}});
		loadButton.add(loadMenuBar);
		JButton removeButton = new JButton("Remove");
		removeButton.setPreferredSize(new Dimension(65, 37));
		removeButton.setMargin(new Insets(1,2,1,1));
		removeButton.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				if (shipList.getSelectedIndex() != -1){
					selectedPlayer.ships.remove(shipList.getSelectedIndex());
					selectedPlayer.updateBudget();
					updateShipList();
				}
		}});
		JButton outfitButton = new JButton("Outfit");
		outfitButton.setPreferredSize(new Dimension(135, 37));
		outfitButton.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				if (shipList.getSelectedIndex() != -1)
					Main.addWindow(new ShipOutfitWindow(selectedPlayer.ships.get(shipList.getSelectedIndex())));
		}});
		JPanel fleetPanel = new JPanel();
		fleetPanel.setPreferredSize(new Dimension(219, 438));
		fleetPanel.setBorder(BorderFactory.createEtchedBorder());
		JLabel fleetLabel = new JLabel("Player Fleet");
		fleetLabel.setFont(new Font("Courier", Font.PLAIN, 19));
		fleetPanel.add(fleetLabel);
		fleetPanel.add(shipList);
		fleetPanel.add(addButton);
		fleetPanel.add(copyButton);
		fleetPanel.add(loadButton);
		fleetPanel.add(removeButton);
		fleetPanel.add(outfitButton);
		
		univBudgetField = new JTextField();
		univBudgetField.setPreferredSize(new Dimension(50, 20));
		univBudgetField.setInputVerifier(INT_VERIFIER);
		JPanel univBudgetPanel = new JPanel(DEFAULT_LAYOUT);
		univBudgetPanel.setPreferredSize(new Dimension(100, 45));
		univBudgetPanel.add(new JLabel("Universal Budget: "));
		univBudgetPanel.add(univBudgetField);
		
		gameSpeedLabel = new JLabel();
		setGameSpeed(100);
		gameSpeedPanel = new JPanel(DEFAULT_LAYOUT);
		gameSpeedPanel.setPreferredSize(new Dimension(120, 45));
		gameSpeedPanel.add(gameSpeedLabel);
		
		optionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 4));
		optionPanel.setPreferredSize(new Dimension(714, 55));
		optionPanel.setBorder(BorderFactory.createEtchedBorder());
		optionPanel.add(univBudgetPanel);
		optionPanel.add(gameSpeedPanel);
		
		playersPanel = new JPanel();
		playersPanel.setPreferredSize(new Dimension(494, 438));
		playersPanel.setBorder(BorderFactory.createEtchedBorder());
		
		bottomPanel = new JPanel(DEFAULT_LAYOUT);
		bottomPanel.setOpaque(false);
		bottomPanel.setPreferredSize(new Dimension(800, 40));
		backButton = new JButton("Back");
		backButton.setPreferredSize(new Dimension(120, 35));
		backButton.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				Main.removeWindow();
		}});
		bottomPanel.add(backButton);
		
		titleLabel = new Title("Game Setup", 550, 75);
		
		//this.add(createSpacer(1000, 45));
		this.add(titleLabel);
		this.add(playersPanel);
		this.add(fleetPanel);
		this.add(optionPanel);
		this.add(bottomPanel);
	}
	
	public void returnValue(Object type){
		Ship ship = new Ship((ShipType)type);
		selectedPlayer.ships.add(ship);
		selectedPlayer.updateBudget();
		Main.addWindow(new ShipOutfitWindow(ship));
	}
	
	public void resume(){
		updateShipList();
		selectedPlayer.updateBudget();
	}
	
	public void setGameSpeed(int speed){
		gameSpeed = speed;
		gameSpeedLabel.setText("Game Speed: " + speed + "%");
	}
	
	protected void loadShip(String shipName){
		Ship ship = null;
		BufferedReader in = null;
		try{
			File file = new File(Main.saveDir + "/ships/" + shipName + ".txt");
			in = new BufferedReader(new FileReader(file));
			ship = Ship.read(in);
		}catch (IOException ex){}
		try{
			if (in != null)
				in.close();
		}catch (IOException ex){}
		if (ship != null){
			ship.setName(shipName);
			selectedPlayer.ships.add(ship);
		}
		selectedPlayer.updateBudget();
		updateShipList();
	}
	
	protected void updateShipList(){
		shipList.setListData(selectedPlayer.ships);
	}
	
	protected PlayerPanel getPanel(String name){
		for (Object component : playersPanel.getComponents())
			if (((PlayerPanel)component).getName().equals(name))
				return ((PlayerPanel)component);
		return null;
	}
	
	protected boolean readyToStart(){
		for (Object component : playersPanel.getComponents()){
			PlayerPanel panel = (PlayerPanel)component;
			if (panel.getName().replaceAll(" ", "").length() == 0 ||
					panel.getBudget() > panel.getMaxBudget())
				return false;
		}
		return true;
	}
	
	protected void start(final int randomSeed){
		if (readyToStart()){
			clickSound.play();
			Game game = new Game(arena, null, 0.01*gameSpeed);
			for (int x = 0; x < playersPanel.getComponentCount(); x++)
				game.addPlayer(((PlayerPanel)playersPanel.getComponent(x)).getPlayer());
			Main.startGame(game, randomSeed, randomSeed);
		}
	}
	
	// One PlayerPanel is created for each player in the game lobby
	static final Color NEUTRAL_COLOR = new Color(230, 230, 230);
	static final Color IN_GAME_COLOR = new Color(90, 160, 90);
	protected class PlayerPanel extends JPanel{
		
		public final PlayerType playerType;
		public final Vector<Ship> ships;
		
		JTextField nameField;
		JComboBox<String> teamBox;
		JLabel budgetLabel;
		JTextField budgetField;
		
		public PlayerPanel(PlayerType playerType, String name){
			super(new FlowLayout(FlowLayout.LEFT));
			this.setPreferredSize(new Dimension(480, 38));
			this.setBorder(BorderFactory.createEtchedBorder());
			
			ships = new Vector<Ship>();
			this.playerType = playerType;
			
			if (playerType == PlayerType.NET_AI)
				this.add(new JLabel("[AI]   "));
			
			nameField = new JTextField();
			nameField.setPreferredSize(new Dimension(136, 25));
			nameField.setEditable(false);
			nameField.setText(name);
			
			budgetLabel = new JLabel("0");
			budgetLabel.setPreferredSize(new Dimension(30, 25));
			budgetLabel.setHorizontalAlignment(JLabel.RIGHT);
			
			JLabel budgetSpacer = new JLabel("");
			budgetSpacer.setPreferredSize(new Dimension(9, 25));
			
			budgetField = new JTextField(univBudgetField.getText());
			budgetField.setPreferredSize(new Dimension(40, 25));
			budgetField.setEditable(false);
			budgetField.setInputVerifier(INT_VERIFIER);
			budgetField.setBackground(Window.PANEL_VERY_LIGHT);
			
			JLabel teamLabel = new JLabel("   Team:");
			teamBox = new JComboBox<String>();
			teamBox.setPreferredSize(new Dimension(38, 20));
			teamBox.setBackground(Window.PANEL_VERY_LIGHT);
			teamBox.setEnabled(false);
			if (arena != null)
				setTeams();
			
			this.add(nameField);
			this.add(new JLabel("   Budget:  "));
			if (playerType != PlayerType.NET_HOST && playerType != PlayerType.NET_AI){
				this.add(budgetLabel);
				budgetSpacer.setText(" /");
			}else
				budgetSpacer.setPreferredSize(new Dimension(44, 25));
			this.add(budgetSpacer);
			this.add(budgetField);
			this.add(teamLabel);
			this.add(teamBox);
			this.addMouseListener(new MouseAdapter(){
				public void mousePressed(MouseEvent e){
					if (isEditable())
						select();
			}});
			
			//setBackground(isEditable() ? NEUTRAL_COLOR : playersPanel.getBackground());
			setBackground(NEUTRAL_COLOR);
			
			if (playerType == PlayerType.HOST || playerType == PlayerType.NET_CLIENT)
				select();
			
			this.validate();
		}
		
		public void updateBudget(){
			int cost = 0;
			for (Ship ship : ships)
				cost += ship.getCost();
			budgetLabel.setText(String.valueOf(cost));
			if (cost > Integer.parseInt(budgetField.getText())){
				budgetLabel.setForeground(Color.RED);
			}else
				budgetLabel.setForeground(Color.DARK_GRAY);
		}
		
		public boolean isEditable(){
			PlayerType type = PlayerPanel.this.playerType;
			return type == PlayerType.HOST || type == PlayerType.AI || type == PlayerType.NET_CLIENT;
		}
		
		public void autoLoadout(){
			/*for (int x = 0; x < 50; x++){
				ships.clear();
				updateBudget();
				
				for (int y = 0; y < 200; y++){
					Ship ship = new Ship(Main.shipTypes[(int)(random()*Main.shipTypes.length)]);
					ship.autoLoadout();
					if (getBudget()+ship.getCost() <= getMaxBudget()){
						ships.add(ship);
						updateBudget();
					}
				}
				if (getBudget() > getMaxBudget()-50)
					break;
			}*/
			Player.makeFleet(ships, getMaxBudget(), 0);
			updateShipList();
			updateBudget();
		}
		
		public void setToUnivBudget(){
			budgetField.setText(univBudgetField.getText());
			updateBudget();
		}
		
		public void setTeams(){
			for (int x = teamBox.getItemCount()-1; x >= arena.teamPos.length; x--)
				teamBox.removeItemAt(x);
			for (int x = teamBox.getItemCount(); x < arena.teamPos.length; x++)
				teamBox.addItem(String.valueOf(x+1));
		}
		
		public int getBudget(){
			return Integer.parseInt(budgetLabel.getText());
		}
		
		public int getMaxBudget(){
			return Integer.parseInt(budgetField.getText());
		}
		
		public int getTeam(){
			return Integer.parseInt((String)teamBox.getSelectedItem());
		}
		
		public String getName(){
			return nameField.getText();
		}
		
		public Vector<Ship> getCopiedShips(){
			Vector<Ship> cloned = new Vector<Ship>(ships.size());
			for (Ship ship : ships)
				cloned.add(ship.copy());
			return cloned;
		}
		
		public Player getPlayer(){
			return new Player(getName(), getTeam(), getCopiedShips(), getMaxBudget(), arena);
		}
		
		public void select(){
			if (selectedPlayer != null)
				selectedPlayer.setBorder(Window.UNSELECTED_BORDER);
			PlayerPanel.this.setBorder(Window.SELECTED_BORDER);
			selectedPlayer = PlayerPanel.this;
			updateShipList();
		}
	}
	
}
