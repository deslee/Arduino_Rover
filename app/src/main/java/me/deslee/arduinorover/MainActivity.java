package me.deslee.arduinorover;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.zerokol.views.JoystickView;

import java.util.Observable;
import java.util.Observer;

import me.deslee.arduinorover.dialogs.BluetoothConnectDialog;
import me.deslee.arduinorover.utilities.BluetoothUtility;

public class MainActivity extends AppCompatActivity implements Observer, JoystickView.OnJoystickMoveListener {

    public static final int REQUEST_ENABLE_BT = 1;

    public static final String TAG = "RoverApp";

    private TextView statusView;
    private boolean bluetoothEnabled = false;
    private JoystickView joystick;
    private int lastJoystickDirection = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View mainView = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(mainView);
        BluetoothUtility utility = ((MyApplication) getApplication()).bluetoothUtility;

        statusView = (TextView) mainView.findViewById(R.id.status);
        joystick = (JoystickView) findViewById(R.id.joystickView);

        joystick.setOnJoystickMoveListener(this, JoystickView.DEFAULT_LOOP_INTERVAL);

        if (utility.bluetoothAdapter == null) {
            statusView.setText(R.string.no_bluetooth_status);
        } else if (!utility.bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            bluetoothEnabled = true;
        }

        utility.addObserver(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_CANCELED) {
                    statusView.setText(R.string.no_bluetooth_status);
                } else if (resultCode == RESULT_OK) {
                    bluetoothEnabled = true;
                }
                invalidateOptionsMenu();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BluetoothUtility utility = ((MyApplication) getApplication()).bluetoothUtility;
        utility.deleteObserver(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connect_bluetooth, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setEnabled(bluetoothEnabled);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_connect) {
            DialogFragment dialog = new BluetoothConnectDialog();
            dialog.show(getSupportFragmentManager(), "BluetoothConnect");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void update(Observable observable, Object data) {
        try {
            BluetoothUtility utility = (BluetoothUtility) observable;
            final BluetoothUtility.BluetoothUtilityEvent event = (BluetoothUtility.BluetoothUtilityEvent) data;
            if (event.eventCode == BluetoothUtility.EventCode.CONNECTING) {
                joystick.setVisibility(View.INVISIBLE);
                statusView.setText("Connecting...");
            } else if (event.eventCode == BluetoothUtility.EventCode.CONNECTED) {
                joystick.setVisibility(View.VISIBLE);
                statusView.setText("Connected!");
            } else if (event.eventCode == BluetoothUtility.EventCode.ERROR) {
                joystick.setVisibility(View.INVISIBLE);
                statusView.setText(event.message);
            } else if (event.eventCode == BluetoothUtility.EventCode.DISCONNECTED) {
                joystick.setVisibility(View.INVISIBLE);
            }
        } catch(ClassCastException e) {
            throw new ClassCastException(observable.toString()
                    + " must implement BluetoothUtility");
        }
    }

    @Override
    public void onValueChanged(int angle, int power, int direction) {
        if (direction == lastJoystickDirection) {
            return;
        }
        lastJoystickDirection = direction;

        int cmd_stop = 0;
        int cmd_lf = 1;
        int cmd_lr = 2;
        int cmd_rf = 4;
        int cmd_rr = 8;

        int cmd;

        switch (direction) {
            case JoystickView.FRONT:
                Log.i(TAG, "Front");
                cmd = cmd_lf | cmd_rf;
                break;
            case JoystickView.FRONT_RIGHT:
                Log.i(TAG, "FRONT_RIGHT");
                cmd = cmd_lf;
                break;
            case JoystickView.RIGHT:
                Log.i(TAG, "RIGHT");
                cmd = cmd_lf | cmd_rr;
                break;
            case JoystickView.RIGHT_BOTTOM:
                Log.i(TAG, "RIGHT_BOTTOM");
                cmd = cmd_lr;
                break;
            case JoystickView.BOTTOM:
                Log.i(TAG, "BOTTOM");
                cmd = cmd_lr | cmd_rr;
                break;
            case JoystickView.BOTTOM_LEFT:
                Log.i(TAG, "BOTTOM_LEFT");
                cmd = cmd_rr;
                break;
            case JoystickView.LEFT:
                Log.i(TAG, "LEFT");
                cmd = cmd_lr | cmd_rf;
                break;
            case JoystickView.LEFT_FRONT:
                Log.i(TAG, "LEFT_FRONT");
                cmd = cmd_rf;
                break;
            default:
                cmd = cmd_stop;
        }

        BluetoothUtility utility = ((MyApplication) getApplication()).bluetoothUtility;
        utility.sendByte(cmd);
    }
}
