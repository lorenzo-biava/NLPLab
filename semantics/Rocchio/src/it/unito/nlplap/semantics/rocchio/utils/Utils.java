package it.unito.nlplap.semantics.rocchio.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Utils {

	public static <A extends Comparable<A>> Map<String, A> sortByComparator(
			Map<String, A> unsortMap, boolean reverse) {
	
		final boolean rev = reverse;
	
		// Convert Map to List
		List<Map.Entry<String, A>> list = new LinkedList<Map.Entry<String, A>>(
				unsortMap.entrySet());
	
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, A>>() {
			public int compare(Map.Entry<String, A> o1, Map.Entry<String, A> o2) {
				return (o1.getValue()).compareTo(o2.getValue())
						* (rev ? -1 : 1);
			}
		});
	
		// Convert sorted map back to a Map
		Map<String, A> sortedMap = new LinkedHashMap<String, A>();
		for (Iterator<Map.Entry<String, A>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, A> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	public static <A, B> Map<A, B> clone(Map<A, B> map) {
		Map<A, B> clone = new HashMap<A, B>();
		for (Map.Entry<A, B> entry : map.entrySet()) {
			clone.put(entry.getKey(),
					(B) ((Cloneable) entry.getValue()).clone());
		}
		return clone;
	}

}
