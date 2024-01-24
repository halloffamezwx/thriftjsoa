package com.halloffame.thriftjsoa.core.server;

import lombok.Data;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;

public class MyNioServer {

    public static void execute() throws Exception {
        // 创建一个在本地端口进行监听的服务Socket信道并设置为非阻塞方式
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(new InetSocketAddress(4567));
        serverChannel.configureBlocking(false);

        // 创建一个选择器并将serverChannel注册到它上面
        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            // 等待某个信道就绪
            if( selector.select(3000) == 0 ){
                System.out.println(".");
                continue;
            }

            // 获得就绪信道的键迭代器
            Iterator<SelectionKey> keyIt = selector.selectedKeys().iterator();
            // 使用迭代器进行遍历就绪信道
            while (keyIt.hasNext()) {
                SelectionKey key = keyIt.next();

                // 这种情况是有客户端连接过来，准备一个Channel与之通信
                if (key.isValid() && key.isAcceptable()) {
                    SocketChannel inChannel = ((ServerSocketChannel) key.channel()).accept();
                    inChannel.configureBlocking(false);
                    SelectionKey selectionKey = inChannel.register(key.selector(), SelectionKey.OP_READ);
                    selectionKey.attach(new MyNioServer.Attachment());
                }

                // 客户端有写入时
                if (key.isValid() && key.isReadable()) {
                    // 获得与客户端通信的信道
                    SocketChannel channel = (SocketChannel) key.channel();
                    MyNioServer.Attachment attachment = (MyNioServer.Attachment) key.attachment();

                    if (attachment.getLength() <= 0) {
                        if (attachment.getLengthBuffer().position() < 4) {
                            long bytesRead = channel.read(attachment.getLengthBuffer());
                            if (bytesRead == -1) {
                                channel.close();
                            } else {
                                key.interestOps(SelectionKey.OP_READ);
                            }
                        } else {
                            attachment.setLength(attachment.getLengthBuffer().getInt(0));
                            attachment.setBoydBuffer(ByteBuffer.allocate(attachment.getLength()));
                            key.attach(attachment);
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    } else {
                        if (attachment.getBoydBuffer().remaining() == 0) {
                            System.out.println(Arrays.toString(attachment.getBoydBuffer().array()));
                            key.interestOps(SelectionKey.OP_WRITE);
                            //channel.write();
                        } else {
                            //可以分批读取body，读完每一批copy合并到attachment
                            long bytesRead = channel.read(attachment.getBoydBuffer());
                            if (bytesRead == -1) {
                                channel.close();
                            } else {
                                key.interestOps(SelectionKey.OP_READ);
                            }
                        }
                    }
                }

                if (key.isValid() && key.isWritable()) {

                }

                keyIt.remove();
            }
        }
    }

    @Data
    public static class Attachment {
        private int length;
        private ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        private ByteBuffer boydBuffer;
    }

    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(8);

        //buffer.putInt(5000);
        buffer.put((byte) 12);
        buffer.put((byte) 12);
        System.out.println(buffer.getInt(0));
        //System.out.println(buffer.getInt(0));
        //System.out.println(buffer.get(3));
        //System.out.println(buffer.get(7));
        System.out.println(buffer.remaining());
        System.out.println(buffer.position());
        System.out.println(buffer.limit());

        System.out.println(Arrays.toString(buffer.array()));

        byte[] bytes = new byte[0];
        System.out.println(Arrays.toString(bytes));
        System.out.println(bytes.length);
    }

}
