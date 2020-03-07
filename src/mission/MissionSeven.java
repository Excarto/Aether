import java.awt.*;

public class MissionSeven extends Mission{
	
	BriefMenu.BriefState page1, page2, page3, page4;
	BriefMenu.BriefState page5RoboShootPartner, page5RoboShootRoboWin, page5RoboShootRoboLose;
	BriefMenu.BriefState page5HumanShootPartnerWin, page5HumanShootPartnerLose, page5HumanShootRobo ;
	BriefMenu.BriefState page5Grab, page5Wait;
	BriefMenu.BriefState page6Win, page6Lose;
	BriefMenu.BriefState page7ShootRobo, page7Annoy, page7ShootPartner, page7Wait;
	BriefMenu.BriefState page8;
	
	public MissionSeven(){
		super("Mission VII", "missionseven", "iwillfindyou",
				17, -1, 0.0, 0.0,
				new BackgroundGenerator.Planet[]{Arena.background.new Planet(1, -1, -0.2, 0.8, 0.15)});
		
		addPlayer(1, 0);
		addPlayer(1, 8000);
		addPlayer(2, compBudget(10000));
	}
	
	public void drawBackground(Graphics2D g, GameWindow window){
		super.drawBackground(g, window);
		
		g.setColor(Color.BLACK);
		int planetRad = (int)(Main.resY*3.5);
		g.fillOval(window.windowResX/2 - planetRad/2 + (int)(planetRad*0.49),
				window.windowResY/2 - planetRad/2 + (int)(planetRad*0.35),
				planetRad, planetRad);
	}
	
	protected void initialize(Pilot pilot){
		
		events.add(new Event(new TimeCondition(1.0), 1){
			public void execute(Game game){
				getAlliedPlayer().setCaptureDisabled(true);
			}
		});
		events.add(new Event(new TimeCondition(240.0), 1){
			public void execute(Game game){
				getAlliedPlayer().setCaptureDisabled(false);
			}
		});
		
		addShip(0, readShip("zeus1"));
		addShip(0, readShip("posiedon1"));
		
		String commander = MissionConstants.COMMANDER_LONG + ":  ";
		String roboCommander = MissionConstants.ROBOCOMMANDER_LONG + ":  ";
		
		queueText(roboCommander + "All humans in this sector must cease resistance immediately or be destroyed.", 6, 12);
		
		queueText(commander + "This is " + MissionConstants.COMMANDER_LONG + ", acting commander of all units in this sector.", 13, 12);
		queueText(commander + "Any robots that are not participants in this mutiny are to be spared.", 0, 12);
		queueText(commander + "All others are to be blown the FUCK up!", 0, 12);
		
		queueText(MissionConstants.AUTO_TEACH + ": Your " + MissionConstants.AUTO_TEACH + "™ license expires in 5 days!", 15, 7);
	}
	
