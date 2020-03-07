
public class MissionFive extends Mission{
	
	public MissionFive(){
		super("Mission V", "battlelarge", "outfoxingthefox",
				1697984175, 28, 0.9, 0.27,
				new BackgroundGenerator.Planet[]{Arena.background.new Planet(2, 39, 0.0, 0.0, 0.0)});
		
		int budget = 3000;
		addPlayer(1, budget);
		addPlayer(2, compBudget(budget));
	}
	
	protected void initialize(Pilot pilot){
		Condition condition = new TimeCondition(4){
			public boolean isSatisfied(Game game){
				return super.isSatisfied(game) && playerFleet.size() <= 2;
			}
		};
		
		events.add(new TextEvent(condition,
				MissionConstants.ROBOPARTNER + "If I may say so, " + pilot.type.title + ", it might be risky to bring so few ships to the fight.", 20));
		events.add(new TextEvent(condition,
				MissionConstants.ROBOPARTNER + "It could be difficult to control enough space.", 20));
	}
	
	public BriefMenu.BriefState createBriefing(BriefMenu menu, Pilot pilot){
		
		String commander = MissionConstants.COMMANDER;
		String robo = MissionConstants.ROBOPARTNER;
		
		BriefMenu.BriefState page1 = menu.new BriefState(
				"You once again find yourself in " + MissionConstants.COMMANDER_LONG + "'s office. " +
				"\n \n " +
				"\"" + pilot.name + ", we have a new batch of eager cadets joining up and they need good pilots to show them the ropes.\" " +
				commander + " pauses to check her infoscreen before continuing. " +
				"\n \n " +
				"\"Your new partner will be " + MissionConstants.ROBOPARTNER_LONG + ". " +
				robo + " is an A-880 model. A former manufacturing tech, like yourself.\""
		);
		
		BriefMenu.BriefState page2a = menu.new BriefState(
				"You hear the clank of metal behind you and turn around to see " + robo + "'s expressionless eyes staring at you. " +
				"\n \n " +
				"\"I am ready for launch when you are, " + pilot.type.title + ",\" " + robo + " addresses you with a salute."
		);
		
		BriefMenu.BriefState page2b = menu.new BriefState(
				commander + " gives you an exasperated look while pointing behind you. You turn around to see " +
				robo + "'s expressionless eyes starting at you. " +
				"\n \n " +
				"\"I am ready for launch when you are, " + pilot.type.title + ",\" " + robo + " addresses you with a salute."
		){
			public void onActivate(){
				pilot.setVar(MissionVar.ANTIBOT_ROBO_1, "1");
				pilot.setVar(MissionVar.ANNOY_COMMANDER_2, "1");
		}};;
		
		if (pilot.type == Pilot.Type.ROBOT){
			page1.addOption("Continue", page2a);
		}else{
			page1.addOption("\"I'll have " + robo + " up to speed in no time\"", page2a);
			page1.addOption("\"I would rather not have a bot as a partner\"", page2b);
		}
		
		page2a.addStartOption("Start");
		page2b.addStartOption("Start");
		
		return page1;
	}
	
}
