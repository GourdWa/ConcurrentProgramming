# Java并发编程之美

## 第6章

### 6.1 LockSupport工具类

LockSupport工具类是rt.jar包下的，它的主要作用是挂起和唤醒线程，该工具类是创建锁和其他同步类的基础

**LockSupport类与每个使用它的线程都会关联一个许可证，在默认情况下调用LockSupport工具类的方法的线程是不持有许可证的**，其底层是使用Unsafe类实现的

#### 1）、park()方法

如果调用线程已经拿到关联许可证，则线程直接返回，否在调用线程会被阻塞挂起；**只有当其他线程调用unpark(Thread t)方法，将当前线程作为参数传入时，被阻塞的线程会返回**。另外，如果其他线程调用了阻塞线程的interrupt方法，设置了中断标志或者被虚假唤醒，阻塞线程也会返回，所以在调用park方法时最好使用循环判断。值得注意的是，**调用park方法被阻塞的线程如果被其他线程中断返回时不会抛出中断异常**

另外，park方法还有一个重载方法：**park(Object blocker)**，Thread类中有一个变量`volatile Object parkBlocker;`变量，用来记录传递过来的Object，**当线程被阻塞挂起时，这个blocker会被记录到线程内部**，一般我们使用带参数的park方法并且将`this`指针作为参数传递进去，当线程阻塞时可以通过诊断工具参看被阻塞的原因

#### 2）、unpark(Thread thread)方法

当一个线程调用unpark时，**如果参数thread没有持有thread与LockSupport类关联的许可证，则让thread线程持有并马上返回；如果thread线程之前被park方法阻塞挂起，则调用该方法后，thread线程会被唤醒**

#### 3）、parkNanos(long nanos)方法

与park方法类似，不过该方法在线程挂起nanos时间后，如果线程仍然没有取得许可证，方法会自动返回

### 6.2 抽象同步队列AQS概述

#### 6.2.1 AQS

**AbstractQueuedSynchronizer**简称AQS，它是实现同步器的基础，并发包中所的底层就是使用AQS实现的

##### 1）、Node内部类

AQS是一个FIFO的双向队列，通过Node类型的head和tail来记录队首元素和队尾元素，其中**Node是其内部类**。Node类中的thread字段用来保存存入AQS队列的节点，其中**waitStatus变量**用来表示当前线程的状态

```java
static final class Node {
	//标记当前线程是获取共享资源被阻塞挂起存放进AQS队列
    static final Node SHARED = new Node();
	//标记线程是获取独占资源时被阻塞挂起并放入AQS队列的
    static final Node EXCLUSIVE = null;
	//表示线程被取消了
    static final int CANCELLED =  1;
	//表示线程需要被唤醒
    static final int SIGNAL    = -1;
	//线程正在条件队列里等待
    static final int CONDITION = -2;
 	//释放共享资源时需要通知其他节点
    static final int PROPAGATE = -3;
    //记录线程的等待状态，也就是上面四种
    volatile int waitStatus;
    ...
    //
	Node() {    // Used to establish initial head or SHARED marker
    }
	//
    Node(Thread thread, Node mode) {     // Used by addWaiter
        this.nextWaiter = mode;
        this.thread = thread;
    }
	//
    Node(Thread thread, int waitStatus) { // Used by Condition
        this.waitStatus = waitStatus;
        this.thread = thread;
    }
}
```

##### 2）、CondtionObject内部类

**CondtionObject**是AQS的内部类，用来结合锁实现线程的同步，其实现了Condition接口，是一个条件变量，每个条件变量对应了一个条件队列，用来存放嗲用await方法被阻塞的线程

##### 3）、state变量

在AQS中维持了一个单一状态信息**state**，通过getState、setState和compareAndSetState函数来修改其值。**对于ReentrantLock的实现来说，state表示当前线程获取锁的可重入次数；对于读写锁来说，state高16位表示读锁的状态（也就是获取读锁的次数），低16位表示写锁的可重入次数；对于CountDownLatch来说，state表示计数器当前的值；对于semaphore来说，state表示当前可用信号数**

**state变量是线程同步的关键**，操作state的方式分为**独占式和共享式**

在独占式方式下获取到资源时，资源与具体的线程绑定，也就是如果一个线程获取到资源后，会标记这个线程获取到了，其他线程再次尝试操作state变量获取时会发现当前资源已经被其他线程持有，会被阻塞挂起；在共享方式下获取资源时，当一个线程获取到资源后，另外的线程再去获取时，如果当前资源满足要求只需要通过简单的CAS操作获取即可

###### 独占方式获取与释放资源的流程

* 获取独占资源

当一个线程调用acquire方法获取独占资源时，会尝试使用tryAcquire方法获取资源（注意，**tryAcquire方法是子类实现的，AQS内部并未实现**），如果获取成功则直接返回；**否在将当前线程封装为Node.EXCLUSIVE的Node节点后插入AQS阻塞队列为尾部，并调用LockSupport.park(this)方法挂起**

```java
public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }
//acquireQueued方法
private Node addWaiter(Node mode) {
    //将当前线程包装成Node.EXCLUSIVE模式的Node节点
    Node node = new Node(Thread.currentThread(), mode);
    Node pred = tail;
    //如果有尾节点
    if (pred != null) {
        node.prev = pred;
        //将当前插入的节点设置为尾节点，并返回当前节点
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    //如果尾节点为null，则调用enq方法将当前队列插入AQS阻塞队列
    enq(node);
    return node;
}
//enq方法，画图理解
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) { //第一次进来tail肯定是null
            //创建一个哨兵节点作为头节点同时让tail也指向头节点
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            //如果tail不为空，则将新插入的节点的前驱节点指向尾节点，同时将新节点设置为尾节点并将之前的尾节点的后继节点设置为新节点
            //返回旧的尾节点（如果是第一次插入返回的其实是哨兵节点）
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
//acquireQueued方法
final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                //阻塞当前线程
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
```

* 释放独占资源

首先调用tryRelease方法，这个方法也是由子类实现，同样也是设置state的值。如果释放成功则调用**LockSupport.unpark方法激活AQS队列中一个被阻塞的线程**

```java
public final boolean release(int arg) {
    if (tryRelease(arg)) {
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```

因为AQS内部并未实现tryAcquire和tryRelease方法，具体的实现是子类完成的。以ReentrantLock独占锁为例，定义state为0时表示锁为空闲状态，为1表示锁被占用。在重写tryAcquire方法时，在内部使用CAS算法查看当前state的值，如果为0则设置为1，并设置当前线程为持有线程并返回true；否在返回false

在实现tryRelease方法时，内部借助CAS操作将state的值从1修改为0，并设置当前持有线程为null，并返回true；否在返回false

###### 共享方式获取与释放资源的流程

* 获取共享资源

调用acquireShared方法获取共享资源时会首先调用tryAcquireShared方法，该方法也是由子类实现，具体也是设置state的值。如果设置成功则返回，否在将当前线程包装成Node.SHARED的Node节点放入阻塞队列并挂起（和独占模式类似）

```java
public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)
        doAcquireShared(arg);
}
```

* 释放共享资源

也会调用子类实现的tryReleaseShared方法，如果成功则激活AQS阻塞队列的一个线程

```java
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}
```

比如读写锁在实现tryAcquireShared方法时首先看写锁是否被其他线程持有，如果是，直接返回false；否则使用CAS操作递增state的高16位（也就是读锁）；在调用tryReleaseShared方法时，读锁会将高16位减1并返回true

#### 6.2.2 AQS条件变量的支持

条件变量的signal和await方法类似notify和wait方法，配合锁来实现线程之间的同步。**synchronized同时只能与一个共享变量的wait和signal，而AQS的一个锁可以对应多个条件变量**；与wait和notify方法相同的是，await方法和signal方法在调用之前也要获得对应的锁

在调用ReentrantLock的newContion方法时，实质是返回了AQS队列中的内部类**ConditionObject的实例**，一个Lock可以创建多个条件变量。ConditionObject是AQS的内部类，所以可以直接访问其内部变量和方法，例如state。**在每个条件变量的内部都维护了一个条件队列，用来存放因调用await方法被阻塞的线程**

##### 1）、await方法

```java
public final void await() throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    //将当前执行线程包装成Node实例并添加进条件队列并返回
    Node node = addConditionWaiter();
    //释放锁，里面也是调用的release方法实现的
    int savedState = fullyRelease(node);
    int interruptMode = 0;
    //如果包装成功
    while (!isOnSyncQueue(node)) {
        //调用LockSupport方法将该线程挂起
        LockSupport.park(this);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    if (node.nextWaiter != null) // clean up if cancelled
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}
//addConditionWaiter
private Node addConditionWaiter() {
    Node t = lastWaiter;
    // If lastWaiter is cancelled, clean out.
    //去除不是CONDITION类型的节点
    if (t != null && t.waitStatus != Node.CONDITION) {
        unlinkCancelledWaiters();
        t = lastWaiter;
    }
    //将当前执行线程包装成Node节点，并放入条件队列
    Node node = new Node(Thread.currentThread(), Node.CONDITION);
    if (t == null)
        firstWaiter = node;
    else
        t.nextWaiter = node;
    lastWaiter = node;
    return node;
}
```

##### 2）、小结

当多个线程同时调用lock.lock()方法获取锁时，只有一个线程能获取到锁，其他线程会被转换为Node节点插入到AQS阻塞队列中，并自旋尝试获取锁；如果获取到锁的线程调用了对应条件变量的await方法，则该线程会释放锁，并转换为Node节点插入到条件变量对应的条件队列中，此时AQS队列中的一个线程会获取到被释放的锁，如果该线程也调用await方法，则该线程也会释放锁被放入条件变量；当另外的线程调用条件变量的signal或者signalAll方法时，会把条件队列中的一个或全部Node节点移动到AQS队列中，等待时机获取锁

**一个锁对应一个AQS队列，对应多个条件变量，每个条件变量对应一个条件队列**

### 6.3 独占锁ReentrantLock的原理

#### 6.3.1 类图结构

ReentrantLock是可重入的独占锁，同时只能有一个线程可以获取锁其他线程获取锁会被阻塞放入AQS阻塞队列

```java
//默认构造器创建一个非公平锁
public ReentrantLock() {
    sync = new NonfairSync();
}

public ReentrantLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
}
```

**Sync类继承自AQS，其子类NonfairSync和FairSync对应了公平锁与非公平锁的实现策略**。在ReentrantLock中，**AQS的state字段表示获取锁的可重入次数**，默认情况下为0，表示当前没有任何线程持有该锁。

#### 6.3.2 获取锁

##### 1）、void lock方法

当调用独占锁的lock方法时，如果当前没有其他线程占有并且当前线程之前没有获得锁，则当前线程会获取到锁，然后设置锁的拥有线程为当前线程，并且将state的值设置为1；如果当前线程已经持有该锁并再次获取锁，则将state加1；如果该锁已经被其他线程持有，则将该线程加入阻塞队列

```java
public void lock() {
    //实质是调用sync字段的lock方法
    sync.lock();
}
```

* **非公平锁**

首先假设锁为非公平锁，此时调用RenentrantLock的lock方法时，实质是委托给了NonfairSync的lock方法

```java
final void lock() {
    //首先利用CAS操作将state置为1
    if (compareAndSetState(0, 1))
        //如果成功，将当前线程设为锁的排他持有线程
        setExclusiveOwnerThread(Thread.currentThread());
    else
        //否在调用AQS的acquire方法
        acquire(1);
}
//AQS的acquire方法
public final void acquire(int arg) {
    //tryAcquire方法在AQS类中并未实现，其委托给子类实现，因此，可以在NofairSync中查看
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))//如果tryAcquire方法返回false，则将当前线程包装成Node节点放入AQS阻塞队列，并调用LockSupport挂起线程
            selfInterrupt();
    }
```

因此，现在重点来到NofairSync的tryAcquire方法

```java
protected final boolean tryAcquire(int acquires) {
    //传入的参数为1
    return nonfairTryAcquire(acquires);
}
final boolean nonfairTryAcquire(int acquires) {
    //获取当前线程和state的值
    final Thread current = Thread.currentThread();
    int c = getState();
    //如果state的值为0，说明没有线程持有该锁
    if (c == 0) {
        //尝试利用CAS操作将state的值变为1，并将当前线程设置为锁的持有线程
        if (compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    //如果state不等于0，判断当前线程是否是锁的持有线程
    else if (current == getExclusiveOwnerThread()) {
        //如果是，将state的值+1
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    //否在返回false
    return false;
}
```

* **公平锁**

公平锁与非公平锁的差别在于各自实现的tryAcquire方法

```java
protected final boolean tryAcquire(int acquires) {
    //获取当前线程和state的值
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        //这里是与非公平锁的不同之处，对于非公平锁而言，如果state的值为0，只需要将state设置为1并设置当前线程为持有线程即可
        //但是对于公平锁，会先调用hasQueuedPredecessors方法，这个方法的目的是判断AQS队列中是否已经有阻塞线程了，如果有，则当前线程则不能持有该锁
        //遵循先来先得的公平策略
        if (!hasQueuedPredecessors() &&
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0)
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
//hasQueuedPredecessors方法
public final boolean hasQueuedPredecessors() {
	//这个方法要结合AQS的enq方法一起看
    Node t = tail; // Read fields in reverse initialization order
    Node h = head;
    Node s;
    //如果h==t，说明队列为空，返回false
    //如果h!=t，但是h的next为null，说明刚好执行了enq的步骤2，但是还未执行tail=head。也就是有一个元素将作为第一个节点入队
    //如果h!=t，h.next!=null，说明现在队列中有节点，判断该节点是否是当前申请锁的线程，如果不是返回true
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}
private Node enq(final Node node) {
    for (;;) {
        Node t = tail; //1
        if (t == null) { // Must initialize
            if (compareAndSetHead(new Node()))//2
                tail = head;
        } else {
            node.prev = t;//3
            if (compareAndSetTail(t, node)) {//4
                t.next = node;
                return t;
            }
        }
    }
}
```

