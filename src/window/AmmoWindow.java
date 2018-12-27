import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public final class AmmoWindow extends Window{
	static final int BAR_HEIGHT = 58;
	
	private final double[] ammoRatios;
	private final double ammoSpace;
	
	private final AmmoBar[] ammoBars;
	private int ammoToDecrease;
	
	public AmmoWindow(double[] ammoRatios, double ammoSpace){
		super(true);
		this.ammoRatios = ammoRatios;
		this.ammoSpace = ammoSpace;
		ammoToDecrease = 0;
		
		JPanel ammoPanel = new JPanel();
		ammoBars = new AmmoBar[Main.ammoMass.length];
		for (int x = 0; x < ammoBars.length; x++)
			ammoPanel.add(ammoBars[x] = new AmmoBar(x));
		ammoPanel.setPreferredSize(new Dimension(280, 5+(BAR_HEIGHT+2)*ammoBars.length));
		
		JScrollPane pane = new JScrollPane(ammoPanel);
		pane.setPreferredSize(new Dimension(300, 600));
		pane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		//pane.getViewport().add(ammoPanel);
		
		JPanel exitPanel = new JPanel();
		exitPanel.setPreferredSize(new Dimension(900, 45));
		exitPanel.setOpaque(false); //.setBackground(BACKGROUND_COLOR);
		JButton exitButton = new JButton("Finish");
		exitButton.setPreferredSize(new Dimension(120, 35));
		exitButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Main.removeWindow();
				Main.getCurrentWindow().returnValue(null);
		}});
		exitPanel.add(exitButton);
		
		//JLabel titleLabel = new JLabel("Manual Ammo", JLabel.CENTER);
		//titleLabel.setPreferredSize(new Dimension(900, 65));
		//titleLabel.setFont(new Font("Courier", Font.BOLD, 25));
		
		this.add(new Title("Manual Ammunition Settings", 900, 80));
		this.add(pane);
		this.add(exitPanel);
	}
	
	private class AmmoBar extends JPanel{
		private final JSlider slider;
		private final JLabel shots, percent;
		
		public final int type;
		private boolean decreaseCalled;
		
		public AmmoBar(int type){
			super(DEFAULT_LAYOUT);
			
			this.type = type;
			this.setPreferredSize(new Dimension(250, BAR_HEIGHT));
			this.setBorder(BorderFactory.createRaisedBevelBorder());
			this.setLayout(Window.DEFAULT_LAYOUT);
			decreaseCalled = false;
			
			slider = new JSlider(0, 100, (int)(ammoRatios[type]*100));
			slider.setPreferredSize(new Dimension(230, 16));
			slider.addChangeListener(new AmmoSliderListener());
			
			shots = new JLabel((int)(ammoRatios[type]*ammoSpace/Main.ammoMass[type]) + " shots");
			shots.setFont(new Font("Courier", Font.PLAIN, 12));
			percent = new JLabel("   " + slider.getValue() + "%   ");
			percent.setFont(new Font("Courier", Font.PLAIN, 12));
			
			String titleString = "";
			for (WeaponType weaponType : Main.weaponTypes)
				if (weaponType.ammoType == type){
					if (titleString.length() > 0)
						titleString += ", ";
					titleString += weaponType.name;
				}
			
			JLabel title = new JLabel(titleString);
			title.setFont(new Font("Courier", Font.PLAIN, 12));
			
			this.add(title);
			this.add(slider);
			this.add(percent);
			this.add(new JLabel("  "));
			this.add(shots);
		}
		
		public void decrease(){
			decreaseCalled = true;
			slider.setValue(slider.getValue()-1);
			ammoRatios[type] = slider.getValue()*0.01;
		}
		
		private class AmmoSliderListener implements ChangeListener{
			public void stateChanged(ChangeEvent e){
				ammoRatios[type] = slider.getValue()*0.01;
				percent.setText("   " + slider.getValue() + "%   ");
				shots.setText((int)(ammoRatios[type]*ammoSpace/Main.ammoMass[type]) + " shots");
				
				if (!decreaseCalled){
					double total = 0;
					for (int x = 0; x < ammoRatios.length; x++)
						total += ammoRatios[x];
					
					while (total > 1){
						ammoToDecrease++;
						if (ammoToDecrease%ammoBars.length != type){
							total = 0;
							ammoBars[ammoToDecrease%ammoBars.length].decrease();
							for (int x = 0; x < ammoRatios.length; x++)
								total += ammoRatios[x];
						}
					}
				}else
					decreaseCalled = false;
			}
		}
	}
}
