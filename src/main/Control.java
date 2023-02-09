
// Configurable keyboard controls

public enum Control{
	SCROLL_UP("Scroll Camera Up"),
	SCROLL_DOWN("Scroll Camera Down"),
	SCROLL_LEFT("Scroll Camera Left"),
	SCROLL_RIGHT("Scroll Camera Right"),
	ACCEL_UP("Accelerate Camera Up"),
	ACCEL_DOWN("Accelerate Camera Down"),
	ACCEL_LEFT("Accelerate Camera Left"),
	ACCEL_RIGHT("Accelerate Camera Right"),
	MATCH_CAMERA("Match Camera"),
	ZOOM_IN("Zoom In"),
	ZOOM_OUT("Zoom Out"),
	CHAT("Chat"),
	CLEAR_ORDERS("Clear Orders"),
	CLEAR_TARGET("Clear Target"),
	FIRE("Fire"),
	MANUAL_AIM("Manual Aim"),
	FUNCTION1("Function 1"),
	FUNCTION2("Function 2"),
	MENU("Toggle Menu");
	
	public final String description;
	Control(String description){
		this.description = description;
	}
}
