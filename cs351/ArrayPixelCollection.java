//Alex Kuehnl
//Homework 3 - CS 351-401
//Discussed with Nathan Edwards, and Nathan Stout.
//We discussed design but did not share code.
//Other resources: Professor Boyland's office hours // Piazza // Chapter 3 of textbook


package edu.uwm.cs351;

import java.awt.Color;
import java.awt.Point;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * An extensible Raster that satisfies {@link java.util.Collection} 
 * and uses array lists internally.
 */

public class ArrayPixelCollection extends AbstractCollection<Pixel> implements Cloneable {
	private static final int DEFAULT_INITIAL_WIDTH=1;
	private static final int DEFAULT_INITIAL_HEIGHT=0;
	private List<List<Color>> pixels;
	private int size;
	private int version;

	private static Consumer<String> reporter = (s) -> System.out.println("Invariant error: "+ s);

	private boolean report(String error) {
		reporter.accept(error);
		return false;
	}
	
	private boolean wellFormed() {
		
		//1. the outer array is never null
		if (pixels == null) return report("invariant #1: outer array cannot be null");
		
		// 2. The "size" field is correct -- it is the number
		// of non-null pixels in the raster.
		int nonNulls = 0;
		int j = 0; //column being checked
		
		while (j < pixels.size()) {
			if (pixels.get(j) == null) {
				++j;
				continue;
			}
			
			for (int i = 0; i < pixels.get(j).size(); i++) {
				if (pixels.get(j).get(i) != null) 
					nonNulls++;
				
			}
			
			++j;
		}
		
		if (nonNulls != size) return report("invariant #2: size is not equal to number of non-nulls in the raster");
		
		// If no problems discovered, return true
		return true;
	}

	// This is only for testing the invariant.  Do not change!
	private ArrayPixelCollection(boolean testInvariant) { }

	//Default constructor
	public ArrayPixelCollection() {
		this(DEFAULT_INITIAL_WIDTH, DEFAULT_INITIAL_HEIGHT);
	}

	//Main constructor
	public ArrayPixelCollection(int w, int h) {
		if (w <= 0 || h < 0) throw new IllegalArgumentException("Constructor - width must be at least 1.");
		
		if (w >= 1_000_000 || h >= 1_000_000) throw new OutOfMemoryError("Too big");
		
		this.pixels = new ArrayList<>(w);
		for (int i = 0; i < w; i++) {
			this.pixels.add(new ArrayList<Color>(h));
		}
		
		size = 0;
		version = 1;
		
		assert wellFormed() : "invariant broken: constructor";
	}

	// TODO: methods need to be implemented.
	// Several Raster-specific methods and then
	// some Collection overridings.
	// Make sure to comment reasons for any overrides,
	// and to provide full documentation for public methods that do not override.
	// You are expect to copy documentation from the
	// DynamicRaster class, with changes relevant to the lack of cursors.
	
	@Override //implementation
	public ArrayPixelCollection clone() {
		ArrayPixelCollection result;
		try {
			result = (ArrayPixelCollection) super.clone();
		} catch(CloneNotSupportedException ex) {
			throw new IllegalStateException("did you forget to implement Cloneable?");
		}
		
		result.pixels = new ArrayList<>();
		
		for (int i = 0; i < this.pixels.size(); i++) {
			result.pixels.add(new ArrayList<Color>());
		}
		
		int j = 0;
		while (j < this.pixels.size()) {
			if (this.pixels.get(j) == null) {
				j++;
				continue;
			}
			
			for (int i = 0; i < this.pixels.get(j).size(); i++) {
				
				if (this.pixels.get(j).get(i) != null) {
					Pixel tempPixel = new Pixel(j, i, this.pixels.get(j).get(i));
					result.pixels.get(j).add(tempPixel.color());
				} else {
					result.pixels.get(j).add(null);
				}
			}
			
			j++;
		}
		
		
		return result;
	}
	
	

