package com.halloffame.thriftjsoa.core.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MyBioClient {

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("127.0.0.1", 9090);
        InputStream is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();

        //System.out.println("send:" + new String(buffer, 0, temp, StandardCharsets.UTF_8));
        //可以循环发起连续的请求
        String bodyStr = "test";
        byte[] bodyBytes = bodyStr.getBytes(StandardCharsets.UTF_8);

        //System.arraycopy();
        os.write(bodyBytes.length);
        os.write(bodyBytes);

        byte[] readLengthBytes = readBytes(4, is);
        if (readLengthBytes != null) {
            byte[] readBodyBytes = readBytes(ByteBuffer.wrap(readLengthBytes).getInt(), is);
            if (readBodyBytes != null) {
                //
            }
        }
        closeAll(socket,  is,  os);
    }

    public static byte[] readBytes(int length, InputStream is) throws Exception {
        if(length < 0){
            //return null;
            //return new byte[0];
            throw new RuntimeException("length不合法");
        }
        byte[] buffer = new byte[length];
        for (int i = 0; i < length;) {
            int readLength = is.read(buffer, i, length - i);
            if (readLength == -1) {
                //break;
                return null;
            }
            i += readLength;
        }
        return buffer;
    }

    private static void closeAll(Socket socket, InputStream is, OutputStream os) {
        try {
            if (is != null) is.close();
            if (os != null) {
                os.flush();
                os.close();
            }
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
