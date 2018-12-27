import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class StatusPanel extends JPanel{
	public static final int HEIGHT = 48;
	
	final RepairTarget target;
	final JComponent icon;
	final JCheckBox repairBox, rechargeBox, rearmBox;
	final JPanel optionsPanel;
	
	public StatusPanel(RepairTarget repairTarget){
		this.target = repairTarget;
		
		this.setPreferredSize(new Dimension(245, HEIGHT));
		this.setBorder(BorderFactory.createEtchedBorder());
		this.setLayout(Window.DEFAULT_LAYOUT);
		this.setBackground(Window.PANEL_LIGHT);
		
		icon = new JComponent(){
			public void paint(Graphics g){
				g.drawImage(target.unit.getIcon(), 0, 0, null);
		}};
		icon.setPreferredSize(new Dimension(CraftType.ICON_SIZE+6, CraftType.ICON_SIZE));
		
		repairBox = new JCheckBox("Repair");
		repairBox.setPreferredSize(new Dimension(110, 13));
		repairBox.setBackground(Window.PANEL_LIGHT);
		repairBox.setLayout(Window.DEFAULT_LAYOUT);
		repairBox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				target.repair = repairBox.isSelected();
		}});
		
		rechargeBox = new JCheckBox("Capacitor");
		rechargeBox.setPreferredSize(new Dimension(110, 13));
		rechargeBox.setBackground(Window.PANEL_LIGHT);
		rechargeBox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				target.recharge = rechargeBox.isSelected();
		}});
		
		rearmBox = new JCheckBox("Ammo");
		rearmBox.setPreferredSize(new Dimension(110, 13));
		rearmBox.setBackground(Window.PANEL_LIGHT);
		rearmBox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				target.rearm = rearmBox.isSelected();
		}});
		
		optionsPanel = new JPanel(Window.DEFAULT_LAYOUT);
		optionsPanel.setPreferredSize(new Dimension(114, 42));
		optionsPanel.setBackground(Window.PANEL_LIGHT);
		optionsPanel.add(repairBox);
		optionsPanel.add(rechargeBox);
		optionsPanel.add(rearmBox);
		
		this.add(icon);
		this.add(optionsPanel);
	}
	
	public void update(){
		repairBox.setText("Hull: " + (int)(100*target.unit.hull/target.unit.type.hull) + "%");
		repairBox.setSelected(target.repair);
		rechargeBox.setText("Capacitor: " + (int)(100*target.unit.totalCap/target.unit.type.capacitor) + "%");
		rechargeBox.setSelected(target.recharge);
		double totalAmmo = 0;
		for (int x = 0; x < target.unit.ammo.length; x++)
			totalAmmo += target.unit.ammo[x]*Main.ammoMass[x];
		rearmBox.setText("Ammo: " + (int)(100*totalAmmo/target.unit.type.storageSpace) + "%");
		rearmBox.setSelected(target.rearm);
	}
}