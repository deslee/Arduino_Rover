package me.deslee.arduinorover.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

import me.deslee.arduinorover.MyApplication;
import me.deslee.arduinorover.R;
import me.deslee.arduinorover.utilities.BluetoothUtility;

/**
 * Created by desmo on 8/2/2015.
 */
public class BluetoothConnectDialog extends DialogFragment implements DialogInterface.OnClickListener, Observer, View.OnClickListener {

    private View layoutView;
    private Button refreshButton;
    public static final String TAG = "RoverApp_BTDiag";
    private ArrayAdapter<BluetoothUtility.DeviceItem> adapter;
    private ArrayList<BluetoothUtility.DeviceItem> deviceItems;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        BluetoothUtility utility = getBluetoothUtility();
        utility.addObserver(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        BluetoothUtility utility = getBluetoothUtility();
        utility.deleteObserver(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        layoutView = inflater.inflate(R.layout.dialog_bluetooth_connect, null);
        refreshButton = (Button) layoutView.findViewById(R.id.btn_bt_refresh);
        refreshButton.setOnClickListener(this);
        BluetoothUtility utility = getBluetoothUtility();
        utility.getDevices();

        deviceItems = new ArrayList<>();
        adapter = new ArrayAdapter<>(getActivity(), R.layout.simple_list_item_1, deviceItems);

        return builder
            .setView(layoutView)
            .setTitle(R.string.select_device)
            .setAdapter(adapter, this)
            .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        BluetoothUtility utility = getBluetoothUtility();
        BluetoothUtility.DeviceItem item = deviceItems.get(which);
        utility.connect(item.device);
        Log.i(TAG, "clicked " + which);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btn_bt_refresh:
                Log.i(TAG, "refresh button clicked");
                BluetoothUtility utility = getBluetoothUtility();
                utility.getDevices();
                break;
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        try {
            final BluetoothUtility.BluetoothUtilityEvent event = (BluetoothUtility.BluetoothUtilityEvent) data;
            this.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (event.eventCode == BluetoothUtility.EventCode.DEVICES_UPDATED) {
                        refreshButton.setText(R.string.refresh_button);
                        refreshButton.setEnabled(true);
                        deviceItems.clear();
                        deviceItems.addAll(Arrays.asList(event.deviceItems));
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        } catch(ClassCastException e) {
            throw new ClassCastException(observable.toString()
                    + " must implement BluetoothUtility");
        }
    }

    public BluetoothUtility getBluetoothUtility() {
        return ((MyApplication) this.getActivity().getApplication()).bluetoothUtility;
    }
}