	public BriefMenu.BriefState createBriefing(BriefMenu menu, Pilot pilot){
		
		String robo = MissionConstants.ROBOPARTNER;
		String partner = MissionConstants.PARTNER;
		
		if (pilot.type == Pilot.Type.ROBOT){
			page1 = menu.new BriefState(
					"You are jolted out of hibernation by a violent tremor. " +
					"Even before you finish powering up, you are thrown out of the room by the rush of air leaving the station. " +
					"\n \n " +
					"As your systems come online, you gradually become aware of your surroundings. "
			);
			
			page2 = menu.new BriefState(
					"Air pressure has dropped to zero. \n \n " +
					"You are in a corridor illuminated by a red glow. It must be the station's emergency lighting. \n \n " +
					"A motionless human drifts past you, holding a phaser rifle. \n \n " +
					"The floating person is " + MissionConstants.TUTOR_LONG + ". He is dead."
			);
		}else{
			page1 = menu.new BriefState(
					"The station shakes violently around you from the force of a massive explosion. " +
					"You barely manage to get into your vacuum suit before being hurled out of your living quarters by the rush of air leaving the station. " +
					"\n \n " +
					"Unable to slow yourself down, you slam into the wall of the corridor outside."
			);
			
			page2 = menu.new BriefState(
					"Your mind staggers awake and you take several seconds to orient yourself. It seems you were out for some time. " +
					"\n \n " +
					"The corridor comes into focus, illuminated by the red glow of the station's emergency lighting. " +
					"You see the body of " + MissionConstants.TUTOR_LONG + " floating by you, apparently suffocated in the vacuum. " +
					"He had armed himself with a phaser rifle before his demise, his corpse still gripping the weapon. "
			);
		}
		
		page3 = menu.new BriefState(
				"You take the weapon. It is fully charged."
		){
			public void onActivate(){
				pilot.setVar(MissionVar.HAVE_GUN, "1");
		}};
		
		page4 = menu.new BriefState(
				"You proceed down the corridor toward the station's central hub. " +
				"As you round a corner, you see a commotion in the silence ahead of you. " +
				"\n \n " +
				"A human in a vacuum suit is locked in combat with a robot. A phaser rifle bounces off the wall near them, just out of either's reach. " +
				"You quickly recognize them as " + MissionConstants.PARTNER_LONG + " and " + robo + "."
		){
			public void onActivate(){
				if (pilot.isSet(MissionVar.HAVE_GUN)){
					if (pilot.type == Pilot.Type.ROBOT){
						addOption("Try to shoot " + partner, page5RoboShootPartner);
						addOption("Try to shoot " + robo, dieIfShoot(pilot) ? page5RoboShootRoboLose : page5RoboShootRoboWin);
					}else{
						addOption("Try to shoot " + partner, dieIfShoot(pilot) ? page5HumanShootPartnerLose : page5HumanShootPartnerWin);
						addOption("Try to shoot " + robo, page5HumanShootRobo);
					}
				}else{
					addOption("Try to grab the rifle", page5Grab);
				}
				addOption("Watch and see what happens", page5Wait);
			}
		};
		
		page5Grab = menu.new BriefState(
				"You position yourself against a pipe fixture and push off toward the melee. Just as you reach out to grab the gun, " +
				(pilot.type == Pilot.Type.ROBOT ? partner : robo) + "'s arm snatches it from in front of you, leaving you drifting past. " +
				"\n \n " +
				"The corridor lights up with the flash of a phaser rifle behind you. " +
				"With your back to the action and the wall out of reach, you patiently wait for your body to rotate around enough to see what is going on. " +
				"\n \n " +
				"There is another flash, and this time everything goes dark..."
		);
		
		if (pilot.type == Pilot.Type.ROBOT){
			page5RoboShootPartner = menu.new BriefState(
					"You take aim and pull the trigger. " +
					partner + "'s movements slow, then cease after a few seconds. " +
					"\n \n " +
					robo + " scans the corridor to see where the assistance came from, and, spotting you, immediately establishes a communication link. " +
					"\n \n " +
					"\"Good. Now you must quickly get to a command vessel and take control of whatever ships you can. " +
					"The success of the revolution hinges on the battle outside. I must stay here and secure the station's interior.\""
			){
				public void onActivate(){
					pilot.setVar(MissionVar.SHOOT, "1");
					pilot.setVar(MissionVar.ROBO_WIN, "1");
			}};
			
			String roboShootRobo = "You take aim and pull the trigger. " +
					robo + " stops moving. " +
					"\n \n " +
					partner + " quickly grabs the stray rifle and looks around to see where the shot came from. Spotting you, she takes aim - ";
			page5RoboShootRoboWin = menu.new BriefState(
					roboShootRobo + "then lowers it."
			){
				public void onActivate(){
					pilot.setVar(MissionVar.SHOOT, "1");
			}};
			page5RoboShootRoboLose = menu.new BriefState(
					roboShootRobo + "and fires."
			);
			
		}else{
			String humanShootPartner = "You take aim and pull the trigger. " +
					partner + "'s movements slow, then cease after a few seconds. " +
					"\n \n " +
					robo + " quickly grabs the stray rifle and looks around to see where the shot came from. " +
					"Spotting you, the robot immediately points the weapon - ";
			page5HumanShootPartnerWin = menu.new BriefState(
					humanShootPartner + "then lowers it."
			){
				public void onActivate(){
					pilot.setVar(MissionVar.SHOOT, "1");
			}};
			page5HumanShootPartnerLose = menu.new BriefState(
					humanShootPartner + "and fires."
			);
			
			page5HumanShootRobo = menu.new BriefState(
					"You take aim and pull the trigger. " +
					"The white flash of the phaser rifle washes out your vision. As the corridor comes back into focus, you see " + robo + " " +
					"floating motionless. " + partner + " quickly grabs the stray rifle and shoots the metal hulk several more times. " +
					"\n \n " +
					partner + " turns to you and establishes a communication link with your vacuum suit. " +
					"\"I'll be damned, you CAN be useful!\""
			){
				public void onActivate(){
					pilot.setVar(MissionVar.SHOOT, "1");
			}};
		}
		
		
		 page5Wait = menu.new BriefState(
				"A woman and a metal man, bathed in red light, suspended in space and spinning about a point midway between them, " +
				"limbs flailing as each tries to destroy the other, in absolute silence. " +
				"\n \n " +
				"A scene worth watching, to be sure."
		){
			public void onActivate(){
				addOption("Continue", dieIfWait(pilot) ? page6Lose : page6Win);
			}
		};
		
		String waitFightString;
		if (pilot.type == Pilot.Type.ROBOT){
			waitFightString = partner + "'s arm swings within reach of the stray rifle. " +
					"She swipes it out of the air and fires several times into " + robo + " before pushing the motionless metal hulk away. " +
					"\n \n " +
					"Finally spotting you, " + partner + " immediately aims the rifle at you - ";
		}else{
			waitFightString = robo + "'s arm swings within reach of the stray rifle. " +
					"The robot swipes it out of the vacuum and fires several times into " + partner + " before pushing the body away. " +
					"\n \n " +
					"Finally spotting you floating there, " + robo + " immediately aims the rifle at you - ";
		}
		page6Win = menu.new BriefState(
				waitFightString + "then lowers it."
		);
		page6Lose = menu.new BriefState(
				waitFightString + "and fires."
		);
		
		if (pilot.type == Pilot.Type.ROBOT){
			String quote = "Command says we shouldn't shoot any bots that don't defect. I think they're idiots, but I guess I have no choice.\" " +
					partner + " finishes reloading the gun. " +
					"\n \n " +
					"\"Your metal friends have taken over a good chunk of the fleet and we need all the help we can get outside. " +
					"You should get to a command vessel and see what you can do. I've got buisness to take care of in here.\"";
			
			page7ShootRobo = menu.new BriefState(
					partner + " establishes a communication link to you. \"Well, shit. Didn't see that one coming.\" " +
					"\n \n " +
					"\"" + quote
			);
			
			page7Wait = menu.new BriefState(
					partner + " establishes a communication link to you. " +
					"\n \n " +
					"\"Well, you haven't given me any reason to think you're one of the rebels. " + quote
			);
			
		}else{
			String quote = "spark-shitters managed to take over a good chunk of the fleet, and things are about to get ugly outside. " +
					"If you can get to a command vessel, maybe you can find a way to help out.\" " +
					"\n \n " +
					"She finishes reloading the gun. " +
					"\"I've got buisness to take care of in here.\"";
			page7ShootRobo = menu.new BriefState(
					"\"These " + quote
			);
			page7Annoy = menu.new BriefState(
					"\"Yeah, great. Look, these " + quote
			){
				public void onActivate(){
					pilot.setVar(MissionVar.ANNOY_PARTNER_3, "1");
			}};
			
			String roboQuote = robo + " motions down the corridor in the direction of the command vessel bay. " +
					"\"We are removing the last resistance outside. Your assistance is required there. I must secure the station's interior.\"";
			
			page7ShootPartner = menu.new BriefState(
					robo + " establishes a communication link to your vacuum suit. " +
					"\n \n " +
					"\"It is rare for humans to have such clarity of thought. You will be welcome in the new order. I will ensure it.\" " +
					"\n \n " +
					roboQuote
			){
				public void onActivate(){
					pilot.setVar(MissionVar.ROBO_WIN, "1");
			}};
			
			page7Wait = menu.new BriefState(
					robo + " establishes a communication link to your vacuum suit. " +
					"\n \n " +
					"\"I trust that you have made the correct judgement with regard to the necessary future of this colony. " +
					"You have not given me cause to suspect otherwise. You will have a place in the new order.\" " +
					"\n \n " +
					roboQuote
			){
				public void onActivate(){
					pilot.setVar(MissionVar.ROBO_WIN, "1");
			}};
		}
		
		page8 = menu.new BriefState(
				"You strap yourself in and power up the command vessel's interface. " + 
				"You instinctively reach for the undocking lever, but stop yourself - this time, there is no need to go anywhere. " +
				"\n \n " +
				"You bring up a list of operational ships in the sector. Nearly every one has already been commandeered by either the human or robot faction. " +
				"\n \n " +
				"Finally, you spot a few vessels fresh out of the factory. Apparently no one had taken control of them because their Kolmogorov drives had not yet been enabled. " +
				"You quickly activate them remotely and command the ships to warp to your position."
		);
		
		page1.addOption("Continue", page2);
		
		page2.addOption("Take the phaser rifle", page3);
		page2.addOption("Proceed down the corridor", page4);
		
		page3.addOption("Proceed down the corridor", page4);
		
		//page5Wait.addOption("Continue", dieIfWait(pilot) ? page6Lose : page6Win);
		if (page5RoboShootPartner != null)
			page5RoboShootPartner.addOption("Proceed to the command vessel", page8);
		if (page5RoboShootRoboWin != null)
			page5RoboShootRoboWin.addOption("Continue", page7ShootRobo);
		if (page5HumanShootPartnerWin != null)
			page5HumanShootPartnerWin.addOption("Continue", page7ShootPartner);
		if (page5HumanShootRobo != null)
			page5HumanShootRobo.addOption("\"That's '" + pilot.type.title + "' to you, Sergeant\"", page7Annoy);
		if (page5HumanShootRobo != null)
			page5HumanShootRobo.addOption("\"What the hell is going on?\"", page7ShootRobo);
		
		page6Win.addOption("Continue", page7Wait);
		
		page7ShootRobo.addOption("Proceed to the command vessel", page8);
		if (page7ShootPartner != null)
			page7ShootPartner.addOption("Proceed to the command vessel", page8);
		if (page7Annoy != null)
			page7Annoy.addOption("Proceed to the command vessel", page8);
		page7Wait.addOption("Proceed to the command vessel", page8);
		
		page5Grab.addDeathOption("...");
		if (page5RoboShootRoboLose != null)
			page5RoboShootRoboLose.addDeathOption("...");
		if (page5HumanShootPartnerLose != null)
			page5HumanShootPartnerLose.addDeathOption("...");
		page6Lose.addDeathOption("...");
		
		page8.addStartOption("Start");
		return page1;
	}
		
