package me.deslee.arduinorover.utilities;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.util.Observable;
import java.util.Set;
import java.util.UUID;

import me.deslee.arduinorover.R;

/**
 * Created by desmo on 8/2/2015.
 */
public class BluetoothUtility extends Observable {
    public static final String TAG = "RoverApp_BTUtil";
    private Application application;

    public class BluetoothUtilityEvent {
        public ErrorCode errorCode;
        public EventCode eventCode;
        public String errorText;
        public DeviceItem[] deviceItems;
    }

    BluetoothSocket socket;

    private UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

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

    public enum EventCode {
        DEVICES_UPDATED, CONNECTING, CONNECTED, DISCONNECTED, ERROR
    }

    public enum ErrorCode {
        BLUETOOTH_NOT_SUPPORTED,
        BLUETOOTH_DISABLED,
    }

    public BluetoothAdapter bluetoothAdapter;

    public BluetoothUtility(Application application) {
        this.application = application;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void getDevices() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                BluetoothUtilityEvent e = new BluetoothUtilityEvent();
                e.deviceItems = new DeviceItem[pairedDevices.size()];
                int i = 0;
                for (BluetoothDevice device : pairedDevices) {
                    e.deviceItems[i++] = new DeviceItem(device);
                }
                e.eventCode = EventCode.DEVICES_UPDATED;
                setChanged();
                notifyObservers(e);
            }
        }).start();
    }

    public void connect(BluetoothDevice device) {
        final BluetoothUtilityEvent event = new BluetoothUtilityEvent();
        ParcelUuid[] parcelUuids = device.getUuids();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
        try {
            // Use the UUID of the device that discovered // TODO Maybe need extra device object
            if (device != null)
            {
                Log.i(TAG, "Device Name: " + device.getName());
                Log.i(TAG, "Device UUID: " + device.getUuids()[0].getUuid());
                socket = device.createInsecureRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());

            }
            else Log.d(TAG, "Device is null.");
        }
        catch (NullPointerException e)
        {
            Log.d(TAG, " UUID from device is null, Using Default UUID, Device name: " + device.getName());
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(DEFAULT_UUID);
            } catch (IOException e1) {
                event.errorText = e1.getMessage();
                event.eventCode = EventCode.ERROR;
                setChanged();
                socket = null;
                notifyObservers(event);
                return;
            }
        }
        catch (IOException e) {
            event.errorText = e.getMessage();
            event.eventCode = EventCode.ERROR;
            setChanged();
            socket = null;
            notifyObservers(event);
            return;
        }

        if (socket != null) {
            final BluetoothSocket finalSocket = socket;

            event.eventCode = EventCode.CONNECTING;
            setChanged();
            notifyObservers(event);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BluetoothUtilityEvent event = new BluetoothUtilityEvent();
                        finalSocket.connect();
                        socket = finalSocket;
                        event.eventCode = EventCode.CONNECTED;
                        setChanged();
                        notifyObservers(event);
                    } catch (IOException e) {
                        // Unable to connect; close the socket and get out
                        try {
                            finalSocket.close();
                        } catch (IOException closeException) { }

                        socket = null;
                        event.errorText = e.getMessage();
                        event.eventCode = EventCode.ERROR;
                        setChanged();
                        notifyObservers(event);
                        return;
                    }
                }
            }).start();
        } else {
            event.errorText = "Socket is null";
            event.eventCode = EventCode.ERROR;
            setChanged();
            notifyObservers(event);
        }
    }


    public void sendByte(final int cmd) {
        if (socket != null && socket.isConnected()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket.getOutputStream().write(cmd);
                        Log.i(TAG, "Sent " + cmd);
                    } catch (IOException e) {
                        BluetoothUtilityEvent event = new BluetoothUtilityEvent();
                        event.eventCode = EventCode.ERROR;
                        event.errorText = e.getMessage();
                        setChanged();
                        notifyObservers(event);
                    }
                }
            }).start();
        } else {
            BluetoothUtilityEvent event = new BluetoothUtilityEvent();
            event.eventCode = EventCode.DISCONNECTED;
            setChanged();
            notifyObservers(event);
        }
    }
}
