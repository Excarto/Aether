import java.awt.image.*;

public interface Repairable {
	public void repair(double material, boolean isScrap);
	public BuyType getType();
	public double getHull();
	public BufferedImage getIcon();
	public Player getPlayer();
}