##### 2）、 void lockInterruptibly方法

该方法可以对中断进行响应，也就是当前线程在调用该方法时，如果其他线程调用了当前线程的interrupt方法，则当前线程会抛出异常

```java
public void lockInterruptibly() throws InterruptedException {
sync.acquireInterruptibly(1);
}
public final void acquireInterruptibly(int arg)
    throws InterruptedException {
    	//如果被中断，则抛出异常
        if (Thread.interrupted())
       		throw new InterruptedException();
    	//否在调用AQS可被中断的方法
        if (!tryAcquire(arg))
        	doAcquireInterruptibly(arg);
}
```

##### 3）、boolean tryLock()方法

该方法不会阻塞当前线程，如果获取到了锁则返回true，否在返回false

```java
public boolean tryLock() {
    //和非公平锁获取的思路类似，不过其并未阻塞挂起线程
    return sync.nonfairTryAcquire(1);
}
final boolean nonfairTryAcquire(int acquires) {
    //获取当前线程和state的值
    final Thread current = Thread.currentThread();
    int c = getState();
    //如果state的值为0，说明没有线程持有该锁
    if (c == 0) {
        //尝试利用CAS操作将state的值变为1，并将当前线程设置为锁的持有线程
        if (compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    //如果state不等于0，判断当前线程是否是锁的持有线程
    else if (current == getExclusiveOwnerThread()) {
        //如果是，将state的值+1
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    //否在返回false
    return false;
}
```

#### 6.3.3 释放锁

##### 1）、void unlock()方法

调用ReentrantLock实例的unlock方法实质也是调用的AQS中的release方法，通过release方法再调用其实现类的tryRelease方法

```java
public void unlock() {
    //实质是调用AQS的release方法
    sync.release(1);
}
//AQS的release方法
public final boolean release(int arg) {
    //再调用其实现类的tryRelease方法
    if (tryRelease(arg)) {
        //如果返回true，唤醒阻塞队列中的一个线程
        Node h = head;
        if (h != null && h.waitStatus != 0)
        unparkSuccessor(h);
        return true;
    }
    return false;
}
```

子类Sync实现的tryRelease方法如下，不管是公平实现还是非公平实现都是使用这个函数

```java
protected final boolean tryRelease(int releases) {
    int c = getState() - releases;
    //如果不是锁的持有线程则抛出异常
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    boolean free = false;
    //查看state减1之后是否为0，如果是，则说明释放了锁
    if (c == 0) {
        free = true;
        setExclusiveOwnerThread(null);
    }
    setState(c);
    return free;
}
```

#### 6.3.4 小结

ReentrantLock底层是使用AQS实现的可重入锁，内部类Sync继承自AQS，再通过公平策略FairSync和非公平策略NoFairSync继承Sync实现了锁的获取与释放。在这里，AQS的状态值state为0表示当前锁是空闲状态，大于等于1表示当前锁被占用

### 6.4 读写锁

#### 6.4.1 类图结构

读写锁ReentrantReadWriteLock内部维护了一个ReadLock和WriteLock（内部类），**这两个内部类都有一个sycn字段，这个字段是读写锁内部类Sync类型**（和独占锁很相似）。Sync继承自AQS，同样也提供了公平和非公平的实现。接下来的分析只针对非公平的读写锁，公平的读写锁的差别并不大可以做同样的分析

