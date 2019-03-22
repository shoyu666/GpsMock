package com.nixiaoning.test.gpsmock;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 模拟服务
 */
public class MockService extends Service {
    public static final String TAG = MockService.class.getSimpleName();
    public static final String TestProvder = LocationManager.GPS_PROVIDER;
    private ScheduledExecutorService mExecutorService;
    private ConcurrentLinkedQueue<Location> playList = new ConcurrentLinkedQueue<>();

    public Runnable test = new Runnable() {
        @Override
        public void run() {
            try {
                if (playList.size() == 0) {
                    InputStreamReader inputReader = new InputStreamReader(getResources().getAssets().open("test_tji.csv"));
                    addLocaton(inputReader);
                }
                Location location = playList.poll();
                location.setTime(new Date().getTime());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                }
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.setTestProviderLocation(TestProvder, location);
                LogUtil.d(TAG, "mock "+location.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void addLocaton(InputStreamReader inputReader) {
        LogUtil.d(TAG, "addLocaton");
        playList.clear();
        try {
            String line = "";
            BufferedReader bufReader = new BufferedReader(inputReader);
            while ((line = bufReader.readLine()) != null) {
                String[] lines = line.split(",");
                Location location = new Location(TestProvder);
                float latitude = Float.parseFloat(lines[0]);
                float longitude = Float.parseFloat(lines[1]);
                HashMap<String, Double> hm = delta(latitude, longitude);
                location.setLatitude(hm.get("lat"));
                location.setLongitude(hm.get("lon"));
                location.setSpeed(Float.parseFloat(lines[2]));
                location.setBearing(Float.parseFloat(lines[3]));
                location.setAccuracy(20f); // 精度（米）
                playList.add(location);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mExecutorService = Executors.newScheduledThreadPool(2);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.addTestProvider(TestProvder, true, true, false,
                false, true, true, true, Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
        locationManager.setTestProviderEnabled(TestProvder, true);
        mExecutorService.scheduleWithFixedDelay(test, 1, 1, TimeUnit.SECONDS);
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onFileEvent(FileEvent event) {
        try {
            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(event.path));
            addLocaton(inputReader);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeTestProvider(TestProvder);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //圆周率 GCJ_02_To_WGS_84
    double PI = 3.14159265358979324;

    /**
     * @param 需要转换的经纬度
     * @return 转换为真实GPS坐标后的经纬度
     * @throws <异常类型> {@inheritDoc} 异常描述
     * @author 作者:
     * 方法描述:方法可以将高德地图SDK获取到的GPS经纬度转换为真实的经纬度，可以用于解决安卓系统使用高德SDK获取经纬度的转换问题。
     */
    public HashMap<String, Double> delta(double lat, double lon) {
        double a = 6378245.0;//克拉索夫斯基椭球参数长半轴a
        double ee = 0.00669342162296594323;//克拉索夫斯基椭球参数第一偏心率平方
        double dLat = this.transformLat(lon - 105.0, lat - 35.0);
        double dLon = this.transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * this.PI;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * this.PI);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * this.PI);

        HashMap<String, Double> hm = new HashMap<String, Double>();
        hm.put("lat", lat - dLat);
        hm.put("lon", lon - dLon);

        return hm;
    }

    //转换经度
    public double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * this.PI) + 20.0 * Math.sin(2.0 * x * this.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * this.PI) + 40.0 * Math.sin(x / 3.0 * this.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * this.PI) + 300.0 * Math.sin(x / 30.0 * this.PI)) * 2.0 / 3.0;
        return ret;
    }

    //转换纬度
    public double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * this.PI) + 20.0 * Math.sin(2.0 * x * this.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * this.PI) + 40.0 * Math.sin(y / 3.0 * this.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * this.PI) + 320 * Math.sin(y * this.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }
}
