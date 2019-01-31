package com.halloffame.thriftjsoa.common;

/**
 * 服务模式
 */
public enum ServerType {
    SIMPLE("simple", "单线程阻塞io"),
    THREAD_POOL("thread-pool", "多线程（池）阻塞io"),
    NONBLOCKING("nonblocking", "单条线程非阻塞io"),
    THREADED_SELECTOR("threaded-selector", "非阻塞io，有一条线程专门负责accept，若干条Selector线程处理网络IO，一个Worker线程池处理消息");

    private String value;
    private String desc;

    ServerType(String value, String desc) {
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
