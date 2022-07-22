package com.meidical.meigeserialport;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;

import com.meidical.utils.LogUtils;


public class SerialPortManager {
    private static final String TAG = "SerialPortManager";
    //串口地址
    private String path;
    //奇偶校验:
    // 0 --> 无校验
    // 1 --> 奇校验
    // 2 --> 偶校验
    // 3 --> Space校验
    private int parity = 0;
    //停止位:
    // 1 --> 1位
    // 2 --> 2位
    private int stopBits = 1;
    //数据位:
    // 5 --> 5位
    // 6 --> 6位
    // 7 --> 7位
    // 8 --> 8位
    private int dataBits = 8;
    //波特率
    private int baudRate = 0;
    //数据流控:
    // 0 --> 无流控
    // 1 --> 硬件流控
    // 1 --> 软件流控
    private int flowControl = 0;

    private int state;
    private int timeout = Integer.MAX_VALUE;
    private SerialPort serialPort;
    private WeakReference<SerialPortDataListener> listener;
    //线程命名
    private final String name;
    private OptionHandler mOptionHandler;
    private HandlerThread mHandlerThread;
    //读取数据线程
    private ReadThread mReadThread;

    public static final int SERIAL_PORT_STATE_IDLE = 1;
    public static final int SERIAL_PORT_STATE_OPENED = 2;
    public static final int SERIAL_PORT_STATE_CLOSED = 3;
    public static final int SERIAL_PORT_STATE_RELEASED = 4;

