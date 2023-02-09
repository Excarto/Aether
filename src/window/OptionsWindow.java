import static java.lang.Math.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.Map;
import java.util.*;
import java.io.*;

// Window giving options that can be configured in-game, and saving them to the options.txt file

public class OptionsWindow extends Window{
	static final int OPTION_HEIGHT = 32, OPTION_WIDTH = 320, LABEL_WIDTH = 140;
	
	JButton cancelButton, okButton;
	
	Map<String, String> options;
	Map<String, JComponent> components;
	
	boolean resetRequired;
	
	public OptionsWindow(){
		super(Size.NORMAL);
		
		try{
			options = Utility.readDataFile(Main.saveDir + "/options.txt");
		}catch (IOException ex){
			ex.printStackTrace();
			Main.crash(Main.saveDir + "/options.txt");
		}
		components = new HashMap<String, JComponent>();
		resetRequired = false;
		
		OptionPanel gamePanel = new OptionPanel("Game");
		gamePanel.addOption("Player Name", "username", new JTextField(){
			public void setName(String name){
				setText(name);
			}
			public String toString(){
				return getText();
			}
		});
		String[] difficulties = new String[]{"Normal", "Hard"};
		gamePanel.addOption("Difficulty", "difficulty", new JComboBox<String>(difficulties){
			public void setName(String val){
				this.setSelectedIndex(Integer.parseInt(val));
			}
			public String toString(){
				return String.valueOf(getSelectedIndex());
			}
		});
		
		OptionPanel netPanel = new OptionPanel("Network");
		netPanel.addOption("Server Port", "server_listen_port", new JTextField(){
			public void setName(String val){
				setText(val);
				setInputVerifier(new PortVerifier());
			}
			public String toString(){
				return getText();
			}
		});
		netPanel.addOption("Client Port", "client_port", new JTextField(){
			public void setName(String val){
				setText(val);
				setInputVerifier(new PortVerifier());
			}
			public String toString(){
				return getText();
			}
		});
		netPanel.addOption("UDP Port", "udp_port", new JTextField(){
			public void setName(String val){
				setText(val);
				setInputVerifier(new PortVerifier());
			}
			public String toString(){
				return getText();
			}
		});
		netPanel.addOption("LAN Broadcast Port", "server_broadcast_port", new JTextField(){
			public void setName(String val){
				setText(val);
				setInputVerifier(new PortVerifier());
			}
			public String toString(){
				return getText();
			}
		});
		netPanel.addOption("Use Only TCP", "force_tcp", new JCheckBox(){
			public void setName(String val){
				setSelected(Boolean.parseBoolean(val));
			}
			public String toString(){
				return String.valueOf(isSelected());
			}
		});
		netPanel.addOption("Enable UPnP", "enable_upnp", new JCheckBox(){
			public void setName(String val){
				setSelected(Boolean.parseBoolean(val));
			}
			public String toString(){
				return String.valueOf(isSelected());
			}
		});
		netPanel.addOption("Internet Lobby Server", "lobby_server", new JTextField(){
			public void setName(String val){
				setText(val);
			}
			public String toString(){
				return getText();
			}
		});
		
		OptionPanel soundPanel = new OptionPanel("Sound");
		soundPanel.addOption("Master Volume", "master_volume", new JSlider(0, 100){
			public void setName(String val){
				setValue((int)(Double.parseDouble(val)*100));
			}
			public String toString(){
				return String.valueOf(getValue()/100.0);
			}
		});
		soundPanel.addOption("Music Volume", "music_volume", new JSlider(0, 100){
			public void setName(String val){
				setValue((int)(Double.parseDouble(val)*100));
			}
			public String toString(){
				return String.valueOf(getValue()/100.0);
			}
		});
		soundPanel.addOption("Audio Channels", "audio_channels", new JSlider(4, 16){
			public void setName(String val){
				Hashtable<Integer,JLabel> table = new Hashtable<Integer,JLabel>();
				table.put(4, new JLabel("4"));
				table.put(8, new JLabel("8"));
				table.put(12, new JLabel("12"));
				table.put(16, new JLabel("16"));
				setLabelTable(table);
				setPaintLabels(true);
				setValue(Integer.parseInt(val));
			}
			public String toString(){
				return String.valueOf(getValue());
			}
		});
		
		OptionPanel videoPanel = new OptionPanel("Video");
		String[] sizes = new String[Main.displayModes.length];
		for (int x = 0; x < sizes.length; x++)
			sizes[x] = Main.displayModes[x].getWidth() + "x" + Main.displayModes[x].getHeight();
		videoPanel.addOption("Screen Resolution", "screen_size", new JComboBox<String>(sizes){
			{
				this.addActionListener(new ResetListener());
			}
			public void setName(String val){
				this.setSelectedItem(val);
			}
			public String toString(){
				return (String)getSelectedItem();
			}
		});
		videoPanel.addOption("Hardware Acceleration", "use_hardware_accel", new JCheckBox(){
			{
				this.addActionListener(new ResetListener());
			}
			public void setName(String val){
				setSelected(Boolean.parseBoolean(val));
			}
			public String toString(){
				return String.valueOf(isSelected());
			}
		});
		String[] modes = new String[]{"fullscreen", "window"};//, "window"};
		videoPanel.addOption("Display Mode", "window_mode", new JComboBox<String>(modes){
			{
				this.addActionListener(new ResetListener());
			}
			public void setName(String val){
				this.setSelectedItem(val);
			}
			public String toString(){
				return (String)getSelectedItem();
			}
		});
		
		OptionPanel graphicsPanel = new OptionPanel("Graphics");
		graphicsPanel.addOption("Antialiasing", "antialiasing", new JCheckBox(){
			public void setName(String val){
				setSelected(Boolean.parseBoolean(val));
			}
			public String toString(){
				return String.valueOf(isSelected());
			}
		});
		graphicsPanel.addOption("High Image Quality", "scaling_quality", new JCheckBox(){
			public void setName(String val){
				setSelected(Integer.parseInt(val) > 1);
			}
			public String toString(){
				return String.valueOf(isSelected() ? 3 : 2);
			}
		});
		graphicsPanel.addOption("Num. Render Angles", "render_angles_multiplier", new JSlider(1, 4){
			public void setName(String val){
				Hashtable<Integer,JLabel> table = new Hashtable<Integer,JLabel>();
				table.put(1, new JLabel("Low"));
				table.put(4, new JLabel("High"));
				setLabelTable(table);
				setPaintLabels(true);
				setValue(Integer.parseInt(val));
			}
			public String toString(){
				return String.valueOf(getValue());
			}
		});
		/*graphicsPanel.addOption("Frames Per Second", "frames_per_sec", new JSlider(){
			int minVal, maxVal;
			public void setName(String val){
				minVal = 1;
				while (getFramesPerSec(minVal, 1.0) > 80)
					minVal += 1;
				maxVal = 20;
				while (getFramesPerSec(maxVal, 1.0) < 20)
					maxVal -= 1;
				Hashtable<Integer,JLabel> table = new Hashtable<Integer,JLabel>();
				for (int x = 0; x <= maxVal-minVal; x++)
					table.put(x, new JLabel(String.valueOf(getFramesPerSec(maxVal-x, 1.0))));
				setMaximum(maxVal-minVal);
				setMinimum(0);
				setLabelTable(table);
				setPaintLabels(true);
				setValue(maxVal-getTurnsPerFrame(Main.options.framesPerSec, 1.0));
			}
			public String toString(){
				return String.valueOf(getFramesPerSec(maxVal-getValue(), 1.0));
			}
		});*/
		graphicsPanel.addOption("Unit Status Size", "unit_status_size", new JSlider(10, 30){
			public void setName(String val){
				setMajorTickSpacing(2);
				setSnapToTicks(true);
				setValue(Integer.parseInt(val));
			}
			public String toString(){
				return String.valueOf(getValue());
			}
		});
		graphicsPanel.addOption("Amount of Debris", "debris_amount", new JSlider(0, 75){
			public void setName(String val){
				setValue((int)(Double.parseDouble(val)*100));
			}
			public String toString(){
				return String.valueOf(getValue()/100.0);
			}
		});
		graphicsPanel.addOption("Zoom Render Scaling", "render_scaling", new JSlider(75, 100){
			public void setName(String val){
				Hashtable<Integer,JLabel> table = new Hashtable<Integer,JLabel>();
				table.put(80, new JLabel("Enlarged"));
				table.put(100, new JLabel("True"));
				setLabelTable(table);
				setPaintLabels(true);
				setValue((int)(Double.parseDouble(val)*100));
			}
			public String toString(){
				return String.valueOf(getValue()/100.0);
			}
		});
		
		
		OptionPanel controlsPanel = new OptionPanel("Controls");
		controlsPanel.addOption("Camera Scroll Speed", "camera_scroll_speed", new JSlider(200, 1000){
			public void setName(String val){
				setValue(Integer.parseInt(val));
			}
			public String toString(){
				return String.valueOf(getValue());
			}
		});
		controlsPanel.addOption("Camera Acceleration", "camera_accel_rate", new JSlider(20, 100){
			public void setName(String val){
				setValue(Integer.parseInt(val));
			}
			public String toString(){
				return String.valueOf(getValue());
			}
		});
		
		JPanel exitPanel = new JPanel();
		exitPanel.setPreferredSize(new Dimension(900, 100));
		exitPanel.setOpaque(false);
		cancelButton = new JButton("Cancel");
		cancelButton.setPreferredSize(new Dimension(120, 35));
		cancelButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Main.removeWindow();
		}});
		okButton = new JButton("Save");
		okButton.setPreferredSize(new Dimension(120, 35));
		okButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				saveFile();
		}});
		exitPanel.add(cancelButton);
		exitPanel.add(okButton);
		
		JPanel optionPanel = new JPanel();
		optionPanel.setOpaque(false);
		optionPanel.add(gamePanel);
		optionPanel.add(controlsPanel);
		optionPanel.add(videoPanel);
		optionPanel.add(soundPanel);
		optionPanel.add(graphicsPanel);
		optionPanel.add(netPanel);
		
		int panelCount = 0;
		int totalHeight = 0;
		OptionPanel rightPanel = null;
		for (int x = 0; x < optionPanel.getComponentCount(); x++){
			if (optionPanel.getComponent(x) instanceof OptionPanel){
				OptionPanel panel = (OptionPanel)optionPanel.getComponent(x);
				panelCount++;
				if (panelCount%2 == 0){
					int numRowOptions = max(panel.numOptions, rightPanel.numOptions);
					int rowHeight = (OPTION_HEIGHT+6)*numRowOptions+34;
					panel.setPreferredSize(new Dimension(OPTION_WIDTH, rowHeight));
					rightPanel.setPreferredSize(new Dimension(OPTION_WIDTH, rowHeight));
					totalHeight += rowHeight+6;
				}else
					rightPanel = panel;
			}
		}
		optionPanel.setPreferredSize(new Dimension(700, totalHeight));
		
		this.add(new Title("Settings", 900, 65));
		this.add(optionPanel);
		this.add(exitPanel);
	}
	
	private void saveFile(){
		for (String option : components.keySet())
			options.put(option, components.get(option).toString());
		
		String filename = Main.options.file;
		File file = new File(filename);
		try{
			file.delete();
			PrintWriter writer = new PrintWriter(file);
			for (String option : options.keySet())
				writer.println(option + "= " + options.get(option));
			writer.close();
			
			if (resetRequired)
				Main.exit();
			
			Main.options = new Options(filename);
			Sound.initialize(Main.options.audioChannels, Main.options.masterVolume);
			
			Main.removeWindow();
		}catch(IOException ex){
			okButton.setText("Cannot save!");
		}
	}
	
	private class OptionPanel extends JPanel{
		int numOptions;
		
		public OptionPanel(String title){
			setBorder(BorderFactory.createEtchedBorder());
			
			JLabel label = new JLabel(title);
			label.setPreferredSize(new Dimension(OPTION_WIDTH, 24));
			label.setFont(new Font("Courier", Font.BOLD, 18));
			label.setHorizontalAlignment(JLabel.CENTER);
			this.add(label);
		}
		
		public void addOption(String optionTitle, String optionName, JComponent component){
			numOptions++;
			String value = options.get(optionName);
			if (value == null){
				Main.crash(optionName+" does not exist in "+"data/options.txt");
			}
			JLabel titleLabel = new JLabel(optionTitle+"   ");
			titleLabel.setPreferredSize(new Dimension(LABEL_WIDTH, OPTION_HEIGHT));
			titleLabel.setHorizontalAlignment(JLabel.RIGHT);
			component.setPreferredSize(new Dimension(OPTION_WIDTH-LABEL_WIDTH-34, OPTION_HEIGHT));
			component.setName(value);
			component.setToolTipText(component.getToolTipText());
			titleLabel.setToolTipText(component.getToolTipText());
			this.add(titleLabel);
			this.add(component);
			components.put(optionName, component);
		}
	}
	
	private class ResetListener implements ActionListener{
		public void actionPerformed(ActionEvent e){
			if (okButton != null){
				resetRequired = true;
				okButton.setText("Save and exit");
			}
		}
	}
	
	public class PortVerifier extends InputVerifier{
		public boolean verify(JComponent input){
			boolean verified = true;
			try{
				if (Integer.parseInt(((JTextField)input).getText()) >= 65536)
					verified = false;
			}catch (NumberFormatException e){
				verified = false;
			}
			okButton.setEnabled(verified);
			return verified;
		}
	}
}
