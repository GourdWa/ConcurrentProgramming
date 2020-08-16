import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Zixiang Hu
 * @description
 * @create 2020-07-29-9:39
 */
public class ConcurrentLinkedQueueDemo1 {
    public static void main(String[] args) {
        new ConcurrentLinkedQueue<>();
//        ReentrantLock
/*        public boolean offer(E e) {
            // 1
            checkNotNull(e);
            // 2
            final ConcurrentLinkedQueue.Node<E> newNode = new ConcurrentLinkedQueue.Node<E>(e);
            // 3
            for (ConcurrentLinkedQueue.Node<E> t = tail, p = t;;) {
                ConcurrentLinkedQueue.Node<E> q = p.next;
                // 4
                if (q == null) {
                    // 5
                    if (p.casNext(null, newNode)) {
                        // 6
                        if (p != t) // hop two nodes at a time
                            casTail(t, newNode);  // Failure is OK.
                        return true;
                    }
                }
                else if (p == q) // 7
                    p = (t != (t = tail)) ? t : head;
                else // 8
                    p = (p != t && t != (t = tail)) ? t : q;
            }
        }*/


     /*   public E poll() {
            1
            restartFromHead:
            2
            for (;;) {
                for (ConcurrentLinkedQueue.Node<E> h = head, p = h, q;;) {
                    3
                    E item = p.item;
                    4
                    if (item != null && p.casItem(item, null)) {
                        5
                        if (p != h) // hop two nodes at a time
                            updateHead(h, ((q = p.next) != null) ? q : p);
                        return item;
                    }
                    6
                    else if ((q = p.next) == null) {
                        updateHead(h, p);
                        return null;
                    }
                    7
                    else if (p == q)
                        continue restartFromHead;
                    else 8
                        p = q;
                }
            }
        }
        final void updateHead(Node<E> h, Node<E> p) {
        if (h != p && casHead(h, p))
            h.lazySetNext(h);
    }
        */
    }
}
