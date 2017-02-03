/*-
 * #%L
 * che-starter
 * %%
 * Copyright (C) 2017 Red Hat, Inc.
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package io.fabric8.planner.che.starter.client;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;

import io.fabric8.planner.che.starter.model.Workspace;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class CheRestClientTest {

    private static final Logger LOG = LogManager.getLogger(CheRestClientTest.class);

    @Autowired
    private CheRestClient client;

    @Test
    public void listWorkspaces() {
        List<Workspace> workspaces = this.client.listWorkspaces();
        LOG.info("Number of workspaces: {}", workspaces.size());
        workspaces.forEach(w -> LOG.info("workspace ID: {}", w.getId()));
        List<Workspace> runningWorkspaces = workspaces.stream().filter(w -> w.getStatus().equals("RUNNING"))
                .collect(Collectors.toList());
        assertFalse(workspaces.isEmpty());
        assertTrue(CollectionUtils.isEqualCollection(workspaces, runningWorkspaces));
    }

    @Test(expected = HttpClientErrorException.class)
    public void createAndStartWorkspace() {
        client.createAndStartWorkspace();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void stopAllWorskpaces() {
        client.stopAllWorkspaces();
    }

    @Test
    public void stopWorskpace() {
        List<Workspace> workspaces = client.listWorkspaces();
        if (!workspaces.isEmpty()) {
            client.stopWorkspace(workspaces.get(0).getId());
        }
    }

}
