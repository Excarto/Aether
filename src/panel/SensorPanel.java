import static java.lang.Math.*;
import javax.swing.*;

public class SensorPanel extends SystemPanel{
	
	JLabel refreshTimeLabel;
	//JSlider timeToLive;
	//JLabel timeToLiveLabel;
	Sensor sensor;
	
	public SensorPanel(){
		super();
		
		refreshTimeLabel = new JLabel();
		//refreshTimeLabel.setPreferredSize(new Dimension(conditionPanel.getWidth()-4, 20));
		conditionPanel.add(refreshTimeLabel);
		
		/*timeToLive = new JSlider(20, 500, 100);
		timeToLive.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				timeToLiveLabel.setText("Ping Time To Live: " + timeToLive.getValue() + "%");
				sensor.timeToLive = (int)(timeToLive.getValue()/100.0*sensor.type.refreshPeriod);
			}
		});
		
		timeToLiveLabel = new JLabel();
		timeToLiveLabel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH, 20));
		timeToLiveLabel.setHorizontalAlignment(JLabel.CENTER);
		
		JPanel timeToLivePanel = new JPanel();
		timeToLivePanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH, 60));
		timeToLivePanel.add(timeToLive);
		timeToLivePanel.add(timeToLiveLabel);*/
		
		//this.add(timeToLivePanel);
	}
	
	protected void refresh(){
		super.refresh();
		if (sensor != null)
			refreshTimeLabel.setText(round(100*(1-(double)sensor.getTimeToRefresh()/sensor.type.refreshPeriod)) + "% Charged");
	}
	
	public void setSystem(Sensor sensor){
		super.setSystem(sensor);
		this.sensor = sensor;
		//timeToLive.setValue((int)round(sensor.timeToLive*100.0/sensor.type.refreshPeriod));
		//timeToLive.getChangeListeners()[0].stateChanged(null);
	}
}
