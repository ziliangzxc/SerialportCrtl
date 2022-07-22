//
//

#include "SerialPortOptions.h"
#include "SerialPort.h"
#include <android/log.h>

extern "C"
JNIEXPORT jlong JNICALL
Java_com_meidical_meigeserialport_SerialPort_open(JNIEnv *env, jobject thiz, jstring path,
                                                  jint baudrate, jint stopbits, jint databits,
                                                  jint parity, jint flow_control) {
    SerialPort *serial = new SerialPort();
    serial->setParity(parity);
    serial->setBaudrate(baudrate);
    serial->setDataBits(databits);
    serial->setStopBits(stopbits);
    serial->setFlowControl(flow_control);
    const char *path_utf = env->GetStringUTFChars(path, 0);
    serial->setFilePath(path_utf);
    env->ReleaseStringUTFChars(path, path_utf);
    if (serial->serialPortOpen() == -1) {
        delete serial;
        return -1;
    }
    return (jlong) serial;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_meidical_meigeserialport_SerialPort_select(JNIEnv *env, jobject thiz, jlong serial_port) {
    // TODO: implement select()
    SerialPort *serial = (SerialPort *) serial_port;
    return serial->serialPortSelect();
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_meidical_meigeserialport_SerialPort_descriptor(JNIEnv *env, jobject thiz,
                                                        jlong serial_port) {
    SerialPort *serial = (SerialPort *) serial_port;
    jclass cFileDescriptor = env->FindClass("java/io/FileDescriptor");
    jmethodID iFileDescriptor = env->GetMethodID(cFileDescriptor, "<init>", "()V");
    jfieldID descriptorID = env->GetFieldID(cFileDescriptor, "descriptor", "I");
    jobject fileDescriptor = env->NewObject(cFileDescriptor, iFileDescriptor);
    env->SetIntField(fileDescriptor, descriptorID, (jint) serial->getFd());
    return fileDescriptor;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_meidical_meigeserialport_SerialPort_close(JNIEnv *env, jobject thiz, jlong serial_port) {
    SerialPort *serial = (SerialPort *) serial_port;
    serial->serialPortClose();
    delete serial;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_meidical_meigeserialport_SerialPort_writeFile(JNIEnv *env, jclass clazz, jstring path,
                                                       jstring content) {
    jboolean iscopy;
    const char *path_utf = env->GetStringUTFChars(path, &iscopy);
    int fd = open(path_utf, O_CREAT | O_RDWR, 1);
    if (fd == -1) {
        LOGE("Cannot open file (%s) %d", path_utf, errno);
        return;
    }
    const char *content_utf = env->GetStringUTFChars(content, &iscopy);
    int len = write(fd, content_utf, strlen(content_utf));
    env->ReleaseStringUTFChars(content, content_utf);
    if (len == -1) {
        LOGE("Cannot write file (%s) %d", path_utf, errno);
    }
    env->ReleaseStringUTFChars(path, path_utf);
    close(fd);
}