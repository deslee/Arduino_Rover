package me.deslee.arduinorover.utilities.tasks;

import android.os.AsyncTask;

import me.deslee.arduinorover.utilities.BluetoothUtilityService;

public abstract class BluetoothTask<T1, T2, T3> extends AsyncTask<T1, T2, T3> {
    protected final BluetoothUtilityService bluetoothUtilityService;

    public BluetoothTask(BluetoothUtilityService bluetoothUtilityService) {
        this.bluetoothUtilityService = bluetoothUtilityService;
    }
}
