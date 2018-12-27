import java.util.*;

public class IdList<T> extends Vector<T>{
	
	public IdList(){
		super();
	}
	public IdList(int capacity){
		super(capacity);
	}
	
	public boolean add(T id){
		for (int x = size()-1; x >= 0; x--){
			if (((Id)get(x)).getId() < ((Id)id).getId()){
				add(x+1, id);
				return true;
			}
		}
		add(0, id);
		return true;
	}
	
	public T getById(short id){
		int low = 0, high = size()-1, mid;
        while(low <= high){
            mid = (low+high)/2;
            short midId = ((Id)get(mid)).getId();
            if (midId < id){
                low = mid + 1;
            }else if (midId > id){
                high = mid-1;
            }else
                return get(mid);
        }
        return null;
	}
}
