import java.io.*;

public class BeamContactMsg extends InGameMsg{
	public byte getId(){return MsgId.BEAM_CONTACT.id;}
	
	public Beam beam;
	public Controllable target;
	public double posX, posY;
	public int randomSeed;
	public boolean explodes;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeByte(Main.game.players.indexOf(beam.unit.player));
		stream.writeShort(beam.unit.getId());
		stream.writeShort(beam.getId());
		stream.writeByte(Main.game.players.indexOf(target.getPlayer()));
		stream.writeShort(target.getId());
		stream.writeFloat((float)posX);
		stream.writeFloat((float)posY);
		stream.writeInt(randomSeed);
		stream.writeBoolean(explodes);
	}
	public void decode(DataInputStream stream) throws IOException{
		Player beamPlayer = Main.game.players.get(stream.readByte());
		Unit beamUnit = (Unit)beamPlayer.controllables.getById(stream.readShort());
		short beamId = stream.readShort();
		beam = beamUnit != null ? (Beam)beamUnit.weapons.getById(beamId) : null;
		Player targetPlayer = Main.game.players.get(stream.readByte());
		target = targetPlayer.controllables.getById(stream.readShort());
		posX = stream.readFloat();
		posY = stream.readFloat();
		randomSeed = stream.readInt();
		explodes = stream.readBoolean();
	}
	
	protected boolean isGood(){
		return beam != null && target != null;
	}
	
	public void confirmed(){
		Unit.RANDOM.setSeed(randomSeed);
		if (beam.contact(target, posX+((Sprite)target).getPosX(), posY+((Sprite)target).getPosY())
				&& Main.server != null)
			explodes = true;
		if (explodes)
			target.explode();
	}
}
