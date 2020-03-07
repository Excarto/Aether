import static java.lang.Math.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class BriefMenu extends Menu{
	
	static final int WINDOW_WIDTH = 900;
	static final int WINDOW_HEIGHT = 510;
	static final int WINDOW_INSET = 35;
	final static int WINDOW_ANIMATION_SPEED = 800;
	final static Font BRIEFING_FONT = new Font("Courier", Font.BOLD, 20);
	final static int TEXT_ANIMATION_SPEED = 150;
	final static int PARAGRAPH_DELAY = 20;
	final static int INITIAL_DELAY = 60;
	
	private Mission mission;
	private Pilot pilot;
	private BriefState state;
	
	private int maxWindowShift, windowShift;
	private int textLine, textChar, textFrameCount;
	
	public BriefMenu(Mission mission, Pilot pilot){
		this.pilot = pilot;
		this.mission = mission;
		
		Sound startSound = new Sound(new File("data/mission_start.wav"));
		startSound.load();
		startSound.play(1.7);
		
		drawTitle = false;
		setState(mission.createBriefing(this, pilot));
		textChar = -INITIAL_DELAY;
		
		maxWindowShift = (Main.resX - WINDOW_WIDTH)/2 + WINDOW_WIDTH;
		windowShift = maxWindowShift;
		
		this.addMouseListener(new MouseAdapter(){
			public void mousePressed (MouseEvent e){
				if (textLine > 0 || textChar > 0){
					textLine = state.lines.size();
					setOptions();
				}
			}
		});
	}
	
	public void setState(BriefState state){
		this.state = state;
		state.onActivate();
		state.lines.clear();
		clearOptions();
		textLine = 0;
		textChar = -PARAGRAPH_DELAY;
	}
	
	public void paint(Graphics graphics){
		Graphics2D g = (Graphics2D)graphics;
		super.paint(g);
		
		if (state.lines.isEmpty())
			state.initLines(g);
		
		int windowPosX = Main.resX/2 - WINDOW_WIDTH/2;
		int windowPosY = 100 + ((Main.resY - Main.RES_Y_SHORT)/2)/2;
		if (windowShift > 0){
			double opacity = pow(1 - windowShift/(double)maxWindowShift, 4);
			g.setColor(new Color(0f, 0f, 0f, (float)opacity));
			for (int posY = windowPosY; posY < windowPosY+WINDOW_HEIGHT; posY++){
				if (posY%2 == 0){
					g.drawLine(windowPosX-windowShift, posY, windowPosX-windowShift+WINDOW_WIDTH, posY);
				}else{
					g.drawLine(windowPosX+windowShift, posY, windowPosX+windowShift+WINDOW_WIDTH, posY);
				}
			}
		}else{
			g.setColor(Color.BLACK);
			g.fillRect(windowPosX, windowPosY, WINDOW_WIDTH, WINDOW_HEIGHT);
			g.setColor(Color.GRAY);
			g.fillRect(windowPosX, windowPosY, WINDOW_WIDTH, 2);
			g.fillRect(windowPosX, windowPosY+WINDOW_HEIGHT-2, WINDOW_WIDTH, 2);
			BufferedImage divider = dividerImg.getSubimage(0, 0, dividerImg.getWidth(), WINDOW_HEIGHT);
			g.drawImage(divider, windowPosX, windowPosY, null);
			g.drawImage(divider, windowPosX+WINDOW_WIDTH-divider.getWidth(), windowPosY, null);
			
			g.setFont(BRIEFING_FONT);
			g.setColor(Color.LIGHT_GRAY);
			int posX = windowPosX + WINDOW_INSET;
			int posY = windowPosY + 45;
			for (int i = 0; i < textLine; i++){
				String line = state.lines.get(i);
				g.drawString(line, posX, posY);
				posY += 22;
			}
			if (textLine < state.lines.size() && textChar > 0){
				String line = state.lines.get(textLine).substring(0, textChar);
				g.drawString(line, posX, posY);
			}
		}
		
	}
	
	public void animateFrame(){
		
		if (windowShift > 0){
			windowShift = max(0, windowShift - WINDOW_ANIMATION_SPEED/Main.options.menuFramesPerSec);
		}else if (textLine < state.lines.size()){
			textFrameCount++;
			if (textFrameCount > Main.options.framesPerSec/TEXT_ANIMATION_SPEED){
				textFrameCount = 0;
				textChar++;
				if (textChar >= state.lines.get(textLine).length()){
					textLine++;
					if (textLine >= state.lines.size()){
						setOptions();
					}else{
						if (state.lines.get(textLine).isBlank()){
							textChar = -PARAGRAPH_DELAY;
						}else
							textChar = 0;
					}
				}
			}
		}else
			super.animateFrame();
	}
	
	private synchronized void setOptions(){
		if (getNumOptions() == 0){
			for (Option option : state.options)
				addOption(option);
		}
	}
	
	protected String getBackgroundFile(){
		return "data/menu_background_blank.png";
	}
	
	public class BriefState{
		ArrayList<Option> options;
		ArrayList<String> lines;
		String text;
		
		public BriefState(String text){
			this.text = text;
			options = new ArrayList<Option>();
			lines = new ArrayList<String>();
		}
		
		public void onActivate(){}
		
		public void addOption(String text, BriefState state){
			options.add(new Option(text){
				public void act(){
					setState(state);
			}});
		}
		
		public void addOption(Option option){
			options.add(option);
		}
		
		public void addStartOption(String text){
			options.add(new Option(text){
				public void act(){
					Main.removeWindow();
					mission.start(pilot);
			}});
		}
		
		public void addDeathOption(String text){
			options.add(new Option(text){
				public void act(){
					Main.removeWindow();
			}});
		}
		
		public void initLines(Graphics2D g){
			String[] words = text.split(" ");
			int iword = 0;
			lines.clear();
			g.setFont(BRIEFING_FONT);
			
			while (iword < words.length){
				String previousLine = "";
				String line = "";
				while (iword < words.length){
					previousLine = line;
					String newWord = words[iword];
					if (newWord.equals("\n")){
						iword++;
						break;
					}else{
						line = previousLine + " " + newWord;
						if (g.getFontMetrics().stringWidth(line.toString()) > WINDOW_WIDTH - 2*WINDOW_INSET - 5){
							line = previousLine;
							break;
						}else
							iword++;
					}
				}
				lines.add(line);
			}
			
		}
	}
	
}
