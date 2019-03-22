package com.nixiaoning.test.gpsmock;

import org.greenrobot.eventbus.EventBus;

public class LogEvent {
    public LogEvent(String log) {
        this.log = log;
    }

    public String log;
    public static void post(String msg) {
        LogEvent event = new LogEvent(msg);
        EventBus.getDefault().post(event);
    }
}
