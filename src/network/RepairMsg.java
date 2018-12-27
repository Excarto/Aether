import java.io.*;

public class RepairMsg extends InGameMsg{
	public byte getId(){return MsgId.REPAIR.id;}
	
	public Repairable repairable;
	public double material;
	public boolean isScrap;
	
	public void encode(DataOutputStream stream) throws IOException{
		Unit unit;
		Component component;
		if (repairable instanceof Unit){
			component = null;
			unit = (Unit)repairable;
		}else{
			component = (Component)repairable;
			unit = component.unit;
		}
		stream.writeByte(Main.game.players.indexOf(unit.player));
		stream.writeShort(unit.getId());
		stream.writeBoolean(component instanceof Weapon);
		stream.writeShort(component == null ? -1 : component.getId());
		stream.writeDouble(material);
		stream.writeBoolean(isScrap);
	}
	public void decode(DataInputStream stream) throws IOException{
		Player player = Main.game.players.get(stream.readByte());
		Unit unit = (Unit)player.controllables.getById(stream.readShort());
		boolean isWeapon = stream.readBoolean();
		short componentId = stream.readShort();
		if (unit == null){
			repairable = null;
		}else{
			if (componentId == -1){
				repairable = unit;
			}else
				repairable = (isWeapon ? unit.weapons : unit.systems).getById(componentId);
		}
		material = stream.readDouble();
		isScrap = stream.readBoolean();
	}
	
	protected boolean isGood(){
		return repairable != null;
	}
	
	public void confirmed(){
		repairable.repair(material, isScrap);
	}
}