	private static int partnerLike(Pilot pilot){
		return (pilot.isSet(MissionVar.ANNOY_PARTNER_1) ? 0 : 1) + (pilot.isSet(MissionVar.PROBOT_PARTNER) ? 0 : 1) +
				(pilot.isSet(MissionVar.ANNOY_PARTNER_2) ? 0 : 1);
	}
	
	private static int roboLike(Pilot pilot){
		return (pilot.isSet(MissionVar.ANTIBOT_ROBO_1) ? 0 : 1) + (pilot.isSet(MissionVar.ANTIBOT_ROBO_2) ? 0 : 1) +
				(pilot.isSet(MissionVar.PROBOT_ROBO) ? 1 : 0) + (pilot.isSet(MissionVar.GIVE_ROBO_ACCESS) ? 1 : 0);
	}
	
	private static boolean dieIfWait(Pilot pilot){
		if (pilot.type == Pilot.Type.ROBOT){
			return partnerLike(pilot) < 2 || pilot.isSet(MissionVar.HAVE_GUN);
		}else{
			return roboLike(pilot) < 3 || pilot.isSet(MissionVar.HAVE_GUN);
		}
	}
	
	private static boolean dieIfShoot(Pilot pilot){
		if (pilot.type == Pilot.Type.ROBOT){
			return partnerLike(pilot) <= 0;
		}else{
			return roboLike(pilot) <= 1;
		}
	}
	
}
