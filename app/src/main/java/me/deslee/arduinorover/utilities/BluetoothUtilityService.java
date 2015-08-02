package me.deslee.arduinorover.utilities;

import android.app.Application;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import de.greenrobot.event.EventBus;
import me.deslee.arduinorover.R;
import me.deslee.arduinorover.utilities.tasks.ConnectTask;
import me.deslee.arduinorover.utilities.tasks.GetDevicesTask;

public class BluetoothUtilityService extends Service {
    public static final String TAG = "RoverApp_BTUtil";
    private final IBinder mBinder = new LocalBinder();
    public BluetoothAdapter bluetoothAdapter;
    BlockingQueue<Integer> queue;
    private BluetoothSocket socket;

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service bind");
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Service created");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        queue = new LinkedBlockingQueue<>();
    }

    @Override
    public void onDestroy() {
        queue.clear();
    }

    public void getDevices() {
        new GetDevicesTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void onReceivedDevices(Set<BluetoothDevice> bluetoothDevices) {

        ArrayList<DeviceItem> deviceItems = new ArrayList<>();
        for (BluetoothDevice device : bluetoothDevices) {
            deviceItems.add(new DeviceItem(device));
        }
        BluetoothUtilityEvent event = new BluetoothUtilityEvent(EventType.RECEIVED_DEVICES);
        event.deviceItems = deviceItems;
        EventBus.getDefault().post(event);
    }

    public void connect(BluetoothDevice device) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
            socket = null;
        }
        BluetoothUtilityEvent event = new BluetoothUtilityEvent(EventType.CONNECTING);
        EventBus.getDefault().post(event);
        new ConnectTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, device);
    }

    public void onConnected(BluetoothSocket bluetoothSocket) {
        this.socket = bluetoothSocket;
        BluetoothUtilityEvent event = new BluetoothUtilityEvent(EventType.CONNECTED);
        EventBus.getDefault().post(event);
    }

    public void onError(String error) {
        this.socket = null;
        BluetoothUtilityEvent event = new BluetoothUtilityEvent(EventType.ERROR);
        event.message = error;
        EventBus.getDefault().post(event);
    }

    public void sendByte(final int cmd) {
        if (socket != null && socket.isConnected()) {
            AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
                String errorMessage = null;

                @Override
                protected Boolean doInBackground(Void... params) {
                    try {
                        socket.getOutputStream().write(cmd);
                        Log.i(TAG, "Sent " + cmd);
                    } catch (IOException e) {
                        errorMessage = e.getMessage();
                        return false;
                    }
                    return true;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if (!result) {
                        BluetoothUtilityEvent event = new BluetoothUtilityEvent(EventType.ERROR);
                        event.message = errorMessage;
                        EventBus.getDefault().post(event);
                    }
                }
            };
            task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        } else {
            BluetoothUtilityEvent event = new BluetoothUtilityEvent(EventType.DISCONNECTED);
            EventBus.getDefault().post(event);
        }
    }

    public enum EventType {
        RECEIVED_DEVICES, CONNECTING, CONNECTED, DISCONNECTED, ERROR
    }

    public class LocalBinder extends Binder {
        public BluetoothUtilityService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BluetoothUtilityService.this;
        }
    }

    public class BluetoothUtilityEvent {
        public final EventType type;
        public String message;
        public List<BluetoothUtilityService.DeviceItem> deviceItems;

        public BluetoothUtilityEvent(EventType type) {
            this.type = type;
        }
    }

    public class DeviceItem {
        public BluetoothDevice device;
        public String name;
        public String address;

        public DeviceItem(BluetoothDevice device) {
            this.name = device.getName();
            this.address = device.getAddress();
            this.device = device;
        }

        @Override
        public String toString() {
            Application application = getApplication();
            return String.format(application.getString(R.string.deviceListItem), name, address);
        }
    }
}
