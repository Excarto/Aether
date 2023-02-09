import static java.lang.Math.*;
import java.awt.Dimension;
import java.awt.*;
import javax.swing.*;

// Side planel shown when a missile is selected

public class MissilePanel extends SidePanel{
	private Missile missile;
	
	private JLabel title;
	private JLabel ETILabel;
	private JLabel fuelLeft, timeLeft;
	
	public MissilePanel(){
		super();
		
		FlowLayout noBorder = new FlowLayout();
		noBorder.setHgap(0);
		noBorder.setVgap(0);
		this.setLayout(noBorder);
		
		title = new JLabel();
		title.setFont(new Font("Courier", Font.BOLD, 18));
		title.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-30, 20));
		this.add(title);
		
		ETILabel = new JLabel();
		ETILabel.setPreferredSize(new Dimension(200, 20));
		ETILabel.setFont(new Font("Courier", Font.BOLD, 12));
		this.add(ETILabel);
		
		fuelLeft = new JLabel();
		fuelLeft.setPreferredSize(new Dimension(200, 20));
		fuelLeft.setFont(new Font("Courier", Font.BOLD, 12));
		this.add(fuelLeft);
		
		timeLeft = new JLabel();
		timeLeft.setPreferredSize(new Dimension(200, 20));
		timeLeft.setFont(new Font("Courier", Font.BOLD, 12));
		this.add(timeLeft);
	}
	
	public void refresh(){
		if (missile.orders().getOrder() instanceof Impact){
			double eti = ((Impact)missile.orders().getOrder()).getETA();
			if (Double.isFinite(eti)){
				ETILabel.setText("ETI: " + (double)round(eti/Main.TPS*10)/10 + "s");
			}else
				ETILabel.setText("ETI: Never");
		}else
			ETILabel.setText("ETI: N/A");
		ETILabel.repaint();
		
		fuelLeft.setText("Fuel Remaining: " + (int)(missile.getEnergy()/missile.type.capacitor*100) + "%"
				+ " (" + (double)round(missile.getEnergy()/missile.type.thrust/Main.config.energyPerThrust/Main.TPS*10)/10 + "s)");
		timeLeft.setText("Lifetime Remaining: " + (double)round(missile.getTimeToLive()/Main.TPS*10)/10 + "s");
	}
	
	public void setMissile(Missile newMissile){
		missile = newMissile;
		title.setText(missile.type.name);
		title.repaint();
		this.repaint();
	}
}
