import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public final class ShipOutfitWindow extends OutfitWindow{
	
	private final JList<Craft> craftList;
	private final JSlider storageRatio;
	private final JLabel ammoLabel, materialLabel;
	
	private final Ship ship;
	
	public ShipOutfitWindow (Ship unit){
		super(unit);
		ship = unit;
		ammoRatio = ship.ammoRatio;
		
		craftList = new JList<Craft>();
		craftList.setPreferredSize(new Dimension(167, 170));
		JButton add = new JButton("Add");
		add.setPreferredSize(new Dimension(80, 27));
		add.addActionListener(new AddListener());
		JButton copy = new JButton("Copy");
		copy.setPreferredSize(new Dimension(80, 27));
		copy.addActionListener(new CopyListener());
		JButton delete = new JButton("Delete");
		delete.setPreferredSize(new Dimension(80, 27));
		delete.setMargin(new Insets(2, 2, 2, 2));
		delete.addActionListener(new DeleteListener());
		JButton outfit = new JButton("Outfit");
		outfit.setPreferredSize(new Dimension(80, 27));
		outfit.addActionListener(new OutfitListener());
		JPanel fleetPanel = new JPanel();
		fleetPanel.setPreferredSize(new Dimension(177, 269));
		fleetPanel.setBorder(BorderFactory.createEtchedBorder());
		JLabel fleetLabel = new JLabel("Craft");
		fleetLabel.setFont(new Font("Arial", Font.BOLD, 12));
		fleetPanel.add(fleetLabel);
		fleetPanel.add(craftList);
		fleetPanel.add(add);
		fleetPanel.add(outfit);
		fleetPanel.add(copy);
		fleetPanel.add(delete);
		if (ship.type.totalCraftMass <= 0){
			craftList.setEnabled(false);
			add.setEnabled(false);
			copy.setEnabled(false);
			delete.setEnabled(false);
			outfit.setEnabled(false);
			fleetLabel.setEnabled(false);
		}
		
		JLabel storageLabel = new JLabel("Storage Space");
		storageLabel.setFont(new Font("Arial", Font.BOLD, 12));
		storageLabel.setPreferredSize(new Dimension(102, 15));
		ammoLabel = new JLabel();
		ammoLabel.setPreferredSize(new Dimension(108, 13));
		materialLabel = new JLabel();
		materialLabel.setPreferredSize(new Dimension(108, 15));
		storageRatio = new JSlider(0, 100, 0);
		storageRatio.setPreferredSize(new Dimension(108, 20));
		storageRatio.addChangeListener(new StorageRatioListener());
		storageRatio.setValue((int)(ammoRatio*100));
		JPanel storagePanel = new JPanel();
		storagePanel.setPreferredSize(new Dimension(110, 80));
		storagePanel.add(storageLabel);
		storagePanel.add(storageRatio);
		storagePanel.add(ammoLabel);
		storagePanel.add(materialLabel);
		
		JButton saveButton = new JButton("Save to file");
		saveButton.setPreferredSize(new Dimension(100, 23));
		saveButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				BufferedWriter out = null;
				try{
					File file = new File("data/saved/"+ship.getName()+".txt");
					file.delete();
					out = new BufferedWriter(new FileWriter(file));
					ship.write(out);
				}catch (IOException ex){}
				try{
					if (out != null)
						out.close();
				}catch (IOException ex){}
		}});
		namePanel.add(saveButton);
		
		rightPanel.add(createSpacer(200,ammoStoragePanel.getPreferredSize().height), rightPanel.getComponentCount()-2); 
		rightPanel.remove(ammoStoragePanel);
		
		ammoStoragePanel.add(storagePanel, 0);
		ammoStoragePanel.setPreferredSize(new Dimension(ammoStoragePanel.getPreferredSize().width, 158));
		
		JPanel leftPanel = new JPanel();
		leftPanel.setPreferredSize(rightPanel.getPreferredSize());
		leftPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		leftPanel.add(ammoStoragePanel);
		leftPanel.add(fleetPanel);
		this.add(leftPanel, 1);
		
		updateUnitList();
	}
	
	public void returnValue(Object type){
		if (type instanceof CraftType){
			Craft craft = new Craft((CraftType)type);
			ship.crafts.add(craft);
			Main.addWindow(new OutfitWindow(craft));
		}else
			super.returnValue(type);
	}
	
	public void resume(){
		super.resume();
		updateUnitList();
	}
	
	private void updateUnitList(){
		/*String[] temp = new String[ship.crafts.size()];
		for (int x = 0; x < ship.crafts.size(); x++)
			temp[x] = ship.crafts.get(x).name;*/
		Craft[] data = new Craft[ship.crafts.size()];
		for (int x = 0; x < ship.crafts.size(); x++)
			data[data.length-1-x] = ship.crafts.get(x);
		craftList.setListData(data);
	}
	
	protected void autoLoadout(){
		super.autoLoadout();
		updateUnitList();
	}
	
	private class AddListener implements ActionListener{
		public void actionPerformed (ActionEvent e){
			Vector<BuyType> craftTypes = new Vector<BuyType>();
			for (CraftType type : Main.craftTypes){
				if (type.mass <= ship.type.craftMass && ship.totalCraftMass()+type.mass <= ship.type.totalCraftMass)
					craftTypes.add(type);
			}
			if (!craftTypes.isEmpty())
				Main.addWindow(new TypeWindow(CraftType.class, craftTypes, ShipOutfitWindow.this));
		}
	}
	
	private class CopyListener implements ActionListener{
		public void actionPerformed (ActionEvent e){
			if (craftList.getSelectedIndex() != -1){
				Craft craft = craftList.getSelectedValue();
				if (ship.totalCraftMass()+craft.type.mass <= ship.type.totalCraftMass){
					Craft newCraft = new Craft(craft.type);
					craft.copyComponentsAndAmmoTo(newCraft);
					
					ship.crafts.add(newCraft);
					updateUnitList();
				}
			}
		}
	}
	
	private class OutfitListener implements ActionListener{
		public void actionPerformed (ActionEvent e){
			if (craftList.getSelectedIndex() != -1)
				Main.addWindow(new OutfitWindow(craftList.getSelectedValue()));
		}
	}
	
	private class DeleteListener implements ActionListener{
		public void actionPerformed (ActionEvent e){
			if (craftList.getSelectedIndex() != -1){
				ship.crafts.remove(craftList.getSelectedValue());
				updateUnitList();
			}
		}
	}
	
	private class StorageRatioListener implements ChangeListener{
		public void stateChanged(ChangeEvent e){
			ammoLabel.setText("Ammunition: " + storageRatio.getValue() + "%");
			materialLabel.setText("Repair Parts: " + (100-storageRatio.getValue()) + "%");
			ammoRatio = storageRatio.getValue()*0.01;
			ship.ammoRatio = ammoRatio;
		}
	}
}
