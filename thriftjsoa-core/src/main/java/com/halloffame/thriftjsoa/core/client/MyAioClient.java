package com.halloffame.thriftjsoa.core.client;

import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class MyAioClient {

    public static void main(String[] args) throws Exception {
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open();
        String bodyStr = "test";
        byte[] bodyBytes = bodyStr.getBytes(StandardCharsets.UTF_8);

        socketChannel.connect(new InetSocketAddress("127.0.0.1", 8000), bodyBytes, new CompletionHandler<Void, byte[]>() {
            @SneakyThrows
            @Override
            public void completed(Void result, byte[] bodyBytes) {
                ByteBuffer buffer = ByteBuffer.allocate(4 + bodyBytes.length);
                buffer.putInt(bodyBytes.length);
                buffer.put(bodyBytes);
                //Integer i = socketChannel.write(ByteBuffer.wrap(bodyBytes)).get();

                socketChannel.write(buffer, 1000, TimeUnit.MILLISECONDS, null, new CompletionHandler<Integer, Object>() {
                    @SneakyThrows
                    @Override
                    public void completed(Integer result, Object attachment) {
                        if (result == -1) {
                            socketChannel.close();
                            return;
                        }
                        //ByteBuffer buffer = ByteBuffer.allocate(4);
                        //Integer len = socketChannel.read(buffer).get();
                        //if (len == -1) { }
                        ByteBuffer byteBuffer = ByteBuffer.allocate(4);

                        socketChannel.read(byteBuffer, 1000, TimeUnit.MILLISECONDS, byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                            @SneakyThrows
                            @Override
                            public void completed(Integer i, ByteBuffer byteBuffer) {
                                if (i == -1) {
                                    socketChannel.close();
                                    return;
                                }

                                if (byteBuffer.position() < 4) {
                                    socketChannel.read(byteBuffer, byteBuffer, this);
                                } else {
                                    if (byteBuffer.limit() <= 4) {
                                        byteBuffer = ByteBuffer.allocate(4 + byteBuffer.getInt(0)).put(byteBuffer);
                                    }
                                    if (byteBuffer.remaining() > 0) {
                                        //可以分批读取body，读完每一批copy合并到attachment
                                        socketChannel.read(byteBuffer, byteBuffer, this);
                                    } else {
                                        byteBuffer.getInt();
                                        System.out.println(Arrays.toString(byteBuffer.array()));
                                        //clientChannel.write(ByteBuffer.allocate(4)).get();
                                    }
                                }
                            }

                            @SneakyThrows
                            @Override
                            public void failed(Throwable exc, ByteBuffer byteBuffer) {
                                exc.printStackTrace();
                                socketChannel.read(byteBuffer, byteBuffer, this);
                            }
                        });
                    }

                    @Override
                    public void failed(Throwable exc, Object attachment) {
                        exc.printStackTrace();
                        //socketChannel.write(buffer, 1000, TimeUnit.MILLISECONDS, null, this);
                    }
                });
            }

            @Override
            public void failed(Throwable exc, byte[] attachment) {
                exc.printStackTrace();
            }
        });
    }

}