**在AQS中，state表征了锁的状态。在读写锁的实现中，巧妙的利用state的高16位作为读锁的状态，也就是获取到读锁的次数；低16位作为写锁的状态，也就是获取到写锁线程的可重入次数**。其内部实现也是借助Sync内部类实现

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
    private static final long serialVersionUID = 6317671515068378041L;
	//偏移量
    static final int SHARED_SHIFT   = 16;
    //读锁的状态单位值65536
    static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
    //共享锁线程最大个数65535
    static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
    //排他锁掩码，15个1
    static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;
    /** Returns the number of shared holds represented in count  */
    //返回读锁的线程个数
    static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
    /** Returns the number of exclusive holds represented in count  */
    //写锁的可重入次数
    static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }
    //记录第一个获取到读锁的线程
    private transient Thread firstReader = null;
    //记录第一个获取到读锁的线程获取读锁的可重入次数
    private transient int firstReaderHoldCount;
    //这是一个ThreadLocal类型的字段，用来存放除去第一个获取读锁线程外的其他线程获取读锁的可重入次数
    private transient ThreadLocalHoldCounter readHolds;
    static final class HoldCounter {
        int count = 0;
        // Use id, not reference, to avoid garbage retention
        final long tid = getThreadId(Thread.currentThread());
    }

    /**
         * ThreadLocal subclass. Easiest to explicitly define for sake
         * of deserialization mechanics.
         */
    static final class ThreadLocalHoldCounter
        extends ThreadLocal<HoldCounter> {
        public HoldCounter initialValue() {
            return new HoldCounter();
        }
    }
}
```

#### 6.4.2 写锁的获取与释放

##### 1）、void lock方法

```java
public void lock() {
    //也是调用AQS内部的acquire方法
    sync.acquire(1);
}
//AQS的acquire方法，最后调用子类的tryAcquire方法，这里就是Sync的tryAcquire方法
public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }
```

Sync继承自AQS，其实现了tryAcquire方法如下

```java
protected final boolean tryAcquire(int acquires) {
    /*
             * Walkthrough:
             * 1. If read count nonzero or write count nonzero
             *    and owner is a different thread, fail.
             * 2. If count would saturate, fail. (This can only
             *    happen if count is already nonzero.)
             * 3. Otherwise, this thread is eligible for lock if
             *    it is either a reentrant acquire or
             *    queue policy allows it. If so, update state
             *    and set owner.
             */
    Thread current = Thread.currentThread();
    int c = getState();
    //获取写锁的可重入次数，也就是将c与15个1相与
    int w = exclusiveCount(c);
    //如果状态值不为0
    if (c != 0) {
        // (Note: if c != 0 and w == 0 then shared count != 0)
        //c不等于0，但是w为0，说明c的第16位为0，但是高16位不为0，即读锁已经被其他线程持有了
        //如果w不为0，说明有线程获取了写锁，如果当前线程不是当前锁的持有线程则返回
        if (w == 0 || current != getExclusiveOwnerThread())
            return false;
        //到这里说明当前线程获得了写锁，判断是否达到上限，如果是则抛出异常
        if (w + exclusiveCount(acquires) > MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
        // 如果上面一切执行正常，将当前线程的可重入次数加1
        setState(c + acquires);
        return true;
    }
    //如果状态值为0，尝试将状态设置为1，并将当前线程设置为持有线程
    if (writerShouldBlock() ||
        !compareAndSetState(c, c + acquires))
        return false;
    setExclusiveOwnerThread(current);
    return true;
}
//非公平锁的writerShouldBlock总是返回false
final boolean writerShouldBlock() {
    return false; // writers can always barge
}
//公平锁的实现，其中hasQueuedPredecessors方法和独占锁中的用途是一样的，也就是看AQS队列中是否有等待线程或者是否有线程正在入队，如果有则返回true
final boolean writerShouldBlock() {
    return hasQueuedPredecessors();
}
```

##### 2）、boolean tryLock方法

这个方法其实和Sync的tryAcquire方法很类似，当写锁调用这个方法时不会阻塞，如果此时没有其他线程持有写锁或者读锁，当前线程会获取写锁成功并返回true；否在直接返回false，不阻塞

```java
public boolean tryLock( ) {
    return sync.tryWriteLock();
}
final boolean tryWriteLock() {
    Thread current = Thread.currentThread();
    int c = getState();
    if (c != 0) {
        int w = exclusiveCount(c);
        if (w == 0 || current != getExclusiveOwnerThread())
            return false;
        if (w == MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
    }
    if (!compareAndSetState(c, c + 1))
        return false;
    setExclusiveOwnerThread(current);
    return true;
}
```

##### 3）、boolean tryLock(long timeout, TimeUnit unit)方法

与tryLock不同之处在于其如果没有申请到锁会挂起等待timeout时间，当时间到了如果还没有获取到锁，直接返回false

##### 4）、void unlock()方法

实质会先调用AQS内部的release方法，在release方法内部再调用子类的tryRelease方法释放锁

```java
public void unlock() {
	sync.release(1);
}
//AQS内部的release方法
public final boolean release(int arg) {
    //调用子类的tryRelease方法，如果成功则调用LockSupport的unpark唤醒一个线程
    if (tryRelease(arg)) {
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```

Sync实现tryRelease方法

```java
protected final boolean tryRelease(int releases) {
    //判断当前线程是否是锁的持有线程，如果不是抛出异常
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    //更改写锁可重入次数的值，不同考虑读锁，因为获取写锁时读锁的值肯定为0
    int nextc = getState() - releases;
    //如果此时可重入次数为0，则释放锁
    boolean free = exclusiveCount(nextc) == 0;
    if (free)
        setExclusiveOwnerThread(null);
    setState(nextc);
    return free;
}
```

#### 6.4.3 读锁的获取与释放

##### 1）、void lock()方法

获取读锁，如果当前没有其他线程持有写锁，则当前线程可以获取读锁，AQS状态值的高16位的值会增加1，然后方法返回；否在如果其他一个线程持有写锁，则执行线程阻塞

```java
public void lock() {
    //调用读锁的lock方法内部也是通过调用AQS的acquireShared方法
    sync.acquireShared(1);
}
//AQS的acquireShared方法内部调用其子类实现的tryAcquireShared方法
public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)
        //调用AQS的doAcquireShared方法，将当前线程加入AQS阻塞队列并挂起
        doAcquireShared(arg);
}
```

Sync实现的tryAcquireShared方法，**如果当前线程获取了写锁也可以获取读锁**

```java
protected final int tryAcquireShared(int unused) {
    Thread current = Thread.currentThread();
    int c = getState();
    //获取写锁的状态值，判断是否是其他线程持有，如果是返回-1
    if (exclusiveCount(c) != 0 &&
        getExclusiveOwnerThread() != current)
        return -1;
    //返回读锁线程个数
    int r = sharedCount(c);
    //readerShouldBlock方法分析在下面，只有返回false才会进入下面的逻辑，如果其返回true说明有线程正在获取写锁
    if (!readerShouldBlock() &&
        r < MAX_COUNT && //获取读锁次数是否达到上限
        compareAndSetState(c, c + SHARED_UNIT)) {
        //如果r为0，说明是第一个获取读锁的线程
        if (r == 0) {
            //记录第一个获取读锁的线程
            firstReader = current;
            firstReaderHoldCount = 1;
        } else if (firstReader == current) {
            //如果当前线程正好是第一个获取读锁的线程则将其可重入次数加1
            firstReaderHoldCount++;
        } else {
            //最后一个获取到读锁的线程或记录其他线程读锁的可重入次数
            HoldCounter rh = cachedHoldCounter;
            if (rh == null || rh.tid != getThreadId(current))
                cachedHoldCounter = rh = readHolds.get();
            else if (rh.count == 0)
                readHolds.set(rh);
            rh.count++;
        }
        return 1;
    }
    //如果有线程正在获取写锁或者读锁达到限制或者CAS操作失败，进入这个函数，这个函数和tryAcquireShared类似，不过其实通过循环自旋获取锁
    return fullTryAcquireShared(current);
}
```



```java
final boolean readerShouldBlock() {
    return hasQueuedPredecessors();
}
public final boolean hasQueuedPredecessors() {
    // The correctness of this depends on head being initialized
    // before tail and on head.next being accurate if the current
    // thread is first in queue.
    Node t = tail; // Read fields in reverse initialization order
    Node h = head;
    Node s;
    //如果队列为空则返回false；如果AQS队列不为空，且有其他线程h.next不为空，且等于当前线程则返回false；如果不等于说明队列中已经有一个元素正在获取写锁
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}
```

##### 2）、void unlock()方法

```java
public void unlock() {
    //委托给AQS执行
    sync.releaseShared(1);
}
//AQS的releaseShared方法调用子类的tryReleaseShared方法
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}
```

Sync中实现的tryReleaseShared方法，在无限循环中获取当前AQS的状态值并将其减去65536（因为读锁是高16位），然后调用CAS更新state的值，并查看其是否为0，如果是返回true，否在返回false

```java
protected final boolean tryReleaseShared(int unused) {
    Thread current = Thread.currentThread();
    if (firstReader == current) {
        // assert firstReaderHoldCount > 0;
        if (firstReaderHoldCount == 1)
            firstReader = null;
        else
            firstReaderHoldCount--;
    } else {
        HoldCounter rh = cachedHoldCounter;
        if (rh == null || rh.tid != getThreadId(current))
            rh = readHolds.get();
        int count = rh.count;
        if (count <= 1) {
            readHolds.remove();
            if (count <= 0)
                throw unmatchedUnlockException();
        }
        --rh.count;
    }
    for (;;) {
        int c = getState();
        int nextc = c - SHARED_UNIT;
        if (compareAndSetState(c, nextc))
            // Releasing the read lock has no effect on readers,
            // but it may allow waiting writers to proceed if
            // both read and write locks are now free.
            return nextc == 0;
    }
}
```

#### 6.4.4 小结

ReentrantReadWriteLock底层也是使用AQS实现的，其读写锁功能借助内部类Sync实现（该类继承自AQS），利用state的高16位表示获取到读锁的个数，低16位表示获取写锁的线程的可重入次数，并通过CAS实现了读写分离

### 6.5 JDK8新特性StampedLock锁

**StampedLock提供了三种模式的读写控制，当获取锁时会返回一个long类型的变量，称之为stamp（戳记）**，这个戳记代表了锁的状态。其中try系列函数是获取锁的函数，获取失败会返回0的戳记。**当调用释放锁和转换锁的方法时需要传入获取锁时返回的stamp值**

StampedLock提供了如下三种模式的读写锁

#### 1、写锁writeLock

写锁是排他锁，其类似与常规读写锁的写锁，**不过这个写锁不可以重入**。请求锁成功后会返回一个stamp标记值来表示锁的版本，当释放锁时需要传入获得锁时取得的stamp值

#### 2、悲观读锁readLock

是一个共享锁，在没有线程获得写锁的时候，多个线程可以同时获取悲观读锁，类似于ReentrantReadWriteLock，不过这个锁也是不可重入的。**悲观是指在具体操作数据前会悲观的认为其他线程要对自己操作的数据做修改，所以需要先对数据加锁。**同样，在获得锁后会返回一个stamp变量表示锁的版本，释放锁时需要传入该变量

#### 3、乐观读锁tryOptimisticRead

相比较悲观读锁，乐观读锁在操作数据时不会通过CAS设置锁的状态，仅仅通过位运算测试，如果没有线程获得写锁则返回一个非0的stamp。但是由于期间没有加锁，**因此在正在操作数据前还需要调用validate方法验证stamp的有效性**



**同时，与ReentrantReadWriteLock不同的是，StampedLock支持这三种锁在一定条件下进行相互转换**

#### 小结

StampedLock提供的读写锁与ReentrantReadWriteLock类似，不过前者提供的是不可重入的锁，但是前者通过**乐观读锁在多线程多读的情况下提供了更好的性能，这是因为获取乐观读锁不需要进行CAS设置状态，只需要简单的测试状态**

---

## 第7章

JDK中提供的并发安全队列按照实现方式不同可以分为阻塞队列和非阻塞队列。阻塞队列使用锁实现，而非阻塞队列使用CAS算法实现

### 7.1 ConcurrentLinkedQueue原理探究

ConcurrentLinkedQueue是线程安全的无界非阻塞队列，**底层数据结构使用单向链表实现，对于入队和出队操作使用CAS来实现线程安全**

#### 7.1.1 类图结构

ConcurrentLinkedQueue内部的队列使用单向链表实现，其中**两个volatile类型的Node节点分别存放队首（head）和队尾（tail）节点**（Node节点是该类的静态内部类）。**默认构造函数其队首和队尾都是指向item为null的节点**。新元素被插入队尾，出队时从队首获取元素。内部使用CAS算法来保证出队和入队的原子性

```java
//静态内部类Node
private static class Node<E> {
    //item代表了这个节点的值
    volatile E item;
    //指向下一个节点
    volatile Node<E> next;

    /**
         * Constructs a new node.  Uses relaxed write because item can
         * only be seen after publication via casNext.
         */
    Node(E item) {
        UNSAFE.putObject(this, itemOffset, item);
    }
    ...
}
//默认的构造函数
public ConcurrentLinkedQueue() {
    //首位节点都是指向一个item为null的节点
    head = tail = new Node<E>(null);
}
```

#### 7.1.2  ConcurrentLinkedQueue原理介绍

##### 1）、offer操作

offer是在队列末尾添加一个元素，除非传递的参数是null会抛出异常，否在一直返回true。由于底层使用的CAS算法，因此不会阻塞挂起线程。具体的代码分析可以参见书本，关键难点在多线程同时插入时只有一个线程会成功，其他的线程会执行for循环，直到插入成功

```java
public boolean offer(E e) {
    // 1、如果是null则抛出异常
    checkNotNull(e);
    // 2、创建一个新的节点，内部调用的是UNSAFE.putObject()方法
    final ConcurrentLinkedQueue.Node<E> newNode = new ConcurrentLinkedQueue.Node<E>(e);
    // 3、在尾部插入节点
    for (ConcurrentLinkedQueue.Node<E> t = tail, p = t;;) {
        ConcurrentLinkedQueue.Node<E> q = p.next;
        // 4、在第一次插入节点时，p,t,head,tail实质都指向的是item为null的哨兵节点，此时q为null
        if (q == null) {
            // 5、将p节点的next节点与新创建的节点交换，这里CAS操作保证只有一个线程会成功插入
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
}
```

offer操作的关键步骤是上述**代码块5**，通过CAS操作来保障原子性，从而只有一个线程可以追加元素到队列末尾。进行CAS失败的线程会通过循环再次尝试插入，直到CAS成功才会返回。也就是这里是使用无限循环不断进行CAS操作来代替阻塞算法将线程挂起。相比阻塞线程，这是使用CPU资源换取阻塞带来的开销

##### 2）、add操作

其内部仍然是调用的offer操作

##### 3）、poll操作

从队列头部获取并移除一个元素，如果队列为空则返回null。具体的源码分析参见书本

```java
public E poll() {
    	//1、goto标记
        restartFromHead:
    	//2、无限循环
        for (;;) {
            for (ConcurrentLinkedQueue.Node<E> h = head, p = h, q;;) {
          	      	//3
                    E item = p.item;
                	//4、如果p节点的item不为null，则将p节点的item置为null
                    if (item != null && p.casItem(item, null)) {
                      		//5、重新设置头节点
                            if (p != h) // hop two nodes at a time
                                updateHead(h, ((q = p.next) != null) ? q : p);
                        return item;
                    }
                	//6、如果当前队列为空则返回null
                    else if ((q = p.next) == null) {
                        updateHead(h, p);
                        return null;
                    }
                	//7
                    else if (p == q)
                        continue restartFromHead;
                else //8、例如在执行到6时，其他线程向队列添加了元素，此时p.next也就是q指向了新添加的节点，这种情况使p也指向新添加的节点
                    p = q;
            }
        }
}
final void updateHead(Node<E> h, Node<E> p) {
    if (h != p && casHead(h, p))
        //内部调用： UNSAFE.putOrderedObject(this, nextOffset, val);
        h.lazySetNext(h);
}
```

poll操作在移除一个元素时，只是简单地使用CAS操作把当前节点的item值设置为null（**步骤1**），然后通过重新设置头节点将之前的头节点从队列中移除（**步骤2**），被移除的节点会被垃圾回收器回收。如果在执行过程中发现头节点被其他线程修改了，则重新循环获取头节点（**步骤3**）

##### 4）、peek操作

peek操作是获取队列头部的元素，但是不移除，如果为空则返回null

```java
public E peek() {
    //1、goto标记
    restartFromHead:
    for (;;) {
        for (Node<E> h = head, p = h, q;;) {
            //2、
            E item = p.item;
            //3、
            if (item != null || (q = p.next) == null) {
                updateHead(h, p);
                return item;
            }
            //4、
            else if (p == q)
                continue restartFromHead;
            else
                p = q;//5、
        }
    }
}
//
final void updateHead(Node<E> h, Node<E> p) {
    if (h != p && casHead(h, p))
        //内部调用： UNSAFE.putOrderedObject(this, nextOffset, val);
        h.lazySetNext(h);
}
```

peek操作相对而言是比较简单的，相当于是poll操作的简化版本，只是获取到头元素的值后不会将其从队列中移除。另外，根据源码可知，当**第一次调用peek操作时，如果这时队列中有元素，peek操作会剔除哨兵节点，并让队列的头节点指向队列中的第一个元素**

##### 5）、size操作

计算队列元素的个数，在并发情况下这个操作的返回的节点个数并不是准确的，因为其内部并未加锁，所以在统计期间其他线程可能会对队列进行增删操作

```java
public int size() {
    int count = 0;
    for (Node<E> p = first(); p != null; p = succ(p))
        if (p.item != null)
            // Collection.size() spec says to max out
            if (++count == Integer.MAX_VALUE)
                break;
    return count;
}
//first()操作有点类似peek，会剔除哨兵节点
Node<E> first() {
    restartFromHead:
    for (;;) {
        for (Node<E> h = head, p = h, q;;) {
            boolean hasItem = (p.item != null);
            if (hasItem || (q = p.next) == null) {
                updateHead(h, p);
                return hasItem ? p : null;
            }
            else if (p == q)
                continue restartFromHead;
            else
                p = q;
        }
    }
}
//返回当前节点的next节点
final Node<E> succ(Node<E> p) {
    Node<E> next = p.next;
    return (p == next) ? head : next;
}
```

size操作的源码并不难，关键是first操作和succ操作。前者的思路类似peek操作，会先将哨兵节点剔除再获取真正的头节点；succ函数目的是返回当前节点的下一个节点，如果当前节点是自引入节点则返回真正的头节点

##### 6）、remove操作

如果队列中存在该元素则删除该元素，如果存在多个则删除第一个，并返回true，否在返回false。其整体逻辑相对也比较简单，如果找到则将目标节点设置为null，并且将前驱节点与next节点连接起来，被删除的节点将被垃圾回收器回收

```java
public boolean remove(Object o) {
    if (o != null) {
        Node<E> next, pred = null;
        for (Node<E> p = first(); p != null; pred = p, p = next) {
            boolean removed = false;
            E item = p.item;
            if (item != null) {
                if (!o.equals(item)) {
                    next = succ(p);
                    continue;
                }
                removed = p.casItem(item, null);
            }

            next = succ(p);
            if (pred != null && next != null) // unlink
                pred.casNext(p, next);
            if (removed)
                return true;
        }
    }
    return false;
}
```

整个操作是从头节点开始遍历并一个一个匹配

##### 7）、contains操作

判断队列中是否有指定的对象，由于遍历整个队列，其**和size操作一样也不是准确的**，有可能在调用该方法前目标对象在队列中，但是在返回前就被其他线程所删除了

```java
public boolean contains(Object o) {
    if (o == null) return false;
    for (Node<E> p = first(); p != null; p = succ(p)) {
        E item = p.item;
        if (item != null && o.equals(item))
            return true;
    }
    return false;
}
```

#### 7.1.3 小结

ConcurrentLinkedQueue的底层是使用单项链表数据结构来保存队列元素，每个元素都被包装成了一个Node节点（静态内部类，有item和next两个字段）。创建队列时，头节点和尾节点都指向一个item为null的哨兵节点。第一次执行peek或者first操作都会把head指向真正的头节点的。同时由于底层使用的CAS算法来保证原子性，因此size操作和contains操作都不是准确的。同时，**CAS操作不保障可见性，因此头节点和尾节点都被声明成volatile类型**

### 7.2 LinkedBlockingQueue原理探究

与ConcurrentLinkedQueue不一样，LinkedBlockingQueue底层是使用锁实现的同步，因此是阻塞队列

#### 7.2.1 类图结构

LinkedBlockingQueue内部也是使用单向链表实现，其静态内部类Node用来包装每个节点，其中还有**AtomicInteger类型的count用来记录队列元素的个数**。并且其**入队操作和出队操作使用的是两把不同的锁（ReentrantLock类型，分别是putLock和takeLock）**。另外，**notEmpty**是takeLock的条件变量；**notFull**是putLock的条件变量

当调用线程在LinkedBlockingQueue实例上执行take、poll等操作时需要获取takeLock锁，保证只有一个线程操作头节点；同样执行put、offer等操作时也需要获取putLock锁

```java
//容量
private final int capacity;
//存储的队列元素数量
private final AtomicInteger count = new AtomicInteger();
//头节点
transient Node<E> head;
//尾节点
private transient Node<E> last;
//获取锁，在获取元素的时候必须持有该锁
private final ReentrantLock takeLock = new ReentrantLock();
//非空条件变量，在take操作时，如果队列为空，则会阻塞线程，并放入该条件变量的条件队列
private final Condition notEmpty = takeLock.newCondition();
//放置锁，在添加元素时必须持有该锁
private final ReentrantLock putLock = new ReentrantLock();
//非满条件变量，在put操作时，如果队列已经满了，则阻塞线程并放入对应的条件队列
private final Condition notFull = putLock.newCondition();

//构造函数
public LinkedBlockingQueue() {
    //默认容量为最大，也可以自己指定容量
    this(Integer.MAX_VALUE);
}

public LinkedBlockingQueue(int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException();
    this.capacity = capacity;
    last = head = new Node<E>(null);
}

```

#### 7.2.2 LinkedBlockingQueue原理介绍

##### 1）、offer操作

向队列插入一个元素，如果队列已满则返回false，该操作是非阻塞的，区别于add操作，put操作是阻塞式的

```java
public boolean offer(E e) {
    //1、如果插入的元素是null，则抛出异常
    if (e == null) throw new NullPointerException();
    final AtomicInteger count = this.count;
    //2、如果队列已经满了，则直接返回
    if (count.get() == capacity)
        return false;
    int c = -1;
    //3、构造新的节点，并获取独占putLock
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    putLock.lock();
    try {
        //如果队列未满，则入队，这里需要再次判断的原因是在上一次判断和获取锁之间可能有其他线程进行了操作
        if (count.get() < capacity) {
            //在队列尾部插入元素，并更新队尾
            enqueue(node);
            //先获取count的旧值，再将count加1
            c = count.getAndIncrement();
            //如果此时队列没有满，则唤醒因调用put操作阻塞的线程
            if (c + 1 < capacity)
                notFull.signal();
        }
    } finally {
        putLock.unlock();
    }
    //如果c的旧值为0，说明之前队列中没有元素，可能会有take操作的线程被阻塞，尝试唤醒，并释放takeLock锁
    if (c == 0)
        //见下面，唤起线程并释放锁
        signalNotEmpty();
    return c >= 0;
}

//  signalNotEmpty()
private void signalNotEmpty() {
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
        notEmpty.signal();
    } finally {
        takeLock.unlock();
    }
}
//
private void enqueue(Node<E> node) {
    // assert putLock.isHeldByCurrentThread();
    // assert last.next == null;
    last = last.next = node;
}
```

综上可知，offer方法通过使用putLock锁保证了在队尾插入元素的原子性和可见性

##### 2）、put操作

put操作也是向队尾插入元素，不过，**如果队列已满则会将线程挂起并放入notFull条件变量的队列中**，这点与offer操作不同

```java
public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    int c = -1;
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();
    try {
        //如果队列已满，则挂起线程，使用while的原因是避免虚假唤醒
        while (count.get() == capacity) {
            notFull.await();
        }
        enqueue(node);
        c = count.getAndIncrement();
        if (c + 1 < capacity)
            notFull.signal();
    } finally {
        putLock.unlock();
    }
    if (c == 0)
        signalNotEmpty();
}
```

put操作的整体思路与offer操作一致，差别是put操作在队列满的时候会将线程挂起，直到能够将元素放入队列

##### 3）、poll操作

从队列头部获取并移除一个元素，该方法不会阻塞线程，如果队列为空则返回null；相比于take操作会阻塞获取线程

```java
public E poll() {
    final AtomicInteger count = this.count;
    //如果队列中元素个数为0则直接返回null
    if (count.get() == 0)
        return null;
    E x = null;
    int c = -1;
    //获取take锁
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
        //同理需要再次判断，避免获取锁期间有其他线程进行了操作
        if (count.get() > 0) {
            x = dequeue();
            c = count.getAndDecrement();
            //如果队列在删除之前不止一个元素，则唤醒在notEmpty条件变量中的线程
            if (c > 1)
                notEmpty.signal();
        }
    } finally {
        takeLock.unlock();
    }
    //如果poll操作之前队列是满队，poll之后唤醒在put上等待的线程
    if (c == capacity)
        signalNotFull();
    return x;
}
//获取头节点并移除头节点
private E dequeue() {
    // assert takeLock.isHeldByCurrentThread();
    // assert head.item == null;
    Node<E> h = head;
    //这里处理了哨兵节点，first才是真正的第一个节点
    Node<E> first = h.next;
    h.next = h; // help GC
    head = first;
    E x = first.item;
    first.item = null;
    return x;
}
```

##### 4）、peek操作

获取头部元素，但是不删除，该方法也是非阻塞的。源码比较简单，如下

```java
public E peek() {
    if (count.get() == 0)
        return null;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
        Node<E> first = head.next;
        if (first == null)
            return null;
        else
            return first.item;
    } finally {
        takeLock.unlock();
    }
}
```

##### 5）、take操作

该方法和poll方法很类似，不过take方法是阻塞的，如果队列中没有元素，执行该方法的线程会被阻塞放入notEmpty条件变量的条件队列中

```java
public E take() throws InterruptedException {
    E x;
    int c = -1;
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
        //如果当前队列为空，则阻塞该线程，直到队列不空
        while (count.get() == 0) {
            notEmpty.await();
        }
        x = dequeue();
        c = count.getAndDecrement();
        if (c > 1)
            notEmpty.signal();
    } finally {
        takeLock.unlock();
    }
    if (c == capacity)
        signalNotFull();
    return x;
}
```

##### 6）、remove操作

删除指定元素，如果没有则返回false。从头节点开始遍历，如果匹配则删除并返回true。另外这里值得注意的时，**在获取多把锁和释放多把锁的时候其顺序是相反的**

```java
public boolean remove(Object o) {
    if (o == null) return false;
    //在删除前或将takeLock和putLock锁都获取到，避免有其他线程进行增删操作
    fullyLock();
    try {
        for (Node<E> trail = head, p = trail.next;
             p != null;
             trail = p, p = p.next) {
            //如果匹配成功
            if (o.equals(p.item)) {
                unlink(p, trail);
                return true;
            }
        }
        return false;
    } finally {
        fullyUnlock();
    }
}

