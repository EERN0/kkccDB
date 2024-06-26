package kangc.kkccdb.backend.manager.transaction;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;

public class TransactionManagerTest {

    private int transCnt = 0;
    private final int noWorkers = 50;
    private final int noWorks = 3000;
    private final Lock lock = new ReentrantLock();
    private TransactionManager tmger;
    private Map<Long, Byte> transMap;
    private CountDownLatch latch;

    @Test
    public void testMultiThread() {
        tmger = TransactionManager.create("/tmp/tranmger_test.trxId");
        transMap = new ConcurrentHashMap<>();
        latch = new CountDownLatch(noWorkers);
        for (int i = 0; i < noWorkers; i++) {
            Runnable r = () -> worker();
            new Thread(r).start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assert new File("/tmp/tranmger_test.trxId").delete();
    }

    private void worker() {
        boolean inTrans = false;
        long transXID = 0;
        for (int i = 0; i < noWorks; i++) {
            int op = new Random(System.nanoTime()).nextInt(6);
            if (op == 0) {
                lock.lock();
                if (inTrans == false) {
                    long xid = tmger.begin();
                    transMap.put(xid, (byte) 0);
                    transCnt++;
                    transXID = xid;
                    inTrans = true;
                } else {
                    int status = (new Random(System.nanoTime()).nextInt(Integer.MAX_VALUE) % 2) + 1;
                    switch (status) {
                        case 1:
                            tmger.commit(transXID);
                            break;
                        case 2:
                            tmger.rollback(transXID);
                            break;
                    }
                    transMap.put(transXID, (byte) status);
                    inTrans = false;
                }
                lock.unlock();
            } else {
                lock.lock();
                if (transCnt > 0) {
                    long xid = (long) ((new Random(System.nanoTime()).nextInt(Integer.MAX_VALUE) % transCnt) + 1);
                    byte status = transMap.get(xid);
                    boolean ok = false;
                    switch (status) {
                        case 0:
                            ok = tmger.isActive(xid);
                            break;
                        case 1:
                            ok = tmger.isCommitted(xid);
                            break;
                        case 2:
                            ok = tmger.isAborted(xid);
                            break;
                    }
                    assert ok;
                }
                lock.unlock();
            }
        }
        latch.countDown();
    }
}
