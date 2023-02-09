import static java.lang.Math.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.awt.*;

// Mission in the single-player campaign. Each mission specifies a mission breifing,
// in-game events, the arena and background, and the player's fleet

public abstract class Mission{
	
	public static Vector<Ship> playerFleet = new Vector<Ship>();
	
	public final String name;
	public final Arena arena;
	public final String musicName;
	
	public final int backgroundSeed;
	public final int nebulaIndex;
	public final double nebulaOpacity;
	public final double cloudOpacity;
	public final BackgroundGenerator.Planet[] planets;
	
	public final List<Event> events;
	
	private Game game;
	private Pilot pilot;
	private List<MissionPlayer> players;
	
	protected abstract void initialize(Pilot pilot);
	public abstract BriefMenu.BriefState createBriefing(BriefMenu menu, Pilot pilot);
	public void gameStarted(Game game){}
	
	public Mission(String name, String arenaName, String musicName,
			int backgroundSeed, int nebulaIndex, double nebulaOpacity, double cloudOpacity,
			BackgroundGenerator.Planet[] planets){
		this.name = name;
		this.musicName = musicName;
		
		this.backgroundSeed = backgroundSeed;
		this.nebulaIndex = nebulaIndex;
		this.nebulaOpacity = nebulaOpacity;
		this.cloudOpacity = cloudOpacity;
		this.planets = planets;
		
		Arena found = null;
		for (Arena arena : Main.arenas){
			if (arena.shortName.equals(arenaName))
				found = arena;
		}
		if (found == null)
			Main.crash("Cannot find arena " + arenaName);
		this.arena = found;
		
		players = new ArrayList<>();
		events = new ArrayList<Event>();
	}
	
	public void queueText(String text, int delay, int lifetime){
		int currentTurn = 0;
		for (Event event : events){
			if (event instanceof TextEvent && event.condition instanceof TimeCondition)
				currentTurn = max(currentTurn, ((TimeCondition)((TextEvent)event).condition).turn);
		}
		double currentTime = currentTurn/(double)Main.TPS;
		events.add(new TextEvent(new TimeCondition(currentTime+delay), text, lifetime));
	}
	
	public void addPlayer(int team, int budget){
		players.add(new MissionPlayer(team, budget));
	}
	
	public void addShip(int player, Ship ship){
		ship.setName(ship.type.className);
		players.get(player).ships.add(ship);
	}
	
	public void start(Pilot pilot){
		this.pilot = pilot;
		if (players.get(0).budget == 0){
			launchGame();
		}else
			Main.addWindow(new MissionFleetWindow(this));
	}
	
	public void launchGame(){
		events.clear();
		for (MissionPlayer player : players)
			player.ships.clear();
		
		initialize(pilot);
		
		for (Ship ship : playerFleet)
			addShip(0, ship.copy());
		
		// If AI's fleets are not specified, randomly create them
		for (int i = 1; i < players.size(); i++){
			if (players.get(i).budget > 0 && players.get(i).ships.isEmpty())
				Player.makeFleet(players.get(i).ships, players.get(i).budget, 0);
		}
		
		game = new Game(arena, this, 1.0);
		
		for (int i = 0; i < players.size(); i++){
			MissionPlayer missionPlayer = players.get(i);
			List<Ship> ships = missionPlayer.ships;
			Player player;
			if (i == 0){
				player = new HumanPlayer(Main.options.username, missionPlayer.team, ships, missionPlayer.budget, arena);
			}else
				player = new ComputerPlayer(Main.options.username, missionPlayer.team, ships, missionPlayer.budget, arena);
			game.addPlayer(player);
		}
		
		Main.startGame(game, backgroundSeed, 0);
	}
	
	public void end(int victoryTeam){
		if (pilot.getCurrentMission() == this){
			if (victoryTeam == 1){
				pilot.setScore(this, 1);
				pilot.save();
			}else
				pilot.setScore(this, -1);
		}
	}
	
	public void act(){
		if (game.turn%9 == 0){
			for (int i = 0; i < events.size(); i++)
				events.get(i).act(game);
		}
	}
	
	public Pilot getPilot(){
		return pilot;
	}
	
	public int getPlayerBudget(){
		return players.get(0).budget;
	}
	
	public Ship readShip(String shipName){
		File file = new File("data/mission_ships/" + getShortName() + "_" + shipName + ".txt");
		Ship ship = null;
		try{
			BufferedReader in = new BufferedReader(new FileReader(file));
			ship = Ship.read(in);
			in.close();
		}catch (IOException ex){
			Main.crash(file.getPath());
		}
		return ship;
	}
	
	public String getShortName(){
		int index = -1;
		for (int i = 0; i < Main.missions.length; i++){
			if (Main.missions[i] == this)
				index = i;
		}
		return index < 0 ? "mis" : "mis" + index;
	}
	
	public String getEndString(){
		return "End mission";
	}
	
	public void drawBackground(Graphics2D g, GameWindow window){
		Arena.background.drawBackground(g, window);
	}
	
	public ComputerPlayer getAlliedPlayer(){
		for (Player player : game.players){
			if (player instanceof ComputerPlayer && player.team == 1)
				return (ComputerPlayer)player;
		}
		return null;
	}
	
	public static int compBudget(int budget){
		return (int)(budget*(1.5 + 0.2*Main.options.difficulty));
	}
	
	private class MissionPlayer{
		int team;
		int budget;
		Vector<Ship> ships;
		
		public MissionPlayer(int team, int budget){
			this.team = team;
			this.budget = budget;
			
			ships = new Vector<Ship>();
		}
	}
	
}
