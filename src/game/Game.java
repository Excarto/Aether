import static java.lang.Math.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public final class Game{
	static final int START_COUNTDOWN_TIME = 10;
	static final int START_COUNTDOWN_TIME_BUY = 20;
	static final int DEFAULT_MAX_EFFECTS = 450;
	static final double ZOOM_VOLUME_SCALING = 8.0;
	
	public static int freeMem = 1000;
	private static Sound deathSeqSound;
	
	public final List<Player> players;
	public final List<Sprite> effects;
	public final List<ExplodingShip> explodingShips;
	public final Arena arena;
	public final Mission mission;
	public final double gameSpeed;
	public final double turnsPerSecond;
	public int turn, activeTurn;
	
	final Queue<QueuedMsg> msgQueue;
	final List<Controllable> toRemove;
	final int timePerTurn, maxEffects;
	final ExplosionType sequenceExplosion;
	GameWindow window;
	PeriodicTimer timer;
	boolean running, paused, isLocal;
	int victoryTeam;
	int numTeams;
	int gameStartTurn;
	long turnStartTime;
	
	private final double[] score, scoreIncrement;
	
	private double realTurn;
	private boolean[] playerLeft;
	
	public Game(Arena arena, Mission mission, double gameSpeed){
		this.players = new ArrayList<Player>();
		this.arena = arena;
		this.mission = mission;
		isLocal = true;
		turn = 0;
		activeTurn = 0;
		realTurn = 0.0;
		victoryTeam = -1;
		this.gameSpeed = gameSpeed;
		timePerTurn = (int)round(1000/(Main.TPS*gameSpeed));
		maxEffects = (int)(Main.options.debrisAmount*DEFAULT_MAX_EFFECTS);
		turnsPerSecond = Main.TPS*gameSpeed;
		score = new double[arena.teamPos.length];
		scoreIncrement = new double[arena.teamPos.length];
		explodingShips = new ArrayList<ExplodingShip>();
		sequenceExplosion = Main.getExplosionType(Main.options.explodingShipExplosion);
		toRemove = new ArrayList<Controllable>();
		effects = new Vector<Sprite>();
		
		//gameThread = new Thread("GameThread"){
		//	public void run(){
		//		timer.run();
		//		synchronized (this){
		//			this.notifyAll();
		//		}
		//}};
		//gameThread.setPriority(Thread.MAX_PRIORITY);
		
		timer = new PeriodicTimer(timePerTurn, Thread.MAX_PRIORITY, 10){
			public void runTimerTask(){
				runTurn();
		}};
		
		msgQueue = new ConcurrentLinkedQueue<QueuedMsg>();
	}
	
	public void endGame(int victoryTeam){
		if (this.victoryTeam < 0){
			this.victoryTeam = victoryTeam;
			for (Player player : players)
				player.gameEnd(victoryTeam);
			if (mission != null)
				mission.end(victoryTeam);
		}
	}
	
	public void start(int backgroundSeed, int gameSeed){
		Sprite.resetId();
		Component.resetId();
		
		arena.initialize();
		if (mission == null){
			Arena.background.initialize(backgroundSeed);
		}else{
			Arena.background.initialize(backgroundSeed, mission.nebulaIndex, mission.nebulaOpacity, mission.cloudOpacity, mission.planets);
		}
		
		numTeams = 0;
		for (Player player : players)
			numTeams = max(player.team, numTeams);
		
		playerLeft = new boolean[players.size()];
		
		Random positionRand = gameSeed == 0 ? new Random() : new Random(gameSeed);
		int[] teamPositions = new int[arena.teamPos.length];
		for (int x = 0; x < teamPositions.length; x++){
			if (x == 0 && mission != null){
				teamPositions[x] = 0;
			}else{
				boolean taken;
				do{
					taken = false;
					teamPositions[x] = positionRand.nextInt(arena.teamPos.length);
					for (int y = 0; y < x; y++)
						taken = taken || teamPositions[y] == teamPositions[x];
				}while (taken);
			}
		}
		
		running = true;
		for (int x = 0; x < players.size(); x++)
			players.get(x).start(teamPositions[players.get(x).team-1]);
		for (Player player : players){
			if (player instanceof HumanPlayer)
				window = ((HumanPlayer)player).getWindow();
			player.setVisibleSprites();
		}
		
		if (mission == null){
			gameStartTurn = (arena.startBudget >= 1.0 ? START_COUNTDOWN_TIME : START_COUNTDOWN_TIME_BUY)*Main.TPS;
		}else{
			gameStartTurn = 0;
			mission.gameStarted(this);
		}
		
		timer.start();
	}
	
	public void stop(){
		//synchronized (this){
		running = false;
		timer.stop(true);
			//try{
			//	this.wait();
			//}catch (InterruptedException e){}
		//}
	}
	
	public void addPlayer(Player player){
		if (player instanceof HostPlayer || player instanceof NetPlayerClient)
			isLocal = false;
		for (int x = 0; x <= players.size(); x++){
			if (x < players.size()){
				if (players.get(x).name.compareTo(player.name) > 0){
					players.add(x, player);
					break;
				}
			}else{
				players.add(player);
				break;
			}
		}
	}
	
	public void updateTime(double newTime){
		realTurn = ((UpdateMsg.TIME_UPDATE_FACTOR-1)*realTurn + newTime)/UpdateMsg.TIME_UPDATE_FACTOR;
	}
	
	public double getScore(int team){
		return score[team-1];
	}
	
	public double getScoreIncrement(int team){
		return scoreIncrement[team-1];
	}
	
	public int turnsToStart(){
		return gameStartTurn - turn;
	}
	
	private void runTurn(){
		realTurn++;
		turn = (int)round(realTurn);
		
		window.renderFrame();
		
		freeMem = (int)(Runtime.getRuntime().freeMemory()/1000000);
		
		if (!isSuspended()){
			activeTurn++;
			
			try{
				for (int x = 0; x < effects.size(); x++)
					effects.get(x).move();
				
				arena.move();
				if (mission != null)
					mission.act();
				
				for (int x = 0; x < players.size(); x++)
					players.get(x).move();
			}catch (Exception ex){
				ex.printStackTrace();
			}
			
			try{
				for (int x = 0; x < effects.size(); x++)
					effects.get(x).act();
				for (int x = 0; x < explodingShips.size(); x++)
					explodingShips.get(x).act();
			}catch (Exception ex){
				ex.printStackTrace();
			}
			
			try{
				arena.act();
				
				for (int x = 0; x < players.size(); x++)
					players.get(x).act();
				
				while (!toRemove.isEmpty()){
					Controllable controllable = toRemove.remove(toRemove.size()-1);
					for (Player player : players)
						player.removeControllable(controllable);
				}
				
				int index = 0;
				while (effects.size() > maxEffects){
					Sprite effect = effects.get(index);
					if (!(effect instanceof Unit)){
						removeGraphic(effect);
					}else
						index++;
				}
				
				for (int x = 0; x < scoreIncrement.length; x++)
					scoreIncrement[x] = 0.0;
				for (Arena.Objective objective : arena.objectives){
					if (objective.isCaptured)
						scoreIncrement[objective.owner-1] += objective.value/Main.TPS;
				}
				for (int x = 0; x < score.length; x++)
					score[x] += scoreIncrement[x];
				
				checkVictoryConditions();
				
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
		
		try{
			while (!msgQueue.isEmpty())
				msgQueue.poll().received();
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		try{
			for (int x = 0; x < players.size(); x++)
				players.get(x).postMove();
		}catch (Exception ex){
			ex.printStackTrace();
		}
	}
	
	private void checkVictoryConditions(){
		for (int x = 0; x < score.length; x++){
			if (arena.maxScore > 0 && score[x] >= arena.maxScore)
				endGame(x+1);
		}
		
		if (turn%64 == 0){
			boolean[] canCapture = new boolean[numTeams];
			boolean[] hasControllables = new boolean[numTeams];
			boolean[] hasShips = new boolean[numTeams];
			for (Player player : players){
				if (player.canCapture())
					canCapture[player.team-1] = true;
				if (!player.controllables.isEmpty())
					hasControllables[player.team-1] = true;
				if (!player.ships.isEmpty())
					hasShips[player.team-1] = true;
			}
			
			for (int x = 1; x <= numTeams; x++){
				boolean enemyCanCap = false;
				boolean enemyHasControllables = false;
				boolean enemyHasShips = false;
				for (int y = 1; y <= numTeams; y++){
					if (y != x){
						enemyCanCap = enemyCanCap || canCapture[y-1];
						enemyHasControllables = enemyHasControllables || hasControllables[y-1];
						enemyHasShips = enemyHasShips || hasShips[y-1];
					}
				}
				if (!enemyCanCap){
					if (canCapture[x-1] && enemyHasShips && !enemyHasControllables && getTimeToLose(x) >= Double.MAX_VALUE)
						endGame(x);
					if (getTimeToWin(x) < 0.9*getTimeToLose(x))
						endGame(x);
				}
			}
		}
	}
	
	public boolean isSuspended(){
		return paused || turnsToStart() > 0;
	}
	
	public int getVictoryTeam(){
		return victoryTeam;
	}
	
	public double getTimeToLose(int team){
		double timeToLose = Double.MAX_VALUE;
		for (int x = 1; x <= numTeams; x++){
			if (x != team){
				double time = (arena.maxScore - getScore(x))/getScoreIncrement(x);
				if (Double.isFinite(time))
					timeToLose = min(timeToLose, time);
			}
		}
		return timeToLose;
	}
	
	public double getTimeToWin(int team){
		double time = (arena.maxScore - getScore(team))/getScoreIncrement(team);
		if (Double.isFinite(time))
			return time;
		return Double.MAX_VALUE;
	}
	
	public boolean isRunning(){
		return running;
	}
	
	public void startExploding(Ship ship){
		addGraphic(ship);
		explodingShips.add(new ExplodingShip(ship));
	}
	
	public void createDeathExplosion(BuyType type,
			double posX, double posY, double velX, double velY){
		
		addDebris(type.debrisSize, type.maxDebrisPiece, 0.09*type.deathExplosion.size, posX, posY, velX, velY);
		Explosion explosion = new Explosion(type.deathExplosion, posX, posY, velX, velY);
		addGraphic(explosion);
		type.deathSound.play();
	}
	
	public void addDebris(int debrisSize, int maxDebrisPiece, double radius,
			double posX, double posY, double velX, double velY){
		int numPieces = (int)sqrt(3*Main.options.debrisAmount*debrisSize);
		while (numPieces-- > 0){
			DebrisType newDebrisType = Main.debrisTypes[(int)(maxDebrisPiece*pow(random(), 6))];
			Debris debris = new Debris(newDebrisType);
			double posMagnitude = random()*radius;
			double velMagnitude = random()*2.3/pow(newDebrisType.size, 0.14);
			double angle = random()*2*PI;
			debris.place(posX+sin(angle)*posMagnitude, posY-cos(angle)*posMagnitude,
					velX+sin(angle)*velMagnitude, velY-cos(angle)*velMagnitude,
					random()*360-180, (random()-0.5)*18.0/pow(newDebrisType.size, 0.25));
			addGraphic(debris);
		}
	}
	
	public void addGraphic(Sprite sprite){
		effects.add(sprite);
		for (Player player : players){
			if (player instanceof HumanPlayer && player.isTeamVisible(sprite))
				((HumanPlayer)player).visibleGraphics.add(sprite);
		}
	}
	
	public void removeGraphic(Sprite effect){
		effects.remove(effect);
		for (Player player : players)
			player.removeEffect(effect);
	}
	
	public void removeControllable(Controllable controllable){
		toRemove.add(controllable);
	}
	
	public void playSound(Sound sound, Controllable source, boolean falloff){
		HumanPlayer player = getHuman();
		if (player != null && player.visibleControllables.contains(source)){
			if (falloff){
				GameWindow window = player.getWindow();
				double dist = hypot(window.posXOnScreen(source.getPosX()), window.posYOnScreen(source.getPosY()));
				double distFactor = exp(-pow(Main.options.soundFalloffRate*dist, 2));
				double zoomFactor = (1 + ZOOM_VOLUME_SCALING)/(ZOOM_VOLUME_SCALING + 1.0/window.getZoom());
				double volume = Main.options.minSoundVolume + (1-Main.options.minSoundVolume)*distFactor*zoomFactor;
				sound.play(volume);
			}else
				sound.play();
		}
	}
	
	public HumanPlayer getHuman(){
		for (Player player : players){
			if (player instanceof HumanPlayer)
				return (HumanPlayer)player;
		}
		return null;
	}
	
	public void playerLeft(String name){
		for (int x = 0; x < players.size(); x++){
			if (players.get(x).name.equals(name))
				playerLeft[x] = true;
		}
		int winningTeam = -1;
		for (int x = 0; x < players.size(); x++){
			if (!playerLeft[x]){
				if (winningTeam == -1){
					winningTeam = players.get(x).team;
				}else{
					if (players.get(x).team != winningTeam)
						return;
				}
			}
		}
		if (winningTeam != -1)
			endGame(winningTeam);
	}
	
	public synchronized void sendMsg(Message msg){
		Player except = null;
		if (msg instanceof UpdateMsg)
			except = ((UpdateMsg)msg).player;
		for (Player player : players){
			if (player instanceof NetPlayerHost && player != except)
				((NetPlayerHost)player).connection.send(msg);
		}
	}
	
	public boolean isLocal(){
		return isLocal;
	}
	
	public void setPause(boolean paused){
		this.paused = paused;
	}
	
	public synchronized void queue(Message msg, byte[] data){
		msgQueue.add(new QueuedMsg(msg, data));
	}
	
	private class QueuedMsg{
		byte[] data;
		Message msg;
		
		public QueuedMsg(Message msg, byte[] data){
			this.msg = msg;
			int length = 2*(data[1] & 0xFF);
			this.data = new byte[length];
			for (int x = 0; x < length; x++)
				this.data[x] = data[x];
		}
		
		public void received(){
			msg.read(data);
		}
	}
	
	public static void load(){
		deathSeqSound = new Sound(new File("data/death_sequence.wav"));
		deathSeqSound.load();
	}
	
	static final int MIN_EXPLOSIONS = 4;
	public class ExplodingShip{
		public final Ship ship;
		
		private final int[] explosionTimes;
		private final int maxAge;
		private int age;
		private Random random;
		
		public ExplodingShip(Ship ship){
			this.ship = ship;
			random = new Random(Unit.RANDOM.nextInt());
			
			maxAge = (int)(ship.type.deathTime*(0.8 + 0.5*random.nextDouble()));
			int timePerExplosion = ShipType.minDeathTime/MIN_EXPLOSIONS;
			int numExplosions = ship.type.deathTime/timePerExplosion;
			if ((ship.type.deathTime - numExplosions*timePerExplosion)/(double)timePerExplosion > random.nextDouble())
				numExplosions++;
			explosionTimes = new int[numExplosions];
			for (int x = 0; x < numExplosions; x++)
				explosionTimes[x] = (int)(random.nextDouble()*(ship.type.deathTime - sequenceExplosion.duration/2));
		}
		
		public void act(){
			age++;
			
			for (int x = 0; x < explosionTimes.length; x++){
				if (explosionTimes[x] == age){
					boolean[][] map = ship.getContactMap();
					int width = map.length, height = map[0].length;
					int posX = 0, posY = 0;
					do{
						posX = random.nextInt(width);
						posY = random.nextInt(height);
					}while (!map[posX][posY]);
					
					double explosionPosX = ship.getPosX() + posX - width/2;
					double explosionPosY = ship.getPosY() + posY - height/2;
					addGraphic(new Explosion(sequenceExplosion, explosionPosX, explosionPosY, ship.getVelX(), ship.getVelY()));
					addDebris(160, 6, 4.0, explosionPosX, explosionPosY, ship.getVelX(), ship.getVelY());
					deathSeqSound.play();
				}
			}
			
			if (age == maxAge-Main.TPS/6){
				createDeathExplosion(ship.type, ship.getPosX(), ship.getPosY(), ship.getVelX(), ship.getVelY());
				for (Player player : players){
					for (Controllable controllable : player.controllables){
						Sprite sprite = (Sprite)controllable;
						double distance = max(1.0, ship.distance(sprite)/(ship.type.deathExplosion.size/2));
						sprite.accel(ship.type.deathExplosionImpulse/(distance*distance)/sqrt(controllable.getType().mass), ship.bearing(sprite));
					}
				}
			}
			if (age >= maxAge){
				removeGraphic(ship);
				explodingShips.remove(this);
			}
		}
	}
	
}
