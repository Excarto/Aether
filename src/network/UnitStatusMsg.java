import java.io.*;

// Send periodically over UDP to update status of a batch of units at a time

public class UnitStatusMsg extends UpdateMsg{
	public byte getId(){return MsgId.UNIT_STATUS.id;}
	
	public static final int UPDATE_INTERVAL = 2*Main.TPS;
	public static final int NUM_UNITS = 5; // Determined by how many can fit in a network packet
	
	public int numUnits;
	public Unit[] units;
	
	public double[] hull, mass, frontShield, rearShield;
	public Controllable[] targets;
	public int[] numWeapons, numSystems;
	public double[][] weaponHull, systemHull;
	public boolean[][] systemEngaged;
	public Weapon.FireMode[][] weaponMode;
	public double[][] weaponAngle;
	
	public UnitStatusMsg(){
		units = new Unit[NUM_UNITS];
		
		hull = new double[NUM_UNITS];
		mass = new double[NUM_UNITS];
		frontShield = new double[NUM_UNITS];
		rearShield = new double[NUM_UNITS];
		targets = new Controllable[NUM_UNITS];
		
		numWeapons = new int[NUM_UNITS];
		weaponHull = new double[NUM_UNITS][8];
		numSystems = new int[NUM_UNITS];
		systemHull = new double[NUM_UNITS][8];
		systemEngaged = new boolean[NUM_UNITS][8];
		weaponMode = new Weapon.FireMode[NUM_UNITS][8];
		weaponAngle = new double[NUM_UNITS][8];
	}
	
	public void encode(DataOutputStream stream) throws IOException{
		super.encode(stream);
		
		stream.writeByte(numUnits);
		for (int x = 0; x < numUnits; x++){
			Unit unit = units[x];
			
			stream.writeShort(unit.getId());
			stream.writeFloat((float)unit.getHull());
			stream.writeFloat((float)unit.mass);
			stream.writeFloat(unit instanceof Ship ? (float)((Ship)unit).frontShield : 10.0f);
			stream.writeFloat(unit instanceof Ship ? (float)((Ship)unit).rearShield : 10.0f);
			
			if (unit.getTarget() == null){
				stream.writeByte(-1);
			}else{
				Controllable target = unit.getTarget().target;
				stream.writeByte(Main.game.players.indexOf(target.getPlayer()));
				stream.writeShort(target.getPlayer().controllables.indexOf(target));
			}
			
			stream.writeShort(unit.weapons.size());
			if (unit.weapons.size() > 0){
				int modeInt = 0;
				for (int y = 0; y < unit.weapons.size(); y++){
					Weapon weapon = unit.weapons.get(y);
					stream.writeFloat((float)weapon.getHull());
					stream.writeByte((int)weapon.getAngle()/2);
					modeInt = modeInt | (weapon.mode.ordinal() << 2*y);
				}
				stream.writeInt(modeInt);
			}
			
			stream.writeShort(unit.systems.size());
			if (unit.systems.size() > 0){
				int engagedByte = 0;
				for (int y = 0; y < unit.systems.size(); y++){
					System system = unit.systems.get(y);
					stream.writeFloat((float)system.getHull());
					if (system.isEngaged())
						engagedByte = engagedByte | (1 << y);
				}
				stream.writeByte(engagedByte);
			}
		}
	}
	public void decode(DataInputStream stream) throws IOException{
		super.decode(stream);
		
		numUnits = stream.readByte();
		for (int x = 0; x < numUnits; x++){
			units[x] = (Unit)player.controllables.getById(stream.readShort());
			hull[x] = stream.readFloat();
			mass[x] = stream.readFloat();
			frontShield[x] = stream.readFloat();
			rearShield[x] = stream.readFloat();
			
			targets[x] = null;
			int targetPlayerIndex = stream.readByte();
			if (targetPlayerIndex != -1)
				targets[x] = Main.game.players.get(targetPlayerIndex).controllables.getById(stream.readShort());
			
			numWeapons[x] = stream.readShort();
			if (numWeapons[x] > 0){
				for (int y = 0; y < numWeapons[x]; y++){
					weaponHull[x][y] = stream.readFloat();
					weaponAngle[x][y] = 2*stream.readByte();
				}
				int modeInt = stream.readInt();
				for (int y = 0; y < numWeapons[x]; y++)
					weaponMode[x][y] = Weapon.FireMode.values()[(modeInt >> 2*y) & 3];
			}
			
			numSystems[x] = stream.readShort();
			if (numSystems[x] > 0){
				for (int y = 0; y < numSystems[x]; y++)
					systemHull[x][y] = stream.readFloat();
				int engagedByte = stream.readByte() & 0xFF;
				for (int y = 0; y < numSystems[x]; y++)
					systemEngaged[x][y] = (engagedByte >> y)%2 == 1;
			}
		}
	}
	
	protected boolean isGood(){
		for (int x = 0; x < numUnits; x++)
			if (units[x] == null)
				return false;
		return true;
	}
	
	public void confirmed(){
		for (int x = 0; x < numUnits; x++){
			Unit unit = units[x];
			if (unit.lastUnitUpdateTime < time){
				unit.hull = hull[x];
				unit.mass = mass[x];
				if (unit instanceof Ship){
					((Ship)unit).frontShield = frontShield[x];
					((Ship)unit).rearShield = rearShield[x];
				}
				
				unit.setTarget(player.getTarget(targets[x]));
				
				for (int y = 0; y < numWeapons[x]; y++){
					unit.weapons.get(y).setHull(weaponHull[x][y]);
					unit.weapons.get(y).mode = weaponMode[x][y];
					unit.weapons.get(y).setAngle(weaponAngle[x][y]);
				}
				for (int y = 0; y < numSystems[x]; y++){
					unit.systems.get(y).setHull(systemHull[x][y]);
					unit.systems.get(y).engaged = systemEngaged[x][y];
				}
				
				units[x].lastUnitUpdateTime = time;
			}
		}
	}
	
}
