package com.nixiaoning.test.gpsmock;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtil {
    public static SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
    public static void d(String tag,String msg){
        String time = format.format(new Date());
        LogEvent.post(time+":"+msg);
    }
}
