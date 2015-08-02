package me.deslee.arduinorover.utilities.tasks;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

import me.deslee.arduinorover.utilities.BluetoothUtility;

public class ConnectTask extends BluetoothTask<BluetoothDevice, Void, BluetoothSocket> {
    public ConnectTask(BluetoothUtility bluetoothUtility) {
        super(bluetoothUtility);
    }

    public static final String TAG = "RoverApp_ConnectTask";
    private UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected BluetoothSocket doInBackground(BluetoothDevice... devices) {
        BluetoothDevice device = devices[0];

        BluetoothSocket socket;

        try {
            // Use the UUID of the device that discovered
            if (device != null)
            {
                Log.i(TAG, "Device Name: " + device.getName());
                Log.i(TAG, "Device UUID: " + device.getUuids()[0].getUuid());
                socket = device.createInsecureRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());
            }
            else {
                Log.d(TAG, "Device is null.");
                return null;
            }
        }
        catch (NullPointerException e)
        {
            Log.d(TAG, " UUID from device is null, Using Default UUID, Device name: " + device.getName());
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(DEFAULT_UUID);
            } catch (IOException e1) {
                return null;
            }
        }
        catch (IOException e) {
            return null;
        }
        try {
            socket.connect();
            return socket;
        } catch (IOException e) {
            // Unable to connect; close the socket and get out
            try {
                socket.close();
            } catch (IOException closeException) { }
            return null;
        }
    }

    @Override
    protected void onPostExecute(BluetoothSocket bluetoothSocket) {
        super.onPostExecute(bluetoothSocket);
        bluetoothUtility.onConnected(bluetoothSocket);
    }
}
