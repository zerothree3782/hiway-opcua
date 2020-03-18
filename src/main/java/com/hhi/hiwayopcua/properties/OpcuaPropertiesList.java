package com.hhi.hiwayopcua.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "opcua")
public class OpcuaPropertiesList {
    private List<OpcuaProperties> serverList;
}
