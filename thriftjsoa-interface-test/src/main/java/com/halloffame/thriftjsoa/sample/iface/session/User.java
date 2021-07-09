package com.halloffame.thriftjsoa.sample.iface.session;

import lombok.Data;

/**
 * 用户（session）
 * @author zhuwx
 */
@Data
public class User {

    /**
     * id
     */
    private int id;

    /**
     * 名称
     */
    private String name;
}
