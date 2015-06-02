package com.sullenart.turntable;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Config;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity
{
    private BluetoothAdapter adapter;
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStreamWriter writer;
    private static final UUID SPP_UUID = UUID.fromString ("00001101-0000-1000-8000-00805F9B34FB");

    private Handler handler;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);

        handler = new Handler ();

        // Add rotation handler
        ImageButton cw = (ImageButton) findViewById (R.id.clockwise);
        cw.setOnTouchListener (new View.OnTouchListener ()
        {
            @Override
            public boolean onTouch (View view, MotionEvent event)
            {
                switch (event.getAction ())
                {
                    case MotionEvent.ACTION_DOWN:
                        btSendSpeed (-95);
                        break;

                    case MotionEvent.ACTION_UP:
                        btSendSpeed (0);
                        break;
                }
                return false;
            }
        });

        // Add rotation handler
        ImageButton acw = (ImageButton) findViewById (R.id.anti_clockwise);
        acw.setOnTouchListener (new View.OnTouchListener ()
        {
            @Override
            public boolean onTouch (View view, MotionEvent event)
            {
                switch (event.getAction ())
                {
                    case MotionEvent.ACTION_DOWN:
                        btSendSpeed (95);
                        break;

                    case MotionEvent.ACTION_UP:
                        btSendSpeed (0);
                        break;
                }
                return false;
            }
        });

        Log.d ("turntable", "Application started");
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig)
    {
        super.onConfigurationChanged (newConfig);
    }

    private void btSendSpeed (int speed)
    {
        if (socket == null)
            return;

        try
        {
            String data = String.format ("%d;", speed);
            socket.getOutputStream ().write (data.getBytes ());
            Log.d ("turntable", String.format ("Sent speed to Bluetooth: %s", data));
        }
        catch (IOException e)
        {
            Log.e ("turntable", String.format ("Error writing speed to Bluetooth: %s", e.getMessage ()));
            Toast.makeText (this, e.getMessage (), Toast.LENGTH_SHORT).show ();
        }
    }

    private void btConnect ()
    {
        final ProgressBar progress = (ProgressBar) findViewById (R.id.progress);
        progress.setVisibility (View.VISIBLE);

        new Thread (new Runnable ()
        {
            @Override
            public void run ()
            {
                try
                {
                    adapter = BluetoothAdapter.getDefaultAdapter ();
                    Log.d ("turntable", String.format ("Adapter found: %s\n", adapter.getName ()));
                    device = null;
                    for (BluetoothDevice bd : adapter.getBondedDevices ())
                    {
                        Log.d ("turntable", String.format ("Paired device: %s", bd.getName ()));

                        if (bd.getName ().equals ("HC-06"))
                            device = bd;
                    }
                    if (device == null)
                        throw new Exception ("Target device not found");

                    Log.d ("turntable", "Target device found");

                    socket = device.createRfcommSocketToServiceRecord (SPP_UUID);
                    socket.connect ();
                    Log.d ("turntable", String.format ("Socket connected\n"));
                }
                catch (final Exception e)
                {
                    Log.e ("turntable", e.getMessage ());

                    handler.post (new Runnable ()
                    {
                        @Override
                        public void run ()
                        {
                            Toast.makeText (MainActivity.this, e.getMessage (), Toast.LENGTH_SHORT).show ();
                        }
                    });
                }

                handler.post (new Runnable ()
                {
                    @Override
                    public void run ()
                    {
                        progress.setVisibility (View.INVISIBLE);
                    }
                });
            }
        }).start ();
    }

    private void btDisconnect ()
    {
        try
        {
            if (socket != null)
            {
                socket.close ();
                Log.d ("turntable", String.format ("Bluetooth disconnected\n"));
            }
        }
        catch (Exception e)
        {
        }
        socket = null;
    }

    @Override
    public void onResume ()
    {
        super.onResume ();

        btDisconnect ();
        btConnect ();
    }

    @Override
    public void onPause ()
    {
        super.onPause ();

        btDisconnect ();
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater ().inflate (R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId ();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected (item);
    }
}
