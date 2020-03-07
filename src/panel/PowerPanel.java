import static java.lang.Math.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

class PowerPanel extends SidePanel{
	static final int LINE_POS_X1 = 14, LINE_POS_X2 = 241;
	static final int LINE_POS_Y1 = 261;
	
	private Unit unit;
	private ArrayList<PowerSettingGeneral> general;
	private ArrayList<PowerSettingSpecific> specific;
	private JPanel generalPanel, specificPanel;
	private int linePosY2;
	//private JLabel unreservedLabelGeneral, unreservedLabelSpecific;
	private UnreservedPanel unreservedPanelGeneral, unreservedPanelSpecific;
	private PowerSettingGeneral selectedCategory;
	private JRadioButton applyUnitType, applyAll;
	
	public PowerPanel(){
		super();
		
		general = new ArrayList<PowerSettingGeneral>();
		specific = new ArrayList<PowerSettingSpecific>();
		
		generalPanel = new JPanel();//new FlowLayout(FlowLayout.RIGHT, 2, 2));
		generalPanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-15, 260));
		generalPanel.setOpaque(false);
		
		//unreservedLabelGeneral = new JLabel();
		//unreservedLabelGeneral.setPreferredSize(new Dimension(250, 15));
		//unreservedLabelGeneral.setHorizontalAlignment(JLabel.CENTER);
		//generalPanel.add(unreservedLabelGeneral);
		unreservedPanelGeneral = new UnreservedPanel();
		unreservedPanelSpecific = new UnreservedPanel();
		generalPanel.add(unreservedPanelGeneral);
		
		specificPanel = new JPanel();
		specificPanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-28, 280));
		specificPanel.setOpaque(false);
		//specificPanel.setBorder(Window.SELECTED_BORDER);
		
		applyUnitType = new JRadioButton("This Class");
		applyUnitType.setPreferredSize(new Dimension(100, 14));
		applyUnitType.setSelected(true);
		applyAll = new JRadioButton("All Units");
		applyAll.setPreferredSize(new Dimension(100, 14));
		ButtonGroup applyUnitGroup = new ButtonGroup();
		applyUnitGroup.add(applyUnitType);
		applyUnitGroup.add(applyAll);
		JPanel applyGroupPanel = new JPanel();
		applyGroupPanel.setPreferredSize(new Dimension(95, 40));
		applyGroupPanel.add(applyUnitType);
		applyGroupPanel.add(applyAll);
		
		JButton applyButton = new JButton("Apply");
		applyButton.setPreferredSize(new Dimension(55, 30));
		applyButton.setMargin(new Insets(1,1,1,1));
		applyButton.addActionListener(new ApplyListener());
		
		JLabel applyLabel = new JLabel("Apply Settings To All");
		applyLabel.setPreferredSize(new Dimension(200, 16));
		applyLabel.setHorizontalAlignment(JLabel.CENTER);
		
		JPanel applyPanel = new JPanel(Window.DEFAULT_LAYOUT);
		applyPanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-8, 62));
		applyPanel.setOpaque(true);
		applyPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
		applyPanel.add(applyLabel);
		applyPanel.add(applyGroupPanel);
		applyPanel.add(applyButton);
		
		this.add(generalPanel);
		this.add(specificPanel);
		this.add(applyPanel);
	}
	
	public void paintComponent(Graphics g){
		g.drawImage(background, 0, 0, null);
		if (selectedCategory != null){
			g.setColor(Window.SELECTED_COLOR);
			g.drawLine(LINE_POS_X1, selectedCategory.posY, LINE_POS_X1+20, selectedCategory.posY);
			g.drawLine(LINE_POS_X1, selectedCategory.posY+2, LINE_POS_X1+20, selectedCategory.posY+2);
			g.drawLine(LINE_POS_X1, selectedCategory.posY, LINE_POS_X1, linePosY2+2);
			g.drawLine(LINE_POS_X1+2, selectedCategory.posY, LINE_POS_X1+2, linePosY2+2);
			g.drawLine(LINE_POS_X1, LINE_POS_Y1, LINE_POS_X2+2, LINE_POS_Y1);
			g.drawLine(LINE_POS_X1+2, LINE_POS_Y1+2, LINE_POS_X2+2, LINE_POS_Y1+2);
			g.drawLine(LINE_POS_X1, linePosY2, LINE_POS_X2+2, linePosY2);
			g.drawLine(LINE_POS_X1, linePosY2+2, LINE_POS_X2+2, linePosY2+2);
			g.drawLine(LINE_POS_X2, LINE_POS_Y1, LINE_POS_X2, linePosY2+2);
			g.drawLine(LINE_POS_X2+2, LINE_POS_Y1, LINE_POS_X2+2, linePosY2+2);
		}
	}
	
	public void refresh(){
		if (unit != null){
			for (int x = 0; x < generalPanel.getComponentCount(); x++){
				if (generalPanel.getComponent(x) instanceof PowerSettingGeneral){
					PowerSettingGeneral setting = ((PowerSettingGeneral)generalPanel.getComponent(x));
					setting.totalPower[1] = 0;
					for (double[] data : setting.data.values())
						setting.totalPower[1] += data[1];
				}
			}
		}
		
		refreshGeneral();
		if (selectedCategory != null)
			refreshSpecific();
	}
	
	public void setUnit(Unit newUnit){
		unit = newUnit;
		general.clear();
		generalPanel.removeAll();
		specificPanel.removeAll();
		
		generalPanel.add(unreservedPanelGeneral);
		unreservedPanelGeneral.setData(unit.unreservedCap);
		
		int posY = 56;
		for (String category : unit.capacitor.keySet()){
			generalPanel.add(this.new PowerSettingGeneral(category, unit.capacitor.get(category), posY));
			posY += SETTING_SIZE.height + 4;
		}
		posY += SETTING_SIZE.height - 2;
		
		//generalPanel.add(unreservedLabelGeneral);
		refreshGeneral();
		((PowerSettingGeneral)generalPanel.getComponent(1)).selectCategory();
	}
	
	private void refreshGeneral(){
		//unreservedLabelGeneral.setText("Unreserved: "+
		//		round(unit.unreservedCap[Unit.CAP]/unit.type.capacitor*100)+" / "+round(unit.unreservedCap[Unit.RES]*100) + "%");
		unreservedPanelGeneral.refresh();
		
		for (int x = 0; x < generalPanel.getComponentCount(); x++){
			if (generalPanel.getComponent(x) instanceof PowerSetting)
				((PowerSetting)generalPanel.getComponent(x)).refresh();
		}
	}
	
	private void refreshSpecific(){
		//unreservedLabelSpecific.setText("Unreserved: " +
		//		round(selectedCategory.data.get("Unreserved")[1]/(unit.type.capacitor*selectedCategory.getPercent())*100) +
		//		" / " + round(selectedCategory.data.get("Unreserved")[Unit.RES]/selectedCategory.getPercent()*100) + "%");
		unreservedPanelSpecific.refresh();
		
		for (int x = 0; x < specificPanel.getComponentCount(); x++){
			if (specificPanel.getComponent(x) instanceof PowerSetting)
				((PowerSetting)specificPanel.getComponent(x)).refresh();
		}
	}
	
	private class ApplyListener implements ActionListener{
		public void actionPerformed(ActionEvent e){
			for (Controllable controllable : unit.player.units){
				if (controllable instanceof Unit && controllable != unit){
					if (applyAll.isSelected() || controllable.getType() == unit.type)
						applyToUnit((Unit)controllable);
					if (controllable instanceof Ship){
						for (Craft craft : ((Ship)controllable).crafts)
							if (applyAll.isSelected() || controllable.getType() == unit.type)
								applyToUnit(craft);
					}
				}
			}
		}
		
		private void applyToUnit(Unit toApply){
			toApply.unreservedCap[0] = unit.unreservedCap[0];
			for (Map<String, double[]> category : toApply.capacitor.values()){
				for (double[] cap : category.values())
					cap[Unit.RES] = 0;
			}
			for (String category : unit.capacitor.keySet()){
				if (toApply.capacitor.containsKey(category)){
					for (String subCategory : unit.capacitor.get(category).keySet()){
						if (toApply.capacitor.get(category).containsKey(subCategory)){
							toApply.capacitor.get(category).get(subCategory)[Unit.RES] = unit.capacitor.get(category).get(subCategory)[Unit.RES];
						}else
							toApply.capacitor.get(category).get("Unreserved")[Unit.RES] += unit.capacitor.get(category).get(subCategory)[Unit.RES];
					}
				}else{
					for (double[] cap : unit.capacitor.get(category).values())
						toApply.unreservedCap[Unit.RES] += cap[Unit.RES];
				}
			}
		}
	}
	
	private static final int BAR_HEIGHT = 9-2, BAR_WIDTH = 110-2, BAR_GAP = 10;
	static final Dimension SETTING_SIZE = new Dimension(210, 38);
	static final Dimension SLIDER_SIZE = new Dimension(BAR_WIDTH+12, 18);
	static final Dimension TITLE_LABEL_SIZE = new Dimension(70, 13);
	//static final Font POWER_FONT = new Font("Arial", Font.PLAIN, 11);
	static final Color BAR_COLOR_DARK = new Color(40, 140, 40);
	static final Color BAR_COLOR_LIGHT = new Color(20, 190, 20);
	
	private class UnreservedPanel extends JPanel{
		private double[] data;
		
		protected final PowerMeter meter;
		protected final JLabel percentLabel;
		
		public UnreservedPanel(){
			super(Window.DEFAULT_LAYOUT);
			//this.setPreferredSize(new Dimension(48, 170));
			this.setPreferredSize(new Dimension(SETTING_SIZE.width, 30));
			this.setBorder(BorderFactory.createEtchedBorder());
			
			percentLabel = new JLabel();
			percentLabel.setPreferredSize(new Dimension(200, 13));
			percentLabel.setFont(Main.defaultFont);
			percentLabel.setHorizontalAlignment(JLabel.CENTER);
			
			meter = new PowerMeter();
			
			this.add(percentLabel);
			this.add(meter);
		}
		
		public void setData(double[] data){
			this.data = data;
			meter.setData(data);
		}
		
		private void refresh(){
			if (data != null){
				percentLabel.setText("Unreserved  " + round(data[Unit.RES]*100) + "%");
				percentLabel.repaint();
				meter.repaint();
			}
		}
	}
	
	private abstract class PowerSetting extends JPanel{
		protected final PowerMeter meter;
		protected final JSlider slider;
		protected final JLabel percentLabel;
		
		public PowerSetting(String title){
			super(Window.DEFAULT_LAYOUT);
			//this.setPreferredSize(new Dimension(48, 170));
			this.setPreferredSize(SETTING_SIZE);
			this.setBorder(BorderFactory.createEtchedBorder());
			this.setBackground(Window.PANEL_LIGHT);
			
			slider = new JSlider(-1,10);
			slider.setOrientation(JSlider.HORIZONTAL);
			slider.setPreferredSize(SLIDER_SIZE);
			slider.setBackground(Window.PANEL_LIGHT);
			slider.addChangeListener(new ChangeListener(){
				public void stateChanged(ChangeEvent e){
					percentChanged();
			}});
			
			percentLabel = new JLabel();
			percentLabel.setPreferredSize(TITLE_LABEL_SIZE);
			percentLabel.setFont(Main.defaultFont);
			
			meter = new PowerMeter();
			
			JLabel titleLabel = new JLabel(title);
			titleLabel.setFont(Main.defaultFont);
			titleLabel.setPreferredSize(TITLE_LABEL_SIZE);
			
			this.add(titleLabel);
			this.add(slider);
			this.add(percentLabel);
			this.add(meter);
			
		}
		
		public abstract void setPercent(double newPercent);
		public abstract double getPercent();
		protected abstract void percentChanged();
		
		public void refresh(){
			meter.repaint();
		}
	}
	
	private class PowerSettingGeneral extends PowerSetting{
		
		private final Map<String, double[]> data;
		private double[] totalPower;
		protected int posY;
		
		public PowerSettingGeneral(String title, Map<String, double[]> data, int posY){
			super(title);
			this.data = data;
			this.posY = posY;
			general.add(this);
			
			totalPower = new double[3];
			for (double[] val : data.values()){
				totalPower[Unit.RES] += val[Unit.RES];
				totalPower[Unit.CAP] += val[Unit.CAP];
			}
			totalPower[Unit.OFF] = data.get("Unreserved")[Unit.OFF];
			meter.setData(totalPower);
			
			this.addMouseListener(new MouseAdapter(){
				public void mousePressed(MouseEvent e){
					selectCategory();
			}});
			
			updatePowerUI();
		}
		
		public double getPercent(){
			return totalPower[Unit.RES];
		}
		
		public void setPercent(double newPercent){
			if (newPercent < 0){
				newPercent = 0;
				totalPower[Unit.OFF] = 1;
			}else{
				totalPower[Unit.OFF] = 0;
			}
			
			if (totalPower[Unit.RES] != 0){
				for (double[] cap : data.values())
					cap[Unit.RES] *= newPercent/totalPower[Unit.RES];
			}else
				data.get("Unreserved")[Unit.RES] = newPercent;
			
			data.get("Unreserved")[Unit.OFF] = totalPower[Unit.OFF];
			
			totalPower[Unit.RES] = newPercent;
			updatePowerUI();
		}
		
		private void updatePowerUI(){
			if (totalPower[Unit.OFF] > 0){
				slider.setValue(-1);
				percentLabel.setText("OFF");
			}else{
				slider.setValue((int)round(totalPower[Unit.RES]*10));
				percentLabel.setText(round(totalPower[Unit.RES]*100) + "%");
			}
			percentLabel.repaint();
		}
		
		public void setColor(boolean selected){
			if (selected){
				this.setBorder(Window.SELECTED_BORDER);
			}else
				this.setBorder(Window.UNSELECTED_BORDER);
			//this.setBackground(selected ? Window.PANEL_ALT_COLOR1 : Window.PANEL_VERY_LIGHT);
			//slider.setBackground(selected ? Window.PANEL_ALT_COLOR1 : Window.PANEL_VERY_LIGHT);
		}
		
		protected void percentChanged(){
			setPercent(slider.getValue()/10.0);
			
			double totalPercent = 0;
			for (PowerSetting setting : general)
				totalPercent += setting.getPercent();
			if (totalPercent > 1.0){
				int numToDecrease = 0;
				double percentToDecrease = totalPercent-1.0;
				for (PowerSetting setting : general){
					if (setting != PowerSettingGeneral.this){
						if (setting.getPercent() < (totalPercent-1.0)/(unit.capacitor.size()-1)){
							percentToDecrease -= setting.getPercent();
							setting.setPercent(0);
						}else
							numToDecrease++;
					}
				}
				for (PowerSetting setting : general){
					if (setting != PowerSettingGeneral.this && setting.getPercent() > 0)
						setting.setPercent(max(0, setting.getPercent()-percentToDecrease/numToDecrease));
				}
			}
			
			unit.unreservedCap[Unit.RES] = 1.0-min(totalPercent, 1.0);
			refreshGeneral();
			percentLabel.repaint();
		}
		
		private void selectCategory(){
			for (PowerSettingGeneral setting : general)
					setting.setColor(false);
			PowerSettingGeneral.this.setColor(true);
			specificPanel.removeAll();
			specific.clear();
			selectedCategory = null;
			
			if (/*totalPower[Unit.RES] > 0 && */data.size() > 2){
				selectedCategory = PowerSettingGeneral.this;
				
				//unreservedLabelSpecific = new JLabel();
				//unreservedLabelSpecific.setPreferredSize(new Dimension(250, 15));
				//unreservedLabelSpecific.setHorizontalAlignment(JLabel.CENTER);
				specificPanel.add(unreservedPanelSpecific);
				unreservedPanelSpecific.setData(selectedCategory.data.get("Unreserved"));
				
				for (String category : data.keySet()){
					if (category != "Unreserved")
						specificPanel.add(new PowerSettingSpecific(category, data.get(category)));
				}
				//specificPanel.add(unreservedLabelSpecific);
				refreshSpecific();
			}
			
			//specificPanel.setPreferredSize(new Dimension(
			//		GameWindow.MENU_WIDTH-32, (specificPanel.getComponentCount()-1)*(5+SETTING_SIZE.height)+42));
			linePosY2 = (specificPanel.getComponentCount() - 1)*(5 + SETTING_SIZE.height) + 300;
			specificPanel.revalidate();
			//specificPanel.repaint();
			PowerPanel.this.repaint();
		}
	}
	
	private class PowerSettingSpecific extends PowerSetting{
		private final double[] data;
		private final String title;
		
		public PowerSettingSpecific(String title, double[] data){
			super(title);
			this.data = data;
			this.title = title;
			specific.add(this);
			
			meter.setData(data);
			//setPercent(data[Unit.RES]);
			updatePowerUI();
		}
		
		public double getPercent(){
			return data[Unit.RES];
		}
		
		public void setPercent(double newPercent){
			if (newPercent < 0){
				newPercent = 0;
				data[Unit.OFF] = 1;
			}else
				data[Unit.OFF] = 0;
			
			data[Unit.RES] = newPercent;
			updatePowerUI();
		}
		
		private void updatePowerUI(){
			if (data[Unit.OFF] > 0){
				slider.setValue(-1);
				percentLabel.setText("OFF");
			}else{
				slider.setValue((int)round(data[Unit.RES]/selectedCategory.getPercent()*10));
				percentLabel.setText(round(data[Unit.RES]*100) + "%");
			}
			percentLabel.repaint();
		}
		
		protected void percentChanged(){
			//data[Unit.RES] = slider.getValue()*selectedCategory.getPercent()/10;
			double percent = slider.getValue()/10.0;
			if (percent > 0)
				percent *= selectedCategory.getPercent();
			setPercent(percent);
			
			double totalPercent = 0;
			for (String category : selectedCategory.data.keySet()){
				if (category != "Unreserved")
					totalPercent += selectedCategory.data.get(category)[Unit.RES];
			}
			if (totalPercent > selectedCategory.getPercent()){
				int numToDecrease = 0;
				double percentToDecrease = totalPercent-selectedCategory.getPercent();
				for (PowerSettingSpecific setting : specific){
					if (setting != PowerSettingSpecific.this && setting.title != "Unreserved" && setting.data[Unit.OFF] == 0){
						if (setting.data[Unit.RES] < (totalPercent-selectedCategory.getPercent())/(unit.capacitor.size()-2)){
							percentToDecrease -= setting.data[Unit.RES];
							setting.setPercent(0);
						}else
							numToDecrease++;
					}
				}
				for (PowerSettingSpecific setting : specific){
					if (setting != PowerSettingSpecific.this && setting.title != "Unreserved" && setting.data[Unit.RES] > 0)
						setting.setPercent(max(0, setting.data[Unit.RES]-percentToDecrease/numToDecrease));
				}
			}
			selectedCategory.data.get("Unreserved")[Unit.RES] = 
					selectedCategory.getPercent()-min(totalPercent, selectedCategory.getPercent());
			
			refreshSpecific();
			//percentLabel.setText(round(data[Unit.RES]/selectedCategory.getPercent()*100) + "%");
			//percentLabel.repaint();
		}
	}
	
	private class PowerMeter extends JComponent{
		private double[] data;
		
		public PowerMeter(){
			this.setPreferredSize(new Dimension(BAR_WIDTH+2, BAR_HEIGHT+2));
		}
		
		public void setData(double[] data){
			this.data = data;
		}
		
		public void paint(Graphics g){
			if (data == null)
				return;
			
			g.setColor(data.length > Unit.OFF && data[Unit.OFF] > 0 ? Color.RED : Color.BLACK);
			g.drawRect(BAR_GAP, 0, BAR_WIDTH-BAR_GAP+1, BAR_HEIGHT+1);
			if (data[Unit.RES] > 0){
				g.setColor(BAR_COLOR_DARK);
				g.fillRect(1+BAR_GAP, 1,
						(int)((BAR_WIDTH - BAR_GAP)*data[Unit.CAP]/unit.type.capacitor/data[Unit.RES]), BAR_HEIGHT);
				g.setColor(BAR_COLOR_LIGHT);
				g.fillRect(1+BAR_GAP, 1,
						(int)((BAR_WIDTH - BAR_GAP)*data[Unit.CAP]/unit.type.capacitor), BAR_HEIGHT);
			}
		}
	}
	
}