	@Override //implementation
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (pixels == null) sb.append("null");
		else {
			sb.append("[");
			for (int i=0; i < pixels.size(); ++i) {
				if (i != 0) sb.append(",");
				if (pixels.get(i) == null) continue;
				sb.append(pixels.get(i).size());
			}
			sb.append("]");
		}
		sb.append(":" + size);
		return sb.toString();

	}



	private class MyIterator implements Iterator<Pixel> {
		int x, y;
		int colVersion;
		int remaining;

		private boolean wellFormed() {
			// First check outer invariant, and if that fails don't proceed further
			// Next, if the versions don't match, pretend there are no problems.
			// (Any problems could be due to being stale, which is not our fault.)
			// Then check that (except for the special case x=0, y = -1),
			// x and y refer to an actually place in the list of lists,
			// and then that "remaining" correctly counts any pixels *after* the current one.

			//	1. Outer invariant checks
			if (pixels == null) return report("outer array is null");
			
			//  1.a Other outer invariant check
			
			int nonNulls = 0;
			int j = 0; //column being checked
			
			while (j < pixels.size()) {
				if (pixels.get(j) == null) {
					++j;
					continue;
				}
				
				for (int i = 0; i < pixels.get(j).size(); i++) {
					if (pixels.get(j).get(i) != null) 
						nonNulls++;
					
				}
				
				++j;
			}
			
			if (nonNulls != size) return report("(outer) invariant #2: size is not equal to number of non-nulls in the raster");
			
			//	2. If the versions don't match pretend there are NO (no problems at all, anywhere, not my fault) problems.
			if (colVersion != version) return true;
			
			//	3. Make sure x values of iterator are within bounds of the array.
			if (x < 0 || (x >= pixels.size() && x != 0)) return report("iterator's x value is invalid for the"
					+ "array");
			
			//	4. Make sure sub array is not null.
			if (pixels.get(x) == null) return report("iterator tried to access null inner array");
			
			//	5. Make sure y is within bounds of array (or allow y to be -1 so long as x == 0)
			if (y < -1 || y >= pixels.get(x).size() || (x != 0 && y == -1))
				return report("iterator y value is invalid.");
			


			
			//	6. "remaining" correctly counts any pixels *after* the current one.
			j = x;
			int k = y;
			int counted = 0;
			
			if (pixels.get(j) != null) {
				for (int i = k + 1; i < pixels.get(j).size(); i++) {
					if (pixels.get(j).get(i) != null) ++counted;
				}
			}
			++j;
		
			while (j < pixels.size()) {
				if (pixels.get(j) == null) {
					j++;
					k = 0;
					continue;
				}
				
				for (int i = 0; i < pixels.get(j).size(); i++) {
					if (pixels.get(j).get(i) != null) ++counted;
				}
				
				++j;
			}
			
			if (counted != remaining) return report("invariant #6 - remaining not matching up with counted");
		
			return true;
		}
		
		MyIterator(boolean unused) {} // do not change this iterator
		
		MyIterator() {
			x = 0;
			y = -1;
			colVersion = version;
			remaining = size;
			assert wellFormed() : "MyIterator constructor - invariant broken";
		}

		
		/**
		 * @return - true if the iteration has more elements
		 * @exception - CME if versions are not the same
		 */
		@Override // required
		public boolean hasNext() {
			assert wellFormed() : "hasNext() - invariant broken at start";
			if (colVersion != version) throw new ConcurrentModificationException("iterator is stale");
			return (remaining != 0);
			
		}

		/**
		 * @return - the next Pixel in the iteration
		 * @exception - ConcurrentModificationException if iterator version and data structure version
		 * differ
		 * @exception NoSuchElementException - fail safe for hasNext - if it does not correctly identify
		 * that there is no next element
		 * @exception NoSuchElementException - another fail safe for if hasNext does not work properly
		 */
		@Override //required
		public Pixel next() {
			assert wellFormed() : "next() - invariant broken at start";
			
			if (colVersion != version) throw new ConcurrentModificationException("this iterator is stale");
			
			if (!(hasNext())) {
				throw new NoSuchElementException("next() - no next element.");
			}
			
			int j = x;
			if (pixels.get(j) != null) {
				
				for (int i = y + 1; i < pixels.get(j).size(); i++) {
					if (pixels.get(j).get(i) != null) {
						y = i;
						--remaining;
						return new Pixel(x, y, pixels.get(x).get(y));
					}
				}
			}
			
			++j;
			
			while (j < pixels.size()) {
				if (pixels.get(j) == null) {
					++j;
					continue;
				}
				
				for (int i = 0; i < pixels.get(j).size(); i++) {
					if (pixels.get(j).get(i) != null) {
						y = i;
						x = j;
						--remaining;
						return new Pixel(x, y, pixels.get(x).get(y));
					}
				}
				
				++j;
			}
			
			assert wellFormed() : "next() - invariant broken at end";
			throw new NoSuchElementException("next() - no next element");
		}
		
		/**
		 * removes the last element returned by this iterator
		 * @exception - IllegalStateException if next() has not yet been called (or, when x = 0, y -1)
		 * @exception - IllegalStateException if nothing to remove at the current locartion
		 */
		@Override //required
		public void remove() {
			assert wellFormed() : "remove() - invariant broken at start";
			if (colVersion != version) throw new ConcurrentModificationException("this iterator is stale");
			
			if (x == 0 && y == -1) {
				throw new IllegalStateException("remove() - next() has not been called yet");
			}
			
			if (pixels.get(x).get(y) == null)	{
				throw new IllegalStateException("remove() - there is no current element; may have already been removed.");
			}
			
			pixels.get(x).set(y, null);
			++version;
			++colVersion;
			--size;
			assert wellFormed() : "remove() - invariant broken at end";
		}

	}

	/**
	 * Class for internal testing.
	 * Do not use in client/application code
	 */
	public static class Spy {
		/**
		 * Return the sink for invariant error messages
		 * @return current reporter
		 */
		public Consumer<String> getReporter() {
			return reporter;
		}

		/**
		 * Change the sink for invariant error messages.
		 * @param r where to send invariant error messages.
		 */
		public void setReporter(Consumer<String> r) {
			reporter = r;
		}

		/**
		 * Create an instance of the ADT with give data structure.
		 * This should only be used for testing.
		 * @param d data array
		 * @param s size
		 * @param v current version
		 * @return instance of DynamicRaster with the given field values.
		 */
		public ArrayPixelCollection create(List<List<Color>> d, int s, int v) {
			ArrayPixelCollection result = new ArrayPixelCollection(false);
			result.pixels = d;
			result.size = s;
			result.version = v;
			return result;
		}

		/**
		 * Create an iterator for testing purposes.
		 * @param outer outer object to create iterator for
		 * @param x x coordinate of current
		 * @param y y coordinate of current
		 * @param r remaining pixels after current
		 * @param cv version of collection this iterator is for
		 * @return iterator to the raster
		 */
		public Iterator<Pixel> newIterator(ArrayPixelCollection outer, int x, int y, int r, int cv) {
			MyIterator result = outer.new MyIterator(false);
			result.x = x;
			result.y = y;
			result.remaining = r;
			result.colVersion = cv;
			return result;
		}
		
		/**
		 * Return whether the wellFormed routine returns true for the argument
		 * @param s transaction seq to check.
		 * @return
		 */
		public boolean wellFormed(ArrayPixelCollection s) {
			return s.wellFormed();
		}

		/**
		 * Return whether the wellFormed routine returns true for the argument
		 * @param s transaction seq to check.
		 * @return
		 */
		public boolean wellFormed(Iterator<Pixel> it) {
			MyIterator myit = (MyIterator)it;
			return myit.wellFormed();
		}

	}

	@Override //required
	public Iterator<Pixel> iterator() {
		// TODO Auto-generated method stub
		assert wellFormed() : "iterator() - invariant broken";
		return new MyIterator();
	}

	//@Override - required
	@Override
	public int size() {
		assert wellFormed() : "size - invariant broken";
		return size;
	}
	
	/**
	 * Remove the pixel, if any, at the given coordinates.
	 * Returns whether there was a pixel to remove.
	 * @param x x-coordinate, must not be negative
	 * @param y y-coordinate, must not be negative
	 * @return whether anything was removed.
	 */
	public boolean clearAt(int x, int y) {
		assert wellFormed() : "clearAt - invariant broken at start";
		
		if (x < 0 || y < 0) {
			throw new IllegalArgumentException("clearAt: x and y must be 0 or greater.");
		}
		
		if (x >= pixels.size() || pixels.get(x) == null || y >= pixels.get(x).size() || pixels.get(x).get(y) == null)
			return false;
		
		pixels.get(x).set(y, null);
		version++;
		size--;
		
		assert wellFormed() : "clearAt - invariant broken at end";
		return true;
	}
	
	/** Get a pixel from the raster
	 * @param x x-coordinate, must not be negative
	 * @param y y-coordinate, must not be negative
	 * @return the pixel at x,y, or null if no pixel.
	 */
	public Pixel getPixel(int x, int y) {
		assert wellFormed() : "getPixel - invariant broken";
		if (x < 0 || y < 0) {
			throw new IllegalArgumentException("getPixel: x and y must be 0 or greater.");
		}
		
		if (x >= pixels.size() || pixels.get(x) == null || y >= pixels.get(x).size() || 
				(pixels.get(x).get(y) == null)) return null;
		
		return new Pixel(x, y, pixels.get(x).get(y));
	}

	@Override //implementation
	public boolean add(Pixel e) {
		assert wellFormed() : "add - invariant broken at start";
		if (e.loc().x < 0 || e.loc().y < 0) {
			throw new IllegalArgumentException("add - argument contains negative location values (x or y or x & y)");
		}
		
		int width = pixels.size();
		int x = e.loc().x;

		
		if (width <= x) {
			for (int i = width; i <= x; i++) {
				pixels.add(null);
			}
		}
		
		if (pixels.get(x) == null) {
			pixels.set(x, new ArrayList<Color>(e.loc().y));
		}
		
		int height = pixels.get(x).size();
		int y = e.loc().y;
		
		if (height <= y) {
			for (int i = height; i <= y; i++) {
				pixels.get(x).add(null);
			}
		}

		Color tempColor = null;
		if (pixels.get(e.loc().x).get(e.loc().y) != null) {  
			tempColor = pixels.get(e.loc().x).get(e.loc().y); 
		}
		
		pixels.get(e.loc().x).set(e.loc().y, e.color()); 
		
		if ((tempColor != null && tempColor.equals(pixels.get(e.loc().x).get(e.loc().y)))) {
			assert wellFormed() : "add - invariant broken at end";
			return false;
		} else if (tempColor != null) {
			assert wellFormed() : "add - invariant broken at end";
			return true;
		}
		
		size++;
		version++;
		assert wellFormed() : "add - invariant broken at end";
		return (pixels.get(e.loc().x).get(e.loc().y) != null);
	}
	
	

	@Override //efficiency
	public boolean remove(Object o) {
		if (o == null || (!(o instanceof Pixel))) return false;
		
		Pixel guest = (Pixel) o;
		
		if (guest.loc().x >= pixels.size() || pixels.get(guest.loc().x) == null || 
				guest.loc().y >= pixels.get(guest.loc().x).size() || 
				pixels.get(guest.loc().x).get(guest.loc().y) == null) return false;
		
		if (getPixel(guest.loc().x, guest.loc().y).equals(guest)) {
			pixels.get(guest.loc().x).set(guest.loc().y, null);
			--size;
			++version;
			return true;
		}
		
		
		return false;
	}

	@Override //efficiency
	public boolean contains(Object o) {
		assert wellFormed() : "contains - invariant broken";
		
		if (o == null || !(o instanceof Pixel)) return false;
		Pixel guest = (Pixel) o;
		
		if (guest.loc().x >= pixels.size() || pixels.get(guest.loc().x) == null || 
			guest.loc().y >= pixels.get(guest.loc().x).size() || 
			pixels.get(guest.loc().x).get(guest.loc().y) == null) return false;
		
		if (getPixel(guest.loc().x, guest.loc().y).equals(guest)) return true;
		
		
		return false;
	}
	
	
	
}
