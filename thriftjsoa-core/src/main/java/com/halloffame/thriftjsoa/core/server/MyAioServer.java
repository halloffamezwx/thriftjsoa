package com.halloffame.thriftjsoa.core.server;

import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;

public class MyAioServer {

    public static void main(String[] args) throws Exception {
        //AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withCachedThreadPool(Executors.newCachedThreadPool(), 10);
        //AsynchronousServerSocketChannel serverChannel1 = AsynchronousServerSocketChannel.open(asynchronousChannelGroup).bind(new InetSocketAddress(8080));

        AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(8080));
        serverChannel.accept(ByteBuffer.allocate(4), new CompletionHandler<AsynchronousSocketChannel, ByteBuffer>() {

            @Override
            public void completed(AsynchronousSocketChannel clientChannel, ByteBuffer byteBuffer) {
                serverChannel.accept(ByteBuffer.allocate(4), this);

                clientChannel.read(byteBuffer, byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                    @SneakyThrows
                    @Override
                    public void completed(Integer i, ByteBuffer byteBuffer) {
                        if (i == -1) {
                            clientChannel.close();
                            return;
                        }

                        if (byteBuffer.position() < 4) {
                            clientChannel.read(byteBuffer, byteBuffer, this);
                        } else {
                            if (byteBuffer.limit() <= 4) {
                                byteBuffer = ByteBuffer.allocate(4 + byteBuffer.getInt(0)).put(byteBuffer);
                            }
                            if (byteBuffer.remaining() > 0) {
                                //可以分批读取body，读完每一批copy合并到attachment
                                clientChannel.read(byteBuffer, byteBuffer, this);
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
                    }
                });
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                exc.printStackTrace();
                serverChannel.accept(ByteBuffer.allocate(4), this);
            }
        });

        System.in.read();
        serverChannel.close();
        //asynchronousChannelGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

}


