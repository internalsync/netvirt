/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netvirt.elan.l2gw.ha.listeners;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;


public class HAJobScheduler {

    ExecutorService executorService;

    static HAJobScheduler instance = new HAJobScheduler();

    private HAJobScheduler() {
        ThreadFactory threadFact = new ThreadFactoryBuilder()
                    .setNameFormat("hwvtep-ha-task-%d").build();
        executorService = Executors.newSingleThreadScheduledExecutor(threadFact);
    }

    public static HAJobScheduler getInstance() {
        return instance;
    }

    public void setThreadPool(ExecutorService service) {
        executorService = service;
    }

    public Future<Void> submitJob(Callable<Void> callable) {
        return executorService.submit(callable);
    }

    public void submitJob(Runnable runnable) {
        executorService.submit(runnable);
    }
}
