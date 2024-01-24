package com.halloffame.thriftjsoa.core.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

public class MyBioServer {

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(4567)) {
            while (true) {
                Socket socket = serverSocket.accept();
                //可改成线程池提升性能
                new SocketThread(socket).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class SocketThread extends Thread {
        private final Socket socket;

        public SocketThread(Socket socket){
            this.socket = socket;
        }

        public void run() {
            //可改成缓冲流BufferedInputStream和BufferedOutputStream提升性能
            InputStream is = null;
            OutputStream os = null;

            try {
                is = socket.getInputStream();
                os = socket.getOutputStream();

                //可以循环读取连续的请求
                byte[] lengthBytes = readBytes(4, is);
                if (lengthBytes != null) {
                    byte[] bodyBytes = readBytes(new BigInteger(lengthBytes).intValue(), is);
                    if (bodyBytes != null) {
                        //os.write();
                        //os.flush();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeAll(socket, is, os);
            }
        }

        private void closeAll(Socket socket, InputStream is, OutputStream os) {
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

        private byte[] readBytes(int length, InputStream is) throws Exception {
            if(length <= 0){
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

        //int转ip(字节数组)
        private byte[] intToByteArrayl(int i) {
            byte[] result = new byte[4];
            result[0] = (byte)((i >> 24) & 0xFF);
            result[1] = (byte)((i >> 16) & 0xFF);
            result[2] = (byte)((i >> 8) & 0xFF);
            result[3] = (byte)(i & 0xFF);
            return result;
        }

        //ip(字节数组)转int
        private int byteArrayToInt(byte[] b) {
            int value = 0;
            for (int i = 0; i < 4; i++) {
                value |= b[i];
                if ( i < 3 ) {
                    value = value << 8;
                }
            }
            return value;
        }
    }

}
