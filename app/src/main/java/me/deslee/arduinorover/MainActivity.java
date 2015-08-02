package me.deslee.arduinorover;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.zerokol.views.JoystickView;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import me.deslee.arduinorover.dialogs.BluetoothConnectDialog;
import me.deslee.arduinorover.utilities.BluetoothUtilityService;

public class MainActivity extends AppCompatActivity implements JoystickView.OnJoystickMoveListener, BluetoothServiceActivity {
    public static final int REQUEST_ENABLE_BT = 1;

    public static final String TAG = "RoverApp";

    private TextView statusView;
    private boolean bluetoothEnabled = false;
    private JoystickView joystick;
    private int lastJoystickDirection = 0;
    private BluetoothUtilityService bluetoothUtilityService;

    private ServiceConnection bluetoothServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Service connected");
            bluetoothUtilityService = ((BluetoothUtilityService.LocalBinder) service).getService();

            if (bluetoothUtilityService.bluetoothAdapter == null) {
                statusView.setText(R.string.no_bluetooth_status);
            } else if (!bluetoothUtilityService.bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                bluetoothEnabled = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothUtilityService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View mainView = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(mainView);
        Intent startServiceIntent = new Intent(this, BluetoothUtilityService.class);
        Log.i(TAG, "binding service");
        boolean isConnected = bindService(startServiceIntent, bluetoothServiceConnection, Context.BIND_AUTO_CREATE);

        statusView = (TextView) mainView.findViewById(R.id.status);
        joystick = (JoystickView) findViewById(R.id.joystickView);

        joystick.setOnJoystickMoveListener(this, JoystickView.DEFAULT_LOOP_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(bluetoothServiceConnection);
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

    @Subscribe
    public void onEvent(BluetoothUtilityService.BluetoothUtilityEvent event) {
        if (event.type == BluetoothUtilityService.EventType.CONNECTING) {
            joystick.setVisibility(View.INVISIBLE);
            statusView.setText("Connecting...");
        } else if (event.type == BluetoothUtilityService.EventType.CONNECTED) {
            joystick.setVisibility(View.VISIBLE);
            statusView.setText("Connected!");
        } else if (event.type == BluetoothUtilityService.EventType.ERROR) {
            joystick.setVisibility(View.INVISIBLE);
            statusView.setText(event.message);
        } else if (event.type == BluetoothUtilityService.EventType.DISCONNECTED) {
            joystick.setVisibility(View.INVISIBLE);
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

        bluetoothUtilityService.sendByte(cmd);
    }

    @Override
    public BluetoothUtilityService getBluetoothService() {
        return bluetoothUtilityService;
    }
}
