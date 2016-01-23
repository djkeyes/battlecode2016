package dk011;

public class DoublyLinkedList<T> {

	public DoublyLinkedListNode<T> head, tail;

	public DoublyLinkedList() {
		head = null;
		tail = null;
	}

	public DoublyLinkedListNode<T> append(T toAppend) {
		DoublyLinkedListNode<T> newNode = new DoublyLinkedListNode<T>(toAppend);
		if (head == null) {
			head = newNode;
			tail = newNode;
		} else {
			tail.next = newNode;
			newNode.prev = tail;
		}
		tail = newNode;
		return newNode;
	}

	public void remove(DoublyLinkedListNode<T> toRemove) {
		if (toRemove == head && toRemove == tail) {
			head = tail = null;
		} else if (toRemove == head && toRemove != tail) {
			head = toRemove.next;
			toRemove.next.prev = null;
		} else if (toRemove == tail && toRemove != head) {
			toRemove.prev.next = null;
			tail = toRemove.prev;
		} else {
			toRemove.prev.next = toRemove.next;
			toRemove.next.prev = toRemove.prev;
		}
	}

	public void removeFirst() {
		if (head == null) {
			return;
		}
		if (head == tail) {
			head = tail = null;
		} else {
			head.next.prev = null;
			head = head.next;
		}
	}

	public T peekFirst() {
		if (head == null) {
			return null;
		}
		return head.data;
	}

	public void moveToEnd(DoublyLinkedListNode<T> toMove) {
		// precondition: toMove must already be in the list

		if (toMove == tail) {
			return;
		}

		if (toMove == head) {
			head = toMove.next;
			toMove.next.prev = null;
		} else {
			toMove.next.prev = toMove.prev;
			toMove.prev.next = toMove.next;
		}

		tail.next = toMove;
		toMove.prev = tail;
		toMove.next = null;
		tail = toMove;
	}

	public static class DoublyLinkedListNode<T> {
		DoublyLinkedListNode<T> prev;
		DoublyLinkedListNode<T> next;
		public T data;

		public DoublyLinkedListNode(T data) {
			this.prev = null;
			this.next = null;
			this.data = data;
		}
	}

	@Override
	public String toString() {
		StringBuilder st = new StringBuilder();
		st.append("[");

		DoublyLinkedListNode<T> cur = head;
		while (cur != null) {
			st.append(cur.data.toString() + ",");
			cur = cur.next;
		}
		st.append("]");
		return st.toString();
	}
}
