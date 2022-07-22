package com.meidical.meigeserialport;


import java.io.IOException;

public interface SerialPortDataListener {
    void onConnected(SerialPortManager helper);

    void onException(SerialPortManager helper);

    void onByteReceived(SerialPortManager helper, byte[] buffer, int length) throws IOException;
}
