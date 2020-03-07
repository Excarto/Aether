import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class CraftPanel extends SidePanel{
	
	Ship ship;
	
	final JPanel bayPanel;
	//final JCheckBox catapult;
	final JCheckBox defaultAmmo, defaultRepair, defaultRecharge;
	
	public CraftPanel(){
		super();
		this.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH, Unit.CONTROLS_HEIGHT));
		
		bayPanel = new JPanel(Window.DEFAULT_LAYOUT){
			public void paintComponent(Graphics g){
				g.drawImage(background, 0, 0, null);
			}
		};
		bayPanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-24, 535));
		JScrollPane launchScrollPane = new JScrollPane(bayPanel);
		launchScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		defaultAmmo = new JCheckBox("Auto Rearm");
		defaultAmmo.setPreferredSize(new Dimension(200, 14));
		defaultAmmo.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				ship.craftDefaultAmmo = defaultAmmo.isSelected();
		}});
		defaultRepair = new JCheckBox("Auto Repair");
		defaultRepair.setPreferredSize(new Dimension(200, 14));
		defaultRepair.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				ship.craftDefaultRepair = defaultRepair.isSelected();
		}});
		defaultRecharge = new JCheckBox("Auto Recharge");
		defaultRecharge.setPreferredSize(new Dimension(200, 14));
		defaultRecharge.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				ship.craftDefaultRecharge = defaultRecharge.isSelected();
		}});
		
		JPanel optionsPanel = new JPanel();
		optionsPanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-4, 100));
		optionsPanel.add(defaultRepair);
		optionsPanel.add(defaultRecharge);
		optionsPanel.add(defaultAmmo);
		
		this.add(launchScrollPane);
		this.add(optionsPanel);
	}
	
	public void setShip(Ship ship){
		this.ship = ship;
		
		defaultAmmo.setSelected(ship.craftDefaultAmmo);
		defaultRepair.setSelected(ship.craftDefaultRepair);
		defaultRecharge.setSelected(ship.craftDefaultRecharge);
		
		bayPanel.removeAll();
	}
	
	public void refresh(){
		if (ship != null){
			
			boolean modified = false;
			for (int x = 0; x < bayPanel.getComponentCount(); x++){
				if (!ship.crafts.contains(((StatusPanel)bayPanel.getComponent(x)).target.unit)){
					bayPanel.remove(x);
					x--;
					modified = true;
				}
			}
			for (Craft craft : ship.crafts){
				boolean exists = false;
				for (int x = 0; x < bayPanel.getComponentCount(); x++)
					exists = exists || ((StatusPanel)bayPanel.getComponent(x)).target.unit == craft;
				if (!exists){
					bayPanel.add(new CraftStatusPanel(ship.getRepairTarget(craft)));
					modified = true;
				}
			}
			if (modified){
				bayPanel.revalidate();
				bayPanel.repaint();
			}
			
			for (int x = 0; x < bayPanel.getComponentCount(); x++)
				((StatusPanel)bayPanel.getComponent(x)).update();
		}
	}
	
	private static final Color LAUNCH_COLOR = Color.BLACK;
	private static final Color CATAPULT_COLOR = new Color(40, 150, 40);
	private static final Color CANCEL_COLOR = new Color(150, 40, 40);
	//public static Font launchButtonFont;
	private class CraftStatusPanel extends StatusPanel{
		
		private final JButton launchButton;
		
		public CraftStatusPanel(RepairTarget repairTarget){
			super(repairTarget);
			
			launchButton = new JButton();
			launchButton.setPreferredSize(new Dimension(60, 20));
			//launchButton.setFont(launchButtonFont);
			launchButton.setMargin(new Insets(1, 1, 1, 1));
			launchButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					//ship.player.launchCraft(ship, (Craft)target.unit);
					if (launchButton.getText().equals("Launch")){
						ship.setQueueStatus((Craft)target.unit, Ship.QueueStatus.QUEUED);
					}else if (launchButton.getText().equals("Catapult")){
						ship.setQueueStatus((Craft)target.unit, Ship.QueueStatus.CATAPULT);
					}else if (launchButton.getText().equals("Cancel"))
						ship.setQueueStatus((Craft)target.unit, Ship.QueueStatus.UNQUEUED);
			}});
			
			this.add(launchButton);
		}
		
		public void update(){
			super.update();
			Ship.QueueStatus status = ship.getQueueStatus((Craft)target.unit);
			if (status == Ship.QueueStatus.UNQUEUED){
				launchButton.setText("Launch");
				launchButton.setForeground(LAUNCH_COLOR);
			}else if (status == Ship.QueueStatus.QUEUED){
				if (ship.type.craftLaunchSpeed > 0){
					launchButton.setText("Catapult");
					launchButton.setForeground(CATAPULT_COLOR);
				}else{
					launchButton.setText("Cancel");
					launchButton.setForeground(CANCEL_COLOR);
				}
			}else if (status == Ship.QueueStatus.CATAPULT){
				launchButton.setText("Cancel");
				launchButton.setForeground(CANCEL_COLOR);
			}
		}
	}
}
