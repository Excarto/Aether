import static java.lang.Math.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class WeaponPanel extends JPanel{
	static final Font MODE_FONT = Main.getDefaultFont(9);
	
	Weapon weapon;
	
	DetailImage img;
	JPanel modeOptionsPanel;
	
	ComponentsPanel.DamageLabel damageLabel;
	JLabel loadedLabel, ammoLabel, timeLabel;
	int loadedInt, ammoInt;
	
	JLabel typeLabel;
	JRadioButton disabled, manual, automatic, autonomous;
	JCheckBox autoMissiles, autoCraft, autoShips;
	JPanel rangePanel;
	JLabel rangeLabel;
	JSlider rangeSlider;
	JButton setTarget;
	JRadioButton applyWeaponType, applyAllWeapons;
	JRadioButton applyUnit, applyUnitType, applyAll;
	JPanel volleyPanel;
	JLabel volleyLabel;
	JSlider volleySlider;
	
	public WeaponPanel(){
		this.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-4, 308));
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setOpaque(false);
		
		typeLabel = new JLabel();
		typeLabel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-12, 15));
		typeLabel.setFont(new Font("Courier", Font.BOLD, 14));
		typeLabel.setHorizontalAlignment(JLabel.CENTER);
		
		img = new DetailImage();
		
		JPanel conditionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 1));
		Dimension conditionPanelSize = new Dimension(130, 70);
		conditionPanel.setPreferredSize(conditionPanelSize);
		conditionPanel.setOpaque(false);
		loadedLabel = new JLabel();
		loadedInt = 1;
		damageLabel = Unit.weaponsPanel.new DamageLabel();
		ammoLabel = new JLabel();
		timeLabel = new JLabel();
		Dimension conditionLabelSize = new Dimension(conditionPanelSize.width-2, 15);
		loadedLabel.setPreferredSize(conditionLabelSize);
		damageLabel.setPreferredSize(conditionLabelSize);
		ammoLabel.setPreferredSize(conditionLabelSize);
		timeLabel.setPreferredSize(conditionLabelSize);
		conditionPanel.add(loadedLabel);
		conditionPanel.add(damageLabel);
		conditionPanel.add(ammoLabel);
		conditionPanel.add(timeLabel);
		
		disabled = new JRadioButton("Weapon Disabled");
		disabled.setPreferredSize(new Dimension(130, 14));
		disabled.setOpaque(false);
		disabled.setFont(MODE_FONT);
		disabled.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if (weapon.getFireMode() == Weapon.FireMode.AUTONOMOUS)
					weapon.setOverrideTarget(null);
				weapon.setFireMode(Weapon.FireMode.DISABLED);
				setComponentsEnabled();
		}});
		manual = new JRadioButton("Manual Targeting");
		manual.setPreferredSize(new Dimension(118, 14));
		manual.setOpaque(false);
		manual.setFont(MODE_FONT);
		//manual.setToolTipText("Fire only when the trigger is pulled");
		manual.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if (weapon.getFireMode() == Weapon.FireMode.AUTONOMOUS)
					weapon.setOverrideTarget(null);
				weapon.setFireMode(Weapon.FireMode.MANUAL);
				setComponentsEnabled();
		}});
		automatic = new JRadioButton("Prefer Manual Target");
		automatic.setOpaque(false);
		automatic.setPreferredSize(new Dimension(130, 14));
		automatic.setFont(MODE_FONT);
		//automatic.setToolTipText("Fire when the target is within the specified range");
		automatic.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if (weapon.getFireMode() == Weapon.FireMode.AUTONOMOUS)
					weapon.setOverrideTarget(null);
				weapon.setFireMode(Weapon.FireMode.AUTOMATIC);
				setComponentsEnabled();
		}});
		autonomous = new JRadioButton("Fully Autonomous");
		autonomous.setOpaque(false);
		autonomous.setPreferredSize(new Dimension(118, 14));
		autonomous.setFont(MODE_FONT);
		//autonomous.setToolTipText("Choose both the target and when to fire autonomously");
		autonomous.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				weapon.setFireMode(Weapon.FireMode.AUTONOMOUS);
				setComponentsEnabled();
		}});
		ButtonGroup group = new ButtonGroup();
		group.add(disabled);
		group.add(manual);
		group.add(automatic);
		group.add(autonomous);
		
		autoMissiles = new JCheckBox("Missiles");
		autoMissiles.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				weapon.autoMissiles = autoMissiles.isSelected();
		}});
		autoCraft = new JCheckBox("Craft");
		autoCraft.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				weapon.autoCraft = autoCraft.isSelected();
		}});
		autoShips = new JCheckBox("Ships");
		autoShips.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				weapon.autoShips = autoShips.isSelected();
		}});
		
		rangeSlider = new JSlider(0, 40, 3);
		rangeSlider.setPreferredSize(new Dimension(148, 16));
		rangeSlider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				if (rangeSlider.getValue() == 0){
					rangeLabel.setText("Range:Manual");
				}else
					rangeLabel.setText("Range: " + rangeSlider.getValue() + "s");
				((Gun)weapon).autoRange = rangeSlider.getValue()*Main.TPS;
		}});
		rangeLabel = new JLabel();
		rangeLabel.setPreferredSize(new Dimension(83, 16));
		rangePanel = new JPanel(Window.DEFAULT_LAYOUT);
		rangePanel.setPreferredSize(new Dimension(260, 18));
		rangePanel.setOpaque(false);
		rangePanel.add(rangeLabel);
		rangePanel.add(rangeSlider);
		
		setTarget = new JButton("Weapon Target Override");
		setTarget.setFont(MODE_FONT);
		setTarget.setPreferredSize(new Dimension(136, 20));
		setTarget.setMargin(new Insets(1, 2, 1, 2));
		setTarget.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				((HumanPlayer)weapon.unit.player).getWindow().getInputHandler().setWeaponToSetTarget(weapon);
		}});
		
		modeOptionsPanel = new JPanel();
		modeOptionsPanel.setOpaque(false);
		modeOptionsPanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-8, 88));
		
		volleyLabel = new JLabel();
		volleyLabel.setPreferredSize(new Dimension(82, 16));
		volleySlider = new JSlider(1, 10, 1);
		volleySlider.setPreferredSize(new Dimension(148, 16));
		volleySlider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				volleyLabel.setText("Volley Size: " + volleySlider.getValue());
				((Launcher)weapon).volleySize = volleySlider.getValue();
		}});
		volleyPanel = new JPanel(Window.DEFAULT_LAYOUT);
		volleyPanel.setPreferredSize(new Dimension(260, 18));
		volleyPanel.setOpaque(false);
		volleyPanel.add(volleyLabel);
		volleyPanel.add(volleySlider);
		
		JPanel optionsPanel = new JPanel();
		optionsPanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-8, 124));
		optionsPanel.setOpaque(false);
		optionsPanel.add(disabled);
		optionsPanel.add(manual);
		optionsPanel.add(automatic);
		optionsPanel.add(autonomous);
		optionsPanel.add(modeOptionsPanel);
		
		applyWeaponType = new JRadioButton("This Type");
		applyWeaponType.setPreferredSize(new Dimension(100, 14));
		applyWeaponType.setOpaque(false);
		applyWeaponType.setSelected(true);
		applyAllWeapons = new JRadioButton("All Weapons");
		applyAllWeapons.setPreferredSize(new Dimension(100, 14));
		applyAllWeapons.setOpaque(false);
		ButtonGroup applyWeaponGroup = new ButtonGroup();
		applyWeaponGroup.add(applyWeaponType);
		applyWeaponGroup.add(applyAllWeapons);
		JPanel applyWeaponGroupPanel = new JPanel(Window.DEFAULT_LAYOUT);
		applyWeaponGroupPanel.setPreferredSize(new Dimension(95, 46));
		applyWeaponGroupPanel.setOpaque(false);
		applyWeaponGroupPanel.add(applyWeaponType);
		applyWeaponGroupPanel.add(applyAllWeapons);
		
		applyUnit = new JRadioButton("This Unit");
		applyUnit.setPreferredSize(new Dimension(100, 14));
		applyUnit.setOpaque(false);
		applyUnit.setSelected(true);
		applyUnitType = new JRadioButton("This Class");
		applyUnitType.setPreferredSize(new Dimension(100, 14));
		applyUnitType.setOpaque(false);
		applyAll = new JRadioButton("All Units");
		applyAll.setPreferredSize(new Dimension(100, 14));
		applyAll.setOpaque(false);
		ButtonGroup applyUnitGroup = new ButtonGroup();
		applyUnitGroup.add(applyUnit);
		applyUnitGroup.add(applyUnitType);
		applyUnitGroup.add(applyAll);
		JPanel applyUnitGroupPanel = new JPanel(Window.DEFAULT_LAYOUT);
		applyUnitGroupPanel.setPreferredSize(new Dimension(95, 46));
		applyUnitGroupPanel.setOpaque(false);
		applyUnitGroupPanel.add(applyUnit);
		applyUnitGroupPanel.add(applyUnitType);
		applyUnitGroupPanel.add(applyAll);
		
		JButton apply = new JButton("Apply");
		apply.setPreferredSize(new Dimension(55, 30));
		apply.setMargin(new Insets(1,1,1,1));
		apply.addActionListener(new ApplyListener());
		
		JLabel applyLabel = new JLabel("Apply Settings To All");
		applyLabel.setPreferredSize(new Dimension(200, 16));
		applyLabel.setHorizontalAlignment(JLabel.CENTER);
		
		JPanel applyPanel = new JPanel(Window.DEFAULT_LAYOUT);
		applyPanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-12, 68));
		applyPanel.setOpaque(true);
		applyPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
		applyPanel.add(applyLabel);
		applyPanel.add(applyWeaponGroupPanel);
		applyPanel.add(applyUnitGroupPanel);
		applyPanel.add(apply);
		
		this.add(typeLabel);
		this.add(img);
		this.add(conditionPanel);
		this.add(optionsPanel);
		this.add(applyPanel);
	}
	
	public void select(Weapon newWeapon){
		weapon = newWeapon;
		
		if (weapon != null){
			typeLabel.setText(weapon.type.className);
			img.select(weapon.type);
			
			disabled.setSelected(newWeapon.getFireMode() == Weapon.FireMode.DISABLED);
			manual.setSelected(newWeapon.getFireMode() == Weapon.FireMode.MANUAL);
			automatic.setSelected(newWeapon.getFireMode() == Weapon.FireMode.AUTOMATIC);
			autonomous.setSelected(newWeapon.getFireMode() == Weapon.FireMode.AUTONOMOUS);
			autoMissiles.setSelected(newWeapon.autoMissiles);
			autoCraft.setSelected(newWeapon.autoCraft);
			autoShips.setSelected(newWeapon.autoShips);
			if (newWeapon instanceof Launcher){
				volleySlider.setVisible(true);
				volleyLabel.setVisible(true);
				volleySlider.setValue(((Launcher)weapon).volleySize);
				volleySlider.getChangeListeners()[0].stateChanged(null);
			}else{
				volleySlider.setVisible(false);
				volleyLabel.setVisible(false);
			}
			if (newWeapon instanceof Gun){
				timeLabel.setVisible(true);
				rangeSlider.setVisible(true);
				rangeLabel.setVisible(true);
				rangeSlider.setValue(((Gun)newWeapon).autoRange/Main.TPS);
				rangeSlider.getChangeListeners()[0].stateChanged(null);
				//rangeLabel.setText("Range: " + rangeSlider.getValue() + "s");
			}else{
				timeLabel.setVisible(false);
				rangeSlider.setVisible(false);
				rangeLabel.setVisible(false);
			}
			//altFire.setSelected(newWeapon.altFire);
			ammoLabel.setVisible(weapon.type.ammoType != -1);
			
			setComponentsEnabled();
		}
	}
	
	public void refresh(){
		damageLabel.refresh();
		if (weapon.timeToLoaded() != loadedInt){
			loadedInt = weapon.timeToLoaded();
			loadedLabel.setText(100-(int)((double)loadedInt/
					weapon.type.reloadTime*100) + "% loaded (" + loadedInt/Main.TPS + "s)");
		}
		if (weapon.type.ammoType != -1 && weapon.unit.ammo[weapon.type.ammoType] != ammoInt){
			ammoInt = weapon.unit.ammo[weapon.type.ammoType];
			ammoLabel.setText(ammoInt + " ammo (" + ammoInt*weapon.type.reloadTime/Main.TPS + "s)");
		}
		if (weapon instanceof Gun){
			if (weapon.getTarget() != null){
				double time = ((Gun)weapon).getMostRecentTimeToTarget()/Main.TPS;
				if (time < 360){
					timeLabel.setText("ETI: "+(double)round(time*100)/100+"s");
				}else
					timeLabel.setText("ETI: Never");
			}else
				timeLabel.setText("ETI: N/A");
		}
	}
	
	private void setComponentsEnabled(){
		modeOptionsPanel.removeAll();
		if (manual.isSelected()){
			modeOptionsPanel.add(rangePanel);
			modeOptionsPanel.add(volleyPanel);
			modeOptionsPanel.add(setTarget);
		}else if (automatic.isSelected()){
			modeOptionsPanel.add(autoMissiles);
			modeOptionsPanel.add(autoCraft);
			modeOptionsPanel.add(autoShips);
			modeOptionsPanel.add(rangePanel);
			//modeOptionsPanel.add(volleyPanel);
			modeOptionsPanel.add(setTarget);
		}else if (autonomous.isSelected()){
			modeOptionsPanel.add(autoMissiles);
			modeOptionsPanel.add(autoCraft);
			modeOptionsPanel.add(autoShips);
			modeOptionsPanel.add(rangePanel);
		}
		modeOptionsPanel.revalidate();
		modeOptionsPanel.repaint();
	}
	
	private class ApplyListener implements ActionListener{
		public void actionPerformed(ActionEvent e){
			if (applyUnit.isSelected()){
				applyToUnit(weapon.unit);
			}else{
				for (Controllable controllable : weapon.unit.player.units){
					if (controllable instanceof Unit && 
							(applyAll.isSelected() || controllable.getType() == weapon.unit.type))
						applyToUnit((Unit)controllable);
					if (weapon.unit instanceof Craft && controllable instanceof Ship)
						for (Craft craft : ((Ship)controllable).crafts)
							if (applyAll.isSelected() || controllable.getType() == weapon.unit.type)
								applyToUnit(craft);
				}
			}
		}
		
		private void applyToUnit(Unit unit){
			for (Weapon otherWeapon : unit.weapons)
				if (applyAllWeapons.isSelected() || otherWeapon.type == weapon.type){
					//otherWeapon.altFire = weapon.altFire;
					otherWeapon.autoCraft = weapon.autoCraft;
					otherWeapon.autoMissiles = weapon.autoMissiles;
					otherWeapon.autoShips = weapon.autoShips;
					if (otherWeapon instanceof Gun && weapon instanceof Gun)
						((Gun)otherWeapon).autoRange = ((Gun)weapon).autoRange;
					otherWeapon.setFireMode(weapon.getFireMode());
					if (otherWeapon instanceof Launcher && weapon instanceof Launcher)
						((Launcher)otherWeapon).volleySize = ((Launcher)weapon).volleySize;
				}
		}
	}
}
