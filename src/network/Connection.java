import java.net.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

public class Connection{
	static final int PACKET_SIZE = 512;
	
	private static DatagramSocket UDPSocket;
	private static List<Connection> UDPConnections = new CopyOnWriteArrayList<Connection>();
	private static byte[] UDPBuffer = new byte[PACKET_SIZE];
	
	public final InetAddress remoteAddress;
	
	private Socket TCPSocket;
	private Message[] messageListeners;
	private boolean[] isSwingListener;
	private byte[] writeBuffer, TCPBuffer;
	private boolean onlyTCP;
	private CloseListener closeListener;
	private boolean running;
	
	public Connection(Socket socket, boolean onlyTCP) throws IOException{
		this.onlyTCP = onlyTCP;
		messageListeners = new Message[256];
		isSwingListener = new boolean[256];
		writeBuffer = new byte[PACKET_SIZE];
		TCPBuffer = new byte[PACKET_SIZE];
		running = true;
		remoteAddress = ((InetSocketAddress)socket.getRemoteSocketAddress()).getAddress();
		
		this.addListener(new CloseConnectionMsg(){
			public void confirmed(){
				closeSockets();
		}});
		
		this.addListener(new ForceTCPMsg(){
			public void confirmed(){
				Connection.this.onlyTCP = true;
		}});
		
		TCPSocket = socket;
		TCPSocket.setTcpNoDelay(true);
		TCPSocket.setKeepAlive(true);
		new Thread("TCPListenThread"){
			public void run(){
				try{
					Thread.sleep(100);
				}catch (InterruptedException ex){}
				listenForTCPMsg();
		}}.start();
		
		if (this.onlyTCP){
			forceTCP();
		}else{
			try{
				if (UDPSocket == null){
					UDPSocket = new DatagramSocket(Main.options.UDPPort);
					UDPSocket.setBroadcast(false);
					Main.UPnPHandler.enableUPnP(Main.options.UDPPort, Protocol.UDP);
					new Thread("UDPListenThread"){
						public void run(){
							listenForUDPMsg();
					}}.start();
				}
				UDPConnections.add(this);
				new Thread("UDPConfirmThread"){
					public void run(){
						confirmUDPConnection();
				}}.start();
			}catch (IOException e){
				forceTCP();
			}
		}
		
	}
	
	private void listenForTCPMsg(){
		try{
			InputStream input = TCPSocket.getInputStream();
			while (running){
				int data = input.read();
				if (data == -1){
					closeSockets();
				}else{
					TCPBuffer[0] = (byte)data;
					int length = 2*input.read();
					TCPBuffer[1] = (byte)(length/2);
					int index = 2;
					while (length > index)
						index += input.read(TCPBuffer, index, length-index);
					receive(TCPBuffer);
					//if (messageListeners[TCPBuffer[0]] != null)
					//	messageListeners[TCPBuffer[0]].read(TCPBuffer);
				}
			}
		}catch (IOException e){
			closeSockets();
		}
	}
	
	public void confirmUDPConnection(){
		final boolean[] UDPConfirmed = new boolean[]{false};
		this.addListener(new UDPConfirmMsg(){
			public void confirmed(){
				UDPConfirmed[0] = true;
		}});
		
		for (int x = 0; x < 20; x++){
			try{
				Thread.sleep(200);
			}catch (InterruptedException e){
				e.printStackTrace();
			}
			if (onlyTCP)
				return;
			send(new UDPConfirmMsg());
		}
		
		if (!UDPConfirmed[0])
			forceTCP();
	}
	
	public void receive(byte[] message){
		Message listener = messageListeners[message[0]];
		if (listener != null){
			if (!isSwingListener[message[0]]){
				listener.read(message);
			}else{
				try{
					SwingUtilities.invokeAndWait(new Runnable(){
						public void run(){
							listener.read(message);
					}});
				}catch (InvocationTargetException e){
					e.printStackTrace();
				}catch (InterruptedException e){}
			}
		}
	}
	
	public synchronized void send(Message message){
		try{
			int length = message.write(writeBuffer);
			if (UDPSocket == null || onlyTCP || message.getProtocol() == Protocol.TCP){
				TCPSocket.getOutputStream().write(writeBuffer, 0, length);
			}else{
				DatagramPacket packet = new DatagramPacket(writeBuffer, length,
						remoteAddress, Main.options.UDPPort);
				UDPSocket.send(packet);
			}
		}catch (IOException e){
			closeSockets();
		}
	}
	
	public void addListener(Message message){
		messageListeners[message.getId()] = message;
		isSwingListener[message.getId()] = false;
	}
	
	public void addSwingListener(Message message){
		messageListeners[message.getId()] = message;
		isSwingListener[message.getId()] = true;
	}
	
	public void close(){
		send(new CloseConnectionMsg());
		try{
			TCPSocket.getOutputStream().flush();
		}catch (IOException ex){}
		closeSockets();
	}
	
	public void forceTCP(){
		if (!onlyTCP)
			java.lang.System.out.println("Using TCP only");
		onlyTCP = true;
		send(new ForceTCPMsg());
	}
	
	private void closeSockets(){
		if (running){
			running = false;
			try{
				TCPSocket.close();
			}catch (IOException e){}
			UDPConnections.remove(this);
			if (closeListener != null){
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						closeListener.closed();
				}});
			}
		}
	}
	
	public void setCloseListener(CloseListener listener){
		closeListener = listener;
	}
	
	public static void listenForUDPMsg(){
		try{
			DatagramPacket packet = new DatagramPacket(UDPBuffer, PACKET_SIZE);
			while (UDPSocket != null){
				UDPSocket.receive(packet);
				if (packet.getPort() == Main.options.UDPPort){
					//if(packet.getAddress().equals(TCPSocket.getInetAddress()) &&
					//		messageListeners[UDPBuffer[0]] != null)
					//	messageListeners[UDPBuffer[0]].read(UDPBuffer);
					for (Connection connection : UDPConnections){
						if (connection.remoteAddress.equals(packet.getAddress()))
							connection.receive(UDPBuffer);
					}
				}
			}
		}catch (IOException e){
			closeUDP();
		}
	}
	
	public static void closeUDP(){
		if (UDPSocket != null){
			UDPSocket.close();
			UDPSocket = null;
		}
		for (Connection connection : UDPConnections)
			connection.forceTCP();
		UDPConnections.clear();
	}
	
	public abstract class CloseListener{
		public abstract void closed();
	}
}
