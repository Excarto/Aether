import java.io.*;

public class FireWeaponMsg extends InGameMsg{
	public byte getId(){return MsgId.FIRE_WEAPON.id;}
	
	public Weapon weapon;
	public double angle;
	public int randomSeed;
	public int time;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeByte(Main.game.players.indexOf(weapon.unit.player));
		stream.writeShort(weapon.unit.getId());
		stream.writeShort(weapon.getId());
		stream.writeDouble(angle);
		stream.writeInt(randomSeed);
		stream.writeInt(time);
	}
	public void decode(DataInputStream stream) throws IOException{
		Player player = Main.game.players.get(stream.readByte());
		Unit unit = (Unit)player.controllables.getById(stream.readShort());
		short weaponId = stream.readShort();
		weapon = unit != null ? unit.weapons.getById(weaponId) : null;
		angle = stream.readDouble();
		randomSeed = stream.readInt();
		time = stream.readInt();
	}
	
	protected boolean isGood(){
		return weapon != null;
	}
	
	public void confirmed(){
		if (Main.server != null)
			time = Main.game.turn;
		Weapon.random.setSeed(randomSeed);
		weapon.fire(angle, time);
	}
}
