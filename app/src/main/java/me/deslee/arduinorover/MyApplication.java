package me.deslee.arduinorover;

import android.app.Application;

import me.deslee.arduinorover.utilities.BluetoothUtility;

/**
 * Created by desmo on 8/2/2015.
 */
public class MyApplication extends Application {
    public BluetoothUtility bluetoothUtility = new BluetoothUtility(this);
}
