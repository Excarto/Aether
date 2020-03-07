
public class MainMenu extends Menu{
	
	public MainMenu(){
		
		this.addOption(new Option("Singleplayer"){
			public void act(){
				Menu menu = new Menu();
				
				menu.addOption(new Option("Missions"){
					public void act(){
						Menu menu = new CreatePilotMenu();
						Main.addWindow(menu);
						menu.start();
				}});
				
				menu.addOption(new Option("Custom Battle"){
					public void act(){
						Main.addWindow(new SetupWindowHost());
				}});
				
				menu.addOption(new Option("Back"){
					public void act(){
						Main.removeWindow();
				}});
				
				Main.addWindow(menu);
				menu.start();
		}});
		
		this.addOption(new Option("Multiplayer"){
			public void act(){
				Menu menu = new Menu();
				
				menu.addOption(new Option("Internet"){
					public void act(){
						Menu menu = new Menu();
				
						menu.addOption(new Option("Host Internet Game"){
							public void act(){
								Main.addWindow(new SetupWindowNetHost(false));
						}});
						
						menu.addOption(new Option("Join Internet Game"){
							public void act(){
								Main.addWindow(new ClientJoinWindow(false));
						}});
						
						menu.addOption(new Option("Back"){
							public void act(){
								Main.removeWindow();
						}});
						
						Main.addWindow(menu);
						menu.start();
				}});
				
				menu.addOption(new Option("Local Area Network"){
					public void act(){
						Menu menu = new Menu();
				
						menu.addOption(new Option("Host LAN Game"){
							public void act(){
								Main.addWindow(new SetupWindowNetHost(true));
						}});
						
						menu.addOption(new Option("Join LAN Game"){
							public void act(){
								Main.addWindow(new ClientJoinWindow(true));
						}});
						
						menu.addOption(new Option("Back"){
							public void act(){
								Main.removeWindow();
						}});
						
						Main.addWindow(menu);
						menu.start();
				}});
				
				menu.addOption(new Option("Back"){
					public void act(){
						Main.removeWindow();
				}});
				
				Main.addWindow(menu);
				menu.start();
		}});
		
		this.addOption(new Option("Options"){
			public void act(){
				Menu menu = new Menu();
				
				menu.addOption(new Option("Settings"){
					public void act(){
						Main.addWindow(new OptionsWindow());
				}});
				
				menu.addOption(new Option("Controls"){
					public void act(){
						Main.addWindow(new ControlsWindow());
				}});
				
				menu.addOption(new Option("Back"){
					public void act(){
						Main.removeWindow();
				}});
				
				Main.addWindow(menu);
				menu.start();
		}});
		
		this.addOption(new Option("Exit"){
			public void act(){
				Main.exit();
		}});
	}
}
