import static java.lang.Math.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.List;
import javax.swing.*;

public class InputHandler{
	private static final int DOUBLE_CLICK_INTERVAL = 220;
	
	final GameWindow window;
	
	private boolean mousePressedLeft, singleClick;
	private List<Controllable> orderSelected;
	private Queue<MouseEvent> mousePressedQueue, mouseReleasedQueue;
	private Queue<KeyEvent> keyPressedQueue, keyReleasedQueue;
	private Queue<MouseWheelEvent> mouseWheelQueue;
	private TimerThread leftMouseTimer, rightMouseTimer, middleMouseTimer;
	private Point leftClickStartPos, middleButtonHoldPos;
	private Locatable rightClickStart;
	private Sprite middleClickStartSprite;
	private Map<Control, Boolean> controlPressed;
	private TimerThread groupTimer;
	private int timedGroup;
	private Weapon weaponToSetTarget;
	private int zoomLevel;
	private List<List<Controllable>> groups;
	private boolean chatToAll;
	private String currentChat;
	private HungarianAlg assigner;
	
	public InputHandler(GameWindow window, JComponent viewWindow){
		this.window = window;
		
		orderSelected = new ArrayList<Controllable>();
		controlPressed = new HashMap<Control, Boolean>(Main.controlVals.size());
		for (Control control : Control.values())
			controlPressed.put(control, new Boolean(false));
		mousePressedQueue = new ConcurrentLinkedQueue<MouseEvent>();
		mouseReleasedQueue = new ConcurrentLinkedQueue<MouseEvent>();
		keyPressedQueue = new ConcurrentLinkedQueue<KeyEvent>();
		keyReleasedQueue = new ConcurrentLinkedQueue<KeyEvent>();
		mouseWheelQueue = new ConcurrentLinkedQueue<MouseWheelEvent>();
		rightMouseTimer = new TimerThread(null);
		leftMouseTimer = new TimerThread(null);
		middleMouseTimer = new TimerThread(null);
		assigner = new HungarianAlg(10);
		
		groups = new ArrayList<List<Controllable>>(10);
		for (int x = 0; x <= 9; x++)
			groups.add(new ArrayList<Controllable>(4));
		
		viewWindow.addMouseListener(new MouseListener(){
			public void mousePressed(MouseEvent e){
				mousePressedQueue.offer(e);
			}
			public void mouseReleased(MouseEvent e){
				mouseReleasedQueue.offer(e);
			}
			public void mouseClicked(MouseEvent e){}
			public void mouseEntered(MouseEvent e){}
			public void mouseExited(MouseEvent e){}
		});
		viewWindow.addMouseWheelListener(new MouseWheelListener(){
			public void mouseWheelMoved(MouseWheelEvent e){
				mouseWheelQueue.offer(e);
			}
		});
		window.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e){
				keyPressedQueue.offer(e);
			}
			public void keyReleased(KeyEvent e){
				keyReleasedQueue.offer(e);
			}
			public void keyTyped(KeyEvent e){}
		});
		
		zoomLevel = 4;
		window.setZoomLevel(zoomLevel);
	}
	
	public void processInput(List<Controllable> selected){
		if (rightClickStart instanceof MousePosition)
			((MousePosition)rightClickStart).move();
		
		if (rightMouseTimer.timeElapsed && rightMouseTimer.event != null){
			issueMouseCommand(rightMouseTimer.event.getPoint(), DOUBLE_CLICK_INTERVAL, null, true);
			rightMouseTimer.event = null;
		}
		
		while (!mousePressedQueue.isEmpty()){
			MouseEvent e = mousePressedQueue.poll();
			if (e.getButton() == MouseEvent.BUTTON2){
				middleButtonHoldPos = e.getPoint();
				middleMouseTimer = new TimerThread(null);
				middleClickStartSprite = getVisibleSpriteAt(e.getPoint(), 0, false, false);
			}else if (weaponToSetTarget != null){
				if (e.getButton() == MouseEvent.BUTTON1){
					Sprite closest = getVisibleSpriteAt(e.getPoint(), 0, false, true);
					if (closest != null){
						weaponToSetTarget.setOverrideTarget(window.player.getTarget((Controllable)closest));
					}else
						weaponToSetTarget.setOverrideTarget(null);
					setWeaponToSetTarget(null);
				}else
					setWeaponToSetTarget(null);
			}else if (e.getButton() == MouseEvent.BUTTON1){
				mousePressedLeft = true;
				if (!controlPressed(Control.MANUAL_AIM)){
					leftClickStartPos = e.getPoint();
					singleClick = leftMouseTimer.timeElapsed;
					leftMouseTimer = new TimerThread(null);
				}
			}else if (e.getButton() == MouseEvent.BUTTON3){
				if (window.isStrategic() && !controlPressed(Control.FUNCTION1)){
					Sprite closest = getVisibleSpriteAt(e.getPoint(), 0, false, false);
					if (closest instanceof Controllable || closest instanceof Arena.ForegroundObject){
						rightClickStart = closest;
					}else
						rightClickStart = new MousePosition(e.getPoint());
				}
			}
		}
		
		while (!mouseReleasedQueue.isEmpty()){
	 		MouseEvent e = mouseReleasedQueue.poll();
			if (e.getButton() == MouseEvent.BUTTON2){
				
				middleButtonHoldPos = null;
				if (!middleMouseTimer.timeElapsed){
					if (middleClickStartSprite != null){
						window.zoomTo(middleClickStartSprite.getRenderPosX(), middleClickStartSprite.getRenderPosY(),
								middleClickStartSprite.getVelX(), middleClickStartSprite.getVelY());
					}
				}
				
			}else if (e.getButton() == MouseEvent.BUTTON1){
				
				mousePressedLeft = false;
				if (leftClickStartPos != null && !controlPressed(Control.MANUAL_AIM)){
					if (e.getPoint() != null && leftClickStartPos.distance(e.getPoint()) < 5){
						Controllable closest = (Controllable)getVisibleSpriteAt(
								leftClickStartPos, singleClick ? 0 : DOUBLE_CLICK_INTERVAL*5/8, true, false);
						
						if (closest != null){
							if (window.isStrategic()){
								if (singleClick){
									if (!controlPressed(Control.FUNCTION1)){
										selected.clear();
										selected.add(closest);
									}else{
										if (!selected.remove(closest))
											selected.add(closest);
									}
								}else{
									if (!controlPressed(Control.FUNCTION1)){
										selected.clear();
										window.setStrategic(false);
										selected.add(closest);
									}else{
										for (Controllable controllable2 : window.player.controllables){
											Sprite sprite2 = (Sprite)controllable2;
											if (closest.getType() == controllable2.getType() && 
													window.posXOnScreen(sprite2) > 0 && window.posXOnScreen(sprite2) < window.windowResX &&
													window.posYOnScreen(sprite2) > 0 && window.posYOnScreen(sprite2) < window.windowResY)
												selected.add(controllable2);
										}
									}
								}
							}else{
								selected.clear();
								selected.add(closest);
								if (singleClick)
									window.setStrategic(true);
							}
						}else{
							if (!controlPressed(Control.FUNCTION1) && window.isStrategic())
								selected.clear();
						}
						
					}else{
						if (!controlPressed(Control.FUNCTION1))
							selected.clear();
						
						boolean containsUnit = false;
						for (int pass = 1; pass <= 2; pass++){
							for (Controllable controllable : window.player.controllables){
								Sprite sprite = (Sprite)controllable;
								if (window.posXOnScreen(sprite) > min((int)e.getPoint().getX(), (int)leftClickStartPos.getX()) &&
										window.posXOnScreen(sprite) < max((int)e.getPoint().getX(), (int)leftClickStartPos.getX()) &&
										window.posYOnScreen(sprite) > min((int)e.getPoint().getY(), (int)leftClickStartPos.getY()) &&
										window.posYOnScreen(sprite) < max((int)e.getPoint().getY(), (int)leftClickStartPos.getY())){
									if (pass == 1){
										if (sprite instanceof Unit)
											containsUnit = true;
									}else{
										if (sprite instanceof Unit || !containsUnit){
											if (!selected.contains(controllable))
												selected.add(controllable);
											window.setStrategic(true);
										}
									}
								}
							}
						}
					}
					
					leftClickStartPos = null;
					window.queueMenuUpdate();
				}
				
			}else if (e.getButton() == MouseEvent.BUTTON3){
				
				if (controlPressed(Control.FUNCTION1) && rightClickStart == null){
					double posX = window.posXInGame(e.getPoint()), posY = window.posYInGame(e.getPoint());
					for (Controllable controllable : selected)
						controllable.orders().removeOrdersWithin(posX, posY, 5/window.getZoom());
				}else{
					if (!rightMouseTimer.timeElapsed){
						//issueMouseOrder(e.getPoint(), DOUBLE_CLICK_INTERVAL*2/3, null, false);
						issueMouseCommand(rightMouseTimer.event.getPoint(), DOUBLE_CLICK_INTERVAL*2/3, null, false);
						rightMouseTimer.event = null;
					}else{
						orderSelected.clear();
						orderSelected.addAll(selected);
						if (rightClickStart != null &&
								hypot(window.posXInGame(e.getPoint())-rightClickStart.getPosX(),
										window.posYInGame(e.getPoint())-rightClickStart.getPosY()) > window.getMultiPosOrderDist()){
							issueMouseCommand(e.getPoint(), 0, rightClickStart, true);
						}else
							rightMouseTimer = new TimerThread(e);
					}
					rightClickStart = null;
				}
				
			}
		}
		
		while (!keyPressedQueue.isEmpty()){
			KeyEvent e = keyPressedQueue.poll();
			Control control = Main.controlVals.get(e.getKeyCode());
			
			if (control == Control.CHAT && !Main.game.isLocal()){
				if (currentChat != null){
					if (!currentChat.isEmpty())
						window.player.sendMessage(currentChat, chatToAll);
					currentChat = null;
				}else{
					currentChat = "";
					boolean alliesPresent = false;
					for (Player player : Main.game.players)
						alliesPresent = alliesPresent || (player != window.player && player.team == window.player.team);
					chatToAll = !alliesPresent || controlPressed(Control.FUNCTION2);
				}
			}else if (currentChat != null){
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE){
					if (!currentChat.isEmpty())
						currentChat = currentChat.substring(0, currentChat.length()-1);
				}else if (control == Control.MENU){
					currentChat = null;
				}else if (Utility.isPrintable(e.getKeyChar()) && currentChat.length() < TextMsg.MAX_LENGTH)
					currentChat += e.getKeyChar();
			}else{
				if (control != null)
					controlPressed.put(control, true);
				
				if (control == Control.MENU)
					window.toggleMenu();
				
				if (control == Control.MATCH_CAMERA && selected.size() > 0)
					window.matchCamera(selected);
				
				if (control == Control.ZOOM_IN)
					zoomLevel = window.setZoomLevel(zoomLevel - 1);
				if (control == Control.ZOOM_OUT)
					zoomLevel = window.setZoomLevel(zoomLevel + 1);
				
				if (control == Control.CLEAR_ORDERS){
					for (Controllable controllable : selected)
						controllable.orders().clear();
				}
				
				if (control == Control.CLEAR_TARGET){
					for (Controllable controllable : selected){
						if (controllable instanceof Unit)
							((Unit)controllable).setTarget(null);
					}
				}
				
				int index = e.getKeyCode()-48;
				if (index >= 0 && index <= 9){
					List<Controllable> group = groups.get(index);
					if (controlPressed(Control.FUNCTION1)){
						group.clear();
						for (int x = 0; x < selected.size(); x++)
							group.add(selected.get(x));
					}else{
						if (!group.isEmpty()){
							if (group.size() > 1)
								window.setStrategic(true);
							if (!controlPressed(Control.FUNCTION2))
								selected.clear();
							for (int x = 0; x < group.size(); x++){
								Controllable controllable = group.get(x);
								if (window.player.controllables.contains(controllable) && !selected.contains(controllable))
									selected.add(controllable);
							}
							window.queueMenuUpdate();
							if (timedGroup == e.getKeyCode() && !groupTimer.timeElapsed)
								window.matchCamera(selected);
							timedGroup = e.getKeyCode();
							groupTimer = new TimerThread(null);
						}
					}
				}
				
			}
		}
		
		while (!keyReleasedQueue.isEmpty()){
			KeyEvent e = keyReleasedQueue.poll();
			controlPressed.put(Main.controlVals.get(e.getKeyCode()), false);
		}
		
		while (!mouseWheelQueue.isEmpty()){
			MouseWheelEvent e = mouseWheelQueue.poll();
			if (e.getPoint() != null){
				if (e.getWheelRotation() < 0){
					if (zoomLevel > 0){
						int oldPosX = window.posXInGame(e.getPoint()), oldPosY = window.posYInGame(e.getPoint());
						zoomLevel = window.setZoomLevel(zoomLevel - 1);
						window.setPosX(window.getPosX() + oldPosX-window.posXInGame(e.getPoint()));
						window.setPosY(window.getPosY() + oldPosY-window.posYInGame(e.getPoint()));
					}
				}else
					zoomLevel = window.setZoomLevel(zoomLevel + 1);
				window.cancelZoomTo();
			}
		}
	}
	
	private void issueMouseCommand(Point mouseLocation, int locationTimeDelay, Locatable startLocation, boolean singleClick){
		if (orderSelected.isEmpty())
			return;
		
		if (!window.isStrategic()){
			issueTacticalMouseCommand(mouseLocation, locationTimeDelay, singleClick);
		}else{
			if (startLocation != null){
				issueStrategicDragMouseCommand(mouseLocation, locationTimeDelay, startLocation, singleClick);
			}else{
				issueStrategicClickMouseCommand(mouseLocation, locationTimeDelay, singleClick);
			}
		}
	}
	
	private void issueTacticalMouseCommand(Point mouseLocation, int locationTimeDelay, boolean singleClick){
		int locX = window.posXInGame(mouseLocation), locY = window.posYInGame(mouseLocation);
		
		Controllable selected = orderSelected.get(0);
		if (selected instanceof Unit){
			Sprite sprite = getVisibleSpriteAt(mouseLocation, locationTimeDelay, false, true);
			if (sprite != null && sprite instanceof Unit){
				((Unit) selected).setTarget(window.player.getTarget((Controllable)sprite));
				return;
			}
		}
		selected.orders().queueOrder(new TurnTo(
				90-toDegrees(atan2(((Sprite)selected).getRenderPosY()-locY, locX-((Sprite)selected).getRenderPosX()))));
	}
	
	private void issueStrategicDragMouseCommand(Point mouseLocation, int locationTimeDelay, Locatable startLocation, boolean singleClick){
		int locX = window.posXInGame(mouseLocation), locY = window.posYInGame(mouseLocation);
		
		double dx = locX-startLocation.getPosX();
		double dy = locY-startLocation.getPosY();
		if (startLocation instanceof Controllable || startLocation instanceof Arena.ForegroundObject){
			
			for (Controllable selected : orderSelected){
				if (startLocation == selected){
					selected.orders().queueOrder(new MovePastPoint(locX, locY, selected.getVelX(), selected.getVelY()));
				}else if(selected instanceof Unit){
					if (startLocation instanceof Arena.ForegroundObject){
						selected.orders().queueOrder(new Orbit(startLocation, hypot(dx, dy)));
					}else{
						if (((Controllable)startLocation).getPlayer().team == window.player.team){
							if (startLocation instanceof Unit){
								selected.orders().queueOrder(new Escort((Unit)startLocation, Unit.ESCORT_SLACK,
										90-toDegrees(atan2(-dy, dx)), hypot(dx, dy)));
							}
						}else{
							if (startLocation instanceof Unit)
								selected.orders().queueOrder(new Orbit(window.player.getTarget((Unit)startLocation), hypot(dx, dy)));
						}
					}
				}
			}
			
		}else if (startLocation instanceof MousePosition){
			
			int incrementX = (locX-(int)startLocation.getPosX())/max(1, orderSelected.size()-1);
			int incrementY = (locY-(int)startLocation.getPosY())/max(1, orderSelected.size()-1);
			if (assigner.matrix.length < orderSelected.size())
				assigner = new HungarianAlg(orderSelected.size());
			
			Locatable[] destinations = new Locatable[orderSelected.size()];
			double posX = startLocation.getPosX();
			double posY = startLocation.getPosY();
			for (int x = 0; x < destinations.length; x++){
				destinations[x] = new Location(posX, posY, startLocation.getVelX(), startLocation.getVelY());
				posX += incrementX;
				posY += incrementY;
			}
			
			for (int x = 0; x < orderSelected.size(); x++){
				for (int y = 0; y < orderSelected.size(); y++){
					double time = Utility.approxTime(orderSelected.get(x), destinations[y]);
					assigner.matrix[y][x] = pow(time, 2);
				}
			}
			
			int[] assignments = assigner.solve(orderSelected.size());
			for (int x = 0; x < orderSelected.size(); x++){
				Locatable dest = destinations[assignments[x]];
				orderSelected.get(x).orders().queueOrder(new MoveToPoint(
						dest.getPosX(), dest.getPosY(),
						dest.getVelX(), dest.getVelY()));
			}
			
		}
	}
	
	private void issueStrategicClickMouseCommand(Point mouseLocation, int locationTimeDelay, boolean singleClick){
		int locX = window.posXInGame(mouseLocation), locY = window.posYInGame(mouseLocation);
		
		Sprite sprite = getVisibleSpriteAt(mouseLocation, locationTimeDelay, false, false);
		if (sprite != null){
			for (Controllable selected : orderSelected){
				if (selected != sprite){
					
					if (sprite instanceof Arena.Objective){
						if (singleClick){
							selected.orders().queueOrder(new MoveToPoint(locX, locY, window.getVelX(), window.getVelY()));
						}else{
							if (selected instanceof Unit && !singleClick && ((Unit)selected).type.captureRate > 0)
								selected.orders().queueOrder(new Capture((Arena.Objective)sprite));
						}
					}else if (sprite instanceof Controllable){
						if (((Controllable)sprite).getPlayer().team == window.player.team){
							if (singleClick){
								if (selected instanceof Unit && sprite instanceof Unit){
									selected.orders().queueOrder(new Escort((Unit)sprite, Unit.ESCORT_SLACK,
											sprite.heading(selected), 300));
								}else{
									selected.orders().queueOrder(new MoveToPoint(
											(int)sprite.getPosX(), (int)sprite.getPosY(), window.getVelX(), window.getVelY()));
								}
							}else{
								if (selected instanceof Unit && sprite instanceof Unit){
									if (selected instanceof Ship){
										selected.orders().queueOrder(new Repair((Unit)sprite));
									}else{
										if (sprite instanceof Ship)
											selected.orders().queueOrder(new Dock((Ship)sprite));
									}
								}else{
									selected.orders().queueOrder(new MovePastPoint(
											(int)sprite.getPosX(), (int)sprite.getPosY(), window.getVelX(), window.getVelY()));
								}
							}
						}else{
							Target target = window.player.getTarget((Controllable)sprite);
							if (selected instanceof Missile){
								selected.orders().queueOrder(new Impact(target));
							}else{
								if ((((Unit)selected).getTarget() == target || controlPressed.get(Control.FUNCTION2)) && window.isStrategic()){
									if (singleClick){
										selected.orders().queueOrder(new AttackSlow(target));
									}else
										selected.orders().queueOrder(new AttackFast(target));
								}else{
									if (target.target instanceof Unit)
										((Unit)selected).setTarget(target);
								}
							}
						}
					}
					
				}
			}
		}else{
			for (Controllable selected : orderSelected){
				if (singleClick){
					selected.orders().queueOrder(new MoveToPoint(locX, locY, window.getVelX(), window.getVelY()));
				}else
					selected.orders().queueOrder(new MovePastPoint(locX, locY, window.getVelX(), window.getVelY()));
			}
		}
	}
	
	private Sprite getVisibleSpriteAt(Point point, int timeDelay, boolean isPlayer, boolean isntPlayer){
		double turnsDelay = Main.TPS*timeDelay/1000.0;
		Sprite closestSprite = null;
		int closestDistance = 1000;
		for (Controllable visibleControllable : window.player.visibleControllables){
			if (isOnScreen((Sprite)visibleControllable)){
				if ((!isPlayer && !isntPlayer)
						|| (isPlayer && visibleControllable.getPlayer() == window.player)
						|| (isntPlayer && visibleControllable.getPlayer() != window.player)){
					int distance = getScreenDistance((Sprite)visibleControllable, point, turnsDelay);
					if (window.isSelected(visibleControllable))
						distance += 4;
					if (distance < closestDistance){
						closestDistance = distance;
						closestSprite = (Sprite)visibleControllable;
					}
				}
			}
		}
		for (Sprite visibleObject : Main.game.arena.foregroundObjects){
			if (isOnScreen(visibleObject) && !isPlayer && !isntPlayer){
				int distance = 5 + getScreenDistance(visibleObject, point, turnsDelay);
				if (distance < closestDistance){
					closestDistance = distance;
					closestSprite = visibleObject;
				}
			}
		}
		return closestSprite;
	}
	private int getScreenDistance(Sprite sprite, Point point, double turnsDelay){
		int timeShiftX = (int)(turnsDelay*(window.getVelX() - sprite.getVelX()));
		int timeShiftY = (int)(turnsDelay*(window.getVelY() - sprite.getVelY()));
		int distanceX = window.posXOnScreen(sprite.getRenderPosX() + timeShiftX) - point.x;
		int distanceY = window.posYOnScreen(sprite.getRenderPosY() + timeShiftY) - point.y;
		int distance = (int)sqrt(distanceX*distanceX + distanceY*distanceY);
		if (distance >= 20 + sprite.getRenderSize(window.getZoom())*3/8)
			distance = Integer.MAX_VALUE/2;
		return distance;
	}
	
	private boolean isOnScreen(Sprite sprite){
		int size = sprite.getRenderSize(window.getRenderZoom());
		int posX = window.posXOnScreen(sprite.getPosX()), posY = window.posYOnScreen(sprite.getPosY());
		return posX > -size && posX < window.windowResX+size &&
				posY > -size && posY < window.windowResY+size;
	}
	
	public void setWeaponToSetTarget(Weapon weapon){
		weaponToSetTarget = weapon;
		this.window.setCursor(new Cursor(weapon != null ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
	}
	public Weapon getWeaponToSetTarget(){
		return weaponToSetTarget;
	}
	
	public boolean controlPressed(Control control){
		return controlPressed.get(control);
	}
	
	public Point getMiddleButtonHoldPos(){
		return middleButtonHoldPos;
	}
	
	public void setMiddleButtonHoldPos(Point point){
		middleButtonHoldPos = point;
	}
	
	public void removeControllable(Controllable controllable){
		if (rightClickStart == controllable)
			rightClickStart = null;
	}
	
	public boolean isMousePressed(){
		return mousePressedLeft;
	}
	
	public Point getleftClickStartPos(){
		return leftClickStartPos;
	}
		
	public Locatable getRightClickStart(){
		return rightClickStart;
	}
	
	public String getChat(){
		return currentChat;
	}
	
	public boolean isChatToAll(){
		return chatToAll;
	}
	
	private class TimerThread extends Thread{
		public boolean timeElapsed;
		private MouseEvent event;
		
		public TimerThread(MouseEvent event){
			super("InputTimerThread");
			this.event = event;
			timeElapsed = false;
			this.start();
		}
		
		public void run(){
			try{
				Thread.sleep(DOUBLE_CLICK_INTERVAL);
			}catch (InterruptedException e){}
			timeElapsed = true;
		}
	}
	
	private class MousePosition implements Locatable{
		double posX, posY, velX, velY;
		public MousePosition(Point point){
			posX = window.posXInGame(point);
			posY = window.posYInGame(point);
			velX = window.getVelX();
			velY = window.getVelY();
		}
		public void move(){
			posX += velX;
			posY += velY;
		}
		public double getPosX(){
			return posX;
		}
		public double getPosY(){
			return posY;
		}
		public double getVelX(){
			return velX;
		}
		public double getVelY(){
			return velY;
		}
	}
}
