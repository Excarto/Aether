import static java.lang.Math.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;

// Window for outfitting a unit with weapons and systems. Displays unit power use and generation
// with the given loadout to help with outfitting

public class OutfitWindow extends Window{
	
	final Unit unit;
	Hardpoint selectedHardpoint;
	WeaponType[] weaponTypes;
	SystemType[] systemTypes;
	double ammoRatio;
	
	final UnitWindow unitWindow;
	final JSlider arcSlider, orientationSlider;
	final JLabel componentNameLabel;
	final DetailImage componentViewPanel;
	final JLabel arcLabel, massLabel, costLabel, orientationLabel;
	final JLabel totalMassValueLabel, totalCostValueLabel;
	final JLabel unitPowerValueLabel, engPowerValueLabel, shieldPowerValueLabel, wepPowerValueLabel, sysPowerValueLabel, capTimeValueLabel;
	final JCheckBox manualAmmo;
	final JButton setAmmo;
	final JPanel rightPanel, ammoStoragePanel;
	final JPanel namePanel;
	final JTextField nameField;
	
	public OutfitWindow (Unit outfitUnit){
		super(Size.NORMAL);
		this.unit = outfitUnit;
		
		ammoRatio = 1.0;
		
		unitWindow = new UnitWindow();
		
		arcLabel = new JLabel("Arc:");
		arcLabel.setPreferredSize(new Dimension(65, 20));
		arcLabel.setEnabled(false);
		arcSlider = new JSlider(0, 0, 0);
		arcSlider.setPreferredSize(new Dimension(90, 20));
		arcSlider.setMajorTickSpacing(2*Weapon.ARC_INCREMENT);
		arcSlider.setSnapToTicks(true);
		arcSlider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				if (arcSlider.isEnabled()){
					Weapon weapon = unit.getWeaponAt(selectedHardpoint);
					if (/*weapons.isSelected() && */weapon != null){
						weapon.setArc(arcSlider.getValue()/2);
						updateLabels();
					}
				}
		}});
		arcSlider.setEnabled(false);
		orientationLabel = new JLabel("Orientation:");
		orientationLabel.setPreferredSize(new Dimension(65, 20));
		orientationLabel.setEnabled(false);
		orientationSlider = new JSlider(0, 100, 50);
		orientationSlider.setPreferredSize(new Dimension(90, 20));
		orientationSlider.setMajorTickSpacing(2);
		orientationSlider.setSnapToTicks(true);
		orientationSlider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				Weapon weapon = unit.getWeaponAt(selectedHardpoint);
				if (/*weapons.isSelected() && */weapon != null)
					weapon.setMountAngle((2*orientationSlider.getValue()-100)/100.0);
		}});
		orientationSlider.setEnabled(false);
		costLabel = new JLabel("   Cost:    ");
		costLabel.setPreferredSize(new Dimension(145, 14));
		costLabel.setEnabled(false);
		massLabel = new JLabel("   Mass:    ");
		massLabel.setPreferredSize(new Dimension(145, 16));
		massLabel.setEnabled(false);
		JPanel arcPanel = new JPanel();
		arcPanel.setPreferredSize(new Dimension(166, 110));
		arcPanel.add(createSpacer(130, 13));
		arcPanel.add(costLabel);
		arcPanel.add(massLabel);
		arcPanel.add(arcLabel);
		arcPanel.add(arcSlider);
		arcPanel.add(orientationLabel);
		arcPanel.add(orientationSlider);
		
		componentNameLabel = new JLabel();
		componentNameLabel.setPreferredSize(new Dimension(160, 13));
		componentNameLabel.setFont(Main.getDefaultFont(12));
		componentNameLabel.setHorizontalAlignment(JLabel.CENTER);
		
		componentViewPanel = new DetailImage();
		
		JPanel componentControlPanel = new JPanel();
		componentControlPanel.setPreferredSize(new Dimension(177, 210));
		componentControlPanel.setBorder(BorderFactory.createEtchedBorder());
		componentControlPanel.add(componentNameLabel);
		componentControlPanel.add(componentViewPanel);
		componentControlPanel.add(arcPanel);
		
		ammoStoragePanel = new JPanel();
		ammoStoragePanel.setPreferredSize(new Dimension(componentControlPanel.getPreferredSize().width, 64));
		ammoStoragePanel.setBorder(BorderFactory.createEtchedBorder());
		
		manualAmmo = new JCheckBox("Manual Ammo");
		manualAmmo.setSelected(unit.manualAmmo);
		manualAmmo.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				unit.setDefaultAmmoRatios();
				unit.manualAmmo = manualAmmo.isSelected();
				setAmmo.setEnabled(manualAmmo.isSelected());
		}});
		
		setAmmo = new JButton("Set Ratios");
		setAmmo.setEnabled(unit.manualAmmo);
		setAmmo.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				Main.addWindow(new AmmoWindow(unit.ammoRatios, unit.type.storageSpace*ammoRatio));
		}});
		
		JPanel ammoPanel = new JPanel();
		ammoPanel.setPreferredSize(new Dimension(106, 53));
		ammoPanel.add(manualAmmo);
		ammoPanel.add(setAmmo);
		ammoStoragePanel.add(ammoPanel);
		
		Dimension overviewLabelSize = new Dimension(113, 16);
		Font overviewFont = new Font("Arial", Font.BOLD, 10);
		Dimension overviewLabelSizeBig = new Dimension(113, 21);
		Font overviewFontBig = new Font("Arial", Font.BOLD, 11);
		
		JLabel totalCostLabel = new JLabel("Total Unit Cost:");
		totalCostLabel.setPreferredSize(overviewLabelSizeBig);
		totalCostLabel.setFont(overviewFontBig);
		totalCostLabel.setHorizontalAlignment(JLabel.LEFT);
		totalCostLabel.setVerticalAlignment(JLabel.TOP);
		JLabel totalMassLabel = new JLabel("Total Unit Mass:");
		totalMassLabel.setPreferredSize(overviewLabelSizeBig);
		totalMassLabel.setFont(overviewFontBig);
		totalMassLabel.setHorizontalAlignment(JLabel.LEFT);
		totalMassLabel.setVerticalAlignment(JLabel.TOP);
		
		JLabel unitPowerLabel = new JLabel("Unit Power Output:");
		unitPowerLabel.setPreferredSize(overviewLabelSize);
		unitPowerLabel.setFont(overviewFont);
		JLabel engPowerLabel = new JLabel("Engine Power Use:");
		engPowerLabel.setPreferredSize(overviewLabelSize);
		engPowerLabel.setFont(overviewFont);
		JLabel shieldPowerLabel = new JLabel("Shield Power Use:");
		shieldPowerLabel.setPreferredSize(overviewLabelSize);
		shieldPowerLabel.setFont(overviewFont);
		JLabel wepPowerLabel = new JLabel("Weapon Power Use:");
		wepPowerLabel.setPreferredSize(overviewLabelSize);
		wepPowerLabel.setFont(overviewFont);
		JLabel sysPowerLabel = new JLabel("System Power Use:");
		sysPowerLabel.setPreferredSize(overviewLabelSize);
		sysPowerLabel.setFont(overviewFont);
		JLabel capTimeLabel = new JLabel("Energy Drain Time:");
		capTimeLabel.setPreferredSize(overviewLabelSize);
		capTimeLabel.setFont(overviewFont);
		
		JPanel overviewPanelLeft = new JPanel(DEFAULT_LAYOUT);
		overviewPanelLeft.setPreferredSize(new Dimension(115, 144));
		overviewPanelLeft.add(totalCostLabel);
		overviewPanelLeft.add(totalMassLabel);
		overviewPanelLeft.add(unitPowerLabel);
		overviewPanelLeft.add(engPowerLabel);
		overviewPanelLeft.add(shieldPowerLabel);
		overviewPanelLeft.add(wepPowerLabel);
		overviewPanelLeft.add(sysPowerLabel);
		overviewPanelLeft.add(capTimeLabel);
		
		Dimension overviewValueSize = new Dimension(45, overviewLabelSize.height);
		Dimension overviewValueSizeBig = new Dimension(45, overviewLabelSizeBig.height);
		
		totalCostValueLabel = new JLabel();
		totalCostValueLabel.setPreferredSize(overviewValueSizeBig);
		totalCostValueLabel.setFont(overviewFontBig);
		totalCostValueLabel.setHorizontalAlignment(JLabel.LEFT);
		totalCostValueLabel.setVerticalAlignment(JLabel.TOP);
		totalMassValueLabel = new JLabel();
		totalMassValueLabel.setPreferredSize(overviewValueSizeBig);
		totalMassValueLabel.setFont(overviewFontBig);
		totalMassValueLabel.setHorizontalAlignment(JLabel.LEFT);
		totalMassValueLabel.setVerticalAlignment(JLabel.TOP);
		
		unitPowerValueLabel = new JLabel();
		unitPowerValueLabel.setPreferredSize(overviewValueSize);
		unitPowerValueLabel.setFont(overviewFont);
		unitPowerValueLabel.setHorizontalAlignment(JLabel.LEFT);
		engPowerValueLabel = new JLabel();
		engPowerValueLabel.setPreferredSize(overviewValueSize);
		engPowerValueLabel.setFont(overviewFont);
		engPowerValueLabel.setHorizontalAlignment(JLabel.LEFT);
		shieldPowerValueLabel = new JLabel();
		shieldPowerValueLabel.setPreferredSize(overviewValueSize);
		shieldPowerValueLabel.setFont(overviewFont);
		shieldPowerValueLabel.setHorizontalAlignment(JLabel.LEFT);
		wepPowerValueLabel = new JLabel();
		wepPowerValueLabel.setPreferredSize(overviewValueSize);
		wepPowerValueLabel.setFont(overviewFont);
		wepPowerValueLabel.setHorizontalAlignment(JLabel.LEFT);
		sysPowerValueLabel = new JLabel();
		sysPowerValueLabel.setPreferredSize(overviewValueSize);
		sysPowerValueLabel.setFont(overviewFont);
		sysPowerValueLabel.setHorizontalAlignment(JLabel.LEFT);
		capTimeValueLabel = new JLabel();
		capTimeValueLabel.setPreferredSize(overviewValueSize);
		capTimeValueLabel.setFont(overviewFont);
		capTimeValueLabel.setHorizontalAlignment(JLabel.LEFT);
		
		JPanel overviewPanelRight = new JPanel(DEFAULT_LAYOUT);
		overviewPanelRight.setPreferredSize(new Dimension(48, 144));
		overviewPanelRight.add(totalCostValueLabel);
		overviewPanelRight.add(totalMassValueLabel);
		overviewPanelRight.add(unitPowerValueLabel);
		overviewPanelRight.add(engPowerValueLabel);
		overviewPanelRight.add(shieldPowerValueLabel);
		overviewPanelRight.add(wepPowerValueLabel);
		overviewPanelRight.add(sysPowerValueLabel);
		overviewPanelRight.add(capTimeValueLabel);
		
		JPanel overviewPanel = new JPanel(DEFAULT_LAYOUT);
		overviewPanel.setPreferredSize(new Dimension(186, 150));
		overviewPanel.add(overviewPanelLeft);
		overviewPanel.add(overviewPanelRight);
		
		rightPanel = new JPanel();
		rightPanel.setPreferredSize(new Dimension(188, unitWindow.getPreferredSize().height));
		rightPanel.add(componentControlPanel);
		rightPanel.add(ammoStoragePanel);
		rightPanel.add(overviewPanel);
		rightPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		nameField = new JTextField(unit.getName());
		nameField.setPreferredSize(new Dimension(300, 28));
		nameField.setFont(new Font("Courier", Font.PLAIN, 17));
		nameField.getDocument().addDocumentListener(new DocumentListener(){
			public void changedUpdate(DocumentEvent e){
				setUnitName();
			}
			public void insertUpdate(DocumentEvent e){
				setUnitName();
			}
			public void removeUpdate(DocumentEvent e){
				setUnitName();
			}
		});
		namePanel = new JPanel();
		namePanel.setPreferredSize(new Dimension(1000, 34));
		namePanel.setOpaque(false);
		namePanel.add(nameField);
		
		JButton autoButton = new JButton("Random Loadout");
		autoButton.setPreferredSize(new Dimension(135, 30));
		autoButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				autoLoadout();
		}});
		JButton exitButton = new JButton("Finish");
		exitButton.setPreferredSize(new Dimension(135, 30));
		exitButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Main.removeWindow();
		}});
		JPanel bottomPanel = new JPanel();
		bottomPanel.setPreferredSize(new Dimension(704, 60));
		bottomPanel.setOpaque(false);
		bottomPanel.add(autoButton);
		bottomPanel.add(exitButton);
		
		this.add(new Title("Outfit Unit", 900, 90));
		this.add(unitWindow);
		this.add(rightPanel);
		this.add(namePanel);
		this.add(bottomPanel);
		
		unitWindow.select(null);
		
		updateComponentUI();
	}
	
	// Called when a higher window in the stack returns a value
	public void returnValue(Object type){
		if (type instanceof WeaponType){//(weapons.isSelected()){
			unit.setWeapon((WeaponType)type, (WeaponHardpoint)selectedHardpoint, Weapon.MIN_ARC, 0);
			updateComponentUI();
			arcSlider.setValue(type instanceof MissileType ? 0 : arcSlider.getMaximum());
			unit.getWeaponAt(selectedHardpoint).setMountAngle(Weapon.AUTO_ANGLE);
		}else if (type instanceof SystemType){
			unit.setSystem((SystemType)type, selectedHardpoint);
			updateComponentUI();
		}
		unitWindow.select(selectedHardpoint);
		unit.initializeAmmoAndMass();
		updateLabels();
	}
	
	public void suspend(){
		unitWindow.exit();
	}
	
	public void resume(){
		unitWindow.resume();
	}
	
	// Called when a component is selected or deselected
	protected void updateComponentUI(){
		Component component = unit.getComponentAt(selectedHardpoint);
		costLabel.setEnabled(component != null);
		massLabel.setEnabled(component != null);
		
		Weapon weapon = unit.getWeaponAt(selectedHardpoint);
		if (/*selectedHardpoint == null || !weapons.isSelected() ||*/ weapon == null){
			arcLabel.setEnabled(false);
			arcSlider.setEnabled(false);
			//costLabel.setEnabled(false);
			//massLabel.setEnabled(false);
			orientationLabel.setEnabled(false);
			orientationSlider.setEnabled(false);
			arcSlider.setMaximum(0);
		}else{
			arcSlider.setEnabled(false);
			int maxArc = weapon.hardpoint.getMaxArc(weapon.type);
			arcSlider.setMaximum(maxArc < Weapon.ARC_INCREMENT ? 0 : 2*maxArc);
			arcLabel.setEnabled(true);
			arcSlider.setEnabled(true);
			//costLabel.setEnabled(true);
			//massLabel.setEnabled(true);
			orientationLabel.setEnabled(true);
			orientationSlider.setEnabled(true);
			orientationSlider.setValue((int)(100*(weapon.getMountAngleFrac()+1.0)/2));
			arcSlider.setValue(2*weapon.getArc());
		}
		updateLabels();
	}
	
	public void updateLabels(){
		double wepPower = 0.0, sysPower = 0.0;
		for (Weapon wep : unit.weapons)
			wepPower += wep.type.getAveragePowerUse();
		for (System sys : unit.systems)
			sysPower += sys.type.getAveragePowerUse();
		double engPower = unit.type.thrust*Main.config.energyPerThrust;
		double shieldPower = unit instanceof Ship ? ((Ship)unit).type.shieldRecharge*Main.config.energyPerShield : 0.0;
		double capDrainTime = unit.type.capacitor/(engPower + wepPower + sysPower - unit.type.power);
		
		arcLabel.setText("Arc: " + arcSlider.getValue());
		unit.initializeAmmoAndMass();
		totalMassValueLabel.setText("" + (int)round(unit.getMass()*10.0)/10.0);
		totalCostValueLabel.setText("" + unit.getCost());
		unitPowerValueLabel.setText("" + (int)round(10.0*unit.type.power*Main.TPS/1000)/10.0);
		engPowerValueLabel.setText("" + (int)round(10.0*engPower*Main.TPS/1000)/10.0);
		shieldPowerValueLabel.setText("" + (int)round(10.0*shieldPower*Main.TPS/1000)/10.0);
		wepPowerValueLabel.setText("" + (int)round(10.0*wepPower*Main.TPS/1000)/10.0);
		sysPowerValueLabel.setText("" + (int)round(10.0*sysPower*Main.TPS/1000)/10.0);
		capTimeValueLabel.setText(capDrainTime > 0 ? (int)round(10.0*capDrainTime/Main.TPS)/10.0 + "s" : "Never");
		
		Component component = unit.getComponentAt(selectedHardpoint);
		if (component != null){
			int massPercent = (int)round(component.getMass()/unit.mass*100);
			massLabel.setText("   Mass: " + component.getMass() + " ("+massPercent+"%)");
			costLabel.setText("   Cost: " + component.getCost());
		}
	}
	
	protected void setUnitName(){
		nameField.setBackground(unit.setName(nameField.getText()) ? Color.WHITE : Color.RED);
	}
	
	protected void autoLoadout(){
		unit.autoLoadout();
		unitWindow.select(null);
	}
	
	// Open a window to select component type for selected hardpoint
	protected void setHardpoint(){
		if (selectedHardpoint != null){
			Vector<BuyType> types = new Vector<BuyType>();
			for (BuyType type : (selectedHardpoint instanceof WeaponHardpoint ? Main.weaponTypes : Main.systemTypes))
				if (type.mass <= selectedHardpoint.mass)
					types.add(type);
			if (types.size() > 0){
				if (selectedHardpoint instanceof WeaponHardpoint){
					Main.addWindow(new WeaponTypeWindow(types, OutfitWindow.this));
				}else
					Main.addWindow(new TypeWindow(SystemType.class, types, OutfitWindow.this));
			}
		}
	}
	
	protected void removeHardpoint(){
		if (selectedHardpoint instanceof WeaponHardpoint){
			unit.setWeapon(null, (WeaponHardpoint)selectedHardpoint, 0, 0);
		}else
			unit.setSystem(null, selectedHardpoint);
		unitWindow.select(null);
	}
	
	private class UnitWindow extends JComponent{
		final static int EXTRA_WIDTH = 20;
		final static int EXTRA_HEIGHT = 45;
		final static int ANIMATION_SPEED = 1000;
		
		private final BufferedImage img;
		private final Font categoryFont, massFont;
		private int smallestWeapon, smallestSystem;
		private int lineLength;
		PeriodicTimer timer;
		
		public UnitWindow(){
			img = unit.type.topImg;
			lineLength = 0;
			
			categoryFont = Main.getDefaultFont(9);
			massFont = new Font("Courier", Font.BOLD, 8);
			
			this.setPreferredSize(new Dimension(
				unit.type.topImg.getWidth()+EXTRA_WIDTH, unit.type.topImg.getHeight()+EXTRA_HEIGHT));
			this.setDoubleBuffered(true);
			this.addMouseListener(new MouseAdapter(){
				public void mousePressed(MouseEvent e){
					for (Hardpoint[] hardpoints : new Hardpoint[][]{unit.type.weaponHardpoints, unit.type.systemHardpoints}){
						for (Hardpoint hardpoint : hardpoints){
							if (hypot(hardpoint.posX*img.getWidth()+EXTRA_WIDTH/2-e.getX(),
									hardpoint.posY*img.getHeight()+EXTRA_HEIGHT/2-e.getY()) < 21){
								
								if (e.getButton() == MouseEvent.BUTTON1){
									if (selectedHardpoint == hardpoint){
										setHardpoint();
									}else
										UnitWindow.this.select(hardpoint);
								}else if (e.getButton() == MouseEvent.BUTTON3){
									if (selectedHardpoint == hardpoint){
										removeHardpoint();
									}else
										UnitWindow.this.select(hardpoint);
								}
								break;
								
							}
						}
					}
			}});
			
			smallestSystem = smallestWeapon = Integer.MAX_VALUE;
			for (Hardpoint hardpoint : unit.type.weaponHardpoints)
				if (hardpoint.mass < smallestWeapon)
					smallestWeapon = hardpoint.mass;
			for (Hardpoint hardpoint : unit.type.systemHardpoints)
				if (hardpoint.mass < smallestSystem)
					smallestSystem = hardpoint.mass;
			
			timer = new PeriodicTimer(1000/Main.options.menuFramesPerSec){
				public void runTimerTask(){
					UnitWindow.this.repaint();
					if (lineLength < 500)
						lineLength += ANIMATION_SPEED/Main.options.menuFramesPerSec;
			}};
			timer.start();
		}
		
		public void exit(){
			timer.stop(false);
		}
		public void resume(){
			timer.start();
		}
		
		private void select(Hardpoint hardpoint){
			selectedHardpoint = hardpoint;
			lineLength = 0;
			updateComponentUI();
			if (hardpoint != null && unit.getComponentAt(hardpoint) != null){
				ComponentType type = unit.getComponentAt(hardpoint).type;
				componentNameLabel.setText(type.className);
				//componentClassLabel.setText(type.typeClass);
				componentViewPanel.select(type);
			}else{
				componentNameLabel.setText("");
				//componentClassLabel.setText("");
				componentViewPanel.select(null);
			}
			//setButton.setEnabled(hardpoint != null);
			//removeButton.setEnabled(unit.getComponentAt(hardpoint) != null);
		}
		
		public void paint(Graphics g){
			((Graphics2D)g).setRenderingHints(Main.menuHints);
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			g.drawImage(img, (this.getWidth()-img.getWidth())/2, (this.getHeight()-img.getHeight())/2, this);
			
			for (int drawSelected = 0; drawSelected <= 1; drawSelected++){ // Want to draw the selected hardpoint on top of others
				for (Hardpoint[] hardpoints : new Hardpoint[][]{unit.type.weaponHardpoints, unit.type.systemHardpoints}){
					for (Hardpoint hardpoint : hardpoints){
						if ((hardpoint == selectedHardpoint) == (drawSelected == 1)){
						
							boolean isWeapon = hardpoint instanceof WeaponHardpoint;
							int posX = (int)(hardpoint.posX*img.getWidth())+EXTRA_WIDTH/2;
							int posY = (int)(hardpoint.posY*img.getHeight())+EXTRA_HEIGHT/2;
							
							if (hardpoint == selectedHardpoint){
								g.setColor(new Color(10,150,10));
								g.drawLine(posX, 0, posX, lineLength);
								g.drawLine(0, posY, lineLength, posY);
							}
							
							g.setColor(Color.LIGHT_GRAY);
							Component component = unit.getComponentAt(hardpoint);
							int size = 44;
							//if (component == null)
							//	size = (int)(10 + 28.0*sqrt(hardpoint.mass/(isWeapon ? smallestWeapon : smallestSystem)));
							g.fillOval(posX-size/2, posY-size/2, size, size);
							
							if (isWeapon){
								int arc = ((WeaponHardpoint)hardpoint).arc;
								int angle = ((WeaponHardpoint)hardpoint).angle;
								g.setColor(Color.GRAY);
								g.fillArc(posX-size/2, posY-size/2, size, size, 90-angle-arc, arc*2);
								if (component != null){
									arc = unit.getWeaponAt(hardpoint).getArc();
									angle = unit.getWeaponAt(hardpoint).getMountAngle();
									g.setColor(Color.DARK_GRAY);
									g.fillArc(posX-size/2, posY-size/2, size, size, 90-angle-arc, arc*2);
								}
							}
							
							if (component == null){
								g.setColor(Color.BLACK);
								g.setFont(categoryFont);
								g.drawString(isWeapon ? "WEP" : "SYS", posX-10, posY-5);
								g.setFont(massFont);
								g.drawString("mass", posX-9, posY+6);
								g.drawString(String.valueOf(hardpoint.mass), posX-(hardpoint.mass < 100 ? 5 : 8), posY+13);//9);
							}else{
								g.drawImage(component.type.icon,
										posX-component.type.icon.getWidth()/2,
										posY-component.type.icon.getHeight()/2, null);
							}
							
							if (hardpoint == selectedHardpoint){
								g.setColor(new Color(10,150,10));
								g.drawOval(posX-20, posY-20, 39, 39);
							}
						}
						
					} // End loop over hardpoints
				} // End loop over weapon vs system hardpoint
			} // Emd loop over selected vs non-selected
			
			g.setColor(new Color(40, 120, 40));
			g.drawRect(0, 0, getWidth()-1, getHeight()-1);
			
		}
		
	}
}
