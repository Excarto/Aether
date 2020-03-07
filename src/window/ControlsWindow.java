import java.awt.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;

public class ControlsWindow extends Window{
	
	private JPanel controlsPanel;
	private ControlPanel panel;
	
	public ControlsWindow(){
		super(Size.NORMAL);
		
		controlsPanel = new JPanel();
		controlsPanel.setPreferredSize(new Dimension(640, 500));
		controlsPanel.setBorder(BorderFactory.createEtchedBorder());
		
		for (Control control : Control.values()){
			int mappedCode = -1;
			for (Integer code : Main.controlVals.keySet()){
				if (Main.controlVals.get(code) == control)
					mappedCode = code;
			}
			controlsPanel.add(new ControlPanel(control, mappedCode));
		}
		
		JPanel exitPanel = new JPanel();
		exitPanel.setPreferredSize(new Dimension(900, 100));
		exitPanel.setOpaque(false); //.setBackground(BACKGROUND_COLOR);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setPreferredSize(new Dimension(120, 35));
		cancelButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Main.removeWindow();
		}});
		JButton saveButton = new JButton("Save");
		saveButton.setPreferredSize(new Dimension(120, 35));
		saveButton.addActionListener(new OKListener());
		exitPanel.add(cancelButton);
		exitPanel.add(saveButton);
		
		this.addKeyListener(new InputListener());
		Main.setDispatcherEnabled(true);
		
		this.add(new Title("Keyboard Controls", 900, 100));
		this.add(controlsPanel);
		this.add(exitPanel);
	}
	
	public void suspend(){
		Main.setDispatcherEnabled(false);
	}
	
	private class ControlPanel extends JPanel{
		final Control control;
		private int code;
		private JLabel title;
		private JButton setControl;
		
		public ControlPanel(Control control, int code){
			this.control = control;
			this.code = code;
			this.setPreferredSize(new Dimension(150, 64));
			FlowLayout noBorder = new FlowLayout();
			noBorder.setHgap(1);
			noBorder.setVgap(1);
			this.setLayout(noBorder);
			this.setBorder(BorderFactory.createEtchedBorder());
			
			title = new JLabel(control.description);
			title.setFont(Main.getDefaultFont(11));
			title.setPreferredSize(new Dimension(146, 20));
			title.setHorizontalAlignment(JLabel.CENTER);
			
			setControl = new JButton(code == -1 ? "" : KeyEvent.getKeyText(code));
			setControl.setPreferredSize(new Dimension(105, 30));
			setControl.addActionListener(new SetListener());
			
			this.add(title);
			this.add(setControl);
		}
		
		public int getCode(){
			return code;
		}
		
		public void setCode(int code){
			this.code = code;
			setControl.setText(KeyEvent.getKeyText(code));
		}
		
		private class SetListener implements ActionListener{
			public void actionPerformed(ActionEvent e){
				if (panel != null)
					panel.setControl.setText(KeyEvent.getKeyText(panel.getCode()));
				panel = ControlPanel.this;
				setControl.setText("Press Key...");
			}
		}
	}
	
	private class InputListener implements KeyListener{
		public void keyPressed(KeyEvent e){
			if (panel != null){
				panel.setCode(e.getKeyCode());
				panel = null;
			}
		}
		
		public void keyTyped(KeyEvent e){}
		public void keyReleased(KeyEvent e){}
	}
	
	private class OKListener implements ActionListener{
		public void actionPerformed(ActionEvent e){
			File file = new File(Main.saveDir + "/controls.txt");
			try{
				file.delete();
				PrintWriter writer = new PrintWriter(new FileWriter(file));
				
				java.awt.Component[] components = controlsPanel.getComponents();
				for (int x = 0; x < components.length; x++){
					if (components[x] instanceof ControlPanel)
						writer.println(((ControlPanel)components[x]).control.name() + "=" + 
								((ControlPanel)components[x]).getCode());
				}
				writer.close();
				
				Main.readControls();
			}catch(IOException ex){
				Main.crash(file.getPath());
			}
			Main.removeWindow();
		}
	}
}
