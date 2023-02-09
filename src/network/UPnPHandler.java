import org.bitlet.weupnp.*;
import org.xml.sax.SAXException;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import java.util.*;

// Uses weupnp library to try to set up UPnP on host network, if enabled

public class UPnPHandler{
	
	private GatewayDevice device;
	private String externalIP;
	private boolean success;
	private List<PortMappingEntry> mappings;
	
	public UPnPHandler(){
		success = true;
		mappings = new ArrayList<PortMappingEntry>();
	}
	
	public void enableUPnP(int port, Protocol protocol){
		if (!Main.options.UPnPEnabled)
			return;
		
		if (device == null)
			getDevice();
		if (device == null){
			success = false;
			java.lang.System.out.println("no device");
			return;
		}
		
		try {
			PortMappingEntry portMapping = new PortMappingEntry();
			if (device.getSpecificPortMappingEntry(port, protocol.name(), portMapping)){
				//Test.p("Port mapping already exists");
				//return;
			}
			
			//Test.p(device.getPortMappingNumberOfEntries());
			//Test.p(portMapping.getExternalPort()+" "+portMapping.getInternalPort()+" "+portMapping.getInternalClient()+" "+portMapping.getEnabled());
			//Test.p(port+" "+device.getLocalAddress().getHostAddress()+" "+protocol.name());
			
			if (!device.addPortMapping(port, port, device.getLocalAddress().getHostAddress(), protocol.name(), "Aether")){
				java.lang.System.out.println("cannot map");
				success = false;
				return;
			}
			
			mappings.add(portMapping);
		} catch (IOException|SAXException e){
			e.printStackTrace();
			success = false;
		}
	}
	
	private void getDevice(){
		try {
			GatewayDiscover discover = new GatewayDiscover();
			discover.discover();
			device = discover.getValidGateway();
			externalIP = device.getExternalIPAddress();
			java.lang.System.out.println(device.getModelName()+" "+device.getExternalIPAddress());
		} catch (IOException|SAXException|ParserConfigurationException e){
			e.printStackTrace();
		}
	}
	
	public void close(){
		if (device != null){
			for (PortMappingEntry entry : mappings){
				try{
					device.deletePortMapping(entry.getExternalPort(), entry.getProtocol());
				}catch (IOException|SAXException e){
					e.printStackTrace();
				}
			}
		}
	}
	
	public boolean isSuccessful(){
		return success;
	}
	
	public String getExternalIP(){
		return externalIP;
	}
}
