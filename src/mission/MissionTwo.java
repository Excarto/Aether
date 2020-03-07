
public class MissionTwo extends Mission{

	public MissionTwo(){
		super("Mission II", "battlesmall", "morganarides",
				6, 20, 1.0, -1.0,
				new BackgroundGenerator.Planet[]{});
		
		addPlayer(1, 0);
		addPlayer(2, 0);
	}
	
	protected void initialize(Pilot pilot){
		
		addShip(0, readShip("hades_1"));
		addShip(0, readShip("artemis_torp"));
		Ship utilityShip = readShip("apollo_rearm");
		addShip(0, utilityShip);
		
		addShip(1, readShip("athena_1"));
		addShip(1, readShip("hermes_1"));
		
		String partner = MissionConstants.PARTNER + ":  ";
		
		int like = getPartnerLike(pilot);
		String taunt = like > 1 ? "" : ", if you somehow don't screw it up,";
		
		queueText(partner + "Your best bet to take out the enemy cruiser" + taunt + " is to use the attack corvette to hit-and-run.", 7, 18);
		queueText(partner + "If you double right click a target, that will order your unit to fly past at maximum speed. Great for torpedo runs.", 0, 18);
		
		queueText(partner + "Attack from behind if you can, to avoid having point defenses shoot down the torpedos.", 20, 18);
		queueText(partner + "The shields are weaker in the rear, too.", 0, 18);
		
		Condition condition = new TimeCondition(45){
			public boolean isSatisfied(Game game){
				return super.isSatisfied(game) && utilityShip.getHull() > 0;
			}
		};
		events.add(new TextEvent(condition, partner + "If you run out of torpedos, the utility frigate has spares.", 20));
		events.add(new TextEvent(condition, partner + "You can order it to repair and rearm the corvette by double right clicking.", 20));
		events.add(new TextEvent(condition, partner + "But make sure to stop the corvette from manuevering first, so the utility frigate can do its job.", 20));
		//queueText(partner + "If you run out of torpedos, the utility frigate has spares.", 60, 20);
		//queueText(partner + "You can order it to repair and rearm the corvette by double right clicking.", 0, 20);
		//queueText(partner + "But make sure to stop the corvette from manuevering first, so the utility frigate can do its job.", 0, 20);
		
		events.add(new TextEvent(new DestroyCondition(utilityShip), partner + "There goes the utility frigate. I guess we won't be reloading any torpedos.", 20));
		
	}
	
	public BriefMenu.BriefState createBriefing(BriefMenu menu, Pilot pilot){
		
		String partner = MissionConstants.PARTNER;
		int like = getPartnerLike(pilot);
		
		BriefMenu.BriefState page1 = menu.new BriefState(
				"\"Command has seen fit to once again make you my problem,\" " + partner + " greets you as you prepare for your next mission. " +
				"\n \n " +
				(pilot.type == Pilot.Type.ROBOT ?
						"\"If it were up to me, you rustbuckets wouldn't be allowed near the controls of a warship. " +
						(like == 0 ? "And you're somehow even less tolerable than the last one I was stuck with.\" "
						: like == 1 ? ""
						: "Although I admit you're more tolerable than the last one I was stuck with.\" ")
				:
						"\"Could be worse, I guess. At least I didn't get paired with another clanking rustbucket. " +
						(like == 0 ? "Although you're not much of a step up.\" "
						: like == 1 ? ""
						: "And I admit you've handled yourself alright so far.\" ")
				)
		);
		
		String transmission =
				"\n \n " +
				partner + "'s rant is cut off by an incoming transmission. " +
				"\n \n " +
				"\"Intel reports an enemy cruiser in your mission sector, so we're augmenting your force with a torpedo-equipped corvette,\" " +
				MissionConstants.TUTOR + "'s voice crackles through the intercom. \"Use it wisely.\" ";
		
		BriefMenu.BriefState page2a = menu.new BriefState(
				(like == 0 ?
						partner + " slams the thusters to full power before you finish strapping in, jolting your head into the control panel. \"You stupid little-\" " :
						"\"What did you just say to me, you little-\" ") +
				transmission
		){
			public void onActivate(){
				pilot.setVar(MissionVar.ANNOY_PARTNER_2, "1");
		}};
		
		BriefMenu.BriefState page2b = menu.new BriefState(
				(pilot.type == Pilot.Type.ROBOT ?
						partner + "'s eyes bulge. \"When you worthless spark-shitters finally compute yourselves a way to stab us in the back, don't think I won't-\" " :
						"\"You're an idiot to trust 'em. Mark my words, one of these days, just you wait, as soon as-\" ") +
				transmission
		){
			public void onActivate(){
				pilot.setVar(MissionVar.PROBOT_PARTNER, "1");
				if (pilot.type == Pilot.Type.ROBOT)
					pilot.setVar(MissionVar.ANNOY_PARTNER_2, "1");
		}};
		
		BriefMenu.BriefState page2c = menu.new BriefState(
				"\"Oh, and ANOTHER thing-\" " +
				transmission
		);
		
		page1.addOption("\"You don't exactly inspire confidence yourself.\"", page2a);
		if (pilot.type == Pilot.Type.MALE)
			page1.addOption("\"After this mission, you want to head down to the crew bar for a few drinks?\"", page2a);
		if (pilot.type == Pilot.Type.ROBOT){
			page1.addOption("\"Enjoy your 90 year lifespan, meat bag.\"", page2b);
		}else
			page1.addOption("\"You should have a little respect for our metal friends.\"", page2b);
		page1.addOption("Say nothing", page2c);
		
		page2a.addStartOption("Start");
		page2b.addStartOption("Start");
		page2c.addStartOption("Start");
		
		return page1;
	}
	
	private static int getPartnerLike(Pilot pilot){
		return (pilot.isSet(MissionVar.ANNOY_PARTNER_1) ? 0 : 1) +
				(pilot.isSet(MissionVar.SHIP_LOST) ? 0 : 1) + (pilot.isSet(MissionVar.ANNOY_PARTNER_2) ? 0 : 1);
	}
	
}
