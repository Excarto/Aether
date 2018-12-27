import java.io.*;

public class UnitDescriptionMsg extends Message {
	public byte getId(){return MsgId.UNIT_DESCRIPTION.id;}
	
	public String player;
	public Unit unit;
	
	public void encode(DataOutputStream stream) throws IOException{
		boolean isShip = unit instanceof Ship;
		stream.writeUTF(player);
		stream.writeBoolean(isShip);
		for (int x = 0; x < (isShip ? Main.shipTypes : Main.craftTypes).length; x++)
			if (unit.type == (isShip ? Main.shipTypes : Main.craftTypes)[x])
				stream.writeByte(x);
		stream.writeUTF(unit.getName());
		stream.writeByte(unit.weapons.size());
		for (Weapon weapon : unit.weapons){
			for (int x = 0; x < Main.weaponTypes.length; x++)
				if (weapon.type == Main.weaponTypes[x])
					stream.writeByte(x);
			for (int x = 0; x < weapon.unit.type.weaponHardpoints.length; x++)
				if (weapon.hardpoint == weapon.unit.type.weaponHardpoints[x])
					stream.writeByte(x);
			stream.writeInt(weapon.getArc());
			stream.writeDouble(weapon.getMountAngleFrac());
		}
		stream.writeByte(unit.systems.size());
		for (System system : unit.systems){
			for (int x = 0; x < Main.systemTypes.length; x++)
				if (system.type == Main.systemTypes[x])
					stream.writeByte(x);
			for (int x = 0; x < system.unit.type.systemHardpoints.length; x++)
				if (system.hardpoint == system.unit.type.systemHardpoints[x])
					stream.writeByte(x);
		}
		stream.writeBoolean(unit.manualAmmo);
		for (int x = 0; x < unit.ammoRatios.length; x++)
			stream.writeDouble(unit.ammoRatios[x]);
	}
	public void decode(DataInputStream stream) throws IOException{
		player = stream.readUTF();
		unit = stream.readBoolean() ? new Ship(Main.shipTypes[stream.readByte()]) : new Craft(Main.craftTypes[stream.readByte()]);
		unit.setName(stream.readUTF());
		for (int x = stream.readByte()-1; x >= 0; x--)
			unit.setWeapon(Main.weaponTypes[stream.readByte()], unit.type.weaponHardpoints[stream.readByte()], stream.readInt(), stream.readDouble());
		for (int x = stream.readByte()-1; x >= 0; x--)
			unit.setSystem(Main.systemTypes[stream.readByte()], unit.type.systemHardpoints[stream.readByte()]);
		unit.manualAmmo = stream.readBoolean();
		for (int x = 0; x < unit.ammoRatios.length; x++)
			unit.ammoRatios[x] = stream.readDouble();
	}
}