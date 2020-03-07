
public class MissionThree extends Mission{
	
	public MissionThree(){
		super("Mission III", "battlemedium", "outfoxingthefox",
				7, 22, 0.9, -1.0,
				new BackgroundGenerator.Planet[]{Arena.background.new Planet(5, -1, -0.45, -0.1, 0.30)});
		
		addPlayer(1, 0);
		addPlayer(2, 0);
	}
	
	protected void initialize(Pilot pilot){
		
		Ship ship1 = readShip("atlas_1");
		addShip(0, ship1);
		Ship ship2 = readShip("hades_1");
		addShip(0, ship2);
		Ship ship3 = readShip("ares_cobra");
		addShip(0, ship3);
		
		Ship enemyShip1 = readShip("atlas_2");
		addShip(1, enemyShip1);
		Ship enemyShip2 = readShip("hyperion_1");
		addShip(1, enemyShip2);
		Ship enemyShip3 = readShip("hermes_1");
		addShip(1, enemyShip3);
		Ship enemyShip4 = readShip("apollo_repair");
		addShip(1, enemyShip4);
	}
	
	public BriefMenu.BriefState createBriefing(BriefMenu menu, Pilot pilot){
		
		String tutor = MissionConstants.TUTOR;
		
		BriefMenu.BriefState page1 = menu.new BriefState(
				MissionConstants.TUTOR_LONG + " greets you in the briefing room. " +
				"\n \n " +
				"\"I'm sure you won't let us down on this next mission, err..., " + pilot.getRank().name + " " + pilot.name + "\", " +
				"he says, glancing at his datascreen. \n " +
				"\"Your friend " + MissionConstants.PARTNER + " won't be joining you this time.\""
		);
		
		BriefMenu.BriefState page2 = menu.new BriefState(
				"\"Yes, yes, very good,\" he says distractedly. " +
				"\"Oh, and it looks like you have a carrier at your disposal for this one. " +
				"The crafts that it deploys have limited ammo and energy, so they will probably need to return to the mothership to rearm.\""
		);
		
		BriefMenu.BriefState page3 = menu.new BriefState(
				tutor + " has already left the room."
		);
		
		page1.addOption("\"I would not call " + MissionConstants.PARTNER + " my 'friend'\"", page2);
		page1.addOption("\"That is most unfortunate.\"", page2);
		
		page2.addOption("\"How do I deploy and retrieve the crafts from the carrier?\"", page3);
		page2.addOption("\"I won't let you down, sir.\"", page3);
		if (pilot.type == Pilot.Type.FEMALE)
			page2.addOption("\"Any plans tonight, Lieutenant? Maybe you and I could share a drink or two after I get back.\"", page3);
		
		page3.addStartOption("Start");
		
		return page1;
	}
	
}
