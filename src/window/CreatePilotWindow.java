import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class CreatePilotWindow extends Window{
	
	private static final char[] VALID_NAME_CHARS =
			" abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_+-".toCharArray();
	
	JPanel panel;
	JTextField nameField;
	JRadioButton[] typeButtons;
	//JComboBox<Pilot.Type> typeList;
	
	public CreatePilotWindow(Pilot existingPilot){
		super(Size.SMALL);
		
		Dimension fieldDim = new Dimension(158, 22);
		Dimension labelDim = new Dimension(90, 45);
		
		nameField = new JTextField("Spaceman");
		nameField.setPreferredSize(fieldDim);
		Utility.setLengthLimit(nameField, 14);
		JLabel nameLabel = new JLabel("Name:");
		nameLabel.setPreferredSize(labelDim);
		nameLabel.setHorizontalAlignment(JLabel.RIGHT);
		
		//typeList = new JComboBox<>(Pilot.Type.values());
		//typeList.setPreferredSize(fieldDim);
		JLabel typeLabel = new JLabel("");
		typeLabel.setPreferredSize(labelDim);
		typeLabel.setHorizontalAlignment(JLabel.RIGHT);
		
		ButtonGroup buttonGroup = new ButtonGroup();
		JPanel typePanel = new JPanel();
		typePanel.setPreferredSize(new Dimension(fieldDim.width, 66));
		typeButtons = new JRadioButton[Pilot.Type.values().length];
		for (int i = 0; i < Pilot.Type.values().length; i++){
			JRadioButton button = new JRadioButton(Pilot.Type.values()[i].name);
			buttonGroup.add(button);
			typePanel.add(button);
			button.setPreferredSize(new Dimension(fieldDim.width-6, 17));
			typeButtons[i] = button;
		}
		typeButtons[0].setSelected(true);
		
		panel = new JPanel();
		panel.setPreferredSize(new Dimension(320, 140));
		panel.setBorder(BorderFactory.createEtchedBorder());
		panel.add(nameLabel);
		panel.add(nameField);
		panel.add(typeLabel);
		//panel.add(typeList);
		panel.add(typePanel);
		
		JPanel exitPanel = new JPanel();
		exitPanel.setPreferredSize(new Dimension(900, 100));
		exitPanel.setOpaque(false);
		JButton backButton = new JButton("Back");
		backButton.setPreferredSize(new Dimension(100, 28));
		backButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Main.removeWindow();
				Main.getCurrentWindow().returnValue(null);
		}});
		JButton doneButton = new JButton("Done");
		doneButton.setPreferredSize(new Dimension(100, 28));
		doneButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Pilot pilot = getPilot();
				if (pilot != null){
					Main.removeWindow();
					Main.getCurrentWindow().returnValue(pilot);
				}
		}});
		
		JLabel warningLabel = new JLabel("");
		warningLabel.setPreferredSize(new Dimension(exitPanel.getPreferredSize().width-50, 25));
		warningLabel.setHorizontalAlignment(JLabel.CENTER);
		if (existingPilot != null)
			warningLabel.setText("This will delete the existing pilot");
		exitPanel.add(warningLabel);
		exitPanel.add(backButton);
		exitPanel.add(doneButton);
		
		this.add(new Title("Create Pilot", 900, 90));
		this.add(panel);
		this.add(exitPanel);
	}
	
	private Pilot getPilot(){
		String name = Utility.filter(nameField.getText().trim(), VALID_NAME_CHARS);
		if (name.isEmpty())
			return null;
		//Pilot.Type type = typeList.getItemAt(typeList.getSelectedIndex());
		
		Pilot.Type type = null;
		for (int i = 0; i < typeButtons.length; i++){
			if (typeButtons[i].isSelected())
				type = Pilot.Type.values()[i];
		}
		
		Pilot pilot = new Pilot(name, type);
		return pilot;
	}
	
}
