import java.util.*;

public class HumanPlayer extends Player{
	
	private GameWindow window;
	private boolean manualAim;
	public final Set<Sprite> visibleGraphics;
	
	public HumanPlayer(String name, int team, List<Ship> ships, int budget, Arena arena){
		super(name, team, ships, budget, arena);
		visibleGraphics = new HashSet<Sprite>();
		//turnsPerFrame = Window.getTurnsPerFrame(Main.framesPerSec, gameSpeed);
	}
	
	public void setWindow(GameWindow window){
		this.window = window;
	}
	public GameWindow getWindow(){
		return window;
	}
	
	public void act(){
		super.act();
		window.move();
		
		if (turnsPassed%27 == 0)
			setVisibleGraphics();
	}
	
	public void postMove(){
		window.act();
		manualAim = window.getInputHandler().controlPressed(Control.MANUAL_AIM) && window.isInWindow(Main.getMousePosition());
		
		super.postMove();
		//window.render();
		//if (postMoveTime++%turnsPerFrame == 0)
		//	window.render();
	}
	
	public void start(int position){
		super.start(position);
		window.initialize();
	}
	
	public void setVisibleGraphics(){
		visibleGraphics.clear();
		
		for (Player player : Main.game.players){
			for (Projectile projectile : player.projectiles){
				if (player.team == team || isTeamVisible(projectile))
					visibleGraphics.add(projectile);
			}
		}
		for (Sprite effect : Main.game.effects){
			if (isTeamVisible(effect))
				visibleGraphics.add(effect);
		}
	}
	
	public void removeControllable(Controllable controllable){
		super.removeControllable(controllable);
		window.removeControllable(controllable);
	}
	
	public void removeEffect(Sprite effect){
		visibleGraphics.remove(effect);
	}
	
	public void updateBudget(){
		super.updateBudget();
		window.updateBudget();
	}
	
	public void contact(Projectile projectile, Controllable controllable){
		if (projectile.contact(controllable))
			controllable.explode();
	}
	
	public void contact(Beam beam, Controllable controllable, double posX, double posY){
		if (beam.contact(controllable, posX, posY))
			controllable.explode();
	}
	
	public void sendMessage(String message, boolean toAll){}
	
	public void gameEnd(int victoryTeam){
		super.gameEnd(victoryTeam);
		if (team == victoryTeam){
			window.triggerVictory();
		}else
			window.triggerDefeat();
	}
	
	public boolean manualAim(Unit unit){
		return manualAim && window.isSelected(unit);
	}
	
	public boolean isMaster(){
		return true;
	}
	
	public boolean controlPressed(Control control){
		if (window == null)
			return false;
		return window.getInputHandler().controlPressed(control);
	}
}
