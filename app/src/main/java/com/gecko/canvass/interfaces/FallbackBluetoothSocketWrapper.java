package com.gecko.canvass.interfaces;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface FallbackBluetoothSocketWrapper {

    InputStream getInputStream() throws IOException;
    OutputStream getOutputStream() throws IOException;
    String getRemoteDeviceName();
    void connect() throws IOException;
    String getRemoteDeviceAddress();
    void close() throws IOException;
    BluetoothSocket getUnderlyingSocket();
}

