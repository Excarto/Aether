import java.awt.*;

public class MissionMenu extends Menu{
	
	Pilot pilot;
	
	public MissionMenu(Pilot pilot){
		this.pilot = pilot;
	}
	
	public void start(){
		if (pilot.getScore(Main.missions[0]) > 0){
			setOptions();
			super.start();
		}else
			startMissionBrief(Main.missions[0]);
	}
	
	public void resume(){
		if (pilot.getScore(Main.missions[0]) == 0){
			Main.removeWindow();
		}else{
			setOptions();
			super.resume();
		}
	}
	
	private void startMissionBrief(Mission mission){
		pilot.load();
		Mission.playerFleet.clear();
		Menu menu = new BriefMenu(mission, pilot);
		Main.addWindow(menu);
		menu.start();
	}
	
	private void setOptions(){
		clearOptions();
		
		int score = 1;
		for (final Mission mission : Main.missions){
			if (score > 0){
				this.addOption(new Option(mission.name){
					public void act(){
						startMissionBrief(mission);
					}
					public void playSound(){}
				});
			}
			score = pilot.getScore(mission);
		}
		
		this.addOption(new Option("Back"){
			public void act(){
				Main.removeWindow();
		}});
	}
	
	public void paint(Graphics g){
		super.paint(g);
		
		if (pilot.ioFailed()){
			g.setColor(Color.RED);
			g.setFont(Menu.TITLE_FONT);
			g.drawString("Cannot save pilot!", Main.resX/2-110, Main.resY/2-100);
		}
	}
	
}