    public SerialPortManager(String name) {
        this.name = name;
        this.state = SERIAL_PORT_STATE_RELEASED;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getParity() {
        return parity;
    }

    public void setParity(int parity) {
        this.parity = parity;
    }

    public int getStopBits() {
        return stopBits;
    }

    public void setStopBits(int stopBits) {
        this.stopBits = stopBits;
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        this.dataBits = dataBits;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public int getFlowControl() {
        return flowControl;
    }

    public void setFlowControl(int flowControl) {
        this.flowControl = flowControl;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void init(SerialPortDataListener listener) {
        if (isReleased()) {
            this.createProcessHandler();
            this.listener = new WeakReference<>(listener);
            this.changeSerialPortState(SERIAL_PORT_STATE_IDLE);
        }
    }

    public void open() {
        if (isIdle() || isClosed()) {
            this.changeSerialPortState(SERIAL_PORT_STATE_OPENED);
        }
    }

    public synchronized void write(byte[] bytes) {
        try {
            if (isWriteable()) {
                this.serialPort.outputStream().write(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.onException();
        }
    }

    public synchronized void writeAndFlush(byte[] bytes) {
        try {
            if (isWriteable()) {
                this.serialPort.outputStream().write(bytes);
                this.serialPort.outputStream().flush();
                this.serialPort.outputStream().getFD().sync();
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.onException();
        }
    }

    public void close() {
        if (this.isOpened()) {
            this.changeSerialPortState(SERIAL_PORT_STATE_CLOSED);
        }
    }

    public void release() {
        if (!isReleased()) {
            this.changeSerialPortState(SERIAL_PORT_STATE_RELEASED);
        }
    }

    private boolean isIdle() {
        if (this.state == SERIAL_PORT_STATE_IDLE) {
            return true;
        }
        return false;
    }

    private boolean isOpened() {
        if (this.state == SERIAL_PORT_STATE_OPENED) {
            return true;
        }
        return false;
    }

    private boolean isClosed() {
        if (this.state == SERIAL_PORT_STATE_CLOSED) {
            return true;
        }
        return false;
    }

    private boolean isReleased() {
        if (this.state == SERIAL_PORT_STATE_RELEASED) {
            return true;
        }
        return false;
    }

    private boolean isReadable() {
        if (this.isOpened() && this.serialPort != null && this.serialPort.inputStream() != null) {
            return true;
        }
        return false;
    }

    private boolean isWriteable() {
        if (this.isOpened() && this.serialPort != null && this.serialPort.outputStream() != null) {
            return true;
        }
        return false;
    }

    private synchronized void onOpen() {
        this.createSerialPort();
        if (this.serialPort != null) {
            this.createReadThread();
            if (this.listener != null) {
                this.listener.get().onConnected(this);
            }
        } else {
            LogUtils.d(TAG, "this.serialPort ==null");
            this.onException();
        }
    }

    private synchronized void onException() {
        this.destoryReadThread();
        this.destorySerialPort();
        if (!isClosed() && !isReleased()) {
            if (this.listener != null) {
                this.listener.get().onException(this);
            }
            this.mOptionHandler.sendEmptyMessageDelayed(SERIAL_PORT_STATE_OPENED, 3000);
        }
    }

    private synchronized void onClose() {
        this.destoryReadThread();
        this.destorySerialPort();
    }

    private synchronized void onRelease() {
        this.destoryReadThread();
        this.destorySerialPort();
        this.destoryProcessHandler();
    }

    private void changeSerialPortState(int state) {
        if (this.state != state) {
            this.state = state;
            this.mOptionHandler.sendEmptyMessage(this.state);
        }
    }

    private void createReadThread() {
        if (this.mReadThread == null) {
            this.mReadThread = new ReadThread();
            this.mReadThread.start();
        }
    }

    private void destoryReadThread() {
        if (this.mReadThread != null) {
            this.mReadThread.finish();
            this.mReadThread = null;
        }
    }

    private void createSerialPort() {
        if (this.serialPort == null) {
            this.serialPort = new SerialPort.Builder()
                    .path(this.path)
                    .parity(this.parity)
                    .stopBits(this.stopBits)
                    .dataBits(this.dataBits)
                    .baudRate(this.baudRate)
                    .flowControl(this.flowControl)
                    .build();
        }
    }

    private void destorySerialPort() {
        if (this.serialPort != null) {
            this.serialPort.release();
            this.serialPort = null;
        }
    }

    private void createProcessHandler() {
        if (this.mHandlerThread == null && this.mOptionHandler == null) {
            mHandlerThread = new HandlerThread("Process-Thread-" + this.name);
            mHandlerThread.start();
            mOptionHandler = new OptionHandler(mHandlerThread.getLooper());
        }
    }

    private void destoryProcessHandler() {
        if (this.mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
            mOptionHandler = null;
        }
    }

    private class OptionHandler extends Handler {
        public OptionHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SERIAL_PORT_STATE_IDLE:
                    break;
                case SERIAL_PORT_STATE_OPENED:
                    if (!isClosed() && !isReleased()) {
                        SerialPortManager.this.onOpen();
                    }
                    break;
                case SERIAL_PORT_STATE_CLOSED:
                    SerialPortManager.this.onClose();
                    break;
                case SERIAL_PORT_STATE_RELEASED:
                    SerialPortManager.this.onRelease();
                    break;
                default:
                    break;
            }
        }
    }

    private class ReadThread extends Thread {
        private int times = 0;
        private boolean finished = false;

        public ReadThread() {

        }

        public void finish() {
            this.finished = true;
        }

        private void checkTimeout() throws IOException {
            this.times++;
            if (timeout == 0) {
                this.times = 0;
                return;
            }
            if (this.times >= timeout) {
                throw new IOException("SerialPort(" + name + ") read timeout");
            }
        }

        @Override
        public void run() {
            super.run();
            try {
                byte[] buffer = new byte[256];
                while (!finished && isReadable()) {
                    int ret = serialPort.select();
                    if (ret == 0) {
                        this.checkTimeout();
                        continue;
                    }
                    this.times = 0;
                    if (!isReadable()) {
                        break;
                    }
//                    sleep(50);
                    int length = serialPort.inputStream().read(buffer);
                    if (listener != null) {
                        listener.get().onByteReceived(SerialPortManager.this, buffer, length);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                SerialPortManager.this.onException();
            }
        }
    }
}
