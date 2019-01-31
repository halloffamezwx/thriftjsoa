package com.halloffame.thriftjsoa.common;

/**
 * 通信方式
 */
public enum TransportType {
    BUFFERED("buffered", "使用经典的缓冲流Socket"),
    FRAMED("framed", "基于帧的方式的Socket，每个帧都是按照4字节的帧长加上帧的内容来组织，帧内容就是我们要收发的数据。读的时候按长度预先将整Frame数据读入Buffer，再从Buffer慢慢读取。写的时候，每次flush将Buffer中的所有数据写成一个Frame。framed这种方式有点类似于http协议的chunked编码"),
    FASTFRAMED("fastframed", "和framed相比是内存利用率更高的一个内存读写缓存区，它使用自动增长的byte[](不够长度才new)，而不是每次都new一个byte[]，提高了内存的使用率。framed的ReadBuffer每次读入Frame时都会创建新的byte[]，WriteBuffer每次flush时如果大于初始1K也会重新创建byte[]"),
    HTTP("http", "http，服务端java的依赖servlet");

    private String value;
    private String desc;

    TransportType(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return value;
    }

    public String getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

}
