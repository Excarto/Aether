import javax.swing.*;
import java.awt.*;

public class BoosterPanel extends SystemPanel{
	
	Booster booster;
	JLabel fuelLabel;
	
	public BoosterPanel(){
		super();
		
		fuelLabel = new JLabel();
		fuelLabel.setPreferredSize(new Dimension(150, 25));
		
		this.add(fuelLabel);
	}
	
	protected void refresh(){
		super.refresh();
		
		fuelLabel.setText("Fuel Remaining: " + ((int)((double)booster.getFuel()/
					booster.type.lifetime*100)) + "% (" + (booster.getFuel()/Main.TPS) + "s)");
	}
	
	public void setSystem(Booster booster){
		super.setSystem(booster);
		this.booster = booster;
	}
	
}
