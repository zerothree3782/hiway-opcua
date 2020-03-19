package com.hhi.hiwayopcua;

import com.hhi.hiwayopcua.core.ClientBaseRunner;
import com.hhi.hiwayopcua.properties.OpcuaProperties;
import com.hhi.hiwayopcua.properties.OpcuaPropertiesList;
import com.hhi.hiwayopcua.service.BrowseBase;
import com.hhi.hiwayopcua.service.InfluxService;
import com.hhi.hiwayopcua.service.ReadBase;
import com.hhi.hiwayopcua.service.SubscriptionBase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

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

            //root태그 밑의 모든 태그를 가져오는 클래스
            BrowseBase browseBase = new BrowseBase();
            browseBase.setOpcuaProperties(opcuaProperties);
//            browseBase.addHiwayOpcuaListner(new HiwayOpcuaListner() {
//                @Override
//                public void onReadTag(List<ReferenceDescription> referenceDescriptionList, List<String> identList) {
//                    //받은 태그로 수행할 서비스 추가하지 않을 경우 기본으로 로그를 찍습니다.
//                }
//            });
            new ClientBaseRunner(browseBase).run();

            String mode = opcuaProperties.getMode();
            if(mode.equals("polling")){
                //polling방식으로 데이터를 가져오는 클래스
                ReadBase readBase = new ReadBase();
                readBase.setOpcuaProperties(opcuaProperties);
//                readBase.addHiwayOpcuaListner(new HiwayOpcuaListner() {
//                    @Override
//                    public void onReadData(List<DataValue> dataValues) {
//                        //가져온 데이터로 수행할 서비스 추가하지 않을 경우 기본으로 로그를 찍습니다.
//                    }
//                });
                new ClientBaseRunner(readBase).run();

            }else if(mode.equals("subscription")){
                //subscription방식으로 데이터를 가져오는 클래스
                SubscriptionBase subscriptionExample = new SubscriptionBase();
                subscriptionExample.setOpcuaProperties(opcuaProperties);
//                subscriptionExample.addHiwayOpcuaListner(new HiwayOpcuaListner() {
//                    @Override
//                    public void onSubscriptionDataChange(List<UaMonitoredItem> monitoredItems, List<DataValue> dataValues, DateTime publishTime) {
//                        //가져온 데이터로 수행할 서비스 추가하지 않을 경우 기본으로 로그를 찍습니다.
//                        influxService.opcUaInsertList(monitoredItems,dataValues,opcuaProperties.getServerName());
//                    }
//                });
                new ClientBaseRunner(subscriptionExample).run();

            }
        }
    }
}
