import static java.lang.Math.*;
import java.awt.*;

public abstract class LocatableOrder extends Order implements Locatable{
	static final int BOX_SHRINK_RATE = max(1, 150/Main.TPS);
	static final int BOX_MAX_SIZE = 17;
	static final int LINE_LEN = 200;
	static final int LINE_GAP_LEN = 16;
	static final double LINE_SPEED = 130.0/Main.TPS;
	static final int ARROW_SIZE = 5;
	
	public final int creationTurn;
	
	private boolean drawArrow;
	
	public LocatableOrder(){
		creationTurn = Main.game.turn;
		this.drawArrow = false;
	}
	
	public void setDrawArrow(boolean drawArrow){
		this.drawArrow = drawArrow;
	}
	
	public double draw(Graphics2D g, GameWindow window, double fromPosX, double fromPosY){
		if (getColor() == null)
			return 0.0;
		if (!Double.isFinite(getRenderPosX()) || !Double.isFinite(getRenderPosY()))
			return 0.0;
		
		g.setColor(getColor());
		
		int posX = window.posXOnScreen(getRenderPosX());
		int posY = window.posYOnScreen(getRenderPosY());
		fromPosX = window.posXOnScreen(fromPosX);
		fromPosY = window.posYOnScreen(fromPosY);
		
		double dX = posX - fromPosX;
		double dY = posY - fromPosY;
		double mag = sqrt(dX*dX + dY*dY);
		double rateX = dX/mag;
		double rateY = dY/mag;
		
		double offset = (Main.game.turn*LINE_SPEED)%LINE_LEN;
		
		while (mag > 1){
			
			double endPosX, endPosY;
			if (offset-LINE_GAP_LEN < mag){
				double drawLen = max(0, offset-LINE_GAP_LEN);
				endPosX = fromPosX + rateX*drawLen;
				endPosY = fromPosY + rateY*drawLen;
			}else{
				endPosX = posX;
				endPosY = posY;
			}
			if (window.isInWindow((int)fromPosX, (int)fromPosY) || window.isInWindow((int)endPosX, (int)endPosY))
				g.drawLine((int)fromPosX, (int)fromPosY, (int)endPosX, (int)endPosY);
			
			if (drawArrow && offset-LINE_GAP_LEN < mag && offset-LINE_GAP_LEN > LINE_LEN/2){
				double arrowPosX = fromPosX + rateX*LINE_LEN/2;
				double arrowPosY = fromPosY + rateY*LINE_LEN/2;
				for (int dir = -1; dir <= 1; dir += 2){
					double arrowEndX = arrowPosX + (-rateX + dir*rateY)*ARROW_SIZE;
					double arrowEndY = arrowPosY + (-rateY - dir*rateX)*ARROW_SIZE;
					g.drawLine((int)arrowPosX, (int)arrowPosY, (int)arrowEndX, (int)arrowEndY);
				}
			}
			
			fromPosX += rateX*offset;
			fromPosY += rateY*offset;
			mag -= offset;
			offset = LINE_LEN;
		}
		
		//g.drawLine(lastPosX, lastPosY, posX, posY);
		int timeAlive = Main.game.turn - creationTurn;
		int drawSize = max(3, BOX_MAX_SIZE - BOX_SHRINK_RATE*timeAlive);
		g.drawRect(posX-drawSize, posY-drawSize, 2*drawSize, 2*drawSize);
		
		return mag/LINE_SPEED;
	}
	
}
