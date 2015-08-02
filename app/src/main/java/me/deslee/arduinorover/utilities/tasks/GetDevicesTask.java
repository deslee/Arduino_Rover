package me.deslee.arduinorover.utilities.tasks;

import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.util.Set;

import me.deslee.arduinorover.utilities.BluetoothUtility;

public class GetDevicesTask extends BluetoothTask<Void, Void, Set<BluetoothDevice>> {
    public GetDevicesTask(BluetoothUtility bluetoothUtility) {
        super(bluetoothUtility);
    }

    @Override
    protected Set<BluetoothDevice> doInBackground(Void... params) {
        Set<BluetoothDevice> pairedDevices = bluetoothUtility.bluetoothAdapter.getBondedDevices();
        return pairedDevices;
    }

    @Override
    protected void onPostExecute(Set<BluetoothDevice> bluetoothDevices) {
        super.onPostExecute(bluetoothDevices);
        bluetoothUtility.onReceivedDevices(bluetoothDevices);
    }
}
