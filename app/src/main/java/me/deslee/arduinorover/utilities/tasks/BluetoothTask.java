package me.deslee.arduinorover.utilities.tasks;

import android.os.AsyncTask;

import me.deslee.arduinorover.utilities.BluetoothUtility;

public abstract class BluetoothTask<T1, T2, T3> extends AsyncTask<T1, T2, T3> {
    protected final BluetoothUtility bluetoothUtility;

    public BluetoothTask(BluetoothUtility bluetoothUtility) {
        this.bluetoothUtility = bluetoothUtility;
    }
}
