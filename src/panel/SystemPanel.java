import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

public class SystemPanel extends JPanel{
	
	System system;
	JLabel typeLabel;
	JCheckBox engaged;
	DetailImage img;
	ComponentsPanel.DamageLabel damage;
	JPanel conditionPanel;
	
	public SystemPanel(){
		this.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-4, 285));
		this.setOpaque(false);
		this.setBorder(BorderFactory.createEtchedBorder());
		
		typeLabel = new JLabel();
		typeLabel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-12, 13));
		typeLabel.setFont(new Font("Arial", Font.BOLD, 12));
		typeLabel.setHorizontalAlignment(JLabel.CENTER);
		
		conditionPanel = new JPanel(Window.DEFAULT_LAYOUT);
		conditionPanel.setPreferredSize(new Dimension(140, 50));
		conditionPanel.setOpaque(false);
		img = new DetailImage();
		damage = Unit.systemsPanel.new DamageLabel();
		damage.setPreferredSize(new Dimension(125, 18));
		conditionPanel.add(damage);
		
		engaged = new JCheckBox("Engaged");
		engaged.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				engaged.setSelected(system.setEngaged(engaged.isSelected()));
		}});
		engaged.setPreferredSize(new Dimension(100, 15));
		engaged.setOpaque(false);
		engaged.setHorizontalAlignment(JCheckBox.CENTER);
		
		this.add(typeLabel);
		this.add(conditionPanel);
		this.add(img);
		this.add(engaged);
	}
	
	public void setSystem(System system){
		this.system = system;
		typeLabel.setText(system.type.typeClass);
		engaged.setSelected(system.isEngaged());
		img.select(system.type);
	}
	
	protected void refresh(){
		if (system != null && engaged.isSelected() != system.isEngaged())
			engaged.setSelected(system.isEngaged());
		damage.refresh();
	}
}
