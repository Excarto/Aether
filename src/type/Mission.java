import java.util.*;
import java.io.*;

public class Mission extends Type{
	
	final String name;
	final Arena arena;
	final String[][] shipNames;
	final int maxBudget;
	final int randomSeed;
	Game game;
	HumanPlayer humanPlayer;
	Queue<TextEvent> textQueue;
	
	public Mission(String folder){
		super("missions/" + folder);
		name = folder;
		
		maxBudget = getInt("budget");
		randomSeed = getInt("random_seed");
		
		String arenaName = getString("arena").toLowerCase();
		Arena arena = null;
		for (int x = 0; x < Main.arenas.length; x++){
			if (Main.arenas[x].arenaName.replaceAll("\\s+","").replace("_","").toLowerCase().equals(arenaName))
				arena = Main.arenas[x];
		}
		if (arena == null)
			Main.crash(directory);
		this.arena = arena;
		
		int numPlayers = 0;
		while (getString("player" + (numPlayers+1) + "_ship1") != null)
			numPlayers++;
		
		shipNames = new String[numPlayers][];
		for (int i = 0; i < numPlayers; i++){
			int numShips = 1;
			while (getString("player" + (i+1) + "_ship" + (numShips+1)) != null)
				numShips++;
			shipNames[i] = new String[numShips];
			for (int j = 0; j < numShips; j++)
				shipNames[i][j] = getString("player" + (i+1) + "_ship" + (j+1));
		}
		
		textQueue = new ArrayDeque<TextEvent>();
		File textFile = new File(directory + "/text.txt");
		if (textFile.exists()){
			double totalTime = 0.0;
			try{
				BufferedReader input = new BufferedReader(new FileReader(textFile));
				String line;
				while((line = input.readLine()) != null){
					if (!line.trim().isEmpty() && line.charAt(0) != '#'){
						if (line.startsWith("wait")){
							totalTime += Integer.parseInt(line.substring(5));
						}else if (line.startsWith("display")){
							int index = 8;
							while (line.charAt(index) != ' ')
								index++;
							int displayTime = Integer.parseInt(line.substring(8, index));
							String displayText = line.substring(index+1);
							textQueue.add(new TextEvent(displayText, totalTime, displayTime));
						}else
							Main.crash(textFile.toString());
					}
				}
				input.close();
			}catch (Exception e){
				e.printStackTrace();
				Main.crash(textFile.toString());
			}
		}
	}
	
	public void start(){
		game = new Game(arena, this, 1.0);
		
		for (int i = 0; i < shipNames.length; i++){
			List<Ship> ships = new ArrayList<Ship>(shipNames[i].length);
			for (int j = 0; j < shipNames[i].length; j++){
				try{
					File file = new File(directory + "/" + shipNames[i][j] + ".txt");
					BufferedReader in = new BufferedReader(new FileReader(file));
					ships.add(Ship.read(in));
					in.close();
				}catch (IOException ex){
					Main.crash(directory);
				}
			}
			
			int team = i+1;
			
			Player player;
			if (i == 0){
				humanPlayer = new HumanPlayer(Main.username, team, ships, maxBudget, arena);
				player = humanPlayer;
			}else
				player = new ComputerPlayer(Main.username, team, ships, maxBudget, arena);
			game.addPlayer(player);
		}
		
		Main.startGame(game, randomSeed);
	}
	
	public void act(){
		TextEvent event = textQueue.peek();
		if (event != null){
			//Main.print(game.turn+" "+event.executeTurn+" "+event.lifetime+" "+event.text);
			if (game.turn >= event.executeTurn){
				textQueue.poll();
				humanPlayer.getWindow().receiveMessage(event.text, 0, event.lifetime);
				//Main.print(event.text+" "+event.lifetime);
			}
		}
	}
	
	private class TextEvent{
		int executeTurn;
		int lifetime;
		String text;
		
		public TextEvent(String text, double executeTime, double lifetime){
			this.text = text;
			this.executeTurn = (int)(executeTime*Main.TPS);
			this.lifetime = (int)(lifetime*Main.TPS);
		}
	}
}
