package epitech.chirou_a.epifeu;

import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Aridias on 28/07/2016.
 */
public class BluetoothThread extends Thread{
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private String mStr;
    private View mView;
    private BluetoothSocket mSocket;
    private ImageView mImg;

    static ColorFilter screen(int c) {
        return new LightingColorFilter(0xFFFFFFFF - c, c);
    }

    //creation of the connect thread
    public BluetoothThread(BluetoothSocket socket, View view, String str, ImageView img) {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            //Create I/O streams for connection
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        mView = view;
        mStr = str;
        mSocket = socket;
        mImg = img;
    }

    public void run() {
        write(mStr);
        read();
    }

    public void close(int bytes)
    {
        if (bytes == -1  && mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException x) {
                x.printStackTrace();
            }
        }
    }

    public int read()
    {
        while(!Thread.currentThread().isInterrupted())
        {
            int bytesAvailable;
            int readBufferPosition = 0;

            try {
                bytesAvailable = mmInStream.available();
                if(bytesAvailable > 0)
                {
                    byte[] packetBytes = new byte[bytesAvailable];
                    Log.e("Aquarium recv bt","bytes available");
                    byte[] readBuffer = new byte[1024];
                    mmInStream.read(packetBytes);

                    for(int i=0;i<bytesAvailable;i++)
                    {
                        byte b = packetBytes[i];
                        if(b == 33)
                        {
                            byte[] encodedBytes = new byte[readBufferPosition];
                            System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                            final String data = new String(encodedBytes, "UTF-8");
                            readBufferPosition = 0;
                            Log.e("DATA", data);
                            if (data.equals("RED"))
                            {
                                mImg.post(new Runnable() {
                                    public void run() {
                                        mImg.setColorFilter(screen(Color.RED));
                                    }
                                });
                                Snackbar.make(mView, "Status is RED !", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                            else if (data.equals("GREEN"))
                            {
                                mImg.post(new Runnable() {
                                    public void run() {
                                        mImg.setColorFilter(screen(Color.GREEN));
                                    }
                                });
                                Snackbar.make(mView, "Status is GREEN !", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                            else
                            {
                                mImg.post(new Runnable() {
                                    public void run() {
                                        mImg.setColorFilter(screen(Color.BLACK));
                                    }
                                });
                                Snackbar.make(mView, "Undefined Status : " + data, Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                            break;
                        }
                        else
                        {
                            readBuffer[readBufferPosition++] = b;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }

        }
        return 0;
    }

    public void write(String input) {
        byte[] msgBuffer = input.getBytes();
        try {
            mmOutStream.write(msgBuffer);
        } catch (IOException e) {
            Log.e("ERROR", e.toString());
        }
    }
}
