package com.example.nilif.wifiapplication;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by nilif on 2016/5/10.
 */
public class DeviceDetailFragment extends Fragment implements WifiP2pManager.ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private ProgressDialog progressDialog;
    private WifiP2pInfo info;

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail,null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = device.deviceAddress;
                        config.wps.setup = WpsInfo.PBC;
                        if (progressDialog != null
                                && progressDialog.isShowing())
                        {
                            progressDialog.dismiss();
                        }
                        progressDialog = ProgressDialog.show(getActivity(),
                                "Press back to cancel", "Connecting to :"
                                        + device.deviceAddress, true, true
                                // new DialogInterface.OnCancelListener() {
                                //
                                // @Override
                                // public void onCancel(DialogInterface dialog) {
                                // ((DeviceActionListener)
                                // getActivity()).cancelDisconnect();
                                // }
                                // }
                        );
                        ((DeviceListFragment.DeviceActionListener) getActivity()).connect(config);
                    }
                }
        );

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((DeviceListFragment.DeviceActionListener) getActivity()).disconnect();
            }
        });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,CHOOSE_FILE_RESULT_CODE);
            }
        });
        return mContentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri = data.getData();
        TextView statuesText = (TextView) mContentView.findViewById(R.id.status_text);
        statuesText.setText("Sending: "+uri);
        Log.d(MyWiFiActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(),
                FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH,
                uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT,
                8988);
        getActivity().startService(serviceIntent);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(
                R.string.yes) : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - "
                + info.groupOwnerAddress.getHostAddress());

        if (info.groupFormed && info.isGroupOwner){
            new FileServerAsyncTask(getActivity(),mContentView.findViewById(R.id.status_text)).execute();
        }else if (info.groupFormed){
            mContentView.findViewById(R.id.btn_start_client).setVisibility(
                    View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text))
                    .setText(getResources().getString(R.string.client_text));
        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView
                .findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(
                View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView
                .findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());
    }

    public static class FileServerAsyncTask extends AsyncTask<Void,Void,String>{

        private Context context;
        private TextView statusText;

        public FileServerAsyncTask(Context context,View statusText){
            this.context = context;
            this.statusText = (TextView) statusText;

        }
        @Override
        protected String doInBackground(Void... params) {// 多个参数,相当于void[] params
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(MyWiFiActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(MyWiFiActivity.TAG, "Server: connection done");
                final File f = new File(
                        Environment.getExternalStorageDirectory() + "/"
                        +context.getPackageName() + "/wifip2pshared-"
                        +System.currentTimeMillis() + ".jpg");
                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();
                Log.d(MyWiFiActivity.TAG, "server: copying files " + f.toString());
                InputStream inputStream = client.getInputStream();
                copyFile(inputStream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath(); // 返回绝对路径
            } catch (IOException e) {
                Log.e(MyWiFiActivity.TAG, e.getMessage());
                return null;
            }
        }



        @Override
        protected void onPostExecute(String result)
        {
            if (result != null)
            {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }

        }

        @Override
        protected void onPreExecute()
        {
            statusText.setText("Opening a server socket");
        }
    }
    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1){
                out.write(buf,0,len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(MyWiFiActivity.TAG, e.toString());
            return false;
        }
        return true;
    }
}
