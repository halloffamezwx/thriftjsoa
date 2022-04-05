package com.halloffame.thriftjsoa.constant;

import lombok.Getter;
import lombok.ToString;

/**
 * 服务模式
 * @author zhuwx
 */
@Getter
@ToString
public enum ServerType {

    HTTP_TOMCAT("http-tomcat", "tomcat实现的http"),
    SIMPLE("simple", "单线程阻塞io"),
    THREAD_POOL("thread-pool", "多线程（池）阻塞io"),
    NONBLOCKING("nonblocking", "单条线程非阻塞io"),
    THREADED_SELECTOR("threaded-selector", "非阻塞io，有一条线程专门负责accept，若干条Selector线程处理网络IO，一个Worker线程池处理消息");

    /**
     * 编码
     */
    private String code;

    /**
     * 描述
     */
    private String desc;

    ServerType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
