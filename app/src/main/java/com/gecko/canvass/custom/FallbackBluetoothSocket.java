package com.gecko.canvass.custom;

import android.bluetooth.BluetoothSocket;

import com.gecko.canvass.exceptions.FallbackException;
import com.gecko.canvass.interfaces.FallbackBluetoothSocketWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class FallbackBluetoothSocket implements FallbackBluetoothSocketWrapper {

    private BluetoothSocket fallbackSocket;
    public FallbackBluetoothSocket(BluetoothSocket tmp,int mode) throws FallbackException {
        //super(tmp);
        try
        {
            if(mode == 0)//secure socket
            {
                Class<?> clazz = tmp.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[] {Integer.valueOf(1)};
                fallbackSocket = (BluetoothSocket) m.invoke(tmp.getRemoteDevice(), params);
            }
            else if(mode == 1)//in-secure
            {
                Class<?> clazz = tmp.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
                Method m = clazz.getMethod("createInsecureRfcommSocket", paramTypes);
                Object[] params = new Object[] {Integer.valueOf(1)};
                fallbackSocket = (BluetoothSocket) m.invoke(tmp.getRemoteDevice(), params);
            }

        }
        catch (Exception e)
        {
            throw new FallbackException(e);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return (fallbackSocket==null)?null:fallbackSocket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return (fallbackSocket==null)?null:fallbackSocket.getOutputStream();
    }

    @Override
    public String getRemoteDeviceName() {
        return (fallbackSocket==null)?null:fallbackSocket.getRemoteDevice().getName();
    }

    @Override
    public void connect() throws IOException {
        fallbackSocket.connect();
    }

    @Override
    public String getRemoteDeviceAddress() {
        return (fallbackSocket==null)?null:fallbackSocket.getRemoteDevice().getAddress();
    }

    @Override
    public void close() throws IOException {
        if(fallbackSocket!=null)
            fallbackSocket.close();
    }

    @Override
    public BluetoothSocket getUnderlyingSocket() {
        return (fallbackSocket==null)?null:fallbackSocket;
    }
}
