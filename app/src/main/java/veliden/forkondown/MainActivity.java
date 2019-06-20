/*
    Name: Nolan Mey, Jonathan Rochin, Khanh Truong, Chou Thao
    Course: CECS 490B
    Project: ForkOnDown
 */

package veliden.forkondown;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;

import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.ToggleButton;
import android.widget.CompoundButton;

import android.support.v7.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    boolean stopThread;
    boolean connected;
    byte buffer[];
    String data = "";

    ToggleButton btnTog;
    Button btnF, btnB, btnL, btnR;
    Button btnRFork, btnLFork;

    Button btnHome, btnDock, btnSClear, btnRClear;
    Button btn11, btn12, btn13;
    Button btn21, btn22, btn23;
    Button btn31, btn32, btn33;

    TextView sView, rView;

    // Insert BT Address here
    //private final String DEVICE_ADDRESS = "2017:2:154762";

    // insert BT UUID here
    private final UUID btUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Serial Port ID

    // insert BT Name here
    private final String DEVICE_NAME = "chou";

    private BluetoothDevice device;
    private BluetoothSocket socket;

    private InputStream inputStream;
    private OutputStream outputStream;

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Set<BluetoothDevice>bondedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUI();
        btInitialization();
        connectDevice();
        openSocket();
        startSerialCom();
        receiveData();
        autoButtons();
        manualButtons();
        neutralButtons();
        sendData();
        userInterface();

        sView.setText("");
        rView.setText("");
    }

    public void setUI() {
        btnTog = (ToggleButton)findViewById(R.id.btnToggle);
        btnF = (Button)findViewById(R.id.btnFwd);
        btnB = (Button)findViewById(R.id.btnBwd);
        btnL = (Button)findViewById(R.id.btnLeft);
        btnR = (Button)findViewById(R.id.btnRight);

        btnRFork = (Button)findViewById(R.id.btnRaise);
        btnLFork = (Button)findViewById(R.id.btnLower);

        btnHome = (Button)findViewById(R.id.home);
        btnDock = (Button)findViewById(R.id.dock);
        btnSClear = (Button)findViewById(R.id.sClear);
        btnRClear = (Button)findViewById(R.id.rClear);

        btn11 = (Button)findViewById(R.id.grid_11);
        btn12 = (Button)findViewById(R.id.grid_12);
        btn13 = (Button)findViewById(R.id.grid_13);
        btn21 = (Button)findViewById(R.id.grid_21);
        btn22 = (Button)findViewById(R.id.grid_22);
        btn23 = (Button)findViewById(R.id.grid_23);
        btn31 = (Button)findViewById(R.id.grid_31);
        btn32 = (Button)findViewById(R.id.grid_32);
        btn33 = (Button)findViewById(R.id.grid_33);

        sView = (TextView)findViewById(R.id.sConsole);
        sView.setMovementMethod(new ScrollingMovementMethod());

        rView = (TextView)findViewById(R.id.rConsole);
        rView.setMovementMethod(new ScrollingMovementMethod());
    }
    
    public void btInitialization() {
        if(mBluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "This device does not support Bluetooth features. ",
                    Toast.LENGTH_LONG).show();
            finish();
        } else if(!mBluetoothAdapter.isEnabled()) {
            Intent enableAdapter = new Intent (BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableAdapter);
        }

        if(mBluetoothAdapter.isEnabled()) {
            connectDevice();
        }
    }

    public boolean connectDevice() {
        bondedDevices = mBluetoothAdapter.getBondedDevices();
        boolean found = false;

        if (bondedDevices.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please pair your device first",
                    Toast.LENGTH_SHORT).show();
        } else {
            for(BluetoothDevice iterator : bondedDevices) {
                if(iterator.getName().equals(DEVICE_NAME)) {
                    device = iterator;
                    found = true;
                    break;
                }
            }
        }

        return found;
    }

    public boolean openSocket() {
        connected = true;

        try {
            socket = device.createRfcommSocketToServiceRecord(btUUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }

        if(connected) {
            try {
                outputStream = socket.getOutputStream();
            } catch(IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream = socket.getInputStream();
            } catch(IOException e) {
                e.printStackTrace();
            }

            Toast.makeText(MainActivity.this, "IOStreams open",
                    Toast.LENGTH_LONG).show();

            sView.setText("");
            rView.setText("");
        }
        else {
            Toast.makeText(MainActivity.this, "Failed to connect to device, running in Sandbox Mode.",
                    Toast.LENGTH_LONG).show();
        }
        return connected;
    }

    public void startSerialCom() {
        if(connectDevice()) {
            if(connected) {
                rView.append("\nConnected to Forklift!\n");
                Toast.makeText(MainActivity.this, "Connected to Forklift!",
                        Toast.LENGTH_LONG).show();
                receiveData();
            }
        }
    }

    void receiveData() {
        if(connected) {
            rView.append("Ready to receive:\n");
            final Handler handler = new Handler();
            stopThread = false;
            buffer = new byte[1024];
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    while (!Thread.currentThread().isInterrupted() && !stopThread) {
                        try {
                            int byteCount = inputStream.available();
                            if (byteCount > 0) {
                                byte[] rawBytes = new byte[byteCount];
                                inputStream.read(rawBytes);
                                final String string = new String(rawBytes, "UTF-8");
                                handler.post(new Runnable() {
                                    public void run() {
                                        rView.append(string + "\n");
                                    }
                                });
                            }
                        } catch (IOException e) {
                            stopThread = true;
                        }
                    }
                }
            });
            thread.start();
        }
    }

    public void autoButtons() {
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // supposed to sendData() method with string for commands
                //sView.append("Sent Data: Going Home\n");
                data = "Home\n";
                sendData();
            }
        });

        btnDock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // supposed to sendData() method with string for commands
                //sView.append("Sent Data: Moving to Dock\n");
                data = "Dock\n";
                sendData();
            }
        });

        btn11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // supposed to sendData() method with string for commands
                //sView.append("Sent Data: 1,1\n");
                data = "1,1\n";
                sendData();
            }
        });

        btn12.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // supposed to sendData() method with string for commands
                //sView.append("Sent Data: 1,2\n");
                data = "1,2\n";
                sendData();
            }
        });

        btn13.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // supposed to sendData() method with string for commands
                //sView.append("Sent Data: 1,3\n");
                data = "1,3\n";
                sendData();
            }
        });

        btn21.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // supposed to sendData() method with string for commands
                //sView.append("Sent Data: 2,1\n");
                data = "2,1\n";
                sendData();
            }
        });

        btn22.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // supposed to sendData() method with string for commands
                //sView.append("Sent Data: 2,2\n");
                data = "2,2\n";
                sendData();
            }
        });

        btn23.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // supposed to sendData() method with string for commands
                //sView.append("Sent Data: 2,3\n");
                data = "2,3\n";
                sendData();
            }
        });

        btn31.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // supposed to sendData() method with string for commands
                //sView.append("Sent Data: 3,1\n");
                data = "3,1\n";
                sendData();
            }
        });

        btn32.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // supposed to sendData() method with string for commands
                //sView.append("Sent Data: 3,2\n");
                data = "3,2\n";
                sendData();
            }
        });

        btn33.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // supposed to sendData() method with string for commands
                //sView.append("Sent Data: 3,3\n");
                data = "3,3\n";
                sendData();
            }
        });
    }

    public void manualButtons() {

        btnRFork.setOnTouchListener(new View.OnTouchListener() {

            private Handler mHandler;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 0);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override public void run() {
                    //sView.append("Sent Data: Raise\n");
                    data = "Raise\n";
                    sendData();
                    mHandler.postDelayed(this, 500);
                }
            };
        });

        btnLFork.setOnTouchListener(new View.OnTouchListener() {

            private Handler mHandler;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 0);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override public void run() {
                    //sView.append("Sent Data: Lower\n");
                    data = "Lower\n";
                    sendData();
                    mHandler.postDelayed(this, 500);
                }
            };
        });

        btnF.setOnTouchListener(new View.OnTouchListener() {

            private Handler mHandler;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 0);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override public void run() {
                    //sView.append("Sent Data: Forward\n");
                    data = "Forward\n";
                    sendData();
                    mHandler.postDelayed(this, 500);
                }
            };
        });

        btnB.setOnTouchListener(new View.OnTouchListener() {

            private Handler mHandler;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 0);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override public void run() {
                    //sView.append("Sent Data: Backward\n");
                    data = "Backward\n";
                    sendData();
                    mHandler.postDelayed(this, 500);
                }
            };
        });

        btnL.setOnTouchListener(new View.OnTouchListener() {

            private Handler mHandler;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 0);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override public void run() {
                    //sView.append("Sent Data: Left\n");
                    data = "Left\n";
                    sendData();
                    mHandler.postDelayed(this, 500);
                }
            };
        });

        btnR.setOnTouchListener(new View.OnTouchListener() {

            private Handler mHandler;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        mHandler.postDelayed(mAction, 0);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override public void run() {
                    //sView.append("Sent Data: Right\n");
                    data = "Right\n";
                    sendData();
                    mHandler.postDelayed(this, 500);
                }
            };
        });
    }

    public void neutralButtons() {
        btnSClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sView.setText("");
            }
        });

        btnRClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rView.setText("");
            }
        });
    }

    public void sendData() {
        if(connected) {
            try {
                outputStream.write(data.getBytes());
                sView.append("Sent Data: " + data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            sView.append("Sent Data: " + data);
        }
    }

    public void userInterface() {
        btnTog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    Toast.makeText(MainActivity.this, "Manual Controls Enabled",
                            Toast.LENGTH_SHORT).show();

                    btnF.setVisibility(btnF.VISIBLE);
                    btnB.setVisibility(btnB.VISIBLE);
                    btnL.setVisibility(btnL.VISIBLE);
                    btnR.setVisibility(btnR.VISIBLE);

                    btnRFork.setVisibility(btnRFork.VISIBLE);
                    btnLFork.setVisibility(btnLFork.VISIBLE);

                    btnHome.setVisibility(btnHome.INVISIBLE);
                    btnDock.setVisibility(btnDock.INVISIBLE);

                    btn11.setVisibility(btn11.INVISIBLE);
                    btn12.setVisibility(btn12.INVISIBLE);
                    btn13.setVisibility(btn13.INVISIBLE);
                    btn21.setVisibility(btn21.INVISIBLE);
                    btn22.setVisibility(btn22.INVISIBLE);
                    btn23.setVisibility(btn23.INVISIBLE);
                    btn31.setVisibility(btn31.INVISIBLE);
                    btn32.setVisibility(btn32.INVISIBLE);
                    btn33.setVisibility(btn33.INVISIBLE);
                } else {
                    Toast.makeText(MainActivity.this, "Auto Mode Enabled",
                            Toast.LENGTH_SHORT).show();

                    btnF.setVisibility(btnF.INVISIBLE);
                    btnB.setVisibility(btnB.INVISIBLE);
                    btnL.setVisibility(btnL.INVISIBLE);
                    btnR.setVisibility(btnR.INVISIBLE);

                    btnRFork.setVisibility(btnRFork.INVISIBLE);
                    btnLFork.setVisibility(btnLFork.INVISIBLE);

                    btnHome.setVisibility(btnHome.VISIBLE);
                    btnDock.setVisibility(btnDock.VISIBLE);

                    btn11.setVisibility(btn11.VISIBLE);
                    btn12.setVisibility(btn12.VISIBLE);
                    btn13.setVisibility(btn13.VISIBLE);
                    btn21.setVisibility(btn21.VISIBLE);
                    btn22.setVisibility(btn22.VISIBLE);
                    btn23.setVisibility(btn23.VISIBLE);
                    btn31.setVisibility(btn31.VISIBLE);
                    btn32.setVisibility(btn32.VISIBLE);
                    btn33.setVisibility(btn33.VISIBLE);
                }
            }
        });
    }


}
