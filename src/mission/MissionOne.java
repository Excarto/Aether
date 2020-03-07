
public class MissionOne extends Mission{

	public MissionOne(){
		super("Mission I", "missionone", "stay-the-course",
				2, 34, 0.9, -1.0,
				new BackgroundGenerator.Planet[]{});
		
		addPlayer(1, 0);
		addPlayer(2, 0);
	}
	
	protected void initialize(Pilot pilot){
		
		Ship ship1 = readShip("ares1");
		addShip(0, ship1);
		Ship ship2 = readShip("hermes1");
		addShip(0, ship2);
		
		Ship enemyShip = readShip("hyperion1");
		addShip(1, enemyShip);
		
		String partner = MissionConstants.PARTNER + ":  ";
		events.add(new TextEvent(new SpotCondition(enemyShip, true, false),
				partner + "That yellow circle is an enemy unit picked up by one of your sensors. Proceed with caution.", 8));
		events.add(new TextEvent(new SpotCondition(enemyShip, false, true),
				partner + "Stay the hell out of range of those particle beams!", 8));
		events.add(new TextEvent(new DestroyCondition(ship1),
				partner + "Well, we're screwed.", 8){
			public void onExecute(){
				getPilot().setVar(MissionVar.SHIP_LOST, "1");
			}
		});
		events.add(new TextEvent(new DestroyCondition(ship2),
				partner + "Nice one. We probably didn't need that, anyway.", 8){
			public void onExecute(){
				getPilot().setVar(MissionVar.SHIP_LOST, "1");
			}
		});
		events.add(new TextEvent(
				new DestroyCondition(enemyShip){
					public boolean isSatisfied(Game game){
						return super.isSatisfied(game) && ship1.getHull() > 0 && ship2.getHull() > 0;
					}
				}, partner + "That could have gone worse.", 8
		));
	}
	
	public BriefMenu.BriefState createBriefing(BriefMenu menu, Pilot pilot){
		
		String tutor = MissionConstants.TUTOR;
		String partner = MissionConstants.PARTNER;
		
		BriefMenu.BriefState page1 = menu.new BriefState(
				MissionConstants.TUTOR_LONG + " shows you to to the command vessel and points to a station next to an annoyed-looking woman. " +
				"\n \n " +
				"\"" + pilot.name + ", this is " + MissionConstants.PARTNER_LONG + ", your partner for your first mission.\" " +
				"\n \n " +
				(pilot.type == Pilot.Type.ROBOT ?
						"\"Another bot, seriously?\", says " + partner + " loudly to herself without looking up from her infoscreen. " :
						partner + " studies her infoscreen and makes no effort to acknowledge you. ") +
				"\n \n " +
				"\"Ok, have fun out there,\" calls " + tutor + ", already floating back through the airlock into the station."
		);
		
		BriefMenu.BriefState page2 = menu.new BriefState(
				"\"If we don't manage to extract a critical mass of fuel before they do, we won't be coming back,\" "
				+ partner + " says dryly, still not bothering to look at you. \"So try not to get me killed.\""
		);
		
		BriefMenu.BriefState page3a = menu.new BriefState(
				partner + "'s eye twitches with spite as she pulls the lever to undock the command vessel."
		){
			public void onActivate(){
				pilot.setVar(MissionVar.ANNOY_PARTNER_1, "1");
		}};
		BriefMenu.BriefState page3b = menu.new BriefState(
				partner + " pulls the lever to undock the command vessel, and you prepare to warp."
		);
		
		page1.addOption("Continue", page2);
		if (pilot.type == Pilot.Type.ROBOT){
			page2.addOption("\"Beep, boop.\"", page3a);
		}else{
			page2.addOption("\"Just stay out of my way.\"", page3a);
		}
		page2.addOption("\"Let's go.\"", page3b);
		page3a.addStartOption("Start");
		page3b.addStartOption("Start");
		
		return page1;
	}
	
}
