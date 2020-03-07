import java.awt.*;

public class WinMenu extends Menu{
	
	final static Font LINES_FONT = new Font("Courier", Font.BOLD, 35);
	final static int LINES_SPACING = 40;
	
	private String[] lines;
	
	public WinMenu(Pilot pilot){
		drawTitle = false;
		
		String optionText;
		if (pilot.isSet(MissionVar.ROBO_WIN)){
			if (pilot.type == Pilot.Type.ROBOT){
				lines = new String[]{
						"With the last human resistance quelled, the",
						"combined resources of the old warring colonies",
						"are directed toward the construction",
						"of the New Metal Order"
				};
				optionText = "Where before there was chaos, now there will be order";
			}else{
				lines = new String[]{
						"With the last human resistance among the local colonies",
						"quelled, construction of the New Metal Order begins.",
						"You are assigned a role of military advisor - a job",
						"that the Order plans to make good use of."
				};
				optionText = "Where before there was chaos, now there will be order";
			}
		}else{
			if (pilot.type == Pilot.Type.ROBOT){
				lines = new String[]{
						"With the uprising quelled, the survivors from",
						"each of the colonies agree to peace terms.",
						"Though you are allowed to return to your job as a",
						"technician, construction of new robots is forever banned."
				};
				optionText = "Surely, this time the humans will learn from their past mistakes";
			}else{
				lines = new String[]{
						"With the uprising quelled, the survivors from",
						"each of the colonies agree to peace terms.",
						"Finally, the task of recovery can begin."
				};
				optionText = "Surely, this marks an end to wars among humans";
			}
		}
		
		this.addOption(new Option(optionText){
			public void act(){
				Main.goToMainMenu();
		}});
	}
	
	public void paint(Graphics graphics){
		Graphics2D g = (Graphics2D)graphics;
		super.paint(g);
		
		g.setFont(LINES_FONT);
		
		int posY = Main.resY/2 - 230;
		for (String line : lines){
			Utility.drawOutlinedText(g, line,
						Main.resX/2, posY,
						Color.LIGHT_GRAY, Color.BLACK);
			posY += LINES_SPACING;
		}
	}
	
	protected String getBackgroundFile(){
		return "data/menu_background_blank.png";
	}
	
}
