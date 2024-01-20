package com.halloffame.thriftjsoa.core.client;

import com.halloffame.thriftjsoa.core.server.MyNioServer;
import lombok.Data;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

public class MyNioClient {

    public static void main(String[] args) throws Exception {
        Selector selector = Selector.open();

        SocketChannel outChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 9090));
        outChannel.configureBlocking(false);
        SelectionKey ocKey = outChannel.register(selector, SelectionKey.OP_WRITE);

        String body = "test";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ocKey.attach(bytes);

        while (true) {
            // 等待某个信道就绪
            if( selector.select(3000) == 0 ){
                System.out.println(".");
                continue;
            }

            // 获得就绪信道的键迭代器
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            // 使用迭代器进行遍历就绪信道
            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();
                SocketChannel channel = (SocketChannel) key.channel();

                if (key.isValid() && key.isWritable()) {
                    byte[] writeBytes = (byte[]) key.attachment();

                    ByteBuffer buffer = ByteBuffer.allocate(4 + writeBytes.length);
                    buffer.putInt(writeBytes.length);
                    buffer.put(writeBytes);
                    // 将缓冲区准备为数据传出状态
                    buffer.flip();
                    channel.write(buffer);

                    key.attach(new MyNioServer.Attachment());
                    key.interestOps(SelectionKey.OP_READ);
                }

                if (key.isValid() && key.isReadable()) {
                    MyNioServer.Attachment attachment = (MyNioServer.Attachment) key.attachment();

                    if (attachment.getLength() <= 0) {
                        if (attachment.getLengthBuffer().position() < 4) {
                            long bytesRead = channel.read(attachment.getLengthBuffer());
                            if (bytesRead == -1) {
                                channel.close();
                            }
                        } else {
                            attachment.setLength(attachment.getLengthBuffer().getInt(0));
                            attachment.setBoydBuffer(ByteBuffer.allocate(attachment.getLength()));
                            key.attach(attachment);
                        }
                        key.interestOps(SelectionKey.OP_READ);

                    } else {
                        if (attachment.getBoydBuffer().remaining() == 0) {
                            System.out.println(Arrays.toString(attachment.getBoydBuffer().array()));
                            //key.interestOps(SelectionKey.OP_WRITE);
                            //channel.write();
                        } else {
                            //可以分批读取body，读完每一批copy合并到attachment
                            long bytesRead = channel.read(attachment.getBoydBuffer());
                            if (bytesRead == -1) {
                                channel.close();
                            }
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    }
                }

                keyIter.remove();
            }
        }
    }

    @Data
    public static class Attachment {
        private int length;
        private ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        private ByteBuffer boydBuffer;
    }

}
