package com.hhi.hiwayopcua.properties;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OpcuaProperties {
    private String serverName;
    private String ip;
    private int port;
    private String user;
    private String password;
    private int namespace;
    private String rootNode;
    private long readInterval;
    private List<String> tagList;
    private String mode;
}
