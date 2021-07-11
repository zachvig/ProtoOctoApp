package de.crysxd.octoapp.base.utils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.HashSet;

public class UPnPDiscovery extends AsyncTask {

    private static final String TAG = UPnPDiscovery.class.getSimpleName();
    private static final String LINE_END = "\r\n";
    private static final String DEFAULT_QUERY = "M-SEARCH * HTTP/1.1" + LINE_END +
            "HOST: 239.255.255.250:1900" + LINE_END +
            "MAN: \"ssdp:discover\"" + LINE_END +
            "MX: 1" + LINE_END +
            //"ST: urn:schemas-upnp-org:service:AVTransport:1" + LINE_END + // Use for Sonos
            //"ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1" + LINE_END + // Use for Routes
            "ST: ssdp:all" + LINE_END + // Use this for all UPnP Devices
            LINE_END;
    private static final String DEFAULT_ADDRESS = "239.255.255.250";
    private static int DISCOVER_TIMEOUT = 1500;
    private static int DEFAULT_PORT = 1900;
    private HashSet<UPnPDevice> devices = new HashSet<>();
    private Context mContext;
    private int mTheardsCount = 0;
    private String mCustomQuery;
    private String mInetAddress;
    private int mPort;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private OnDiscoveryListener mListener;

    public UPnPDiscovery(Context context, OnDiscoveryListener listener) {
        mContext = context.getApplicationContext();
        mListener = listener;
        mTheardsCount = 0;
        mCustomQuery = DEFAULT_QUERY;
        mInetAddress = DEFAULT_ADDRESS;
        mPort = DEFAULT_PORT;
    }

    private UPnPDiscovery(Context context, OnDiscoveryListener listener, String customQuery, String address, int port) {
        mContext = context.getApplicationContext();
        mListener = listener;
        mTheardsCount = 0;
        mCustomQuery = customQuery;
        mInetAddress = address;
        mPort = port;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        mListener.OnStart();
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            WifiManager.MulticastLock lock = wifi.createMulticastLock("The Lock");
            lock.acquire();
            DatagramSocket socket = null;
            try {
                InetAddress group = InetAddress.getByName(mInetAddress);
                int port = mPort;
                String query = mCustomQuery;
                socket = new DatagramSocket(port);
                socket.setReuseAddress(true);

                DatagramPacket datagramPacketRequest = new DatagramPacket(query.getBytes(), query.length(), group, port);
                socket.send(datagramPacketRequest);

                long time = System.currentTimeMillis();
                long curTime = System.currentTimeMillis();

                while (curTime - time < DISCOVER_TIMEOUT) {
                    DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
                    socket.setSoTimeout(DISCOVER_TIMEOUT);
                    socket.receive(datagramPacket);
                    String response = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                    if (response.substring(0, 12).toUpperCase().equals("HTTP/1.1 200")) {
                        UPnPDevice device = new UPnPDevice(datagramPacket.getAddress().getHostAddress(), response);
                        mListener.OnFoundNewDevice(device);
                    }
                    curTime = System.currentTimeMillis();
                }
            } catch (SocketTimeoutException e) {
                // Do nothing
            } catch (final IOException e) {
                mListener.OnError(e);
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
            lock.release();
        }
        mListener.OnFinish();
        return null;
    }

    public interface OnDiscoveryListener {
        void OnStart();

        void OnFoundNewDevice(UPnPDevice device);

        void OnFinish();

        void OnError(Exception e);
    }

}
