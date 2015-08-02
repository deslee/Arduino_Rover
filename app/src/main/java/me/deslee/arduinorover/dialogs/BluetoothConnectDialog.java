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

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import me.deslee.arduinorover.BluetoothServiceActivity;
import me.deslee.arduinorover.R;
import me.deslee.arduinorover.utilities.BluetoothUtilityService;

/**
 * Created by desmo on 8/2/2015.
 */
public class BluetoothConnectDialog extends DialogFragment implements DialogInterface.OnClickListener, View.OnClickListener {

    public static final String TAG = "RoverApp_BTDiag";
    private View layoutView;
    private Button refreshButton;
    private ArrayAdapter<BluetoothUtilityService.DeviceItem> adapter;
    private ArrayList<BluetoothUtilityService.DeviceItem> deviceItems;
    private BluetoothUtilityService bluetoothUtilityService;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            bluetoothUtilityService = ((BluetoothServiceActivity) activity).getBluetoothService();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement BluetoothServiceActivity");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        layoutView = inflater.inflate(R.layout.dialog_bluetooth_connect, null);
        refreshButton = (Button) layoutView.findViewById(R.id.btn_bt_refresh);
        refreshButton.setOnClickListener(this);
        bluetoothUtilityService.getDevices();

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
        BluetoothUtilityService.DeviceItem item = deviceItems.get(which);
        bluetoothUtilityService.connect(item.device);
        Log.i(TAG, "clicked " + item.name);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btn_bt_refresh:
                Log.i(TAG, "refresh button clicked");
                bluetoothUtilityService.getDevices();
                break;
        }
    }

    @Subscribe
    public void onEvent(BluetoothUtilityService.BluetoothUtilityEvent event) {
        if (event.type == BluetoothUtilityService.EventType.RECEIVED_DEVICES) {
            deviceItems.clear();
            deviceItems.addAll(event.deviceItems);

            refreshButton.setText(R.string.refresh_button);
            refreshButton.setEnabled(true);
            adapter.notifyDataSetChanged();
        }
    }
}
