import java.io.*;

// Menu giving list of currently existing Pilots for the single-player campaign,
// and an option to create a new one

public class CreatePilotMenu extends Menu{
	
	Pilot pilot;
	boolean writeError;
	
	public CreatePilotMenu(){
		pilot = null;
		writeError = false;
	}
	
	public void start(){
		try{
			pilot = new Pilot();
		}catch (IOException ex){}
		setOptions();
		super.start();
	}
	
	public void returnValue(Object value){
		if (value != null){
			pilot = (Pilot)value;
			setOptions();
		}
	}
	
	private void setOptions(){
		clearOptions();
		
		if (pilot != null){
			this.addOption(new Option(pilot.getRank().abbreviation + " " + pilot.name){
				public void act(){
					Menu menu = new MissionMenu(pilot);
					Main.addWindow(menu);
					menu.start();
			}});
		}
		
		this.addOption(new Option("New Pilot"){
			public void act(){
				Main.addWindow(new CreatePilotWindow(pilot));
		}});
		
		this.addOption(new Option("Back"){
			public void act(){
				Main.removeWindow();
		}});
	}
	
}
