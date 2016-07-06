package com.example.nilif.wifiapplication;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

/**
 * Created by nilif on 2016/7/4.
 */
public class FileTransferService extends IntentService{

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

    public FileTransferService()
    {
        super("FileTransferService");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public FileTransferService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = getApplicationContext();
        if (intent.getAction() == ACTION_SEND_FILE){
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                Log.d(MyWiFiActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect(new InetSocketAddress(host,port),SOCKET_TIMEOUT);
                Log.d(MyWiFiActivity.TAG,
                        "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream inputStream = null;

                inputStream = cr.openInputStream(Uri.parse(fileUri));

                DeviceDetailFragment.copyFile(inputStream,stream);
                Log.d(MyWiFiActivity.TAG, "Client: Data written");
            } catch (IOException e) {
                Log.e(MyWiFiActivity.TAG, e.getMessage());
            }finally {
                if (socket != null){
                    if (socket.isConnected()){
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
