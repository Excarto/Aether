import javax.swing.*;
import javax.swing.event.*;
import java.awt.Dimension;
import java.util.*;

public class WeaponTypeWindow extends TypeWindow{
	static final String[] TYPE_NAMES = new String[]{"Gun", "Beam", "Missile"};
	
	public WeaponTypeWindow(Vector<BuyType> input, Window user){
		super(WeaponType.class, input, user);
		
		ButtonGroup buttonGroup = new ButtonGroup();
		JPanel groupPanel = new JPanel();
		groupPanel.setPreferredSize(new Dimension(120, 85));
		groupPanel.setBorder(BorderFactory.createEtchedBorder());
		
		boolean categorySelected = false;
		JRadioButton selectedCategoryButton = null;
		for (String typeName : TYPE_NAMES){
			final Vector<BuyType> classTypes = new Vector<BuyType>();
			for (BuyType type : types){
				if (type.getClass().getName().contains(typeName))
					classTypes.add(type);
			}
			
			/*final HashMap<String, Double> maxVals = new HashMap<String, Double>();
			final HashMap<String, Double> minVals = new HashMap<String, Double>();
			for (BuyType type : Main.buyTypes){
				if (type.getClass().getName().contains(typeName)){
					for (String[] spec : type.getSpecs()){
						if (!spec[1].equals("CATEGORY") && !spec[1].equals("N/A")){
							Double highVal = maxVals.get(spec[0]), lowVal = minVals.get(spec[0]);
							double specVal = Double.parseDouble(spec[1]);
							if (highVal == null || highVal < specVal)
								maxVals.put(spec[0], specVal);
							if (lowVal == null || lowVal > specVal)
								minVals.put(spec[0], specVal);
						}
					}
				}
			}*/
			
			final JRadioButton button = new JRadioButton(typeName + "s");
			button.setPreferredSize(new Dimension(100, 20));
			button.addChangeListener(new ChangeListener(){
				public void stateChanged(ChangeEvent e){
					if (button.isSelected()){
						types = classTypes;
						//highestValues = maxVals;
						//lowestValues = minVals;
						typeList.setListData(classTypes);
					}
				}
			});
			if (!classTypes.isEmpty() && !categorySelected){
				categorySelected = true;
				selectedCategoryButton = button;
			}
			
			buttonGroup.add(button);
			groupPanel.add(button);
		}
		
		typePanel.add(groupPanel, 1);
		selectedCategoryButton.setSelected(true);
		//((JRadioButton)groupPanel.getComponent(0)).setSelected(true);
		typeList.setSelectedIndex(0);
	}
}
