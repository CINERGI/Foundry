package org.neuinfo.foundry.jms.producer;

/**
 * Created by bozyurt on 5/8/14.
 */
public class TimeCheckPointScheduler implements Runnable {
    private long periodInMillisecs = 300000l; // every 5 minutes
    private TimeCheckPointManager manager = null;

    public TimeCheckPointScheduler() {
        manager = TimeCheckPointManager.getInstance();
    }

    @Override
    public void run() {
        while (true) {

            long start = System.currentTimeMillis();
            long timeLeft = periodInMillisecs;
            do {
                synchronized (this) {
                    try {
                        this.wait(timeLeft);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
                timeLeft = periodInMillisecs - (System.currentTimeMillis() - start);
            } while(timeLeft > 0);

            try {
                manager.checkpoint();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }
}
