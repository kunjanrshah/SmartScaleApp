package com.krs.smart.wifiUtils;

import android.os.Handler;
import android.util.Log;

import com.krs.smart.MainActivity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;

public class WifiClientThread implements Runnable {

    private final String TAG = WifiClientThread.class.getSimpleName();
    private Socket socket;
    public String message;
    public String macAddress;
    private final Handler handler=new Handler();

    @Override
    public void run() {

        try {
            Log.e(TAG, "server ip: " + WifiUtils.SERVER_IP);
            InetAddress serverAddr = InetAddress.getByName(WifiUtils.SERVER_IP);
            socket = new Socket(serverAddr, WifiUtils.SERVERPORT);
            InputStream is = socket.getInputStream();               // SOCKET READ
            byte[] buffer = new byte[1024];
            int read;

            try {
                serverAddr = InetAddress.getByName(socket.getLocalAddress().getHostAddress());
                macAddress =getMacAddr(serverAddr);
                Log.e(TAG, "mac address: " + macAddress);

            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            while ((read = is.read(buffer)) != -1) {
                message = new String(buffer, 0, read);
                System.out.print(message);
                System.out.flush();
                Log.e(TAG, "message from server: " + message);
                message=message.substring(0,7);
                showMessage(message,macAddress);
            }

        } catch (UnknownHostException e1) {
            e1.printStackTrace();
            Log.e(TAG, "UnknownHostException: ");
        } catch (IOException e1) {
            e1.printStackTrace();
            Log.e(TAG, "IOException:");
        }
    }

    public void showMessage(String message,String macAddress){
        handler.post(() -> {
            MainActivity.Companion.getWeighingScaleListAdapter().getWifiScaleWeight().setText(message.trim());
            MainActivity.Companion.getWeighingScaleListAdapter().getWifiScaleName().setText(macAddress.trim());
        });
    }

    public void sendMessage(final String message) {
        new Thread(() -> {
            try {
                if (null != socket) {
                //  SOCKET WRITE
                    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                        /*PrintWriter out = new PrintWriter(socket.getOutputStream());
                        out.println();
                        out.flush();*/
                    Log.e(TAG, "sendMessage: " + message);
                    out.println(message);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    public String getMacAddr(InetAddress addr) {
        String macAddress = "";
        try {

            NetworkInterface network = NetworkInterface.getByInetAddress(addr);
            byte[] macArray = network.getHardwareAddress();
            StringBuilder str = new StringBuilder();
            for (int i = 0; i < macArray.length; i++) {
                str.append(String.format("%02X%s", macArray[i], (i < macArray.length - 1) ? " " : ""));
                macAddress = str.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getStackTrace().toString());
        }
        return macAddress;
    }

    public void setStop(){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
