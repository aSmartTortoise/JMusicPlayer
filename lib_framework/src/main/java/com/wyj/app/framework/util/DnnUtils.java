package com.wyj.app.framework.util;


import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;

import com.blankj.utilcode.util.LogUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.List;

public class DnnUtils {


    private static final String TAG = "DnnUtils";

    public static int privateNetWorkDnn1Id = 0;
    public static int publicNetWorkDnn2Id = 0;
    public static int publicNetWorkDnn3Id = 0;
    public static int publicNetWorkDnn4Id = 0;
    public static Network privateNetWorkDnn1 = null;
    public static Network publicNetWorkDnn2 = null;
    public static Network publicNetWorkDnn3 = null;
    public static Network publicNetWorkDnn4 = null;

    private static final String DNN1_SUFFIX = "106.40";
    private static final String DNN2_SUFFIX = "103.40";
    private static final String DNN3_SUFFIX = "107.40";
    private static final String DNN4_SUFFIX = "108.40";


    public static void switchToAllNetwork(Context context) {
        boolean enable = isAllNetworkEnabled(context);
        LogUtils.i("switchToAllNetwork() called with: enable = [" + enable + "]");
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!enable) {
            Network[] networks = connMgr.getAllNetworks();
            LogUtils.i("networks length:" + networks.length);
            for (Network network : networks) {
                LinkProperties lp = connMgr.getLinkProperties(network);
                if (lp != null) {
                    for (LinkAddress linkAddress : lp.getLinkAddresses()) {
                        String address = linkAddress.getAddress().toString();
                        LogUtils.v("address:" + address + "," + network);
                        if (address.endsWith(DNN1_SUFFIX)) {
                            privateNetWorkDnn1Id = Integer.parseInt(network.toString());
                            privateNetWorkDnn1 = getPrivateDnsNet(network);
                        }
                        if (address.endsWith(DNN2_SUFFIX)) {
                            publicNetWorkDnn2Id = Integer.parseInt(network.toString());
                            publicNetWorkDnn2 = getPrivateDnsNet(network);
                        }
                        if (address.endsWith(DNN3_SUFFIX)) {
                            publicNetWorkDnn3Id = Integer.parseInt(network.toString());
                            publicNetWorkDnn3 = getPrivateDnsNet(network);
                            List<InetAddress> dnsServers = lp.getDnsServers();
                            if (!dnsServers.isEmpty()) {
                                for (int i = 0; i < dnsServers.size(); i++) {
                                    LogUtils.v("dnsServers[" + i + "]:" + dnsServers.get(i).toString());
                                }
                            } else {
                                LogUtils.i("dnsServers is empty");
                            }
                        }
                        if (address.endsWith(DNN4_SUFFIX)) {
                            publicNetWorkDnn4Id = Integer.parseInt(network.toString());
                            publicNetWorkDnn4 = getPrivateDnsNet(network);
                            bindToNetwork(publicNetWorkDnn4, connMgr);
                        }
                    }
                }
            }
        } else {
            boolean ret = connMgr.bindProcessToNetwork(null);
            LogUtils.i("unbindProcessToNetwork ret:" + ret);
        }
    }


    private static Network getPrivateDnsNet(Network network) {
        Method getPrivateDnsBypassingCopyMethod;
        Network netWorkPrivate = null;
        try {
            getPrivateDnsBypassingCopyMethod = Network.class.getDeclaredMethod("getPrivateDnsBypassingCopy");
            getPrivateDnsBypassingCopyMethod.setAccessible(true);
            netWorkPrivate = (Network) getPrivateDnsBypassingCopyMethod.invoke(network);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return netWorkPrivate;
    }

    private static void bindToNetwork(Network network, ConnectivityManager connMgr) {
        boolean result = connMgr.bindProcessToNetwork(network);
        LogUtils.i("bindProcessToNetwork ret:" + result);
    }

    private static boolean isAllNetworkEnabled(Context context) {
        boolean enable = false;
        try {
            Uri uri = Uri.parse("content://com.voyah.ai.voice.export/settings/all_network");
            Cursor cursor = context.getApplicationContext().getContentResolver().query(uri, null, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        enable = cursor.getInt(0) == 1;
                        LogUtils.d("isAllNetworkEnabled enable = [" + enable + "]");
                        break;
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return enable;
    }
}
