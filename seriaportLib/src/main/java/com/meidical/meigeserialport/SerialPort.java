package com.meidical.meigeserialport;


import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.meidical.utils.LogUtils;

/**
 * @date 2020/6/30  16:54
 * @descprition 打开串口 调native方法
 */
public class SerialPort {

    private static final String TAG = "SerialPort";
    private long serialPort;
    /**
     * released
     */
    private boolean released = false;
    private FileDescriptor descriptor;
    private FileInputStream inputStream;
    private FileOutputStream outputStream;

    static {
        System.loadLibrary("orderly-port");
    }

    /**
     * serialPort
     *
     * @param device      串口
     * @param baudrate    波特率
     * @param stopbits    停止位
     * @param databits    数据位
     * @param parity      奇偶校验
     * @param flowControl 流控
     * @throws IOException
     */
    private SerialPort(File device, int baudrate, int stopbits, int databits, int parity, int flowControl) throws IOException {
        // native  open   -1打开失败
        this.serialPort = this.open(device.getAbsolutePath(), baudrate, stopbits, databits, parity, flowControl);
        LogUtils.d(TAG, "SerialPort:" + serialPort);
        if (this.serialPort < 0) {
            throw new IOException("Serial port open failed");
        }
        //native  获取FileDescriptor   获取输入输出流
        this.descriptor = this.descriptor(this.serialPort);
        if (this.descriptor == null) {
            throw new IOException("FileDescriptor is null");
        }
        inputStream = new FileInputStream(this.descriptor);
        LogUtils.d(TAG, "SerialPort   (inputStream==null)   : " + (inputStream == null));
        outputStream = new FileOutputStream(this.descriptor);
        LogUtils.d(TAG, "SerialPort   (outputStream==null)   : " + (outputStream == null));
    }

    public int select() throws IOException {
        if (this.released) {
            throw new IOException("Serial port released");
        }
        int ret = this.select(this.serialPort);
        if (ret < 0) {
            throw new IOException("Serial port select error");
        }
        return ret;
    }

    public FileInputStream inputStream() {
        return this.inputStream;
    }

    public FileOutputStream outputStream() {
        return this.outputStream;
    }

    /**
     * 释放
     */
    public void release() {
        this.released = true;
        if (this.serialPort != 0) {
//            close串口
            this.close(this.serialPort);
            this.serialPort = 0;
        }
        if (this.inputStream != null) {
            try {
//                close输入流
                this.inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                this.inputStream = null;
            }
        }
        if (this.outputStream != null) {
            try {
//                close输出流
                this.outputStream.close();
            } catch (Exception e) {
                LogUtils.d(TAG, "Release SerialPort Error:" + e.getMessage());
                e.printStackTrace();
            } finally {
                this.outputStream = null;
            }
        }
    }

    public native int select(long serialPort);

    public native static void writeFile(String path, String content);

    private native long open(String path, int baudrate, int stopbits, int databits, int parity, int flowControl); //打开串口

    private native FileDescriptor descriptor(long serialPort); //打开串口

    private native void close(long serialPort); //关闭串口


    public static class Builder {
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

        public Builder() {

        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder parity(int parity) {
            this.parity = parity;
            return this;
        }

        public Builder stopBits(int stopBits) {
            this.stopBits = stopBits;
            return this;
        }

        public Builder dataBits(int dataBits) {
            this.dataBits = dataBits;
            return this;
        }

        public Builder baudRate(int baudRate) {
            this.baudRate = baudRate;
            return this;
        }

        public Builder flowControl(int flowControl) {
            this.flowControl = flowControl;
            return this;
        }

        public SerialPort build() {
            File device = new File(path);
            if (!checkDevice(device)) {
                return null;
            }
            try {
                return new SerialPort(
                        device,
                        this.baudRate,
                        this.stopBits,
                        this.dataBits,
                        this.parity,
                        this.flowControl
                );
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private boolean checkDevice(File device) {
            if (device.canRead() && device.canWrite()) {
                return true;
            }
            return chmodDevice(device);
        }

        private boolean chmodDevice(File device) {
            try {
                Process su = Runtime.getRuntime().exec("/system/bin/su");
                String cmd = "chmod 777 " + device.getAbsolutePath() + "\n" + "exit\n";
                su.getOutputStream().write(cmd.getBytes());
                if ((su.waitFor() != 0) || !device.canRead() || !device.canWrite()) {
                    return false;
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
