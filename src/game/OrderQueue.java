import static java.lang.Math.*;
import java.util.*;
import java.util.concurrent.*;

public class OrderQueue implements Iterable<Order>{
	
	private final Controllable host;
	private final LinkedList<Order> queue;
	public final Deque<Order> active;
	private Order defaultOrder;
	
	public OrderQueue(Controllable host){
		queue = new LinkedList<Order>();
		active = new ConcurrentLinkedDeque<Order>();
		this.host = host;
	}
	
	public void act(){
		for (Order order : queue)
			order.move();
		for (Order order : active)
			order.move();
		for (Order order : active)
			order.act();
	}
	
	public void recordPos(){
		for (Order order : queue)
			order.recordPos();
		for (Order order : active)
			order.recordPos();
	}
	
	public void queueOrder(Order order){
		if (!host.getPlayer().controlPressed(Control.FUNCTION2))
			clearAll();
		if (active.contains(defaultOrder))
			clearAll();
		
		if (active.isEmpty()){
			active.push(order);
			order.setHost(host);
		}else
			queue.offer(order);
	}
	
	public void stackOrder(Order newOrder, Order oldOrder){
		if (isActive(oldOrder)){
			while (!active.isEmpty() && active.peek() != oldOrder)
				active.pop().finish();
			//if (active.isEmpty())
			//	return;
			if (newOrder != null){
				active.push(newOrder);
				newOrder.setHost(host);
			}
		}
	}
	
	public void clear(){
		clearAll();
		if (defaultOrder != null){
			active.push(defaultOrder);
			defaultOrder.setHost(host);
		}
	}
	
	private void clearAll(){
		while (!queue.isEmpty())
			queue.poll().finish();
		while (!active.isEmpty())
			active.pop().finish();
	}
	
	public void finish(Order order){
		if (isActive(order)){
			while (!active.isEmpty()){
				Order finished = active.pop();
				finished.finish();
				if (finished == order)
					break;
			}
		}
		
		if (active.isEmpty()){
			if (queue.isEmpty()){
				if (defaultOrder != null)
					queueOrder(defaultOrder);
			}else{
				Order next = queue.poll();
				next.setHost(host);
				active.push(next);
			}
		}
	}
	
	public Order getTopOrder(){
		if (active.isEmpty())
			return null;
		return active.peekFirst();
	}
	
	public Order getOrder(){
		if (active.isEmpty())
			return null;
		return active.peekLast();
	}
	
	public void removeTarget(Controllable target){
		for (Iterator<Order> i = queue.iterator(); i.hasNext();){
			Order order = i.next();
			if (order instanceof TrackOrder && ((TrackOrder)order).target.equals(target))
				i.remove();
		}
		
		Order toRemove;
		do{
			toRemove = null;
			for (Order order : active){
				if (order instanceof TrackOrder && ((TrackOrder)order).target.equals(target)){
					toRemove = order;
					break;
				}
			}
			if (toRemove != null)
				finish(toRemove);
		}while (toRemove != null);
	}
	
	public void removeOrdersWithin(double posX, double posY, double threshold){
		for (Iterator<Order> i = this.iterator(); i.hasNext();){
			Order order = i.next();
			if (order instanceof Locatable){
				Locatable locatable = (Locatable)order;
				if (hypot(posX-locatable.getPosX(), posY-locatable.getPosY()) < threshold)
					i.remove();
			}
		}
	}
	
	public void setDefault(Order defaultOrder){
		this.defaultOrder = defaultOrder;
		if (queue.isEmpty() && active.isEmpty() && defaultOrder != null)
			queueOrder(defaultOrder);
	}
	
	public Order getDefault(){
		return defaultOrder;
	}
	
	public Iterator<Order> iterator(){
		return new Iterator<Order>(){
			private Iterator<Order> queueIterator;
			private Order first;
			public Order next(){
				if (first == null){
					first = active.peekLast();
					return first;
				}else{
					if (queueIterator == null)
						queueIterator = queue.iterator();
					return queueIterator.next();
				}
			}
			public boolean hasNext(){
				if (first == null){
					return !active.isEmpty();
				}else{
					if (queueIterator == null){
						return !queue.isEmpty();
					}else
						return queueIterator.hasNext();
				}
			}
			public void remove(){
				if (queueIterator == null){
					finish(first);
					first = null;
				}else
					queueIterator.remove();
			}
		};
	}
	/*public Iterator<Order> iterator(){
		return new Iterator<Order>(){
			private Iterator<Order> queueIterator = queue.descendingIterator();
			private boolean finished = active.isEmpty();
			public Order next(){
				if (queueIterator.hasNext()){
					return queueIterator.next();
				}else{
					finished = true;
					return active.peekLast();
				}
			}
			public boolean hasNext(){
				return !finished;
			}
			public void remove(){
				if (queueIterator.hasNext()){
					queueIterator.remove();
				}else{
					finished = true;
					finish(active.peekLast());
				}
			}
		};
	}*/
	
	public boolean isEmpty(){
		return queue.isEmpty() && active.isEmpty();
	}
	
	public boolean isActive(Order order){
		return active.contains(order);
	}
	
	public boolean isActive(Class<? extends Order> orderClass){
		for (Order order : active){
			if (orderClass.isInstance(order))
				return true;
		}
		return false;
	}
}
