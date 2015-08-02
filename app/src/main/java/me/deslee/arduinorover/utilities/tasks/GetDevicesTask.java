package me.deslee.arduinorover.utilities.tasks;

import android.bluetooth.BluetoothDevice;

import java.util.Set;

import me.deslee.arduinorover.utilities.BluetoothUtilityService;

public class GetDevicesTask extends BluetoothTask<Void, Void, Set<BluetoothDevice>> {
    public GetDevicesTask(BluetoothUtilityService bluetoothUtilityService) {
        super(bluetoothUtilityService);
    }

    @Override
    protected Set<BluetoothDevice> doInBackground(Void... params) {
        Set<BluetoothDevice> pairedDevices = bluetoothUtilityService.bluetoothAdapter.getBondedDevices();
        return pairedDevices;
    }

    @Override
    protected void onPostExecute(Set<BluetoothDevice> bluetoothDevices) {
        super.onPostExecute(bluetoothDevices);
        bluetoothUtilityService.onReceivedDevices(bluetoothDevices);
    }
}
