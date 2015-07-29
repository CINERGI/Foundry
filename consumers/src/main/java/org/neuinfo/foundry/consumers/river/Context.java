package org.neuinfo.foundry.consumers.river;

import org.neuinfo.foundry.consumers.river.QueueEntry;

import java.util.concurrent.BlockingQueue;

/**
 * Created by bozyurt on 4/4/14.
 */
public class Context {
    private Status status;
    private BlockingQueue<QueueEntry> stream;

    public Context(Status status, BlockingQueue<QueueEntry> stream) {
        this.status = status;
        this.stream = stream;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public BlockingQueue<QueueEntry> getStream() {
        return stream;
    }

    public void setStream(BlockingQueue<QueueEntry> stream) {
        this.stream = stream;
    }
}
