import java.io.*;
import java.util.*;

public class CapSettingsMsg extends Message{
	public byte getId(){return MsgId.CAP_SETTINGS.id;}
	
	public int player;
	public int unit;
	double unreservedCap;
	public int numCaps;
	public double[] cap;
	
	public void encode(DataOutputStream stream) throws IOException{
		stream.writeByte(player);
		stream.writeByte(unit);
		stream.writeDouble(unreservedCap);
		stream.writeByte(numCaps);
		for (int x = 0; x < numCaps; x++)
			stream.writeDouble(cap[x]);
	}
	public void decode(DataInputStream stream) throws IOException{
		player = stream.readByte();
		unit = stream.readByte();
		unreservedCap = stream.readDouble();
		numCaps = stream.readByte();
		for (int x = 0; x < numCaps; x++)
			cap[x] = stream.readDouble();
	}
	
	public void load(Unit unit){
		numCaps = 0;
		unreservedCap = unit.unreservedCap[0];
		for (Map<String, double[]> category : unit.capacitor.values())
			for (double[] specific : category.values())
				cap[numCaps++] = specific[0];
	}
	public void unload(Unit unit){
		unit.unreservedCap[0] = unreservedCap;
		int capIndex = 0;
		for (Map<String, double[]> category : unit.capacitor.values())
			for (double[] specific : category.values())
				specific[0] = cap[capIndex++];
	}
}
