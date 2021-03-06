import static java.lang.Math.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.Component;
import java.util.*;
import java.util.List;

public class TypeWindow extends Window{
	
	List<BuyType> types;
	DisplayWindow perspective, side;
	JList<BuyType> typeList;
	JPanel typePanel;
	JLabel costLabel, massLabel;
	Title title;
	JTextArea description;
	JPanel specsPanel;
	HashMap<String, Double>  highestValues, lowestValues;
	PeriodicTimer timer;
	Bar tooltipBar;
	Font barFont;
	
	public TypeWindow(Class<? extends BuyType> typeClass, Vector<BuyType> availableTypes, Window parentWindow){
		super(Size.NORMAL);
		
		barFont = Main.getDefaultFont(12);
		
		for (int x = 0; x < availableTypes.size(); x++){
			int leastExpensive = x;
			for (int y = x; y < availableTypes.size(); y++){
				if (availableTypes.get(y).cost < availableTypes.get(leastExpensive).cost)
					leastExpensive = y;
			}
			BuyType temp = availableTypes.get(x);
			availableTypes.set(x, availableTypes.get(leastExpensive));
			availableTypes.set(leastExpensive, temp);
		}
		
		types = availableTypes;
		
		int perspectiveWidth = types.get(0).perspectiveImg.getWidth() + 20;
		int perspectiveHeight = types.get(0).perspectiveImg.getHeight() + 20;
		int sideWidth = types.get(0).sideImg.getWidth() + 20;
		int sideHeight = types.get(0).sideImg.getHeight() + 20;
		int imagesHeight = 88+perspectiveHeight+sideHeight;
		
		typeList = new JList<BuyType>(availableTypes);
		typeList.setPreferredSize(new Dimension(110, 270));
		typeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		typeList.setDragEnabled(false);
		typeList.setBorder(BorderFactory.createEtchedBorder());
		typeList.addListSelectionListener(new SelectListener());
		
		JLabel typeLabel = new JLabel("Available Types");
		typeLabel.setFont(Main.getDefaultFont(13));
		
		typePanel = new JPanel();
		typePanel.setOpaque(false);
		typePanel.setPreferredSize(new Dimension(125, 365));
		typePanel.add(typeLabel);
		typePanel.add(typeList);
		
		costLabel = new JLabel();
		costLabel.setPreferredSize(new Dimension(300, 12));
		costLabel.setFont(new Font("Courier", Font.PLAIN, 14));
		massLabel = new JLabel();
		massLabel.setPreferredSize(new Dimension(300, 12));
		massLabel.setFont(new Font("Courier", Font.PLAIN, 14));
		
		perspective = new DisplayWindow(perspectiveWidth, perspectiveHeight, "Perspective");
		side = new DisplayWindow(sideWidth, sideHeight, "Side");
		
		JPanel imagePanel = new JPanel(DEFAULT_LAYOUT);
		imagePanel.setPreferredSize(new Dimension(max(perspectiveWidth, sideWidth), imagesHeight));
		imagePanel.setOpaque(false);
		
		description = new JTextArea();
		description.setPreferredSize(new Dimension(imagePanel.getPreferredSize().width,
				imagesHeight-perspectiveHeight-sideHeight-7));
		description.setEditable(false);
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		description.setSelectionColor(null);
		description.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		description.setMargin(new Insets(5, 5, 5, 5));
		
		imagePanel.add(perspective);
		imagePanel.add(side);
		imagePanel.add(description);
		
		highestValues = new HashMap<String, Double>();
		lowestValues = new HashMap<String, Double>();
		for (BuyType type : Main.buyTypes){
			if (typeClass.isAssignableFrom(type.getClass())){
				for (String[] spec : type.getSpecs()){
					Double highVal = highestValues.get(spec[0]), lowVal = lowestValues.get(spec[0]);
					if (!spec[1].equals("CATEGORY") && !spec[1].equals("N/A")){
						double specVal = Double.parseDouble(spec[1]);
						if (Double.isFinite(specVal)){
							if (highVal == null || highVal < specVal)
								highestValues.put(spec[0], specVal);
							if (lowVal == null || lowVal > specVal)
								lowestValues.put(spec[0], specVal);
						}
					}
				}
			}
		}
		
		specsPanel = new JPanel();
		specsPanel.setPreferredSize(new Dimension(335, imagesHeight-8));
		specsPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		JPanel exitPanel = new JPanel();
		exitPanel.setPreferredSize(new Dimension(700, 38));
		exitPanel.setOpaque(false);
		JButton exitButton = new JButton("Select");
		exitButton.setPreferredSize(new Dimension(120, 34));
		exitButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Main.removeWindow();
				if (typeList.getSelectedIndex() != -1)
					parentWindow.returnValue(typeList.getSelectedValue());
			}
		});
		exitPanel.add(exitButton);
		
		title = new Title("", 1000, 32);
		
		int topSpace = 315 - imagesHeight/2;
		this.add(createSpacer(Main.resX-100, topSpace));
		this.add(title);
		this.add(typePanel);
		this.add(imagePanel);
		this.add(specsPanel);
		this.add(exitPanel);
		
		typeList.setSelectedIndex(0);
		typeList.requestFocus();
		
		timer = new PeriodicTimer(1000/Main.options.menuFramesPerSec){
			public void runTimerTask(){
				animateFrame();
				repaint();
		}};
		timer.start();
	}
	
	public void animateFrame(){
		for (Component component : specsPanel.getComponents())
			if (component instanceof Bar)
				((Bar)component).animate();
		perspective.animate();
		side.animate();
	}
	
	public void suspend(){
		timer.stop(false);
	}
	
	static final int TOOLTIP_HEIGHT = 17;
	public void paint(Graphics g){
		super.paint(g);
		
		Bar bar = tooltipBar;
		if (bar != null && bar.tooltip != null){
			g.setFont(barFont);
			int width = g.getFontMetrics().stringWidth(bar.tooltip) + 10;
			
			Point barPos = bar.getLocationOnScreen();
			Point thisPos = this.getLocationOnScreen();
			int posX = barPos.x - thisPos.x - width/2 + 80;
			int posY = barPos.y - thisPos.y + 19;
			
			g.setColor(Color.WHITE);
			g.fillRect(posX, posY, width, TOOLTIP_HEIGHT);
			g.setColor(Color.BLACK);
			g.drawRect(posX, posY, width, TOOLTIP_HEIGHT);
			g.drawString(bar.tooltip, posX+5, posY+13);
		}
	}
	
	static final Font CATEGORY_FONT = new Font("Courier", Font.BOLD, 12);
	private class SelectListener implements ListSelectionListener{
		public void valueChanged(ListSelectionEvent e){
			if (!typeList.isSelectionEmpty() && !e.getValueIsAdjusting()){
				BuyType type = typeList.getSelectedValue();
				perspective.setImage(type.perspectiveImg);
				side.setImage(type.sideImg);
				
				title.setText(type.getFullName());
				costLabel.setText("Cost: " + type.cost);
				massLabel.setText("Mass: " + type.mass);
				description.setText(type.description);
				
				specsPanel.removeAll();
				JLabel specsLabel = new JLabel("Specifications");
				specsLabel.setFont(new Font("Courier", Font.BOLD, 16));
				specsLabel.setPreferredSize(new Dimension(175, 24));
				specsLabel.setVerticalTextPosition(JLabel.CENTER);
				specsPanel.add(costLabel);
				specsPanel.add(massLabel);
				specsPanel.add(specsLabel);
				
				for (String[] spec : type.getSpecs()){
					if (spec[1].equals("CATEGORY")){
						JLabel category = new JLabel(spec[0].toUpperCase());
						category.setFont(CATEGORY_FONT);
						category.setPreferredSize(new Dimension(190, 13));
						specsPanel.add(category);
					}else if (spec[1].equals("N/A")){
						
					}else{
						String tooltip = null;
						if (spec.length >= 3)
							tooltip = spec[2];
						specsPanel.add(lowestValues.get(spec[0]) >= 0 ?
								new Bar(spec[0], tooltip, Double.parseDouble(spec[1]),
										highestValues.get(spec[0]), lowestValues.get(spec[0]),
										type.getSpecs().length, false):
								new Bar(spec[0], tooltip, -Double.parseDouble(spec[1]),
										-lowestValues.get(spec[0]), -highestValues.get(spec[0]),
										type.getSpecs().length, true));
					}
				}
				
				specsPanel.repaint();
			}
		}
	}
	
	final static int WINDOW_ANIMATION_SPEED = 600;
	private class DisplayWindow extends JComponent{
		BufferedImage img;
		int lineWidth, lineHeight;
		int width, height;
		
		public DisplayWindow(int width, int height, String text){
			//this.text = text;
			this.setBackground(Color.BLACK);
			this.setFont(new Font("Courier", Font.PLAIN, 15));
			this.width = width;
			this.height = height;
			this.setPreferredSize(new Dimension(width, height));
		}
		
		public void setImage(BufferedImage img){
			this.img = img;
			
			lineWidth = 0;
			lineHeight = 0;
			this.repaint();
		}
		
		public void paint(Graphics g){
			((Graphics2D)g).setRenderingHints(Main.menuHints);
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			g.setColor(new Color(10,150,10));
			for (int x = 40; x < width; x += 40)
				g.drawLine(x, 0, x, lineHeight);
			for (int y = 40; y < height; y += 40)
				g.drawLine(0, y, lineWidth, y);
			for (int x = 20; x < width; x += 40)
				g.drawLine(x, height-1, x, height-lineHeight);
			for (int y = 20; y < height; y += 40)
				g.drawLine(width-1, y, width-lineWidth, y);
			
			if (lineWidth < width || lineHeight < height){
				//g.setColor(Color.WHITE);
				//g.drawString("Transferring...", width/2-60, height/2-5);
			}else{
				g.setColor(Color.WHITE);
				g.drawImage(img, 10, 10, null);
				//g.drawString(text, 15, height-15);
			}
			
			g.setColor(Color.LIGHT_GRAY);
			g.drawRect(0, 0, width, height);
		}
		
		public void animate(){
			if (lineWidth < width || lineHeight < height){
				if (lineWidth < width)
					lineWidth += WINDOW_ANIMATION_SPEED/Main.options.menuFramesPerSec;
				if (lineHeight < height)
					lineHeight += WINDOW_ANIMATION_SPEED/Main.options.menuFramesPerSec;
			}
		}
	}
	
	
	static final int BAR_LENGTH = 150, BORDER = 5;
	final static double BAR_ANIMATION_SPEED = 1.0;
	private class Bar extends JComponent{
		String title;
		String tooltip;
		double value, targetValue;
		double maxVal, minVal;
		boolean negative;
		
		public Bar(String title, String tooltip, double value, double maxVal, double minVal, int size, boolean negative){
			this.setPreferredSize(new Dimension(330, (int)sqrt(12*specsPanel.getPreferredSize().height/size)));
			this.title = title;
			this.tooltip = tooltip;
			this.targetValue = (double)((int)(value*100))/100;
			this.maxVal = maxVal;
			this.minVal = minVal/2;
			this.value = minVal;
			
			this.negative = negative;
			this.setBackground(new Color(238, 238, 238));
			
			this.addMouseListener(new MouseAdapter(){
				public void mouseEntered(MouseEvent e){
			       tooltipBar = Bar.this;
			    }
				public void mouseExited(MouseEvent e){
			       tooltipBar = null;
			    }
			});
		}
		
		public void paint(Graphics g){
			((Graphics2D)g).setRenderingHints(Main.menuHints);
			g.setColor(Color.WHITE);
			g.fillRect(165, 0, BAR_LENGTH, 15);
			g.setFont(barFont);
			g.setColor(Color.BLACK);
			g.drawRect(165, 0, BAR_LENGTH+1, 15);
			double val = maxVal > minVal ? (value-minVal)/(maxVal-minVal) : 0.5;
			g.setColor(Utility.getColor(negative ? 1.0-val : val, 0.15));
			g.fillRect(166, 1, (int)(BORDER+(BAR_LENGTH-2*BORDER)*val), 14);
			g.setColor(Color.BLACK);
			g.drawString(title + ": ", 1, 13);
			g.drawString(value == (int)value ? String.valueOf((int)value) :
					String.valueOf((double)((int)(value*100))/100), 230, 13);
		}
		
		public void animate(){
			if (value < targetValue)
				value = min(targetValue, value + BAR_ANIMATION_SPEED*(maxVal-minVal)/Main.options.menuFramesPerSec);
		}
	}
}
