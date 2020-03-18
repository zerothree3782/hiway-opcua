package com.hhi.hiwayopcua.core;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;

import java.util.List;

public interface HiwayOpcuaListner {

    default public void onReadData(List<DataValue> dataValues){};
    default public void onSubscriptionDataChange(List<UaMonitoredItem> monitoredItems, List<DataValue> dataValues, DateTime publishTime){};
    default public void onReadTag(List<ReferenceDescription> referenceDescriptionList,List<String> identList){};
}
