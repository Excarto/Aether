import java.awt.*;

// Anything that can be given orders in-game

public interface Controllable extends Id, Locatable{
	public SidePanel getMenu();
	public void restoreMenuState();
	public boolean isVisible(Sprite sprite);
	public void move();
	public void act();
	public void postMove();
	public OrderQueue orders();
	public void removeControllable(Controllable controllable);
	public void takeHit(double posX, double posY,
			Component subTarget, double direction, boolean continuous,
			double explosiveDamage, double kineticDamage, double EMPDamage);
	public void explode();
	public void setIsNetworkThrusting(int direction, boolean isThrusting);
	public double getAccel();
	public double getTurnAccel();
	public double getEnergy();
	public double getHull();
	public int getSize();
	public double getMass();
	public void accelForward();
	public void accelTurn(boolean direction);
	public void stopTurn();
	public Player getPlayer();
	public BuyType getType();
	public boolean[][] getContactMap();
	public void drawHudBottom(Graphics2D g, GameWindow window);
	public void drawHudTop(Graphics2D g, GameWindow window);
	public void drawOrders(Graphics2D g, GameWindow window);
	public void drawVision(Graphics2D g, GameWindow window);
}
