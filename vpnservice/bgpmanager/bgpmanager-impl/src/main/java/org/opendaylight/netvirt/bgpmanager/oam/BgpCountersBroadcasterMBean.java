/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netvirt.bgpmanager.oam;

import java.util.Map;

/**
 * Created by echiapt on 9/28/2015.
 */
public interface BgpCountersBroadcasterMBean {
    Map<String, String> retrieveCounterMap();
}

