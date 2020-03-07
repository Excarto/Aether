import java.io.*;

public class ContactMsg extends InGameMsg{
	public byte getId(){return MsgId.CONTACT.id;}
	
	public Projectile projectile;
	public Controllable target;
	public double posX, posY;
	public double velX, velY;
	public int randomSeed;
	public boolean explodes;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeBoolean(projectile instanceof Missile);
		stream.writeByte(Main.game.players.indexOf(projectile.player));
		stream.writeShort(projectile.getId());
		stream.writeByte(Main.game.players.indexOf(target.getPlayer()));
		stream.writeShort(target.getId());
		stream.writeFloat((float)posX);
		stream.writeFloat((float)posY);
		stream.writeFloat((float)velX);
		stream.writeFloat((float)velY);
		stream.writeInt(randomSeed);
		stream.writeBoolean(explodes);
	}
	public void decode(DataInputStream stream) throws IOException{
		boolean isMissile = stream.readBoolean();
		Player projPlayer = Main.game.players.get(stream.readByte());
		projectile = (Projectile)(isMissile ?
				projPlayer.controllables.getById(stream.readShort()) :
				projPlayer.projectiles.getById(stream.readShort()));
		Player targetPlayer = Main.game.players.get(stream.readByte());
		target = targetPlayer.controllables.getById(stream.readShort());
		posX = stream.readFloat();
		posY = stream.readFloat();
		velX = stream.readFloat();
		velY = stream.readFloat();
		randomSeed = stream.readInt();
		explodes = stream.readBoolean();
	}
	
	protected boolean isGood(){
		return projectile != null && target != null;
	}
	
	public void confirmed(){
		Sprite sprite = (Sprite)target;
		Unit.RANDOM.setSeed(randomSeed);
		projectile.place(sprite.getPosX()+posX, sprite.getPosY()+posY,
				sprite.getVelX()+velX, sprite.getVelY()+velY,
				projectile.getAngle(), projectile.getTurnSpeed());
		if (projectile.contact(target) && Main.server != null)
			explodes = true;
		if (explodes)
			target.explode();
	}
}
