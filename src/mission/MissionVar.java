
public enum MissionVar{
	
	ANNOY_PARTNER_1(1),
	SHIP_LOST(1),
	ANNOY_PARTNER_2(2),
	PROBOT_PARTNER(2),
	ANNOY_COMMANDER_1(4),
	ANTIBOT_ROBO_1(5),
	ANNOY_COMMANDER_2(5),
	PROBOT_ROBO(6),
	ANTIBOT_ROBO_2(6),
	GIVE_ROBO_ACCESS(6),
	HAVE_GUN(7),
	SHOOT(7),
	ROBO_WIN(7),
	ANNOY_PARTNER_3(7),
	SHOW_ENDING(8);
	
	public final int mission;
	
	MissionVar(int mission){
		this.mission = mission;
	}
	
	public static MissionVar getByName(String name){
		for (MissionVar var : values()){
			if (var.name().equalsIgnoreCase(name))
				return var;
		}
		return null;
	}
}
