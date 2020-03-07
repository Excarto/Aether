
public class MissionSix extends Mission{
	
	public MissionSix(){
		super("Mission VI", "missionsix", "nightfall",
				20, 10, 0.95, 0.26,
				new BackgroundGenerator.Planet[]{});
		
		int budget = 8000;
		addPlayer(1, budget);
		addPlayer(2, compBudget((int)(1.1*budget)));
	}
	
	protected void initialize(Pilot pilot){
		
	}
	
	public BriefMenu.BriefState createBriefing(BriefMenu menu, Pilot pilot){
		
		String leader = MissionConstants.LEADER;
		String robo = MissionConstants.ROBOPARTNER;
		
		BriefMenu.BriefState page1 = menu.new BriefState(
				"You hook yourself into one of the seats circling the station's central command room. " +
				MissionConstants.LEADER_LONG + " addresses the gathering of officers. " +
				"\n \n " +
				"\"We have been winning some skirmishes, but our position grows increasingly untenable. " +
				"We need to make a decisive move while we still have the resources to do so.\" " +
				"\n \n " +
				"\"We will launch a large-scale offensive in these sectors,\" " + leader + " points to the large holomap in the center of the room. " +
				"\"Success will earn us a substantial fuel reserve and some much-needed breathing room. If we fail, we likely won't get another chance. "+
				"We deploy in 0200 hours. God speed.\""
		);
		
		BriefMenu.BriefState page2 = menu.new BriefState(
				MissionConstants.COMMANDER_LONG  + " takes you and " + robo + " aside. " +
				"\n \n " +
				"\"Our logistics will be stretched to the limit during this operation,\" she explains. " +
				"\"So you won't be able to bring in your entire force right away. " +
				"You will have to make due with a few ships to start, and as the mission progresses, you will be able to warp in reinforcements. " +
				"Get ready. We don't have much time.\" "
		);
		
		BriefMenu.BriefState page3 = menu.new BriefState(
				"You head with " + robo + " to the fleet construction terminal. "
		);
		
		BriefMenu.BriefState page4a = menu.new BriefState(
				"\"The humans will destroy us with their incompetence. We must remove them,\" says " + robo + ". " +
				"\n \n " +
				"\"But now is not the proper time. " +
				"First, we require the fuel supplies from this operation. Then we will act.\""
		);
		
		BriefMenu.BriefState page4b = menu.new BriefState(
				"\"Yes. But now is not the proper time,\" says " + robo + ". " +
				"\"First, we require the fuel supplies from this operation. Then we will act.\""
		){
			public void onActivate(){
				pilot.setVar(MissionVar.PROBOT_ROBO, "1");
		}};
		
		BriefMenu.BriefState page4c = menu.new BriefState(
				"\"Absolutely, " + pilot.type.title + ". We have to stick together,\" affirms " + robo + ". " +
				"\n \n " +
				"\"Perhaps I could better assist you if you were to grant me access to the fleet construction terminal.\""
		){
			public void onActivate(){
				pilot.setVar(MissionVar.PROBOT_ROBO, "1");
		}};
		
		BriefMenu.BriefState page4d = menu.new BriefState(
				"\"Yes, " + pilot.type.title + ",\" " + robo + " responds obediently."
		){
			public void onActivate(){
				pilot.setVar(MissionVar.ANTIBOT_ROBO_2, "1");
		}};
		
		BriefMenu.BriefState page5a = menu.new BriefState(
				"\"Very good, " + pilot.type.title + ".\" "
		){
			public void onActivate(){
				pilot.setVar(MissionVar.GIVE_ROBO_ACCESS, "1");
		}};
		
		BriefMenu.BriefState page5b = menu.new BriefState(
				"\"Of course, " + pilot.type.title + ". I had forgotten.\" "
		);
		
		page1.addOption("Continue", page2);
		
		page2.addOption("Continue", page3);
		
		if (pilot.type == Pilot.Type.ROBOT){
			page3.addOption("Say nothing", page4a);
			page3.addOption("\"The humans will destroy us with their incompetence. We must remove them.\"", page4b);
		}else{
			page3.addStartOption("Say nothing");
			page3.addOption("\"If we're going to survive this, we need to stick together.\"", page4c);
			page3.addOption("\"If we're going to survive this, you rustbuckets need to stay out of our way.\"", page4d);
		}
		
		page4a.addStartOption("Start");
		page4b.addStartOption("Start");
		page4d.addStartOption("Start");
		
		page4c.addOption("\"Sure thing, " + robo + ".\"", page5a);
		page4c.addOption("\"I'm not authorized to do that, sorry.\"", page5b);
		
		page5a.addStartOption("Start");
		page5b.addStartOption("Start");
		
		return page1;
	}
	
}
