package com.hhi.hiwayopcua;

import com.hhi.hiwayopcua.core.ClientBaseRunner;
import com.hhi.hiwayopcua.core.HiwayOpcuaListner;
import com.hhi.hiwayopcua.properties.OpcuaProperties;
import com.hhi.hiwayopcua.properties.OpcuaPropertiesList;
import com.hhi.hiwayopcua.service.BrowseBase;
import com.hhi.hiwayopcua.service.InfluxService;
import com.hhi.hiwayopcua.service.ReadBase;
import com.hhi.hiwayopcua.service.SubscriptionBase;
import lombok.RequiredArgsConstructor;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties
public class Runner implements ApplicationRunner {

    private final OpcuaPropertiesList opcuaPropertiesList;
    private final InfluxService influxService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

//        StandardPBEStringEncryptor jasypt = new StandardPBEStringEncryptor();
//        jasypt.setPassword("pwkey");
//        jasypt.setAlgorithm("PBEWithMD5AndDES");
//
//        String encryptedText = jasypt.encrypt("hhi");
//        String plainText = jasypt.decrypt(encryptedText);
//        System.out.println("encryptedText:  " + encryptedText);
//        System.out.println("plainText:  " + plainText);

        for(OpcuaProperties opcuaProperties:opcuaPropertiesList.getServerList()){
            BrowseBase browseBase = new BrowseBase();
            browseBase.setOpcuaProperties(opcuaProperties);
            browseBase.addHiwayOpcuaListner(new HiwayOpcuaListner() {
                @Override
                public void onReadTag(List<ReferenceDescription> referenceDescriptionList, List<String> identList) {

                }
            });

            new ClientBaseRunner(browseBase).run();

            String mode = opcuaProperties.getMode();
            if(mode.equals("polling")){
                ReadBase readBase = new ReadBase();
                readBase.setOpcuaProperties(opcuaProperties);
                new ClientBaseRunner(readBase).run();
            }else if(mode.equals("subscription")){
                SubscriptionBase subscriptionExample = new SubscriptionBase();

                subscriptionExample.setOpcuaProperties(opcuaProperties);
                new ClientBaseRunner(subscriptionExample).run();
            }
        }

        System.out.println("AsyncTest");
    }
}