//    
void unlink(Node<E> p, Node<E> trail) {
    p.item = null;
    trail.next = p.next;
    if (last == p)
        last = trail;
    //如果队列满，在删除后要唤醒等待put的线程
    if (count.getAndDecrement() == capacity)
        notFull.signal();
}
```

##### 7）、size操作

获取当前队列中元素的个数

```java
public int size() {
    return count.get();
}
```

注意这里和ConcurrentLinkedQueue的区别，在LinkedBlockingQueue中保存队列元素个数的count是原子变量，直接返回即可得到队列中元素的个数。但是在ConcurrentLinkedQueue中因为使用的是CAS算法，无法保证整个入队和出队过程的原子性，无法使用原子变量，因此需要逐个遍历统计

#### 7.2.3 小结

LinkedBlockingQueue的内部是使用单向链表实现的，使用头、尾节点来进行入队和出队操作，在进行入队和出队操作是借助了putLock和takeLock两把锁保证操作的原子性。另外对头、尾节点的独占锁都配备了一个条件队列，用来存放被阻塞的线程，结合put和take操作形成了一个生产消费模型

### 7.3 ArrayBlockingQueue原理探究

LinkedBlockingQueue使用有界链表来实现的阻塞队列，而ArrayBlockingQueue使用有界数组来实现

#### 7.3.1 类图结构

因为使用的是数组实现元素的存储，因此ArrayBlockingQueue内部有一个Object类型的数组**items**，其用来存访队列元素；其中还有两个整形变量**putIndex**和**takeIndex**用来表示入队元素的下标和出队下标，count用来统计队列元素的个数，引入一个独占锁lock保证出队和入队的原子性。值得注意的是这些变量都没有使用volatile修饰，因为访问这些变量都是在临界区进行的，保证了变量的内存可见性。另外notEmpty和notFull是独占锁lock的条件变量

因为是有界队列，因此在构造实例时必须传入队列的容量大小

```java
public ArrayBlockingQueue(int capacity, boolean fair) {
    if (capacity <= 0)
        throw new IllegalArgumentException();
    this.items = new Object[capacity];
    lock = new ReentrantLock(fair);
    notEmpty = lock.newCondition();
    notFull =  lock.newCondition();
}
```

#### 7.3.2 ArrayBlockingQueue原理介绍

##### 1）、offer操作

向队列尾部插入一个元素，该方法不是阻塞的，如果队列满则返回false，否在返回true

```java
public boolean offer(E e) {
    //检查元素是否为null
    checkNotNull(e);
    //获得独占锁并锁住
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        //队列满，则直接返回false
        if (count == items.length)
            return false;
        else {
            //队列未满则入队，并且唤醒因notEmpty阻塞的线程
            enqueue(e);
            return true;
        }
    } finally {
        lock.unlock();
    }
}
//检查元素是否为null
private static void checkNotNull(Object v) {
    if (v == null)//如果v为null则抛出空指针异常
        throw new NullPointerException();
}
//入队操作
private void enqueue(E x) {
	//获取目标数组并把当前元素存入数组
    final Object[] items = this.items;
    items[putIndex] = x;
    //计算下一个元素存放的位置（有循环数组那味）
    if (++putIndex == items.length)
        putIndex = 0;
    //元素个数加1
    count++;
    //唤醒阻塞在notEmpty条件队列中的一个线程
    notEmpty.signal();
}
```

可见整个添加元素的操作都是在临界区完成的，保障了count、putIndex和目标数组的可见性，也使得整个操作的原子性得到保障

##### 2）、put操作

put操作与offer操作类似，不过前者是阻塞式的，后者是非阻塞的

```java
public void put(E e) throws InterruptedException {
    checkNotNull(e);
    final ReentrantLock lock = this.lock;
    //获得锁，并且对中断响应
    lock.lockInterruptibly();
    try {
        //如果队列满，则将该线程放入notFull的条件队列并释放锁
        //使用while的原因是避免虚假唤醒
        while (count == items.length)
            notFull.await();
        //入队操作和offer的入队操作一样
        enqueue(e);
    } finally {
        lock.unlock();
    }
}
```

##### 3）、poll操作

poll操作是从队列头部获取并移除一个元素，如果队列为空，该方法不会阻塞，与take操作不同，take操作会阻塞线程

```java
public E poll() {
    //获取独占锁
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        //如果队列为空则直接返回null；否在移除对头元素并返回
        return (count == 0) ? null : dequeue();
    } finally {
        lock.unlock();
    }
}
//获取并移除对头元素
private E dequeue() {
    //获取对头元素并将对头置为null
    final Object[] items = this.items;
    @SuppressWarnings("unchecked")
    E x = (E) items[takeIndex];
    items[takeIndex] = null;
    //计算下一个对头的位置
    if (++takeIndex == items.length)
        takeIndex = 0;
    count--;
    if (itrs != null)
        itrs.elementDequeued();
    //激活因队列满而被阻塞的线程
    notFull.signal();
    return x;
}
```

##### 4）、take操作

take操作的思想与poll操作类似，不过前者会阻塞线程

```java
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        //如果队列为空，则将线程，挂起并释放锁
        while (count == 0)
            notEmpty.await();
        //获取并移除对头元素
        return dequeue();
    } finally {
        lock.unlock();
    }
}
```

##### 5）、peek操作

获取头部元素但是不移除，如果队列为空则返回null，该方法不会阻塞线程

```java
public E peek() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return itemAt(takeIndex); // null when queue is empty
    } finally {
        lock.unlock();
    }
}
//获取指定元素
final E itemAt(int i) {
    return (E) items[i];
}
```

##### 6）、size操作

获取队列中元素的个数，加锁保障了获取的变量都从主内存中读取，保障了可见性

```java
public int size() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return count;
    } finally {
        lock.unlock();
    }
}
```

#### 7.3.3 小结

ArrayBlockingQueue通过使用全局独占锁实现同时只能有一个线程进行入队或者出队操作，这个锁的粒度比较大，有点类似与在方法上添加synchronized关键字。相比于LinkedBlockingQueue的size操作更精准，因为计算前加了全局锁

### 7.4 PriorityBlockingQueue原理探究

#### 7.4.1 介绍

PriorityBlockingQueue是带有优先级的无解阻塞队列，每次出队都返回优先级最高或者最低的元素，内部借助了平衡二叉树堆实现，所以直接变量元素不保证有序。默认使用对象的compareTo方法提供比较规则，也可以自定义comparators

#### 7.4.2 PriorityBlockingQueue类图结构

PriorityBlockingQueue内部使用一个Object类型的数组queue存放队列元素，整形的size用来存放元素的个数。**allocationSpinLock是个自旋锁，被volatile修饰，其使用CAS操作保证同时只有一个线程可以进行扩容，状态为0或者1，其中0表示当前没有线程进行扩容，1表示正在进行扩容**

默认比较器是comparator，也就是元素必须实现Comparable接口；独占锁保证了同一时刻只能有一个线程可以入队或出队；notEmpty是独占锁的条件变量（这里没有notFull的原因是这是一个无界的队列）

```java
//默认队列的大小为11
private static final int DEFAULT_INITIAL_CAPACITY = 11;
public PriorityBlockingQueue() {
    //默认比较器是null，也就是使用元素的比较器
    this(DEFAULT_INITIAL_CAPACITY, null);
}
public PriorityBlockingQueue(int initialCapacity,
                             Comparator<? super E> comparator) {
    if (initialCapacity < 1)
        throw new IllegalArgumentException();
    this.lock = new ReentrantLock();
    this.notEmpty = lock.newCondition();
    this.comparator = comparator;
    this.queue = new Object[initialCapacity];
}
```

#### 7.4.3 原理介绍

##### 1）、offer操作

offer操作向队列插入一个元素，由于是无解队列的原因，因此总是返回true

```java
public boolean offer(E e) {
    if (e == null)
        throw new NullPointerException();
    //获取独占锁
    final ReentrantLock lock = this.lock;
    lock.lock();
    int n, cap;
    Object[] array;
    //如果当前元素的个数大于等于队列的长度，则进行扩容
    while ((n = size) >= (cap = (array = queue).length))
        tryGrow(array, cap);
    try {
        Comparator<? super E> cmp = comparator;
        //默认情况，比较器为null
        if (cmp == null)
            siftUpComparable(n, e, array);
        else
            //自定义比较器
            siftUpUsingComparator(n, e, array, cmp);
        //将队列元素个数加1，并且激活一个因take操作阻塞的线程
        size = n + 1;
        notEmpty.signal();
    } finally {
        lock.unlock();
    }
    return true;
}
//扩容代码
private void tryGrow(Object[] array, int oldCap) {
    //释放之前获取到的锁
    //这里释放锁的原因是扩容数组需要花费比较多的时间，在扩容期间让出锁可以让其他线程进行出队和入队操作。当然也可以不释放锁，不过这减小了并发性
    lock.unlock(); 
    Object[] newArray = null;
    //如果没有线程正在进行扩容，并且CAS操作成功
    if (allocationSpinLock == 0 &&
        UNSAFE.compareAndSwapInt(this, allocationSpinLockOffset,
                                 0, 1)) {
        try {
            //如果原来的队列长度小于64则将容量+2+oldCap；否在进行扩容50%；且最大为MAX_ARRAY_SIZE，也就是Integer.MAX_VALUE - 8
            int newCap = oldCap + ((oldCap < 64) ?
                                   (oldCap + 2) :
                                   (oldCap >> 1));
            if (newCap - MAX_ARRAY_SIZE > 0) {    // possible overflow
                int minCap = oldCap + 1;
                if (minCap < 0 || minCap > MAX_ARRAY_SIZE)
                    throw new OutOfMemoryError();
                //最大为MAX_ARRAY_SIZE，也就是Integer.MAX_VALUE - 8
                newCap = MAX_ARRAY_SIZE;
            }
            //如果扩容完毕，且queue没有被修改
            if (newCap > oldCap && queue == array)
                newArray = new Object[newCap];
        } finally {
            allocationSpinLock = 0;
        }
    }
    //如果没有获取到扩容的权限，则让出cpu使用权（不保证一定让出）
    if (newArray == null) // back off if another thread is allocating
        Thread.yield();
    //再次获取锁
    lock.lock();
    //如果没有完成扩容，则会返回offer操作并再次进行扩容
    if (newArray != null && queue == array) {
        queue = newArray;
        System.arraycopy(array, 0, newArray, 0, oldCap);
    }
}
//建堆算法
private static <T> void siftUpComparable(int k, T x, Object[] array) {
    Comparable<? super T> key = (Comparable<? super T>) x;
    //如果队列元素个数大于0，判断插入位置，否在直接入队
    while (k > 0) {
        //获取父节点
        int parent = (k - 1) >>> 1;
        //拿到父节点的值
        Object e = array[parent];
        //这里其实就是建小顶堆的过程，和堆排序一样的思想
        //如果插入的元素大于其父节点，直接退出插入即可
        if (key.compareTo((T) e) >= 0)
            break;
        //如果插入的节点小于父节点，则将父节点"移下来"
        array[k] = e;
        k = parent;
    }
    array[k] = key;
}
```

##### 2）、poll操作

获取内部堆的根节点元素，如果为空则返回null

```java
public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return dequeue();
    } finally {
        lock.unlock();
    }
}
//获取堆的根元素
private E dequeue() {
    int n = size - 1;
    //如果为空则返回null
    if (n < 0)
        return null;
    else {
        Object[] array = queue;
        //获取根节点元素
        E result = (E) array[0];
        //将最后一个元素获取出来并重新建堆
        E x = (E) array[n];
        array[n] = null;
        Comparator<? super E> cmp = comparator;
        if (cmp == null)
            siftDownComparable(0, x, array, n);
        else
            siftDownUsingComparator(0, x, array, n, cmp);
        size = n;
        return result;
    }
}
```

##### 3）、put操作

由于是非阻塞操作，因此内部直接调用offer操作

```java
public void put(E e) {
    offer(e); // never need to block
}
```

##### 4）、take操作

这是一个阻塞操作，如果队列为空则阻塞线程（放入notEmpty条件变量的条件队列中），其获取元素的思想与poll操作一样

```java
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    E result;
    try {
        //如果为空则将线程挂起并释放锁
        while ( (result = dequeue()) == null)
            notEmpty.await();
    } finally {
        lock.unlock();
    }
    return result;
}
```

##### 5）、size操作

整个过程都是加锁操作，因此不会有其他线程执行入队和出队操作，同时保证了size变量的可见性

```java
public int size() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return size;
    } finally {
        lock.unlock();
    }
}
```

#### 7.4.5 小结

PriorityBlockingQueue队列内部使用二叉树堆维护元素优先级，使用数组存储队列元素，不过这个数组是可以扩容的。当元素的个数大于数组的长度时会进行CAS扩容，出队时始终保持出队的是堆的根节点。

实质上PriorityBlockingQueue有点类似ArrayBlockingQueue，在内部使用一个独占锁来控制元素的入队和出队操作，不过前者只是用了一个条件变量notEmpty；后者使用了notEmpty和notFull两个条件变量，这也是因为前者可以动态扩容，其put和offer方法不会阻塞

### 7.5DelayQueue原理探究

DelayQueue并发队列是一个无界阻塞延迟队列，队列中每个元素都有个过期时间，当从队列获取元素时，**只有过期元素才会出队列。队列头元素就是最快要过期的元素**

#### 7.5.1 Delay Queue类图结构

其实DelayQueue内部使用的是优先队列来存放数据，也就是PriorityQueue，使用独占锁实现线程同步。另外**队列里的元素要实现Delayed接口**，每个元素都有一个过期时间，所以要实现获知当前元素还剩多少时间就过期，且Delay继承了Comparable接口实现了比较功能

```java
public interface Delayed extends Comparable<Delayed> {
    long getDelay(TimeUnit unit);
}

