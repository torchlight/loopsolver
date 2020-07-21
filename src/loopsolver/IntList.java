package loopsolver;

import java.util.Arrays;

public class IntList {

	private int capacity = 12, length = 0;
	private int[] elements = new int[capacity];

	public IntList() {
	}

	public void add(int x) {
		if (length < capacity) {
			elements[length] = x;
			length++;
		}
		else {
			int newCapacity = capacity + Math.min(capacity, 80000000);
			int[] newElements = new int[newCapacity];
			System.arraycopy(elements, 0, newElements, 0, capacity);
			elements = newElements;
			capacity = newCapacity;
			elements[length] = x;
			length++;
		}
	}

	public void addAll(IntList l) {
		if (length + l.length >= capacity) {
			int newCapacity = 2*(length + l.length) - capacity;
			int[] newElements = new int[newCapacity];
			System.arraycopy(elements, 0, newElements, 0, length);
			elements = newElements;
			capacity = newCapacity;
		}
		System.arraycopy(l.elements, 0, elements, length, l.length);
		length += l.length;
	}

	public void addAll(int[] l) {
		if (length + l.length >= capacity) {
			int newCapacity = 2*(length + l.length) - capacity;
			int[] newElements = new int[newCapacity];
			System.arraycopy(elements, 0, newElements, 0, length);
			elements = newElements;
			capacity = newCapacity;
		}
		System.arraycopy(l, 0, elements, length, l.length);
		length += l.length;
	}

	public void clear() {
		length = 0;
	}

	public int get(int index) {
		if (index < 0 || index >= length) throw new IndexOutOfBoundsException();
		return elements[index];
	}

	public void set(int index, int value) {
		if (index < 0 || index >= length) throw new IndexOutOfBoundsException();
		elements[index] = value;
	}

	public boolean isEmpty() {
		return length == 0;
	}

	public int size() {
		return length;
	}

	public int[] toArray() {
		int[] arr = new int[length];
		System.arraycopy(elements, 0, arr, 0, length);
		return arr;
	}

	public void sort() {
		Arrays.sort(elements, 0, length);
	}

	public void uniq() {
		// assume it's already sorted
		if (length < 2) return;
		int last = elements[0];
		int j = 1;
		for (int i = 1; i < length; i++) {
			if (elements[i] == last) continue;
			last = elements[j] = elements[i];
			j++;
		}
		length = j;
	}

	public void reverse() {
		for (int i = 0; i < length/2; i++) {
			int t = elements[i];
			elements[i] = elements[length-1-i];
			elements[length-1-i] = t;
		}
	}

	public int pop() {
		return elements[--length];
	}

	public int peek() {
		return elements[length-1];
	}
}