import static java.lang.Math.*;
import java.util.*;
import java.io.*;

// Pilot object that represents the state of the single-player campaign, including all choices made and mission results.
// Can be written and read from a file. 

public class Pilot{
	
	public final String name;
	public final Type type;
	
	private final Map<Mission,Integer> missionScores;
	private final Map<MissionVar,String> missionVars;
	private boolean isSaved, ioFailed;
	
	public Pilot() throws IOException{
		Map<String,String> data = Utility.readDataFile(Main.saveDir + "/pilot.txt");
		name = getString(data, "name");
		type = Type.getType(getString(data, "type"));
		missionScores = new HashMap<Mission, Integer>();
		missionVars = new EnumMap<MissionVar,String>(MissionVar.class);
		load();
	}
	
	public Pilot(String name, Type type){
		this.name = name;
		this.type = type;
		isSaved = true;
		
		missionScores = new HashMap<Mission, Integer>();
		for (Mission mission : Main.missions)
			missionScores.put(mission, 0);
		missionVars = new EnumMap<MissionVar,String>(MissionVar.class);
	}
	
	public void load(){
		isSaved = true;
		ioFailed = false;
		try{
			Map<String,String> data = Utility.readDataFile(Main.saveDir + "/pilot.txt");
			
			missionScores.clear();
			for (Mission mission : Main.missions){
				int score = 0;
				try{
					score = getInteger(data, mission.getShortName());
				}catch (IOException ex){}
				missionScores.put(mission, score);
			}
			
			missionVars.clear();
			for (String key : data.keySet()){
				if (key.startsWith("-"))
					missionVars.put(MissionVar.getByName(key.substring(1)), data.get(key));
			}
			
		}catch (Exception ex){
			isSaved = false;
			ioFailed = true;
			ex.printStackTrace();
		}
	}
	
	public Rank getRank(){
		int score = 0;
		for (Integer val : missionScores.values())
			score += max(0, val);
		
		Rank rank = Rank.values()[0];
		for (Rank avail : Rank.values()){
			if (avail.scoreLimit <= score && avail.scoreLimit > rank.scoreLimit)
				rank = avail;
		}
		return rank;
	}
	
	public Mission getCurrentMission(){
		for (Mission mission : Main.missions){
			if (getScore(mission) < 1)
				return mission;
		}
		return Main.missions[Main.missions.length-1];
	}
	
	public int getScore(Mission mission){
		return missionScores.get(mission);
	}
	public void setScore(Mission mission, int value){
		isSaved = false;
		missionScores.put(mission, value);
	}
	
	public String getVar(MissionVar key){
		return missionVars.get(key);
	}
	public boolean isSet(MissionVar key){
		return missionVars.get(key) != null;
	}
	public void setVar(MissionVar key, String value){
		isSaved = false;
		missionVars.put(key, value);
	}
	
	public void save(){
		isSaved = true;
		ioFailed = false;
		try{
			File file = new File(Main.saveDir + "/pilot.txt");
			file.delete();
			PrintWriter writer = new PrintWriter(file);
			writer.println("name= " + name);
			writer.println("type= " + type);
			for (Mission mission : Main.missions)
				writer.println(mission.getShortName() + "= " + missionScores.get(mission));
			for (MissionVar var : missionVars.keySet())
				writer.println("-" + var.name() + "=" + missionVars.get(var));
			writer.close();
		}catch (Exception ex){
			isSaved = false;
			ioFailed = true;
			ex.printStackTrace();
		}
	}
	
	public boolean isSaved(){
		return isSaved;
	}
	
	public boolean ioFailed(){
		return ioFailed;
	}
	
	public void revertTo(int mission){
		for (int i = mission; i < Main.missions.length; i++)
			setScore(Main.missions[i], 0);
		for (MissionVar var : missionVars.keySet()){
			if (var.mission >= mission)
				missionVars.remove(var);
		}
	}
	
	private String getString(Map<String, String> data, String label) throws IOException{
		if (data.containsKey(label)){
			return data.get(label);
		}else
			throw new IOException(label);
	}
	
	private int getInteger(Map<String, String> data, String label) throws IOException{
		if (data.containsKey(label)){
			try{
				return Integer.parseInt(data.get(label));
			}catch (NumberFormatException ex){
				throw new IOException(label);
			}
		}else
			throw new IOException(label);
	}
	
	public enum Type{
		MALE("Male", "sir"), FEMALE("Female", "ma'am"), ROBOT("Robot", "sir");
		public final String name, title;
		Type(String name, String title){
			this.name = name;
			this.title = title;
		}
		public String toString(){
			return name;
		}
		public static Type getType(String name){
			for (Type type : Type.values()){
				if (type.name.equals(name))
					return type;
			}
			return null;
		}
	};
	
	public enum Rank{
		CADET("Cadet", "Cadet", 1), SERGEANT("Sergeant", "Sgt.", 3), LIEUTENANT("Lieutenant", "Lt.", 4), CAPTAIN("Captain", "Cpt.", 7);
		public final String name, abbreviation;
		public final int scoreLimit;
		Rank(String name, String abbreviation, int scoreLimit){
			this.name = name;
			this.abbreviation = abbreviation;
			this.scoreLimit = scoreLimit;
		}
		public String toString(){
			return name;
		}
	};
}
