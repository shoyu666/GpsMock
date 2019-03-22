package com.nixiaoning.test.gpsmock;

import org.greenrobot.eventbus.EventBus;

public class FileEvent {
    public FileEvent(String path) {
        this.path = path;
    }

    String path;
    public static void post(String path) {
        FileEvent event = new FileEvent(path);
        EventBus.getDefault().post(event);
    }
}
