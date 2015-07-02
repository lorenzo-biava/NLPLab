package it.unito.nlplab.semantics.rocchio.utils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for parallel tasks execution.
 *
 */
public class Parallel {
	private static final int NUM_CORES = Runtime.getRuntime()
			.availableProcessors();

	private static final ExecutorService forPool = Executors
			.newFixedThreadPool(NUM_CORES);

	/**
	 * Invoke given operation in parallel on each element of the given
	 * {@link Iterable}.
	 * 
	 * @param elements
	 * @param operation
	 */
	public static <T> void For(final Iterable<T> elements,
			final Operation<T> operation) {
		try {
			// invokeAll blocks for us until all submitted tasks in the call
			// complete
			forPool.invokeAll(createCallables(elements, operation));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static <T> Collection<Callable<Void>> createCallables(
			final Iterable<T> elements, final Operation<T> operation) {
		List<Callable<Void>> callables = new LinkedList<Callable<Void>>();
		int t = 99999;
		int i = 0;
		for (final T elem : elements) {
			final int c = i++;
			callables.add(new Callable<Void>() {
				@Override
				public Void call() {
					operation.perform(elem, c, t);
					return null;
				}
			});
		}

		return callables;
	}

	/**
	 * Interface the operation MUST implement.
	 *
	 * @param <T>
	 */
	public static interface Operation<T> {
		/**
		 * 		 
		 * @param pParameter the element
		 * @param index the index of the element
		 * @param total the total number of elements (<b>Currently not working</b>)
		 */
		public void perform(T pParameter, int index, int total);
	}
}