import java.util.*;
import java.io.*;

public abstract class Type{
	
	public final String directory;
	private final Map<String, String> data;
	private final String file;
	
	public Type(String folder){
		directory = "data/" + folder;
		file = directory + "/data.txt";
		Map<String, String> data = null;
		try{
			data = Utility.readDataFile(file);
		}catch (IOException ex){
			Main.crash(file);
		}
		this.data = data;
		
		Main.appendData(data, folder);
	}
	
	protected boolean hasValue(String label){
		return data.containsKey(label);
	}
	protected String getString(String label){
		return data.get(label);
	}
	protected int getInt(String label){
		return (int)getDouble(label);
	}
	protected double getDouble(String label){
		double out = 0;
		try{
			out = data.containsKey(label) ? Double.valueOf(data.get(label)) : 0.0;
		}catch (Exception e){
			Main.crash(file+"  "+label);
		}
		return out;
	}
	protected boolean getBoolean(String label){
		return data.containsKey(label) ? Boolean.valueOf(data.get(label)) : false;
	}
}
