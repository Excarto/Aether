import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Arrays;
import java.io.*;

public class MissionFleetWindow extends Window{
	
	final JList<Ship> shipList;
	final Vector<Ship> ships;
	final JLabel budgetLabel;
	final Mission mission;
	int fleetCost;
	
	public MissionFleetWindow(Mission mission){
		super(Size.SMALL);
		this.mission = mission;
		this.ships = Mission.playerFleet;
		
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
		
		budgetLabel = new JLabel();
		budgetLabel.setPreferredSize(new Dimension(130, 19));
		
		JLabel maxBudgetLabel = new JLabel("Mission Budget: " + mission.getPlayerBudget());
		maxBudgetLabel.setPreferredSize(new Dimension(130, 19));
		
		shipList = new JList<Ship>();
		shipList.setPreferredSize(new Dimension(210, 280));
		JButton addButton = new JButton("Add");
		addButton.setPreferredSize(new Dimension(65, 37));
		addButton.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				Vector<BuyType> shipTypes = new Vector<BuyType>();
				for (BuyType type : Main.shipTypes)
					shipTypes.add(type);
				Main.addWindow(new TypeWindow(ShipType.class, shipTypes, MissionFleetWindow.this));
		}});
		
		JButton copyButton = new JButton("Copy");
		copyButton.setPreferredSize(new Dimension(65, 37));
		copyButton.setMargin(new Insets(1,3,1,1));
		copyButton.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				if (shipList.getSelectedIndex() != -1){
					ships.add(shipList.getSelectedValue().copy());
					updateFleet();
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
					ships.remove(shipList.getSelectedIndex());
					updateFleet();
				}
		}});
		
		JButton outfitButton = new JButton("Outfit");
		outfitButton.setPreferredSize(new Dimension(135, 37));
		outfitButton.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				if (shipList.getSelectedIndex() != -1)
					Main.addWindow(new ShipOutfitWindow(ships.get(shipList.getSelectedIndex())));
		}});
		
		JPanel controlsPanel = new JPanel();
		controlsPanel.setPreferredSize(new Dimension(160, shipList.getPreferredSize().height));
		controlsPanel.setBorder(BorderFactory.createEtchedBorder());
		controlsPanel.add(createSpacer(100, 25));
		controlsPanel.add(addButton);
		controlsPanel.add(copyButton);
		controlsPanel.add(loadButton);
		controlsPanel.add(removeButton);
		controlsPanel.add(outfitButton);
		controlsPanel.add(createSpacer(100, 25));
		controlsPanel.add(maxBudgetLabel);
		controlsPanel.add(budgetLabel);
		
		JButton backButton = new JButton("Abort Mission");
		backButton.setPreferredSize(new Dimension(115, 26));
		backButton.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				Main.removeWindow();
				Main.removeWindow();
		}});
		
		JButton arenaButton = new JButton("View Sector");
		arenaButton.setPreferredSize(new Dimension(115, 26));
		arenaButton.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				Main.addWindow(new MissionArenaWindow(mission));
		}});
		
		JButton startButton = new JButton("Start");
		startButton.setPreferredSize(new Dimension(115, 26));
		startButton.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				if (fleetCost <= mission.getPlayerBudget() && ships.size() > 0){
					clickSound.play();
					Main.removeWindow();
					mission.launchGame();
				}
		}});
		
		JPanel mainPanel = new JPanel();
		mainPanel.setOpaque(false);
		mainPanel.setPreferredSize(new Dimension(500, shipList.getPreferredSize().height+4));
		mainPanel.add(controlsPanel);
		mainPanel.add(shipList);
		
		JPanel bottomPanel = new JPanel(DEFAULT_LAYOUT);
		bottomPanel.setOpaque(false);
		bottomPanel.setPreferredSize(new Dimension(900, 40));
		bottomPanel.add(backButton);
		bottomPanel.add(arenaButton);
		bottomPanel.add(startButton);
		
		this.add(new Title("Fleet Construction", 900, 32));
		this.add(mainPanel);
		this.add(createSpacer(900, 6));
		this.add(bottomPanel);
		
		updateFleet();
	}
	
	public void returnValue(Object type){
		Ship ship = new Ship((ShipType)type);
		ships.add(ship);
		updateFleet();
		Main.addWindow(new ShipOutfitWindow(ship));
	}
	
	public void resume(){
		updateFleet();
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
			ships.add(ship);
		}
		updateFleet();
	}
	
	protected void updateFleet(){
		shipList.setListData(ships);
		
		fleetCost = 0;
		for (int x = 0; x < ships.size(); x++)
			fleetCost += ships.get(x).getCost();
		budgetLabel.setText("Fleet cost: " + fleetCost);
		budgetLabel.setForeground(fleetCost > mission.getPlayerBudget() ? Color.RED : Color.BLACK);
	}
	
}
