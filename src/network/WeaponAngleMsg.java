import java.io.*;

// Sent periodically over UDP to update angle of a batch of weapons at a time

public class WeaponAngleMsg extends UpdateMsg {
	public byte getId(){return MsgId.WEAPON_ANGLE.id;}
	
	public static final int NUM_WEAPONS = 20; // Determined by how many can fit in a network packet
	
	public int numWeapons;
	public Weapon[] weapons;
	public double[] angle;
	
	public WeaponAngleMsg(){
		weapons = new Weapon[NUM_WEAPONS];
		angle = new double[NUM_WEAPONS];
	}
	
	public void encode(DataOutputStream stream) throws IOException{
		super.encode(stream);
		
		stream.writeByte(numWeapons);
		for (int x = 0; x < numWeapons; x++){
			stream.writeShort(weapons[x].unit.getId());
			stream.writeShort(weapons[x].getId());
			stream.writeFloat((float)weapons[x].getAngle());
		}
	}
	public void decode(DataInputStream stream) throws IOException{
		super.decode(stream);
		
		numWeapons = stream.readByte();
		for (int x = 0; x < numWeapons; x++){
			Unit unit = (Unit)player.controllables.getById(stream.readShort());
			short weaponId = stream.readShort();
			weapons[x] = unit != null ? unit.weapons.getById(weaponId) : null;
			angle[x] = stream.readFloat();
		}
	}
	
	public void confirmed(){
		for (int x = 0; x < numWeapons; x++)
			weapons[x].updateAngle(angle[x], time);
	}
	
	protected boolean isGood(){
		for (int x = 0; x < numWeapons; x++)
			if (weapons[x] == null)
				return false;
		return true;
	}
}
