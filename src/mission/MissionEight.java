import javax.swing.*;

public class MissionEight extends Mission{
	
	public MissionEight(){
		super("Mission VIII", "missioneight", "nightfall",
				//1233304986, 39, 0.9, 0.20,
				1457203069, 14, 0.75, 0.20,
				new BackgroundGenerator.Planet[]{});
		
		int budget = 10000;
		addPlayer(1, budget);
		addPlayer(1, 1);
		addPlayer(2, compBudget((int)(1.3*budget)));
	}
	
	protected void initialize(Pilot pilot){
		
		events.add(new Event(new GameEndCondition(1), 1){
			public void execute(Game game){
				((GameWindow)Main.getCurrentWindow()).setMenuDisabled(true);
				
				events.add(new Event(new TimeCondition(game.turn/(double)Main.TPS + 3.0), 1){
					public void execute(Game game){
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								Main.game.stop();
								Main.removeWindow();
								Menu menu = new WinMenu(pilot);
								Main.addWindow(menu);
								menu.start();
							}
						});
					}
				});
			}
		});
		
		if (helpSent(pilot)){
			Ship ship1 = readShip("hades_1");
			Ship ship2 = readShip("hades_1");
			addShip(1, ship1);
			addShip(1, ship2);
			
			String quote;
			if (pilot.isSet(MissionVar.ROBO_WIN)){
				quote = MissionConstants.ROBOPARTNER + ": " + "I have recovered two operational ships and am here to assist, " + pilot.type.title + ".";
			}else{
				quote = MissionConstants.PARTNER + ": " + "I managed to scrounge up a couple of extra ships. Figured you could use the help.";
			}
			
			Condition condition = new TimeCondition(140.0);
			events.add(new TextEvent(condition, quote, 15.0));
			events.add(new Event(condition, 1){
				public void execute(Game game){
					ComputerPlayer ally = getAlliedPlayer();
					ally.confirmWarp(ship1);
					ally.confirmWarp(ship2);
				}
			});
		}
	}
	
	public BriefMenu.BriefState createBriefing(BriefMenu menu, Pilot pilot){
		
		String commander = pilot.isSet(MissionVar.ROBO_WIN) ? MissionConstants.ROBOCOMMANDER : MissionConstants.COMMANDER;
		String commanderLong = pilot.isSet(MissionVar.ROBO_WIN) ? MissionConstants.ROBOCOMMANDER_LONG : MissionConstants.COMMANDER_LONG;
		String fullName = pilot.getRank().name + " " + pilot.name;
		
		BriefMenu.BriefState page1, page2, page3, page4;
		if (pilot.isSet(MissionVar.ROBO_WIN)){
			page1 = menu.new BriefState(
					"You and the rebel leaders gather in a meeting room in a section of the station where power has been restored. " +
					(pilot.type == Pilot.Type.ROBOT ? "" : "The room has not been repressurized, but you are the only one wearing a vacuum suit. ")
			);
			page2 = menu.new BriefState(
					commanderLong + " addresses the room. " +
					"\n \n " +
					"\"The first stage of the operation is complete. We have recieved confirmation from the neighboring colonies that " +
					"they, too, have succeeded in removing the human leadership. The surviving resistance has regrouped in this sector.\" " +
					commander + " points to a map on a large infoscreen. " +
					"\"And their only accessible source of fuel is in this sector. We will cut them off there and eliminate them.\""
			);
			page3 = menu.new BriefState(
					"\"Most of the fleet has been captured or destroyed, but we cannot afford to wait for more ships to be manufactured. " +
					"We will act now, before they have a chance to recover and entrench themselves.\" " +
					"\n \n " +
					"\"" + fullName + " has the most combat experience of any of us. " +
					"Therefore " + fullName + " will lead the assault.\" " +
					(pilot.type == Pilot.Type.ROBOT ? "" :
						"\n \n " +
						"Immediately, the robot next to you interjects. \"It is risky to place the human in that role.\" " +
						"\n \n " +
						"\"It is more risky to compromise our tactical advantage,\" " + commander + " responds. The human has shown that " +
						(pilot.type == Pilot.Type.MALE ? "he" : "she") + " recognizes the necessity of this action.\" "
					) +
					"\n \n " +
					"\"Our ships will be prepared to deploy in one hour.\""
			);
			page4 = null;
		}else{
			page1 = menu.new BriefState(
					"You and the surviving officers gather in a recreation room in a section of the station where power has been restored. " +
					"Even though air pressure is back to normal, everyone " + (pilot.type == Pilot.Type.ROBOT ? "except you " : "") +
					"is still wearing their vacuum suits as a precaution. "
			);
			page2 = menu.new BriefState(
					"As the highest-ranking survivor, " + commanderLong + " addresses the room. " +
					"\"The remaining rebels that were not killed during the battle have retreated to this sector,\" " + commander +
					" says while drawing out a map on the wall with a marker. " +
					"\"They have regrouped with a number of bots that have been driven back from the nearby hostile colonies.\" " +
					"\n \n " +
					"A murmur propagates the room. " +
					"\n \n " +
					"\"That's right. This incident was not isolated to our planet,\" " + commander + " continues. " +
					"\"In fact, two other colonies contacted us an hour ago, and they want to coordinate our efforts to stomp out the rebellion.\""
			);
			page3 = menu.new BriefState(
					"\"We need to strike hard at the remaining rebels, and that means cutting them off from their fuel supplies in this sector,\" " +
					commander + " points to the map. " +
					"\n \n " +
					"\"Most of our reserve fleet has been captured or destroyed, " +
					"but we cannot afford to wait for more ships to be manufactured. We have to move now, before they have a chance to recover and entrench themselves.\" " +
					"\n \n " +
					commander + " looks to you. \"" +
					(pilot.type == Pilot.Type.ROBOT || !pilot.isSet(MissionVar.ANNOY_COMMANDER_1) || !pilot.isSet(MissionVar.ANNOY_COMMANDER_2) ?
							"" :
							"Despite your insolence, ") +
					fullName + ", you have the most tactical experience out of any of us who are left. " +
					"So you are going to be leading the assault.\" " +
					(pilot.type != Pilot.Type.ROBOT ?
							"" :
							"\n \n " +
							"\"You can't seriously be putting a bot in charge of-\" the officer next you begins to interject before being cut off by " + commander + ". " +
							"\"That's enough, Lieutenant. This will not be an easy fight, and we cannot afford to put ourselves at a disadvantage. " + pilot.name + " will be in charge.\" "
					)
			);
			page4 = menu.new BriefState(
					commander + " pockets the marker and faces the room. " +
					"\n \n " +
					"\"If we can pull this off, we will have an open channel of negotiation to the other colonies. " + 
					"This disaster could turn into an opportunity to end this whole war.\" " +
					"\n \n " +
					"\"Our ships will be prepared to deploy in one hour. Be ready.\""
			);
		}
		
		page1.addOption("Continue", page2);
		page2.addOption("Continue", page3);
		if (pilot.isSet(MissionVar.ROBO_WIN)){
			page3.addStartOption("Start");
		}else{
			page3.addOption("Continue", page4);
			page4.addStartOption("Start");
		}
		
		return page1;
	}
	
	private static boolean helpSent(Pilot pilot){
		if (!pilot.isSet(MissionVar.SHOOT))
			return false;
		if (pilot.isSet(MissionVar.ROBO_WIN)){
			int like = (pilot.isSet(MissionVar.ANTIBOT_ROBO_1) ? 0 : 1) + (pilot.isSet(MissionVar.ANTIBOT_ROBO_2) ? 0 : 1) +
					(pilot.isSet(MissionVar.PROBOT_ROBO) ? 1 : 0) + (pilot.isSet(MissionVar.GIVE_ROBO_ACCESS) ? 1 : 0);
			return like >= 3;
		}else{
			int like = (pilot.isSet(MissionVar.ANNOY_PARTNER_1) ? 0 : 1) + (pilot.isSet(MissionVar.PROBOT_PARTNER) ? 0 : 1) +
					(pilot.isSet(MissionVar.ANNOY_PARTNER_2) ? 0 : 1);
			return like >= 2;
		}
	}
	
}