```

**available条件变量来自独占锁lock，其作用是实现线程同步，其中leader变量是Thread类型，用于减少不必要的线程等待**。当一个线程调用队列的take方法变为leader线程后，它会调用available条件变量的waitNanos(delay)方法等待delay秒，但是其他线程（follwer）会调用available的await方法进行无限期等待（这里其实是**Leader-Follwer模式的变体**）。当leader线程延迟过期后会退出take方法并通过调用available的signal方法唤醒一个follwer线程，被唤醒的follwer线程被选举为新的leader线程

```java
public class DelayQueue<E extends Delayed> extends AbstractQueue<E>
    implements BlockingQueue<E> {
	//独占锁
    private final transient ReentrantLock lock = new ReentrantLock();
    //优先队列
    private final PriorityQueue<E> q = new PriorityQueue<E>();
	//leader线程
    private Thread leader = null;
	//条件锁
    private final Condition available = lock.newCondition();

    public DelayQueue() {}

    public DelayQueue(Collection<? extends E> c) {
        this.addAll(c);
    }
    ...
}
```

#### 7.5.2 主要函数原理讲解

##### 1）、offer操作

插入元素到队列，因为是无界队列所以一致返回true，插入元素需要实现Delay接口

```java
public boolean offer(E e) {
    //获取独占锁
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        //调用有优先队列的offer方法
        q.offer(e);
        //如果插入的新元素是最先过期的，那么将leader置为null，并唤醒一个线程，例如执行take操作被阻塞的线程
        if (q.peek() == e) {
            leader = null;
            available.signal();
        }
        return true;
    } finally {
        lock.unlock();
    }
}
```

##### 2）、take操作

获取并移除队列里面延迟时间过期的元素，如果没有过期的元素则等待，这里可以看到通过available条件变量实现的等待操作，妙啊

```java
public E take() throws InterruptedException {
    //获取独占锁并对中断响应
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        for (;;) {
            //获取但不移除队首元素
            E first = q.peek();
            //如果没有，则等待挂起线程并释放锁
            if (first == null)
                available.await();
            else {
                //获取过期时间
                long delay = first.getDelay(NANOSECONDS);
                //如果过期，则将该元素移除并返回
                if (delay <= 0)
                    return q.poll();//从条件队列中移除过期元素
                //如果元素此时并没有过期
                first = null; // don't retain ref while waiting
                //如果leader不为null说明其他线程也在执行take，则将该线程放入条件队列挂起
                if (leader != null)
                    available.await();
                else {
                    //将当前线程设置为leader线程
                    Thread thisThread = Thread.currentThread();
                    leader = thisThread;
                    try {
                        //等待队首元素过期并释放锁，提高并发性（释放锁之后其他线程可以offer和take）
                        available.awaitNanos(delay);
                    } finally {
                        if (leader == thisThread)
                            leader = null;
                    }
                }
            }
        }
    } finally {
        //说明当前线程移除过期元素后，队列中又有了元素，则激活条件队列中的一个线程
        if (leader == null && q.peek() != null)
            available.signal();
        lock.unlock();
    }
}
```

##### 3）、poll操作

从延迟队列中移除过期头元素，如果没有过期元素则返回null。可见这个方法是非阻塞的

```java
public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        E first = q.peek();
        if (first == null || first.getDelay(NANOSECONDS) > 0)
            return null;
        else
            return q.poll();
    } finally {
        lock.unlock();
    }
}
```

##### 4）、size操作

实质是返回优先队列的大小，过期和没有过期的都返回

```java
public int size() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return q.size();
    } finally {
        lock.unlock();
    }
}
```

#### 7.5.4 小结

DelayQueue内部实质是利用PriorityQueue实现存放数据的，用独占锁实现了线程同步功能，存放的元素要实现Delay接口，整个设计巧妙的地方在于运用了一个available条件变量提高并发性的同时避免了线程不必要的等待

---

## 第8章

### 8.1 介绍

**线程池主要解决两个问题**

* 当执行大量异步任务的时线程池能够提供较好的性能，在不使用线程池时，每当需要执行异步任务时直接new一个线程来运行，而线程的创建和销毁的代价是比较大的；
* 线程池提供了一种资源限制和管理手段，可以限制线程的个数，动态新增线程等

**线程池工作原理**

线程池本身也是一个对象，其内部原理如下：**线程池内部可以预先创建一定数量的工作者线程，客户端代码并不需要向线程池借用线程，而是将其要执行的任务作为一个对象提交给线程池，线程池可能将这些任务缓存在队列中，而线程池内部的各个工作者线程则不断地从队列中取出任务并执行之**

在初始状态下，客户每提交一个任务线程池就创建一个工作者线程来处理该任务。随着客户端不断的提交任务，当前线程池的大小也相应的增加。**在当前线程池大小达到核心线程池大小的时候，新来的任务被存入工作队列之中**。这些缓存的任务由线程池中的所有工作者线程负责取出进行执行。线程池将任务存入工作队列的时候调用的是Blocking Queue的非阻塞方法offer(E e)，因此工作队列满不会使提交任务的客户端线程暂停。**当队列满时，线程池会继续创建新的工作者线程，直到当前线程池大小达到最大线程池大小**

![](.\images\线程池原理.png)

### 8.2 类图介绍

![](.\images\线程池类的继承实现关系.png)



ThreadPoolExecutor的成员遍历**ctl是一个Integer类型的原子遍历，用来记录线程池状态和线程池中线程个数**（类似于ReentrantReadWriteLock使用一个原子变量来保存读写锁的状态），其中**高3位表示线程池的状态，后面29位用来记录线程池个数**

```java
public ThreadPoolExecutor(int corePoolSize,//核心线程数
                          int maximumPoolSize,//最大线程数
                          long keepAliveTime,//存活时间。如果线程池中线程数量大于核心线程数量，并且处于空闲状态，则这些空闲线程最多存活的时间
                          TimeUnit unit,//时间单位
                          BlockingQueue<Runnable> workQueue,//阻塞队列类型
                          ThreadFactory threadFactory,//线程工厂，不指定则使用默认的
                          RejectedExecutionHandler handler) {//饱和策略。当线程池满的时候，如果再提交任务，任务是抛出异常还是丢弃等
    if (corePoolSize < 0 ||
        maximumPoolSize <= 0 ||
        maximumPoolSize < corePoolSize ||
        keepAliveTime < 0)
        throw new IllegalArgumentException();
    if (workQueue == null || threadFactory == null || handler == null)
        throw new NullPointerException();
    this.acc = System.getSecurityManager() == null ?
        null :
    AccessController.getContext();
    this.corePoolSize = corePoolSize;
    this.maximumPoolSize = maximumPoolSize;
    this.workQueue = workQueue;
    this.keepAliveTime = unit.toNanos(keepAliveTime);
    this.threadFactory = threadFactory;
    this.handler = handler;
}
```

在ThreadPoolExecutor中，独占锁mainlock用来控制新增**Worker**（内部类）线程操作的原子性；termination是该锁对应的条件变量

**Worker内部类继承了AQS和Runnable接口，是具体承载任务的对象**，其自己实现了简单的不可重入锁；state为0表示锁未被获取，state为1表示锁已经被获取，state为-1是创建时的默认状态。其中字段firstTask记录该工作线程执行的第一个任务，thread字段是具体执行任务的线程

```java
private final class Worker
    extends AbstractQueuedSynchronizer
    implements Runnable
{
    private static final long serialVersionUID = 6138294804551838833L;
    /** Thread this worker is running in.  Null if factory fails. */
    final Thread thread;
    /** Initial task to run.  Possibly null. */
    Runnable firstTask;
    /** Per-thread task counter */
    volatile long completedTasks;
	//只有一个构造器
    Worker(Runnable firstTask) {
        setState(-1); // inhibit interrupts until runWorker
        this.firstTask = firstTask;
        this.thread = getThreadFactory().newThread(this);
    }
    ...
}
```

#### 1）、线程池状态

* **RUNNING**：接受新任务并且处理阻塞队列里的任务
* **SHUTDOWN**：拒绝新任务但是会将正在处理的任务和阻塞队列里的任务处理完毕
* **STOP**：拒绝新任务并且抛弃阻塞队列里的任务，同时还会中断正在处理的任务
* **TIDYING**：所有任务都执行完（包括阻塞队列中的任务）后当前线程池活动线程数为0，将调用terminated方法
* **TERMINATED**：终止状态。terminated方法调用完的状态

线程池状态的转换

**RUNNING--->SHUTDOWN**：显示调用shutdown方法

**SHUTDOWN--->STOP**：显示调用shutdownNow方法

**SHUTDOWN--->TIDYING**：当线程池和任务队列为空时

**STOP--->TIDYING**：当线程池为空

**TIDYING--->TERMINATED**：当**terminated() hook方法**执行完成时

> 类比线程的六种状态
>
> * **NEW**：一个已创建而未启动的线程位于该状态
>
> * **RUNNABLE**：该状态包含两个子状态
>
>   * **READY**：表示该线程可以被线程调度器进行调度而使之处于RUNNING状态，处于READY的线程也称为活跃线程
>   * **RUNNING**：表示该线程正在运行，即对应的run方法所对应的指令正在由处理器执行
>
>   执行yield方法，其状态可能会由RUNNING转换为READY
>
> * **BLOCKED**：一个线程发起一个**阻塞式I/O操作（例如文件读写或者Socket读写）后，或者申请一个其他线程持有的资源（例如锁）**，线程就会处于BLOCKED状态。处于该状态的线程不会占用处理器资源，当线程完成I/O操作或者获得锁后又可以转换为RUNNABLE状态
>
> * **WAITING**：线程执行某些特定方法后就会处于这种等待其他线程执行另外一些特定操作的状态。例如执行Thread.join()、Object.wait()和LockSupport.park(Object)方法；对应的执行Object.notify()/notifyAll和LockSupport.unpark(Object)可以使线程由WAITING状态转换为RUNNABLE状态
>
> * **TIMED_WAITING**：该状态类似于WAITING状态，不过处于该状态的线程可以不经其它线程执行特定操作，而是处于有限时间的等待中，若时间到了自动会转换为RUNNABLE
>
> * **TERMINATED**：已经执行结束的线程处于该状态。一个线程只能一次处于该状态。Thread.run()正常返回或抛出异常而提前终止都会使线程处于该状态
>
> 

#### 2）、几种常见的线程池类型

* **newFixedThreadPool**：创建一个核心线程个数与最大线程个数相等的线程池，阻塞队列的容量是Integer.MAX_VALUE，如果线程数大于核心线程数且处于空闲状态则马上被回收

```java
public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ThreadPoolExecutor(nThreads, nThreads,
                                  0L, TimeUnit.MILLISECONDS,
                                  new LinkedBlockingQueue<Runnable>());
}
```

* **newSingleThreadExecutor**：创建核心线程和最大线程数都为1的线程池，阻塞队列的容量是Integer.MAX_VALUE。如果线程数大于核心线程数且处于空闲状态则马上被回收。可是实现并发转串行，在不超过最大容量的情况下，只有一个线程在运行

```java
public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
    return new FinalizableDelegatedExecutorService
        (new ThreadPoolExecutor(1, 1,
                                0L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>(),
                                threadFactory));
}
```

* **newCachedThreadPool**：核心线程的数量为0，线程池大小为Integer.MAX_VALUE，SynchronousQueue阻塞队列只允许有一个任务在里面，只要空闲线程超过60s就会被回收，适合频繁提交但是执行时间短的任务

```java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                  60L, TimeUnit.SECONDS,
                                  new SynchronousQueue<Runnable>());
}
```

### 8.3 源码分析

#### 1）、Worker类

Worker类继承了AQS并且实现了Runnable接口，因此**它实质是任务的包装**，因为该类只有一个构造器，因此在执行工作线程的时候最后还是会调用其run方法，也就是最后调用runWorker方法

**真正实现线程复用的亮点在Worker类的run方法中**

```java
Worker(Runnable firstTask) {
    setState(-1); // inhibit interrupts until runWorker
    this.firstTask = firstTask;
    //创建一个线程
    this.thread = getThreadFactory().newThread(this);
}
public void run() {
    runWorker(this);
}
//runWorker方法是线程池的方法
final void runWorker(Worker w) {
    //获取当前工作线程
    Thread wt = Thread.currentThread();
    //获取任务
    Runnable task = w.firstTask;
    w.firstTask = null;
    //释放锁，允许中断
    w.unlock(); // allow interrupts
    boolean completedAbruptly = true;
    try {
        //在这里实现了线程的复用，getTask方法不断的获取任务并执行
        while (task != null || (task = getTask()) != null) {
            w.lock();
            //下面实质就是调用run方法
            if ((runStateAtLeast(ctl.get(), STOP) ||
                 (Thread.interrupted() &&
                  runStateAtLeast(ctl.get(), STOP))) &&
                !wt.isInterrupted())
                wt.interrupt();
            try {
                beforeExecute(wt, task);
                Throwable thrown = null;
                try {
                    //执行任务
                    task.run();
                } catch (RuntimeException x) {
                    thrown = x; throw x;
                } catch (Error x) {
                    thrown = x; throw x;
                } catch (Throwable x) {
                    thrown = x; throw new Error(x);
                } finally {
                    afterExecute(task, thrown);
                }
            } finally {
                task = null;
                w.completedTasks++;
                w.unlock();
            }
        }
        completedAbruptly = false;
    } finally {
        //执行清理工作
        processWorkerExit(w, completedAbruptly);
    }
}

