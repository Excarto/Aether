import static java.lang.Math.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

// Side panel used for repairing a ship's own hull, its components, its fighters, or
// the hull and components of another unit if the Repair order is being used.

public class RepairPanel extends SidePanel{
	public static Font autoFont;
	
	Ship ship;
	int material;
	RepairTarget target;
	
	JLabel materialLabel;
	JPanel targetPanel;
	RepairQueue repairQueue;
	RepairUnitPanel unitPanel;
	SelectedPanel selectedPanel;
	JSlider autoSlider;
	
	public RepairPanel(){
		super();
		autoFont = Main.getDefaultFont(8);
		this.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH, Unit.CONTROLS_HEIGHT));
		
		materialLabel = new JLabel();
		materialLabel.setFont(Main.getDefaultFont(13));
		
		targetPanel = new JPanel(Window.DEFAULT_LAYOUT);
		JScrollPane targetScrollPane = new JScrollPane(targetPanel);
		targetScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		targetScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		targetScrollPane.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-5, 3*StatusPanel.HEIGHT+7));
		
		repairQueue = new RepairQueue();
		unitPanel = new RepairUnitPanel();
		selectedPanel = new SelectedPanel();
		
		final JLabel autoLabel = new JLabel();
		autoLabel.setPreferredSize(new Dimension(140, 20));
		autoSlider = new JSlider(0, 100, 0);
		autoSlider.setPreferredSize(new Dimension(105, 20));
		autoSlider.setOpaque(false);
		autoSlider.setMajorTickSpacing(10);
		autoSlider.setSnapToTicks(true);
		autoSlider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent event){
				ship.autoRepair = autoSlider.getValue();
				autoLabel.setText("WEP/SYS Repair: " + ship.autoRepair + "%");
			}
		});
		JPanel autoPanel = new JPanel(Window.DEFAULT_LAYOUT);
		autoPanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-20, 26));
		autoPanel.setOpaque(false);
		autoPanel.add(autoLabel);
		autoPanel.add(autoSlider);
		
		JPanel repairablePanel = new JPanel(Window.DEFAULT_LAYOUT){
			public void paintComponent(Graphics g){
				g.drawImage(background, 0, 0, null);
			}
		};
		repairablePanel.setBorder(BorderFactory.createEtchedBorder());
		repairablePanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-5, 368));
		repairablePanel.add(autoPanel);
		repairablePanel.add(unitPanel);
		repairablePanel.add(selectedPanel);
		
		this.add(materialLabel);
		this.add(targetScrollPane);
		this.add(repairablePanel);
		this.add(repairQueue);
	}
	
	// Update the list of RepairStatusPanels corresponding to the unit's current repairTargets.
	// Only revalidate the gui if necessary, for performance.
	public void refresh(){
		if (ship != null){
			boolean modified = false;
			for (int x = 0; x < targetPanel.getComponentCount(); x++){
				if (!ship.repairTargets.contains(((StatusPanel)targetPanel.getComponent(x)).target)){
					targetPanel.remove(x);
					x--;
					modified = true;
				}
			}
			for (RepairTarget target : ship.repairTargets){
				boolean exists = false;
				for (int x = 0; x < targetPanel.getComponentCount(); x++)
					exists = exists || ((StatusPanel)targetPanel.getComponent(x)).target == target;
				if (!exists){
					RepairStatusPanel panel = new RepairStatusPanel(target);
					panel.setSelected(this.target == target);
					targetPanel.add(panel);
					modified = true;
				}
			}
			if (modified){
				targetPanel.setPreferredSize(new Dimension(
						GameWindow.MENU_WIDTH-22, (1+StatusPanel.HEIGHT)*ship.repairTargets.size()));
				targetPanel.revalidate();
				targetPanel.repaint();
			}
			
			/*if (targetPanel.getComponentCount() != ship.repairTargets.size()){
				targetPanel.removeAll();
				targetPanel.setPreferredSize(new Dimension(
						GameWindow.MENU_WIDTH-22, (1+StatusPanel.HEIGHT)*ship.repairTargets.size()));
				
				for (RepairTarget target : ship.repairTargets){
					RepairStatusPanel panel = new RepairStatusPanel(target);
					panel.setSelected(this.target == target);
					targetPanel.add(panel);
				}
				
				targetPanel.repaint();
			}*/
			
			for (int x = 0; x < targetPanel.getComponentCount(); x++)
				((StatusPanel)targetPanel.getComponent(x)).update();
			
			material = (int)ship.material;
			materialLabel.setText("Available Repair Materaial: " + material);
			
			repairQueue.refresh();
			//unitPanel.refresh();
			selectedPanel.refresh();
		}
	}
	
	public void setShip(Ship ship){
		this.ship = ship;
		targetPanel.removeAll();
		target = ship.repairTargets.get(0);
		autoSlider.setValue(ship.autoRepair);
		setUnit(ship);
	}
	
	public void setUnit(Unit unit){
		unitPanel.setUnit(unit);
		selectedPanel.setRepairable(unit);
		
		for (Object component : targetPanel.getComponents())
			((RepairStatusPanel)component).setSelected(((RepairStatusPanel)component).target.unit == unit);
	}
	
	// Panels corresponding to each available unit that can be repaired, with options for rearming or recharging
	// Panels can be selected so the targeted unit details can be displayed in the unitPanel
	private class RepairStatusPanel extends StatusPanel{
		
		public RepairStatusPanel(RepairTarget target){
			super(target);
			
			if (target.unit == ship)
				optionsPanel.remove(rearmBox);
			if (!ship.crafts.contains(target.unit))
				optionsPanel.remove(rechargeBox);
			
			this.addMouseListener(new MouseAdapter(){
				public void mousePressed(MouseEvent e){
					RepairPanel.this.target = RepairStatusPanel.this.target;
					setUnit(RepairStatusPanel.this.target.unit);
			}});
			
			JLabel nameLabel = new JLabel(target.unit.getName());
			nameLabel.setPreferredSize(new Dimension(79, 20));
			this.add(nameLabel, 1);
		}
		
		public void setSelected(boolean selected){
			if (selected){
				this.setBorder(Window.SELECTED_BORDER);
			}else
				this.setBorder(Window.UNSELECTED_BORDER);
		}
	}
	
	// Shows a graphical list of all units or components queued for repair, with the rightmost one
	// currently in progress. Also displays the current hull value and ETA to completion
	private class RepairQueue extends JPanel{
		static final int SPACING = 7;
		
		IconQueue iconQueue;
		JLabel hullLabel, timeLabel;
		int queueSize;
		RepairQueueItem selected;
		
		public RepairQueue(){
			this.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-5, 74));
			this.setLayout(Window.DEFAULT_LAYOUT);
			this.setBorder(BorderFactory.createEtchedBorder());
			
			iconQueue = new IconQueue();
			
			hullLabel = new JLabel();
			hullLabel.setPreferredSize(new Dimension(110, 17));
			timeLabel = new JLabel();
			timeLabel.setPreferredSize(new Dimension(110, 17));
			
			this.add(iconQueue);
			this.add(hullLabel);
			this.add(timeLabel);
		}
		
		public void refresh(){
			if (queueSize != ship.repairQueue.size()){
				queueSize = ship.repairQueue.size();
				iconQueue.repaint();
			}
			
			if (!ship.repairQueue.isEmpty()){
				if (selected == null)
					selected = ship.repairQueue.peek();
				
				hullLabel.setText("Hull: " + (int)(100*selected.repairable.getHull()/selected.repairable.getType().hull) + "%" +
						" (" + (int)(100*selected.repairTo/selected.repairable.getType().hull) + "%)");
				
				int totalTime = 0;
				boolean containsSelected = false;
				try{
					for (RepairQueueItem item : ship.repairQueue){
						totalTime += (int)(abs(item.repairable.getHull()-item.repairTo)/
								item.repairable.getType().hullPerMaterial/item.repairRate);
						if (item == selected){
							containsSelected = true;
							break;
						}
					}
				}catch (ConcurrentModificationException ex){}
				
				if (containsSelected){
					timeLabel.setText("ETA: " + (int)(totalTime/Main.game.turnsPerSecond) + "s");
				}else
					selected = null;
			}else{
				hullLabel.setText("");
				timeLabel.setText("");
			}
		}
		
		// Graphical list component
		private class IconQueue extends JComponent{
			static final int NUM_ITEMS = 5;
			
			public IconQueue(){
				this.setPreferredSize(new Dimension(250, 51));
				this.addMouseListener(new ClickListener());
			}
			
			public void paint(Graphics g){
				((Graphics2D)g).setRenderingHints(Main.inGameHints);
				
				g.setColor(this.getParent().getBackground());
				g.fillRect(0, 0, 185, 44);
				
				g.setColor(Color.BLACK);
				int pos = 0;
				for (RepairQueueItem item : ship.repairQueue){
					int posX = (NUM_ITEMS-pos-1)*(UnitType.ICON_SIZE+SPACING)+SPACING;
					if (pos < NUM_ITEMS){
						g.drawImage(item.repairable.getIcon(), posX, 0, null);
						if (item.isAuto){
							g.setFont(autoFont);
							g.drawString("AUTO", posX+10, 49);
						}
						if (item == selected){
							g.drawRect(posX-SPACING/2, 0, UnitType.ICON_SIZE+SPACING, UnitType.ICON_SIZE+12);
						}else{
							if (pos < ship.repairQueue.size()-1)
								g.drawLine(posX-SPACING/2, 5, posX-SPACING/2, 38);
						}
						if (pos == 0)
							g.fillRect(posX-SPACING/2-1, 5, 3, 38);
					}
					pos++;
				}
			}
			
			// Mouse listener for removing items from the queue
			class ClickListener extends MouseAdapter{
				public void mousePressed(MouseEvent e){
					int mousePosX = e.getPoint().x;
					int posX = NUM_ITEMS*(UnitType.ICON_SIZE+SPACING)+SPACING/2;
					for (RepairQueueItem item : ship.repairQueue){
						if (mousePosX < posX && mousePosX > posX-(UnitType.ICON_SIZE+SPACING)){
							if (e.getButton() == MouseEvent.BUTTON1){
								selected = item;
							}else{
								ship.repairQueue.remove(item);
								if (item == selected)
									selected = null;
							}
							repaint();
							return;
						}
						posX -= UnitType.ICON_SIZE+SPACING;
					}
				}
			}
		}
	}
	
	// Displays the currently selected RepairTarget, so that individual components can be queued for repair
	private class RepairUnitPanel extends UnitPanel{
		
		public RepairUnitPanel(){
			super(GameWindow.MENU_WIDTH*3/4-5, true, true);
			this.setOpaque(false);
			optionsPanel.setOpaque(false);
			weaponButton.setOpaque(false);
			systemButton.setOpaque(false);
		}
		
		protected void select(Component component){
			if (component != null)
				selectedPanel.setRepairable(component);
		}
		
		protected void deselect(Component component){}
		
		public double repairRate(){
			if (unit == ship){
				return ship.type.selfRepairRate;
			}else if (ship.crafts.contains(unit)){
				return ship.type.craftRepairRate;
			}else
				return ship.type.repairRate;
		}
	}
	
	// Displays the currently selected Repairable item, which can be either a ship, craft, or component.
	// Contains a slider bar so that an order to either repair or scrap the targeted Repairable to the desired hull value
	private class SelectedPanel extends JPanel{
		Repairable repairable;
		double currentHull;
		
		JComponent iconPanel;
		JSlider hullSlider;
		JLabel hullLabel;
		JLabel timeLabel, materialLabel, newHullLabel;
		JButton button;
		
		public SelectedPanel(){
			this.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-13, 67));
			this.setOpaque(false);
			
			iconPanel = new JComponent(){
				public void paint(Graphics g){
					g.setColor(this.getParent().getBackground());
					g.fillRect(0, 0, 40, 40);
					g.drawImage(repairable.getIcon(), 0, 0, null);
			}};
			iconPanel.setPreferredSize(new Dimension(42, 38));
			iconPanel.setOpaque(false);
			hullLabel = new JLabel();
			hullLabel.setPreferredSize(new Dimension(58, 20));
			JPanel hullPanel = new JPanel(Window.DEFAULT_LAYOUT);
			hullPanel.setPreferredSize(new Dimension(62, 60));
			hullPanel.setOpaque(false);
			hullPanel.add(iconPanel);
			hullPanel.add(hullLabel);
			
			hullSlider = new JSlider((int)(100*Main.config.maxScrapDamage), 100, 100);
			hullSlider.setPreferredSize(new Dimension(20, 62));
			hullSlider.setOpaque(false);
			hullSlider.setOrientation(JSlider.VERTICAL);
			hullSlider.setMinorTickSpacing(2);
			hullSlider.addChangeListener(new ChangeListener(){
				public void stateChanged(ChangeEvent e){
					updateRepair();
			}});
			
			newHullLabel = new JLabel();
			newHullLabel.setPreferredSize(new Dimension(98, 15));
			materialLabel = new JLabel();
			materialLabel.setPreferredSize(new Dimension(98, 15));
			timeLabel = new JLabel();
			timeLabel.setPreferredSize(new Dimension(98, 15));
			
			JPanel requiredPanel = new JPanel(Window.DEFAULT_LAYOUT);
			requiredPanel.setPreferredSize(new Dimension(98, 50));
			requiredPanel.setOpaque(false);
			requiredPanel.add(newHullLabel);
			requiredPanel.add(materialLabel);
			requiredPanel.add(timeLabel);
			
			button = new JButton();
			button.setPreferredSize(new Dimension(55, 25));
			button.setMargin(new Insets(1, 1, 1, 1));
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					ship.repairQueue.add(new RepairQueueItem(repairable, target,
							hullSlider.getValue()/100.0*repairable.getType().hull,
							unitPanel.repairRate(),
							hullSlider.getValue() < targetHull(),
							false));
			}});
			
			this.add(hullPanel);
			this.add(hullSlider);
			this.add(requiredPanel);
			this.add(button);
		}
		
		public void setRepairable(Repairable repairable){
			this.repairable = repairable;
			hullSlider.setValue(targetHull());
			
			iconPanel.repaint();
		}
		
		public void refresh(){
			if (currentHull != repairable.getHull()){
				currentHull = repairable.getHull();
				hullLabel.setText("Hull:" + targetHull() + "%");
				updateRepair();
			}
		}
		
		private void updateRepair(){
			double material = (targetHull()-hullSlider.getValue())/100.0*repairable.getType().hull/repairable.getType().hullPerMaterial;
			newHullLabel.setText(hullSlider.getValue() + "%");
			timeLabel.setText("Time: " + (int)(10*abs(material)/unitPanel.repairRate()/Main.game.turnsPerSecond)/10.0 + "s");
			materialLabel.setText("Material: " + (int)(10*material*(hullSlider.getValue() > targetHull() ? 1.0 : Main.config.scrapReturn))/10.0);
			
			if (hullSlider.getValue() > targetHull()){
				button.setEnabled(true);
				button.setText("Repair");
			}else if (hullSlider.getValue() < targetHull()){
				button.setEnabled(true);
				button.setText("Scrap");
			}else{
				button.setEnabled(false);
				button.setText("");
			}
		}
		
		private int targetHull(){
			return (int)(100*repairable.getHull()/repairable.getType().hull);
		}
	}
}
