/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.hhi.hiwayopcua.service;

import com.hhi.hiwayopcua.core.ClientBase;
import com.hhi.hiwayopcua.core.HiwayOpcuaListner;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class SubscriptionBase extends ClientBase {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private HiwayOpcuaListner hiwayOpcuaListner = new HiwayOpcuaListner() {
        @Override
        public void onSubscriptionDataChange(List<UaMonitoredItem> monitoredItems, List<DataValue> dataValues, DateTime publishTime) {
            for(int i = 0; i < monitoredItems.size(); i++){
                logger.info(
                        "subscription value received: item={}, value={}",
                        monitoredItems.get(i).getReadValueId().getNodeId(), dataValues.get(i).getValue());
            }
        }
    };

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        // synchronous connect
        client.connect().get();

        //subscription중 연결이 끊겼다가 다시 연결 되었을때  subscription을 재정의 해줘야 한다.
        client.getSubscriptionManager().addSubscriptionListener(new UaSubscriptionManager.SubscriptionListener() {
            @Override
            public void onSubscriptionTransferFailed(UaSubscription subscription, StatusCode statusCode) {
                try {
                    subscriptionConfig(client);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        subscriptionConfig(client);
    }

    public void subscriptionConfig(OpcUaClient client) throws Exception{
        // create a subscription
        long intervalTime = this.getOpcuaProperties().getReadInterval();
        UaSubscription subscription = client.getSubscriptionManager().createSubscription(intervalTime).get();
        subscription.addNotificationListener(new UaSubscription.NotificationListener() {
            @Override
            public void onDataChangeNotification(UaSubscription subscription, List<UaMonitoredItem> monitoredItems, List<DataValue> dataValues, DateTime publishTime) {
                hiwayOpcuaListner.onSubscriptionDataChange(monitoredItems, dataValues, publishTime);
            }
        });

        List<MonitoredItemCreateRequest> request = createRequestList(subscription,intervalTime);

        // when creating items in MonitoringMode.Reporting this callback is where each item needs to have its
        // value/event consumer hooked up. The alternative is to create the item in sampling mode, hook up the
        // consumer after the creation call completes, and then change the mode for all items to reporting.
        BiConsumer<UaMonitoredItem, Integer> onItemCreated = (item, id) -> {};

        List<UaMonitoredItem> items = subscription.createMonitoredItems(
                TimestampsToReturn.Both,
                newArrayList(request),
                onItemCreated
        ).get();

        for (UaMonitoredItem item : items) {
            if (item.getStatusCode().isGood()) {
                logger.info("item created for nodeId={}", item.getReadValueId().getNodeId());
            } else {
                logger.warn(
                        "failed to create item for nodeId={} (status={})",
                        item.getReadValueId().getNodeId(), item.getStatusCode());
            }
        }

    }

    private List<MonitoredItemCreateRequest> createRequestList(UaSubscription subscription,double intervalTime){
        OpcuaCommonService opcuaCommonService = new OpcuaCommonService();
        List<MonitoredItemCreateRequest> requestList = newArrayList();
        List<NodeId> nodeIds = opcuaCommonService.getNodeIdList(this.getOpcuaProperties().getNamespace(),this.getOpcuaProperties().getTagList());

        for(NodeId nodeId:nodeIds) {
            // subscribe to the Value attribute of the server's CurrentTime node
            ReadValueId readValueId = new ReadValueId(
                    nodeId,
                    AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE
            );

            UInteger clientHandle = subscription.nextClientHandle();

            MonitoringParameters parameters = new MonitoringParameters(
                    clientHandle,
                    intervalTime,     // sampling interval
                    null,       // filter, null means use default
                    uint(1),   // queue size
                    true        // discard oldest
            );

            MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
                    readValueId,
                    MonitoringMode.Reporting,
                    parameters
            );

            requestList.add(request);
        }

        return requestList;
    }

    public void addHiwayOpcuaListner(HiwayOpcuaListner hiwayOpcuaListner){
        this.hiwayOpcuaListner = hiwayOpcuaListner;
    }



}