//processWorkerExit方法
private void processWorkerExit(Worker w, boolean completedAbruptly) {
    if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted
        decrementWorkerCount();

    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        //统计完成的任务数量
        completedTaskCount += w.completedTasks;
        //移除当前的Worker，也就是任务
        workers.remove(w);
    } finally {
        mainLock.unlock();
    }

    tryTerminate();

    int c = ctl.get();
    //如果当前线程个数小于核心数量则增加
    if (runStateLessThan(c, STOP)) {
        if (!completedAbruptly) {
            int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
            if (min == 0 && ! workQueue.isEmpty())
                min = 1;
            if (workerCountOf(c) >= min)
                return; // replacement not needed
        }
        //增加线程
        addWorker(null, false);
    }
}
```

#### 2）、execute方法

该方法的作用是提交任务到线程池执行。简单来说这里分为3步骤：1、如果线程数量小于核心线程数，则尝试新开一个线程执行任务；2、如果核心线程数已经够了，则将任务加到阻塞队列，如果成功，执行双重校验看是否需要开启一个新的线程；3、如果加入队列失败，则尝试新建一个非核心线程，如果还是失败则执行拒绝策略。

实质这个方法的执行思想就是线程池的思想

```java
  public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
		//获取线程池状态和线程个数
        int c = ctl.get();
      //如果线程数量小于核心线程数则开启新线程运行
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true))
                return;
            c = ctl.get();
        }
      //如果线程池正在运行并且向阻塞队列中添加任务成功
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            //再次检查线程池状态，如果没有运行了则将刚刚添加的任务移除并拒接添加
            //执行二次校验的原因是避免线程池状态变化
            if (! isRunning(recheck) && remove(command))
                reject(command);
            else if (workerCountOf(recheck) == 0)//如果线程数量为0，则开启新线程
                addWorker(null, false);
        }
        else if (!addWorker(command, false))//执行到这里，说明上面的if语句失败，或许是阻塞队列满了，因此尝试开启新线程
            reject(command);
    }

```

#### 3）、addWorker方法

这个方法是添加线程和任务的重点，实质是增加线程数并且开始执行任务

```java
private boolean addWorker(Runnable firstTask, boolean core) {
    retry:
    for (;;) {
        int c = ctl.get();
        int rs = runStateOf(c);

        /*
        1、如果当前状态为SHUTDOWN，且第提交任务不为null，则退出
        2、如果当前状态STOP、TIDYING、TERMINATED，则退出
        3、如果当前状态为SHUTDOWN，且阻塞队列为空，则退出
        其实就是线程池各个状态的策略，和上面说的几种状态的解释是一致的
        */
        if (rs >= SHUTDOWN &&
            ! (rs == SHUTDOWN &&
               firstTask == null &&
               ! workQueue.isEmpty()))
            return false;

        for (;;) {
            //循环CAS操作增加线程个数
            int wc = workerCountOf(c);
            //如果线程个数超过了最大线程数则直接返回
            if (wc >= CAPACITY ||
                wc >= (core ? corePoolSize : maximumPoolSize))
                return false;
            //如果增加线程数成功则跳出循环
            if (compareAndIncrementWorkerCount(c))
                break retry;
            //否在获取线程池状态和线程数
            c = ctl.get();  // Re-read ctl
            //查看状态和之前进来的状态是否一致，如果不一致则跳出循环重新获取状态
            if (runStateOf(c) != rs)
                continue retry;
        }
    }
	//到这里说明CAS操作是成功的了，也就是新增线程是欧克了
    boolean workerStarted = false;
    boolean workerAdded = false;
    Worker w = null;
    try {
        //来对任务包装一下
        w = new Worker(firstTask);
        final Thread t = w.thread;
        if (t != null) {
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                // Recheck while holding lock.
                // Back out on ThreadFactory failure or if
                // shut down before lock acquired.
                int rs = runStateOf(ctl.get());
				//获取到线程池状态看下是不是关闭了，如果关闭了，欧克，完犊子，释放锁，over
                if (rs < SHUTDOWN ||
                    (rs == SHUTDOWN && firstTask == null)) {
                    if (t.isAlive()) // precheck that t is startable
                        throw new IllegalThreadStateException();
                    //添加任务，添加到工作集中，一个HashSet集合
                    workers.add(w);
                    int s = workers.size();
                    if (s > largestPoolSize)
                        largestPoolSize = s;
                    workerAdded = true;
                }
            } finally {
                mainLock.unlock();
            }
            if (workerAdded) {
                //开始任务吧
                t.start();
                workerStarted = true;
            }
        }
    } finally {
        if (! workerStarted)
            addWorkerFailed(w);
    }
    return workerStarted;
}
```

#### 4）、shutdown方法

调用shutdown方法后，线程池不再接受新的任务，但是工作队列里面的任务还要继续执行。该方法调用之后就会返回，不会等待队列中的任务执行完

```java
public void shutdown() {
    final ReentrantLock mainLock = this.mainLock;
    //获取锁，shutdown过程中不允许其他线程进行操作
    mainLock.lock();
    try {
        //权限检查
        checkShutdownAccess();
        //使用CAS操作设置线程池状态为SHUTDOWN
        advanceRunState(SHUTDOWN);
        //设置线程的中断标志，中断的是空闲线程，正在运行的线程不会被中断
        interruptIdleWorkers();
        onShutdown(); // hook for ScheduledThreadPoolExecutor
    } finally {
        mainLock.unlock();
    }
    //尝试将线程状态设置为terminated
    tryTerminate();
}

//advanceRunState方法
private void advanceRunState(int targetState) {
    for (;;) {
        //获取线程池状态
        int c = ctl.get();
        //如果状态大于等于SHUTDOWN，比如后三种状态则直接返回；否在设置为SHUTDOWN
        if (runStateAtLeast(c, targetState) ||
            ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
            break;
    }
}
//interruptIdleWorkers方法
private void interruptIdleWorkers(boolean onlyOne) {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        for (Worker w : workers) {
            Thread t = w.thread;
            //如果线程没有被中断，且能获取到自己的锁（只有空闲线程能获取的锁），则中断该线程
            if (!t.isInterrupted() && w.tryLock()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                } finally {
                    w.unlock();
                }
            }
            if (onlyOne)
                break;
        }
    } finally {
        mainLock.unlock();
    }
}
//tryTerminate方法
final void tryTerminate() {
    for (;;) {
        //获取线程池状态
        int c = ctl.get();
        //如果线程池正在运行或已经是TIDYING或TERMINATED状态或者（是shutdown但是队列不为空）则直接返回
        if (isRunning(c) ||
            runStateAtLeast(c, TIDYING) ||
            (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
            return;
        if (workerCountOf(c) != 0) { // Eligible to terminate
            interruptIdleWorkers(ONLY_ONE);
            return;
        }

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            //设置状态为TIDYING
            if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                try {
                    terminated();
                } finally {
                    ctl.set(ctlOf(TERMINATED, 0));
                    termination.signalAll();
                }
                return;
            }
        } finally {
            mainLock.unlock();
        }
        // else retry on failed CAS
    }
}
```

#### 5）、shutdownNow方法

调用shutdownNow方法后，线程池不在接受新任务，同时会丢弃阻塞队列中的任务，正在执行的任务会被中断，返回值为队列中被丢弃的任务列表

```java
public List<Runnable> shutdownNow() {
    List<Runnable> tasks;
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        //检查权限
        checkShutdownAccess();
        //设置线程池状态
        advanceRunState(STOP);
        //中断线程，这里是中断所有线程
        interruptWorkers();
        //移除阻塞队列中的所有任务并返回
        tasks = drainQueue();
    } finally {
        mainLock.unlock();
    }
    tryTerminate();
    return tasks;
}
//interruptWorkers方法
private void interruptWorkers() {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        //
        for (Worker w : workers)
            w.interruptIfStarted();
    } finally {
        mainLock.unlock();
    }
}
//interruptIfStarted方法
void interruptIfStarted() {
    Thread t;
    //中断所有线程，不管是有锁还是无锁的
    if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
        try {
            t.interrupt();
        } catch (SecurityException ignore) {
        }
    }
}
//将阻塞队列中的任务全部移除，并添加到taskList中返回
private List<Runnable> drainQueue() {
    BlockingQueue<Runnable> q = workQueue;
    ArrayList<Runnable> taskList = new ArrayList<Runnable>();
    q.drainTo(taskList);
    if (!q.isEmpty()) {
        for (Runnable r : q.toArray(new Runnable[0])) {
            if (q.remove(r))
                taskList.add(r);
        }
    }
    return taskList;
}
```

### 总结

线程池设计的特别巧妙，通过内部类Worker来封装任务，这个Worker也是很神奇，继承了AQS并且实现了Runnable接口，内部还有个Thread字段。每次添加任务时就将任务用Worker进行封装，并且通过其run方法实现线程复用。除此之外，线程池内的ctl字段也是秒，其实现和读写锁有点类似，其高三位定义了线程池的5中状态，低29位拿来计算线程池中线程的个数，通过线程池的状态转变来实现并控制线程的执行

---

## 第9章

### 9.1 介绍

ScheduledThreadExecutor继承自ThreadPoolExecutor，其阻塞队列的类型类似于之前的延时队列。ScheduledThreadExecutor可以实现指定一定延时时间后或者定时进行任务调度执行的线程池

### 9.2 类图介绍

ScheduledThreadExecutor内部有两个内部类：DelayedWorkQueue，其和DelayedQueue类似，是一个延时队列；ScheduledFutureTask是具有返回值的任务，是对任务的包装

![](.\images\ScheduledFutureTask继承实现关系.png)

ScheduledFutureTask继承自FutureTask类，而这个类又是Runnable的实现类，且实现了Delay接口，能够实现比较和获得过期时间，且在ScheduledFutureTask类中重写了父类FutureTask的run方法。在FutureTask类中，用一个state变量，用来表示任务的状态

```java
private volatile int state;
//初始状态
private static final int NEW          = 0;
//正在运行
private static final int COMPLETING   = 1;
//正常运行结束状态
private static final int NORMAL       = 2;
//运行中发生异常
private static final int EXCEPTIONAL  = 3;
//任务被取消
private static final int CANCELLED    = 4;
//任务正在被中断
private static final int INTERRUPTING = 5;
//任务已经被中断
private static final int INTERRUPTED  = 6;
```

同时在ScheduledFutureTask类内部定义了一个**变量period表示任务的类型**：一次性任务、固定延时的定时可重复执行任务、固定频率的可重复执行任务

```java
/**
* Period in nanoseconds for repeating tasks.  A positive
* value indicates fixed-rate execution.  A negative value
* indicates fixed-delay execution.  A value of 0 indicates a
* non-repeating task.
*/
private final long period;
```

ScheduledThreadPoolExecutor的一个典型构造函数如下

```java
public ScheduledThreadPoolExecutor(int corePoolSize) {
    //调用Thread PoolExecutor的构造器，阻塞队列是延迟队列
    super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
          new DelayedWorkQueue());
}
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
         Executors.defaultThreadFactory(), defaultHandler);
}
```

### 9.3 原理剖析

#### 1）、schedule(Runnable command,long delay,TimeUnit unit)方法

该方法是提交一个延迟执行的任务，任务从提交时间算起延迟delay个unit后执行，提交的任务只会被执行一次

```java
public ScheduledFuture<?> schedule(Runnable command,
                                   long delay,
                                   TimeUnit unit) {
    //如果提交的任务为null或者时间单位不正确则抛出异常
    if (command == null || unit == null)
        throw new NullPointerException();
    //对任务进行包装，decorateTask方法就是直接返回new出来的ScheduledFutureTask实例
    //也就是将提交的任务包装成ScheduledFutureTask实例，最后还是会调用FutureTask对任务进行包装
    RunnableScheduledFuture<?> t = decorateTask(command,
                                                new ScheduledFutureTask<Void>(command, null,
	                                                                              triggerTime(delay, unit)));
    //添加到延迟队列，详情如下分析
    delayedExecute(t);
    return t;
}
//decorateTask方法
protected <V> RunnableScheduledFuture<V> decorateTask(
    Runnable runnable, RunnableScheduledFuture<V> task) {
    return task;
}
```

调用ScheduledFutureTask对任务进行包装的构造方法如下

```java
ScheduledFutureTask(Runnable r, V result, long ns) {
    //调用父类的构造方法，也就是FutureTask的构造器对Runnable进行包装，如下
    super(r, result);
    this.time = ns;
    //说明任务是一次性的
    this.period = 0;
    this.sequenceNumber = sequencer.getAndIncrement();
}
//FutureTask构造器
public FutureTask(Runnable runnable, V result) {
    //Executors.callable实质返回的是RunnableAdapter，这个类对实现了Callable接口，对Runnable进行了包装
        this.callable = Executors.callable(runnable, result);
    //设置当前任务的状态
        this.state = NEW;       // ensure visibility of callable
    }
