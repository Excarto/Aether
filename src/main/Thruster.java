import static java.lang.Math.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.*;

public class Thruster{
	public static final int FORWARD = 0, LEFT = 1, RIGHT = 2;
	public enum ZOrder{TOP, MIDDLE, BOTTOM};
	
	static int numTypes;
	static Renderable[] opaqueRenderable, transparentRenderable;
	static int[] thrustSize;
	static int framesBetweenSwitch;
	
	final int type;
	final double posX, posY, posZ;
	final int direction;
	final ZOrder zOrder;
	final double scale, renderAngle;
	final double angle;
	final double zAngle;
	
	private double centerDistX, centerDistY, posYShift, centerAngle;
	private int lastSwitchFrame;
	private boolean transparent;
	
	public Thruster(int type, double posX, double posY, double posZ, double angleOffset, double zAngle,
			int direction, int zOrder, double thrust, double renderAngle){
		this.type = type;
		this.scale = sqrt(thrust/Main.thrustScale);
		this.renderAngle = renderAngle;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
		this.direction = direction;
		this.zAngle = zAngle;
		if (zOrder < 0){
			this.zOrder = ZOrder.BOTTOM;
		}else if (zOrder > 0){
			this.zOrder = ZOrder.TOP;
		}else
			this.zOrder = ZOrder.MIDDLE;
		
		numTypes = max(numTypes, type+1);
		framesBetweenSwitch = Main.framesPerSec/55;
		
		double angle = angleOffset;
		if (direction == LEFT){
			angle += Game.fixAngle(toDegrees(atan2(posX, -posY)) - 90);
		}else if (direction == RIGHT)
			angle += Game.fixAngle(toDegrees(atan2(posX, -posY)) + 90);
		this.angle = angle;
	}
	
	public void setHostSize(double size){
		double shiftedPosX = posX*size/2 - sin(toRadians(angle))*scale*thrustSize[type]/2;
		double shiftedPosY = posY*size/2 + cos(toRadians(angle))*scale*thrustSize[type]/2;
		centerDistX = hypot(shiftedPosX, shiftedPosY);
		centerDistY = centerDistX*cos(toRadians(renderAngle));
		posYShift = -size/2*posZ*sin(toRadians(renderAngle));
		centerAngle = 90-toDegrees(atan2(-shiftedPosY, shiftedPosX));
	}
	
	public void draw(Graphics2D g, GameWindow window, Sprite sprite, boolean above, BufferedImageOp operation){
		//BufferedImage img = getImage(window.getRenderZoom(), sprite);
		if (lastSwitchFrame > window.getFrameCount())
			lastSwitchFrame = 0;
		
		if (window.getFrameCount() > lastSwitchFrame+framesBetweenSwitch){
			transparent = !transparent;
			lastSwitchFrame = window.getFrameCount();
		}
		
		Renderable renderable = transparent ? transparentRenderable[type] : opaqueRenderable[type];
		Image img = renderable.getImage(window.getRenderZoom()*scale, sprite.renderAngle+angle, true, 0);
		
		if (img != null && above == above(sprite.getAngle())){
			if (operation != null){
				if (img instanceof BufferedImage){
					img = operation.filter((BufferedImage)img, null);
				}else{
					BufferedImage snapshot = ((VolatileImage)img).getSnapshot();
					img = operation.filter(snapshot, null);
					snapshot.flush();
				}
			}
			int posX = -img.getWidth(null)/2 + window.posXOnScreen(
					sprite.renderPosX + window.getRenderZoom()/window.getZoom()*getPosX(sprite));
			int posY = -img.getHeight(null)/2 + window.posYOnScreen(
					sprite.renderPosY + window.getRenderZoom()/window.getZoom()*getPosY(sprite));
			g.drawImage(img, posX, posY, null);
			if (operation != null)
				img.flush();
		}
	}
	
	private final double getPosX(Sprite sprite){
		return +centerDistX*sin(toRadians(centerAngle+sprite.renderAngle));
	}
	private final double getPosY(Sprite sprite){
		return -centerDistY*cos(toRadians(centerAngle+sprite.renderAngle)) + posYShift;
	}
	
	private boolean above(double angle){
		if (zOrder == ZOrder.TOP)
			return true;
		if (zOrder == ZOrder.BOTTOM)
			return false;
		return abs(Game.fixAngle(angle + zAngle)) < 90;
	}
	
	public static void load(){
		opaqueRenderable = new Renderable[numTypes];
		transparentRenderable = new Renderable[numTypes];
		thrustSize = new int[numTypes];
		
		for (int type = 0; type < numTypes; type++){
			File file = new File("data/thrusters/"+(type+1)+".png");
			BufferedImage image = null;
			try{
				image = ImageIO.read(file);
			}catch(IOException e){
				Main.crash(file.getPath());
			}
			opaqueRenderable[type] = new Renderable(1, UnitType.maxNumRenderAngles/2);
			opaqueRenderable[type].load(new BufferedImage[]{image}, 1.0);
			WritableRaster raster = image.getAlphaRaster();
			for (int x = raster.getMinX(); x < raster.getWidth(); x++){
				for (int y = raster.getMinY(); y < raster.getHeight(); y++)
					raster.setSample(x, y, 0, raster.getSample(x, y, 0)/3);
			}
			transparentRenderable[type] = new Renderable(3, UnitType.maxNumRenderAngles/2);
			transparentRenderable[type].load(new BufferedImage[]{image}, 1.0);
			
			thrustSize[type] = opaqueRenderable[type].getImage(1.0, 0.0, false, 0).getHeight(null);
		}
	}
}
