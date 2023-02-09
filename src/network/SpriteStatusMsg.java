import java.io.*;

// Send periodically over UDP to update status of a batch of sprites at a time

public class SpriteStatusMsg extends UpdateMsg{
	public byte getId(){return MsgId.SPRITE_STATUS.id;}
	
	public static final int UPDATE_INTERVAL = Main.TPS/10;
	public static final int NUM_SPRITES = 15; // Determined by how many can fit in a network packet
	
	public int numSprites;
	public Sprite[] sprites;
	public double[] posX, posY, velX, velY, angle, turnSpeed;
	public boolean[][] isThrusting;
	
	public SpriteStatusMsg(){
		sprites = new Sprite[NUM_SPRITES];
		posX = new double[NUM_SPRITES];
		posY = new double[NUM_SPRITES];
		velX = new double[NUM_SPRITES];
		velY = new double[NUM_SPRITES];
		angle = new double[NUM_SPRITES];
		turnSpeed = new double[NUM_SPRITES];
		isThrusting = new boolean[NUM_SPRITES][3];
	}
	
	public void encode(DataOutputStream stream) throws IOException{
		super.encode(stream);
		
		stream.writeByte(numSprites);
		for (int x = 0; x < numSprites; x++){
			stream.writeShort(sprites[x].getId());
			stream.writeFloat((float)sprites[x].getPosX());
			stream.writeFloat((float)sprites[x].getPosY());
			stream.writeFloat((float)sprites[x].getVelX());
			stream.writeFloat((float)sprites[x].getVelY());
			stream.writeFloat((float)sprites[x].getAngle());
			stream.writeFloat((float)sprites[x].getTurnSpeed());
			
			Thruster[][] thrusters = sprites[x].getThrusters();
			byte thrustingByte = 0;
			for (int y = 0; y < thrusters.length; y++){
				if (thrusters[y].length > 0)
					thrustingByte += 1 << y;
			}
			stream.writeByte(thrustingByte);
		}
	}
	public void decode(DataInputStream stream) throws IOException{
		super.decode(stream);
		
		numSprites = stream.readByte();
		for (int x = 0; x < numSprites; x++){
			sprites[x] = (Sprite)player.controllables.getById(stream.readShort());
			posX[x] = stream.readFloat();
			posY[x] = stream.readFloat();
			velX[x] = stream.readFloat();
			velY[x] = stream.readFloat();
			angle[x] = stream.readFloat();
			turnSpeed[x] = stream.readFloat();
			
			byte thrustingByte = stream.readByte();
			for (int y = 0; y < isThrusting[x].length; y++)
				isThrusting[x][y] = ((thrustingByte >> y) & 1) == 1;
		}
	}
	
	protected boolean isGood(){
		for (int x = 0; x < numSprites; x++)
			if (sprites[x] == null)
				return false;
		return true;
	}
	
	public void confirmed(){
		for (int x = 0; x < numSprites; x++){
			if (sprites[x].lastUpdate < time && (Main.game.turn-time) < 2*Main.TPS){
				sprites[x].adjustTo(
						posX[x]+(Main.game.turn-time)*velX[x],
						posY[x]+(Main.game.turn-time)*velY[x],
						velX[x],
						velY[x],
						angle[x]+(Main.game.turn-time)*turnSpeed[x],
						turnSpeed[x]
				);
				for (int y = 0; y < isThrusting[x].length; y++)
					sprites[x].setIsNetworkThrusting(y, isThrusting[x][y]);
				sprites[x].lastUpdate = time;
			}
		}
	}
}
