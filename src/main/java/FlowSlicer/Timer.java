package FlowSlicer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class Timer {
    private String title;
    private long start;
    private long time;
    private boolean running;

    public Timer(String title) {
        this.title = title;
        this.time = 0;
        this.running = false;
    }

    public synchronized void start() {
        if (!this.running) {
            this.start = System.currentTimeMillis();
            this.running = true;
        } else {
            log.debug("Timer (" + this.title + ") already running!");
        }
    }

    public synchronized void stop() {
        if (this.running) {
            this.time += (System.currentTimeMillis() - this.start);
            this.running = false;
        } else {
            log.debug("Timer (" + this.title + ") was not started, yet!");
        }
    }

    public synchronized double spendTimeInSec() {
        if (this.running) {
            return ((double)(System.currentTimeMillis() - this.start)) / 1000;
        } else {
            log.debug("Timer (" + this.title + ") was not started, yet!");
            return 0;
        }
    }

    public synchronized long spendTimeInMilliSec() {
        if (this.running) {
            return (System.currentTimeMillis() - this.start);
        } else {
            log.debug("Timer (" + this.title + ") was not started, yet!");
            return 0;
        }
    }

    public synchronized void setTime(long timeInMS) {
        this.time = timeInMS;
    }

    public synchronized long getTime() {
        return this.time;
    }

    public synchronized String getTimeAsString() {
        long rest = this.time;
        rest = rest / 1000;
        final long s = rest % 60;
        rest = rest / 60;
        final long m = rest;
        return (m > 0 ? m + ":" : "0:") + (s > 0 ? digits(s, 2) : "00");
    }

    private String digits(long input, int num) {
        String output = String.valueOf(input).toString();
        while (output.length() < num) {
            output = "0" + output;
        }
        return output;
    }
}
