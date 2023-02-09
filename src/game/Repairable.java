import java.awt.image.*;

// Anything that can be repaired or scrapped by a ship

public interface Repairable {
	public void repair(double material, boolean isScrap);
	public BuyType getType();
	public double getHull();
	public BufferedImage getIcon();
	public Player getPlayer();
}
