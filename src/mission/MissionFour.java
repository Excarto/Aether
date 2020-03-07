
public class MissionFour extends Mission{
	
	public MissionFour(){
		super("Mission IV", "battlemedium", "stay-the-course",
				138613513, 12, 0.75, 0.0,
				new BackgroundGenerator.Planet[]{});
		
		int budget = 2200;
		addPlayer(1, budget);
		addPlayer(2, compBudget(budget));
	}
	
	protected void initialize(Pilot pilot){
		
	}
	
	public BriefMenu.BriefState createBriefing(BriefMenu menu, Pilot pilot){
		
		String commander = MissionConstants.COMMANDER;
		
		BriefMenu.BriefState page1 = menu.new BriefState(
				"You drift down a corridor with a large observation window running along one wall. " +
				"\n \n " +
				"The dark void of your home planet's night surface dominates the view, broken only by yellow patches of still-burning fires. " +
				"As the orbiting station perpetually plummets toward the black disk, the curve of the horizon slides silently across the sky, consuming in sequence each tiny star in its wake. " +
				"\n \n " +
				"The stars make no perceptible effort to resist their fate."
		);
		
		BriefMenu.BriefState page2 = menu.new BriefState(
				"The window blocks your path."
		);
		
		BriefMenu.BriefState page3 = menu.new BriefState(
				"You arrive at an open doorway labeled \"" + MissionConstants.COMMANDER_LONG.toUpperCase() + "\" and pull yourself through the passage into a well-organized office. " +
				"\n \n " +
				"\"Congratulations on your promotion, " + pilot.getRank().name + " " + pilot.name + ",\" " +
				commander + " greets you, looking up from her infoscreen. " +
				"\"I am granting you access to the fleet construction facilities. You will now be able to " +
				"choose and equip vessels as you see fit for the mission.\""
		);
		
		BriefMenu.BriefState page4a = menu.new BriefState(
				commander + " quickly salutes you and returns her attention to her infoscreen."
		);
		
		BriefMenu.BriefState page4b = menu.new BriefState(
				commander + "'s polite smile disappears and she looks back to what she was doing. " +
				"\n \n " +
				"\"Dismissed.\""
		){
			public void onActivate(){
				pilot.setVar(MissionVar.ANNOY_COMMANDER_1, "1");
		}};
		
		page1.addOption("Move toward the abyss", page2);
		page1.addOption("Continue along the corridor", page3);
		
		page2.addOption("Continue along the corridor", page3);
		
		page3.addOption("\"It's an honor, ma'am.\"", page4a);
		page3.addOption("\"About time.\"", page4b);
		
		page4a.addStartOption("Start");
		page4b.addStartOption("Start");
		
		return page1;
	}
	
}
