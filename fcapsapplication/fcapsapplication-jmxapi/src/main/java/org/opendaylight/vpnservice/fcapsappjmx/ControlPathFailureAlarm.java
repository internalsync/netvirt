/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.vpnservice.fcapsappjmx;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import java.util.ArrayList;

public class ControlPathFailureAlarm extends NotificationBroadcasterSupport implements ControlPathFailureAlarmMBean {

    ArrayList<String> raiseAlarmObject = new ArrayList<String>();
    ArrayList<String> clearAlarmObject = new ArrayList<String>();
    private long sequenceNumber = 1;

    public void setRaiseAlarmObject(ArrayList<String> raiseAlarmObject) {
        this.raiseAlarmObject = raiseAlarmObject;

        Notification notif = new AttributeChangeNotification(this,
                sequenceNumber++, System.currentTimeMillis(),
                "raise alarm object notified ", "raiseAlarmObject", "ArrayList",
                "", this.raiseAlarmObject);
        sendNotification(notif);
    }

    public ArrayList<String> getRaiseAlarmObject() {
        return raiseAlarmObject;
    }

    public void setClearAlarmObject(ArrayList<String> clearAlarmObject) {
        this.clearAlarmObject = clearAlarmObject;

        Notification notif = new AttributeChangeNotification(this,
                sequenceNumber++, System.currentTimeMillis(),
                "clear alarm object notified ", "clearAlarmObject", "ArrayList",
                "", this.clearAlarmObject);
        sendNotification(notif);
    }

    public ArrayList<String> getClearAlarmObject() {
        return clearAlarmObject;
    }

    public synchronized void raiseAlarm(String alarmName, String additionalText, String source) {
        raiseAlarmObject.add(alarmName);
        raiseAlarmObject.add(additionalText);
        raiseAlarmObject.add(source);
        setRaiseAlarmObject(raiseAlarmObject);
        raiseAlarmObject.clear();
    }

    public synchronized void clearAlarm(String alarmName, String additionalText, String source) {
        clearAlarmObject.add(alarmName);
        clearAlarmObject.add(additionalText);
        clearAlarmObject.add(source);
        setClearAlarmObject(clearAlarmObject);
        clearAlarmObject.clear();
    }
}
