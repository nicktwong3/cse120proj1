package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {

	LinkedList waitQueue;

	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		
		waitQueue = new LinkedList<KThread>();
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		Machine.interrupt().disable();
		waitQueue.add(KThread.currentThread());
		conditionLock.release();
		KThread.currentThread().sleep();
		Machine.interrupt().enable();
		conditionLock.acquire();
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		Machine.interrupt().disable();
		if(!waitQueue.isEmpty()) {
			KThread temp = (KThread)waitQueue.removeFirst();
			temp.ready();
			ThreadedKernel.alarm.cancel(temp);
		}
		Machine.interrupt().enable();
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		while(!waitQueue.isEmpty())
			wake();
	}

        /**
	 * Atomically release the associated lock and go to sleep on
	 * this condition variable until either (1) another thread
	 * wakes it using <tt>wake()</tt>, or (2) the specified
	 * <i>timeout</i> elapses.  The current thread must hold the
	 * associated lock.  The thread will automatically reacquire
	 * the lock before <tt>sleep()</tt> returns.
	 */
        public void sleepFor(long timeout) {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		Machine.interrupt().disable();
		KThread temp = KThread.currentThread();
		waitQueue.add(temp);
		conditionLock.release();
		ThreadedKernel.alarm.waitUntil(timeout);
		waitQueue.remove(temp);
		Machine.interrupt().enable();
		conditionLock.acquire();

	}

	private static class InterlockTest {
		private static Lock lock;
		private static Condition2 cv;
		private static class Interlocker implements Runnable {
			public void run() {
				lock.acquire();
				for(int i = 0; i < 10; i++) {
					System.out.println(KThread.currentThread().getName());
					cv.wake();
					cv.sleep();
				}
				lock.release();
			}
		}
		public InterlockTest() {
			lock = new Lock();
			cv = new Condition2(lock);

			KThread ping = new KThread(new Interlocker());
			ping.setName("ping");
			KThread pong = new KThread(new Interlocker());
			pong.setName("pong");

			ping.fork();
			pong.fork();
			ping.join();
		}
	}

	public static void cvTest5() {
		final Lock lock = new Lock();
		final Condition2 empty = new Condition2(lock);
		final LinkedList<Integer> list = new LinkedList<>();

		KThread consumer = new KThread( new Runnable () {
			public void run() {
				lock.acquire();
				while(list.isEmpty()) {
					empty.sleep();
				}
				Lib.assertTrue(list.size() == 5, "List should have 5 values.");
				while(!list.isEmpty()) {
					KThread.currentThread().yield();
					System.out.println("Removed " + list.removeFirst());
				}
				lock.release();
			}
		});

		KThread producer = new KThread( new Runnable () {
			public void run() {
				lock.acquire();
				for(int i = 0; i < 5; i++) {
					list.add(i);
					System.out.println("Added " + i);
					KThread.currentThread().yield();
				}
				empty.wake();
				lock.release();
			}
		});

		consumer.setName("Consumer");
		producer.setName("Producer");
		consumer.fork();
		producer.fork();
		consumer.join();
		producer.join();
	}

	private static void sleepForTest1() {
		Lock lock = new Lock();
		Condition2 cv = new Condition2(lock);

		lock.acquire();
		long t0 = Machine.timer().getTime();
		System.out.println(KThread.currentThread().getName() + " sleeping");
		cv.sleepFor(2000);
		long t1 = Machine.timer().getTime();
		System.out.println(KThread.currentThread().getName() + " woke up, slept for " + (t1 - t0 ) + " ticks");
		lock.release();
	}
	
	public static void selfTest() {
		new InterlockTest();
		cvTest5();
		sleepForTest1();
	}

        private Lock conditionLock;
}
