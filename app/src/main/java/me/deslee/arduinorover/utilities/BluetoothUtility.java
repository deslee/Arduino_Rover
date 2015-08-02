package me.deslee.arduinorover.utilities;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import me.deslee.arduinorover.R;
import me.deslee.arduinorover.utilities.tasks.ConnectTask;
import me.deslee.arduinorover.utilities.tasks.GetDevicesTask;

public class BluetoothUtility extends Observable {
    private BluetoothSocket socket;
    BlockingQueue<Integer> queue;

    public class DeviceItem {
        public DeviceItem(BluetoothDevice device) {
            this.device = device;
        }
        public BluetoothDevice device;

        @Override
        public String toString() {
            return String.format(application.getString(R.string.deviceListItem), device.getName(), device.getAddress());
        }
    }

    public class BluetoothUtilityEvent {
        public EventCode eventCode;
        public String message;
        public ArrayList<DeviceItem> deviceItems;

        public BluetoothUtilityEvent(EventCode eventCode) {
            this.eventCode = eventCode;
        }
    }

    public static final String TAG = "RoverApp_BTUtil";
    private Application application;

    public enum EventCode {
        RECEIVED_DEVICES, CONNECTING, CONNECTED, DISCONNECTED, ERROR
    }

    public BluetoothAdapter bluetoothAdapter;

    public BluetoothUtility(Application application) {
        this.application = application;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        queue = new LinkedBlockingQueue<>();
    }

    public void getDevices() {
        new GetDevicesTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    public void onReceivedDevices(Set<BluetoothDevice> bluetoothDevices) {
        BluetoothUtilityEvent event = new BluetoothUtilityEvent(EventCode.RECEIVED_DEVICES);
        event.deviceItems = new ArrayList<>();
        for (BluetoothDevice device : bluetoothDevices) {
            event.deviceItems.add(new DeviceItem(device));
        }
        this.setChanged();
        this.notifyObservers(event);
    }

    public void connect(BluetoothDevice device) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {}
            socket = null;
        }
        BluetoothUtilityEvent event = new BluetoothUtilityEvent(EventCode.CONNECTING);
        this.setChanged();
        this.notifyObservers(event);
        new ConnectTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, device);
    }
    public void onConnected(BluetoothSocket bluetoothSocket) {
        this.socket = bluetoothSocket;
        BluetoothUtilityEvent event = new BluetoothUtilityEvent(EventCode.CONNECTED);
        this.setChanged();
        this.notifyObservers(event);
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
                        BluetoothUtilityEvent event = new BluetoothUtilityEvent(EventCode.ERROR);
                        event.message = errorMessage;
                        setChanged();
                        notifyObservers(event);
                    }
                }
            };
            task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        } else {
            BluetoothUtilityEvent event = new BluetoothUtilityEvent(EventCode.DISCONNECTED);
            setChanged();
            notifyObservers(event);
        }
    }
}