```

**delayedExecute**方法分析如下

```java
private void delayedExecute(RunnableScheduledFuture<?> task) {
    //如果线程池关闭，则拒绝该任务
    if (isShutdown())
        reject(task);
    else {
        //将任务添加到阻塞队列中
        super.getQueue().add(task);
        //再次检查线程池的状态
        //如果此时线程池关闭了，且从队列中移除了队列，如果任务开始则取消任务
        if (isShutdown() &&
            !canRunInCurrentRunState(task.isPeriodic()) &&
            remove(task))
            task.cancel(false);
        else
            //如果任务添加到线程池成功，确保至少一个线程在处理任务
            ensurePrestart();
    }
}
//添加线程,主要就是添加一个线程在线程池，任务不任务的不重要
void ensurePrestart() {
    int wc = workerCountOf(ctl.get());
    if (wc < corePoolSize)
        addWorker(null, true);
    else if (wc == 0)
        addWorker(null, false);
}
```

将任务添加到队列之中之后，如果任务延迟到了就可以执行任务了，任务的执行还是调用的ScheduleFutureTask重写的父类的run方法

```java
public void run() {
    //判断任务是否是一次性的任务，如果是一次性的则返回false
    boolean periodic = isPeriodic();
    //看线程状态是否要取消任务
    if (!canRunInCurrentRunState(periodic))
        cancel(false);
    //如果是一次性任务则执行下面的语句
    else if (!periodic)
        //直接调用父类的run方法，也就是FutureTask的run方法,下面有详解
        ScheduledFutureTask.super.run();
    //如果是重复任务会来到这个if语句，runAndReset也是调用的是父类的方法，详解如下
    else if (ScheduledFutureTask.super.runAndReset()) {
        setNextRunTime();
        reExecutePeriodic(outerTask);
    }
}
```

* **一次性任务**

FutureTask的run方法如下

```java
public void run() {
    //如果任务状态不是新创建的或者设置持有线程为当前线程失败则直接返回
    if (state != NEW ||
        !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                     null, Thread.currentThread()))
        return;
    try {
        //接下来就是运行任务的逻辑
        Callable<V> c = callable;
        if (c != null && state == NEW) {
            V result;
            boolean ran;
            try {
                result = c.call();
                ran = true;
            } catch (Throwable ex) {
                result = null;
                ran = false;
                //这个方法和set方法类似，也是设置任务的状态，只不过这里会将任务设置为异常终止
                setException(ex);
            }
            //如果成功执行任务，就会调用该set方法，其实就是设置任务的状态，从NEW到COMPLETING再到NORMAL
            if (ran)
                set(result);
        }
    } finally {
        // runner must be non-null until state is settled to
        // prevent concurrent calls to run()
        runner = null;
        // state must be re-read after nulling runner to prevent
        // leaked interrupts
        int s = state;
        if (s >= INTERRUPTING)
            handlePossibleCancellationInterrupt(s);
    }
}
//set方法如下
protected void set(V v) {
    if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
        outcome = v;
        UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
        finishCompletion();
    }
}
```

#### 2）、scheduleWithFixedDelay

该方法能够在任务执行完毕之后，让任务延迟固定时间再次运行。任务会一直运行直到任务被取消或者发生异常或者关闭了线程池

```java
public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                 long initialDelay,
                                                 long delay,
                                                 TimeUnit unit) {
    if (command == null || unit == null)
        throw new NullPointerException();
    if (delay <= 0)
        throw new IllegalArgumentException();
    //注意，这里传递给ScheduledFutureTask进行任务包装的时候，传递的period是-delay，也就印证了之前说的固定延时重复任务的period为负数
    ScheduledFutureTask<Void> sft =
        new ScheduledFutureTask<Void>(command,
                                      null,
                                      triggerTime(initialDelay, unit),
                                      unit.toNanos(-delay));
    RunnableScheduledFuture<Void> t = decorateTask(command, sft);
    sft.outerTask = t;
    delayedExecute(t);
    return t;
}
```

* **重复任务**

重复任务的执行调用的是父类的runAndReset方法。其实runAndReset和run方法的基本逻辑是一样的，只不过在这里面不会设置任务的状态，这也是为了让任务能够重复执行

```java
protected boolean runAndReset() {
    if (state != NEW ||
        !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                     null, Thread.currentThread()))
        return false;
    boolean ran = false;
    int s = state;
    try {
        Callable<V> c = callable;
        if (c != null && s == NEW) {
            try {
                //运行任务
                c.call(); // don't set result
                ran = true;
            } catch (Throwable ex) {
                setException(ex);
            }
        }
    } finally {
        // runner must be non-null until state is settled to
        // prevent concurrent calls to run()
        runner = null;
        // state must be re-read after nulling runner to prevent
        // leaked interrupts
        s = state;
        if (s >= INTERRUPTING)
            handlePossibleCancellationInterrupt(s);
    }
    //注意，上面的程序不会设置任务的状态，因此，如果是正常运行的任务，其状态会一直是NEW
    return ran && s == NEW;
}
```

```java
//重复任务会设置下一次运行的时间
private void setNextRunTime() {
    long p = period;
    //如果p大于0，说明是固定频率的任务
    if (p > 0)
        time += p;
    else
        time = triggerTime(-p);
}
```

**总结**

fixed-delay类型的任务的执行原理是，当添加一个任务到延迟队列后，等待initialDelay时间，任务过期，过期的任务从队列中移除，并执行。执行完毕之后会重新设置任务的延迟时间，然后再把任务放入延迟队列。也就是**从任务结束后再延迟period后执行**

#### 3）、scheduleAtFixedRate

这个方法是按照重复频率执行任务，也就从任务开始执行后，period时间后再次执行任务；如果此时第二轮任务执行时间到了，但是第一轮任务并没有执行完毕，此时第二轮任务会延迟执行，等第一轮任务完成后才执行

```java
public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                              long initialDelay,
                                              long period,
                                              TimeUnit unit) {
    if (command == null || unit == null)
        throw new NullPointerException();
    if (period <= 0)
        throw new IllegalArgumentException();
    //注意这里传递给包装任务的period是正的
    ScheduledFutureTask<Void> sft =
        new ScheduledFutureTask<Void>(command,
                                      null,
                                      triggerTime(initialDelay, unit),
                                      unit.toNanos(period));
    RunnableScheduledFuture<Void> t = decorateTask(command, sft);
    sft.outerTask = t;
    delayedExecute(t);
    return t;
}
```

### 9.4 总结

ScheduledThreadPoolExecutor内部使用DelayQueue来存放任务，提交的任务被其内部类ScheduledFutureTask包装，根据其内部类ScheduledFutureTask的**period字段**将任务分成三类：执行一次、固定延时重复执行（任务结束后固定多少时间重新再次开始任务）和固定频率重复执行（任务开始后固定时间重新开始任务）

---

## 第10章

### 10.1 CountDownLatch

#### 10.1.1 案例介绍



```java
public class CountDownLatchDemo {
    private static CountDownLatch countDownLatch = new CountDownLatch(2);

    public static void main(String[] args) throws InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        //提交线程A
        threadPool.submit(() -> {
            System.out.println("Thread A start");
            try {
                Thread.sleep(1000);
                System.out.println("Thread A over");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                countDownLatch.countDown();
            }
        });
        //提交线程B
        threadPool.submit(() -> {
            System.out.println("Thread B start");
            try {
                Thread.sleep(1000);
                System.out.println("Thread B over");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                countDownLatch.countDown();
            }
        });
        System.out.println("Main thread start");
        countDownLatch.await();
        System.out.println("Main thread over");
        threadPool.shutdown();
    }
}
```

输出

> Thread A start
> Main thread start
> Thread B start
> Thread B over
> Thread A over
> Main thread over

**CountDownLatch和join方法的区别**

1、调用一个子线程的join方法，该线程会一直被阻塞到子线程运行完毕；而CountDownLatch则使用计数器来允许子线程运行完毕或者在运行中递减计数，也就是说CountDownLatch可以在子线程运行的任何时间调用await方法而不必等子线程运行结束

2、另外，如果我们使用线程池来管理线程（也就是上面的Demo），这种情况下就不能调用子线程的join方法，因此相比join方法，CountDownLatch更灵活

#### 10.1.2 原理探究

CountDownLatch内部是借助AQS实现的，其内部类Sync继承自AQS，这个内部类只有一个有参构造，而CountDownLatch也只有一个有参构造器，在创建CountDownLatch实例的时候会调用Sync的有参构造

```java
public CountDownLatch(int count) {
    if (count < 0) throw new IllegalArgumentException("count < 0");
    //调用内部类的有参构造
    this.sync = new Sync(count);
}
Sync(int count) {
    //设置就是设置state的值
    setState(count);
}

