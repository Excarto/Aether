
public class MissionZero extends Mission{

	public MissionZero(){
		super("Introduction", "missionzero", "mtheory",
				0, -1, 0.0, 0.0,
				new BackgroundGenerator.Planet[]{});
		
		addPlayer(1, 0);
		addPlayer(2, 0);
	}
	
	protected void initialize(Pilot pilot){
		
		addShip(0, readShip("ares1"));
		addShip(0, readShip("hermes1"));
		
		addShip(1, readShip("atlas1"));
		
		String tutor = MissionConstants.TUTOR_LONG + ":  ";
		String recording = MissionConstants.AUTO_TEACH + ":  ";
		
		queueText(tutor + "Welcome, cadet", 10, 5);
		queueText(tutor + "to SPACE", 3, 5);
		queueText(tutor + "Actually, this is just the simulator.", 4, 5);
		queueText(tutor + "There should be some hostile targets somewhere, so go ahead and blow those up.", 5, 8);
		queueText(tutor + "Or don't. You don't actually have to.", 3, 5);
		queueText(tutor + "I'm just gonna start the recording.", 5, 5);
		
		queueText(recording + "Welcome! And congratulations on your exciting new career choice!", 6, 8);
		queueText(recording + "This interface is identical to the one you'll use in the command vessel.", 0, 8);
		
		queueText(recording + "Right click a point in space to order your selected units to move to that location.", 10, 15);
		queueText(recording + "Double right click to order them to fly past that location.", 0, 15);
		
		queueText(recording + "You can use WASD to move the camera, or arrow keys to accelerate the camera.", 16, 15);
		queueText(recording + "Use the middle mouse button to center the camera on an object.", 0, 15);
		
		queueText(recording + "The larger circle around your units indicates maximum sensor range, and the smaller circle is maximum vision range.", 16, 8);
		
		queueText(recording + "You can take manual control of a single unit by double left clicking it.", 9, 25);
		queueText(recording + "When in manual control mode, right click a point in space to order the unit to face that direction.", 0, 25);
		queueText(recording + "The W key will accelerate forward.", 0, 25);
		
		queueText(recording + "You can set the target of your selected units by right clicking an enemy unit.", 26, 25);
		queueText(recording + "If your unit has no other orders, it will turn to face its weapons to the target.", 0, 25);
		queueText(recording + "If a unit has no target, or its target is out of range, it will shoot at any nearby enemy. Missiles will only fire at the unit's target.", 0, 25);
		queueText(recording + "To clear a selected unit's orders, press the E key.", 0, 25);
		queueText(recording + "To clear a selected unit's target, press the Q key.", 0, 25);
		
		queueText(recording + "The objective is to capture the station and keep it under control until you have collected enough fuel.", 26, 999);
		queueText(recording + "The current fuel quantity and intake rate for each team is shown in the upper left corner.", 0, 999);
		queueText(recording + "Double right click the station to order your ship to capture it.", 0, 999);
		queueText(recording + "For more detailed information about the interface, bring up the help window using the Esc key.", 0, 999);
	}
	
	public BriefMenu.BriefState createBriefing(BriefMenu menu, Pilot pilot){
		
		BriefMenu.BriefState page1 = menu.new BriefState(
			  	"The factories dotting the charred, uninhabitable landscape far below you were designed to produce transports and supplies needed to colonize the stars. " + 
			  	"They now emit a stream of robotic warships. " +
			  	"\n \n " +
			  	"Life on the surface was wiped out shortly after the war broke out, but the autonomous factories which remain tirelessly extract and " +
			  	"process material from the planet's crust with only remote supervision from the technicians above. " +
			  	"\n \n " +
			  	"You are one such technician, in orbit over your homeworld with the few thousand other survivors of your colony."
		);
		
		BriefMenu.BriefState page2 = menu.new BriefState(
				"Your area of expertise lies in the incorporation of the Kolmogorov mechanism into each newly constructed hull. " +
			  	"This is the device which propels vessels faster than the speed of light, providing the only bridge between your colony and the rest of charted space. " + 
			  	"But this bridge is fleeting: the fuel required for faster-than-light travel is harvested from specific gas clouds, all of which are several lightyears distant, and your colony's supply is dwindling. " +
			  	"\n \n " +
			  	"As is everyone else's."
		);
		
		BriefMenu.BriefState page3 = menu.new BriefState(
				"There are now more warships than pilots to control them, and High Command has issued a request for new volunteers. \n " +
				"You have decided to take up the call. " +
				"\n \n " +
				"You make your way to a part of the station that you have never been to before. The sign in front of you reads \"Combat Operations\". "
		);
		
		BriefMenu.BriefState shameState = menu.new BriefState(
				"Thus ends the story of " + pilot.name + " \n " +
				"as quickly as it began"
		);
		
		page1.addOption("Continue", page2);
		page2.addOption("Continue", page3);
		page3.addOption(menu.new Option("Onward"){
			public void act(){
				pilot.setScore(MissionZero.this, 1);
				pilot.save();
				Main.removeWindow();
				start(pilot);
		}});
		page3.addOption("I'm having second thoughts", shameState);
		shameState.addDeathOption("For shame");
		
		return page1;
	}
	
	public void gameStarted(Game game){
		ComputerPlayer computer = (ComputerPlayer)(game.players.get(game.players.size()-1));
		computer.setAttackDisabled(true);
		computer.setCaptureDisabled(true);
		
		game.getHuman().getWindow().setGridEnabled(true);
	}
	
	public String getEndString(){
		return "Skip tutorial";
	}
	
}