```

##### 1）、void await()方法

当线程调用await方法后，当前线程会被阻塞，直到下面情况之一发生时才会返回：当所有线程都调用了CountDownLatch的countDown方法后，也就是计数器的值为0时；当其他线程调用了当前线程的interrupt方法中断了当前线程，当前线程会抛出异常并返回

```java
//调用CountDownLatch实例的await方法实质调用的是AQS的acquireSharedInterruptibly方法
public void await() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}
//AQS的acquireSharedInterruptibly方法
public final void acquireSharedInterruptibly(int arg)
    throws InterruptedException {
    //如果发生了中断则抛出异常并返回
    if (Thread.interrupted())
        throw new InterruptedException();
    //否在调用子类实现的tryAcquireShared方法，也就是内部类Sync实现了该方法
    //子类实现的方法只会返回1或-1；如果返回1，说明state的值为0；否在大于0
    if (tryAcquireShared(arg) < 0)
        //说明此时state的值大于0，将当前线程包装成Node节点放进阻塞队列并挂起当前线程
        doAcquireSharedInterruptibly(arg);
}
//内部类Sync的tryAcquireShared方法
protected int tryAcquireShared(int acquires) {
    //如果为0，则返回1；否在返回-1
    return (getState() == 0) ? 1 : -1;
}
```

##### 2）、void countDown()方法

调用该方法后，计数器的值减1，此时计数器的值已经为0，则不进行递减并直接返回；如果递减后计数器的值为0，则唤醒所有因调用await方法被放进阻塞队列的线程

```java
//实质也是调用的AQS的releaseShared方法，在该方法内部有调用了其子类的tryReleaseShared方法
public void countDown() {
    sync.releaseShared(1);
}
//AQS的releaseShared方法
public final boolean releaseShared(int arg) {
    //调用内部类实现的tryReleaseShared方法
    if (tryReleaseShared(arg)) {
        //如果state的值为0，则唤醒阻塞的线程
        doReleaseShared();
        return true;
    }
    return false;
}
//内部类Sync实现的tryReleaseShared方法
protected boolean tryReleaseShared(int releases) {
    // Decrement count; signal when transition to zero
    for (;;) {
        //获取state的值，如果为0，直接返回false，避免让state为负数
        int c = getState();
        if (c == 0)
            return false;
        //否在将state减1并更新，如果更新后的state的值为0，返回true
        int nextc = c-1;
        if (compareAndSetState(c, nextc))
            return nextc == 0;
    }
}
```

##### 3）、long getCount()方法

其实就是返回AQS的state的值

```Java
public long getCount() {
	return sync.getCount();
}
int getCount() {
	return getState();
}
```

#### 10.1.3 小结

CountDownLatch相比join方法更加灵活方便。其内部也是使用AQS实现的，初始化的参数就是设置AQS的state的值，当多个线程调用countDown方法时实质就是改变state的值，当state的值为0时，就唤醒因调用await方法阻塞的线程

### 10.2 CyclicBarrier

CountDownLatch计数器是一次性的，根据之前源码的分析可知，当state的值为0之后，再调用await或countDown方法会直接返回。为了满足计数器可以被重置的需求，，引入了CyclicBarrier类

**CyclicBarrier类可以让一组线程全部达到一个状态之后再全部执行，当所有线程执行完毕之后，CyclicBarrier类可以重置状态以便下次再次有线程调用await方法时起作用，等待所有的线程都再调用了await方法后，线程们会冲破屏障，继续往下运行**

#### 10.2.1 案例介绍

```java
public class CyclicBarrierDemo {
    private static CyclicBarrier cyclicBarrier = new CyclicBarrier(2, () -> {
        System.out.println(Thread.currentThread() + " task1 merge result");
    });

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
            try {
                System.out.println(Thread.currentThread() + " step1");
                cyclicBarrier.await();
                System.out.println(Thread.currentThread() + " step2");
                cyclicBarrier.await();
                System.out.println(Thread.currentThread() + " step3");
                cyclicBarrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            } finally {
            }
        });
        executorService.submit(() -> {
            try {
                System.out.println(Thread.currentThread() + " step1");
                cyclicBarrier.await();
                System.out.println(Thread.currentThread() + " step2");
                cyclicBarrier.await();
                System.out.println(Thread.currentThread() + " step3");
                cyclicBarrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            } finally {
            }
        });
        executorService.shutdown();
    }
}
```

> Thread[pool-1-thread-1,5,main] step1
> Thread[pool-1-thread-2,5,main] step1
> Thread[pool-1-thread-2,5,main] task merge result
> Thread[pool-1-thread-2,5,main] step2
> Thread[pool-1-thread-1,5,main] step2
> Thread[pool-1-thread-1,5,main] task merge result
> Thread[pool-1-thread-1,5,main] step3
> Thread[pool-1-thread-2,5,main] step3
> Thread[pool-1-thread-2,5,main] task merge result

案例说明，假如计数器值为N，那么随后调用await方法的N-1个线程都会因为到达屏障点而被阻塞，当第N个线程调用await方法后，计数器的值为0，这时候第N个线程会唤醒前面阻塞的线程并执行barrierCommand方法（如果barrierCommand方法赋值了）

#### 10.2.2 实现原理探究

```java
public class CyclicBarrier {
 	//内部类，用于屏障的复位
    private static class Generation {
        boolean broken = false;
    }
    //可见内部还是使用独占锁实现的，底层也是AQS
    private final ReentrantLock lock = new ReentrantLock();
    //条件变量
    private final Condition trip = lock.newCondition();
	//等待的线程数，不会改变，表示多少线程调用await方法后，线程才会冲破屏障继续运行
    private final int parties;
    //最后一个线程冲破屏障后可以调用的方法
    private final Runnable barrierCommand;
    //内部类实例，其中的变量broken记录当前屏障是否被打破
    private Generation generation = new Generation();
	//正在等待的线程数，当为0时冲破屏障。初始值为parties，每当一个线程调用await方法，该值就减少1
    private int count;
    ...
}
```

##### 1）、await（）方法

当前线程调用CyclicBarrier类的await方法时，该方法会被阻塞，直到满足以下条件之一才会返回：parties个线程都调用了await方法，屏障被冲破；其他线程调用了该线程的中断方法；与当前屏障点关联的Generation对象的broken标志被设置为true，会抛出异常返回

```java
public int await() throws InterruptedException, BrokenBarrierException {
    try {
        //真正起作用的还是dowait方法
        return dowait(false, 0L);
    } catch (TimeoutException toe) {
        throw new Error(toe); // cannot happen
    }
}
```

核心方法dowait方法

```java
private int dowait(boolean timed, long nanos)
    throws InterruptedException, BrokenBarrierException,
           TimeoutException {
    //获取独占锁并上锁
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        //获取当前屏障的generation字段
        final Generation g = generation;
		//如果其broken被设置为true，也就是上面的第三种情况，方法直接异常并结束
        if (g.broken)
            throw new BrokenBarrierException();
		//如果发生中断，也就是第二种情况
        if (Thread.interrupted()) {
            breakBarrier();
            throw new InterruptedException();
        }
		//否在将count的值先自减然后赋值为index
        int index = --count;
        if (index == 0) {  // 如果index为0，说明已经有parties个线程调用了await方法，也就是当前线程是第parties个线程
            boolean ranAction = false;
            try {
                //如果barrierCommand这个方法不为空的话就执行
                final Runnable command = barrierCommand;
                if (command != null)
                    command.run();
                ranAction = true;
                //同时产生下一代，这个方法见下面。主要作用就是唤醒条件队列中的阻塞线程并重置count的值
                nextGeneration();
                return 0;
            } finally {
                //如果上面执行正常是不会进入这个if的，当然如果出现异常，就会执行breakBarrier方法
                //这个方法也简单，也是唤醒条件队列中的阻塞线程重置count的值，不过还要手动将broken设置为true
                if (!ranAction)
                    breakBarrier();
            }
        }

        // loop until tripped, broken, interrupted, or timed out
        //如果index不为0，那么按道理当前线程会被放入条件队列
        for (;;) {
            try {
                //如果不是定时的await就直接将任务挂起
                if (!timed)
                    trip.await();
                else if (nanos > 0L)
                    nanos = trip.awaitNanos(nanos);
            } catch (InterruptedException ie) {
                if (g == generation && ! g.broken) {
                    breakBarrier();
                    throw ie;
                } else {
                    // We're about to finish waiting even if we had not
                    // been interrupted, so this interrupt is deemed to
                    // "belong" to subsequent execution.
                    Thread.currentThread().interrupt();
                }
            }

            if (g.broken)
                throw new BrokenBarrierException();

            if (g != generation)
                return index;

            if (timed && nanos <= 0L) {
                breakBarrier();
                throw new TimeoutException();
            }
        }
    } finally {
        lock.unlock();
    }
}
```

```java
//nextGeneration方法
private void nextGeneration() {
    // signal completion of last generation
    //trip是条件队列，也就是唤醒所有阻塞在队列中的线程
    trip.signalAll();
    // 重新设置count的值，这是与那个CountdownLatch的差别
    count = parties;
    //重新给generation字段赋值，主要还是将里面的broken字段设置为false
    generation = new Generation();
}
```

#### 10.2.3 小结

CountDownLatch和CyclicBarrier的功能十分类似，不过前者不可以复用而后者可以实现复用功能。通过分析二者的源码可以发现，二者的实现并不相同。CountDownLatch内部借助Sync内部类（AQS的实现类来实现state的更改）来实现，每次调用countDown方法其实就是将state的值减少1；而CyclicBarrier内部借助独占锁和条件变量实现（其实也是AQS），每次调用await方法的线程会被放进条件变量的条件队列中

### 10.3 信号量Semaphore

信号量Semaphore也是Java中的一个同步器，其也称为流量控制器

#### 10.3.1 案例介绍

一个简单的流量控制案例如下

**Demo1**

```java
public class SemaphoreDemo {
    public static void main(String[] args) {
        //同一个时刻只能允许三个线程对资源进行访问
        Semaphore semaphore = new Semaphore(3);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire();
                    System.out.println(new Date(System.currentTimeMillis()) + " " + Thread.currentThread().getName() + " is running...");
                    Thread.sleep(1000);
                    System.out.println(new Date(System.currentTimeMillis()) + " " + Thread.currentThread().getName() + " end");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    semaphore.release();
                }
            }).start();
        }
    }
}
```

在构造Semaphore实例的时候，传入了3个配额，并通过其acquire和release方法实现配额的增加与减少，当每调用一次acquire方法时，配额就会减少1，当配额小于0时，当前线程会被阻塞并放入AQS阻塞队列；当配额大于0时，线程直接返回。当每调用一次release方法时，配额就会加1

因此，上面的案例可以实现每次最多三个线程同时执行其中的输出语句

除了在初始化Semaphore实例的时候传入配额之外，在调用acquire和release方法的时候也可以传入参数代表每次减少和增加的配额。其实调用release和acquire空参方法时，相当于调用acquire(1)和release(1)

**Demo2**

```java
public class SemaphoreDemo2 {
    private static Semaphore semaphore = new Semaphore(0);

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
            try {
                System.out.println(Thread.currentThread() + " 结束，释放资源");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        });

        executorService.submit(() -> {
            try {
                System.out.println(Thread.currentThread() + " 结束，释放资源");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        });

        semaphore.acquire(2);
        System.out.println("所有子线程结束");
        executorService.shutdown();
    }
}
```

> Thread[pool-1-thread-1,5,main] 结束，释放资源
> Thread[pool-1-thread-2,5,main] 结束，释放资源
> 所有子线程结束

Demo2在构造Semaphore实例的时候并没有传参，也就是此时配额为0，当在主线程中调用acquire(2)方法时，会发现配额不足，此时主线程会被挂起并放入阻塞队列

#### 10.3.2 实现原理探究

```Java
public class Semaphore implements java.io.Serializable {
    private static final long serialVersionUID = -3222578661600680210L;
    /** All mechanics via AbstractQueuedSynchronizer subclass */
    private final Sync sync;

	//内部类，实现了AQS队列，其还有两个子类，类似独占锁，分别实现公平和非公平策略
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;

        Sync(int permits) {
        setState(permits);
        }
        ...
    }
    public Semaphore(int permits) {
    	sync = new NonfairSync(permits);
    }
    ...
}
```

由Semaphore的部分代码可知，Semaphore内部还是使用AQS实现的。Sync也是对AQS的包装，并且Sync有两个实现类，分别对应公平策略还是非公平策略，默认情况下是非公平策略。**当我们调用Semaphore的构造方法时，传入的配额参数实质也是修改state的值，这里和CountDownLatch一样**

##### 1）、void acquire方法

当线程调用该方法后，如果此时的配额大于0，配额会减少1，然后方法返回；如果配额等于0（注意配额不会为负数，实质上配额就是state），当前线程会被挂起并放入阻塞队列，如果其他线程调用了当前线程的中断方法，该线程会中断返回

```java
//传参和不传参的，本质上都是一样的，内部还是调用AQS内部的方法
public void acquire() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}
public void acquire(int permits) throws InterruptedException {
    if (permits < 0) throw new IllegalArgumentException();
    sync.acquireSharedInterruptibly(permits);
}
//AQS内部的获取方法
public final void acquireSharedInterruptibly(int arg)
    throws InterruptedException {
    //如果中断了直接返回
    if (Thread.interrupted())
        throw new InterruptedException();
    //否在调用子类实现的tryAcquireShared方法，在下面分析
    //如果返回负数，说明当前state不满足需求数量，此时state的值不会更新，并且将当前线程包装成Node节点并放入AQS的阻塞队列；如果返回的数大于等于0，说明state的值进行了更新，并且该方法直接返回
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);
}
//Sync非公平策略实现的
protected int tryAcquireShared(int acquires) {
    return nonfairTryAcquireShared(acquires);
}

final int nonfairTryAcquireShared(int acquires) {
    //
    for (;;) {
        //获取state的值
        int available = getState();
        //如果此时state不满足获取要求，remaining为负数；否在应该大于等于0
        int remaining = available - acquires;
        if (remaining < 0 ||
            compareAndSetState(available, remaining))
            //如果remaining小于0，直接返回负数；否在将state的值更新，返回0或者正数
            return remaining;
    }
}

//公平策略的实现
 protected int tryAcquireShared(int acquires) {
     for (;;) {
         //多了一个判断，这个函数之前分析过。本质就是判断阻塞队列中是否有其他线程已经在等待获取；如果是则当前线程放弃获取返回-1
         if (hasQueuedPredecessors())
             return -1;
         int available = getState();
         int remaining = available - acquires;
         if (remaining < 0 ||
             compareAndSetState(available, remaining))
             return remaining;
     }
 }
```

##### 2）、void release() 方法

该方法的作用是把当前Semaphore对象的信号量增加1，如果当前有线程因为调用acquire方法被阻塞放入AQS队列，则会根据公平策略选择一个信号量个数满足的线程进行激活

```java
//传参和不传参的，本质上都是一样的，内部还是调用AQS内部的方法
public void release() {
    sync.releaseShared(1);
}
public void release(int permits) {
    if (permits < 0) throw new IllegalArgumentException();
    sync.releaseShared(permits);
}
//AQS内部的releaseShared方法，调用子类的tryReleaseShared方法
public final boolean releaseShared(int arg) {
    //返回true则激活一个线程
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}
//Sync实现的tryReleaseShared方法
//其实就是设置state的值
protected final boolean tryReleaseShared(int releases) {
    for (;;) {
        int current = getState();
        int next = current + releases;
        if (next < current) // overflow
            throw new Error("Maximum permit count exceeded");
        if (compareAndSetState(current, next))
            return true;
    }
}
```

#### 10.3.3 小结

通过源码可知，Semaphore内部也是通过Sync操作AQS的state变量来实现线程的暂停与唤醒，这点有点类似CountDownLatch类，不过Semaphore可以通过acquire方法和release方法多次实现更改信号量的控制

































































