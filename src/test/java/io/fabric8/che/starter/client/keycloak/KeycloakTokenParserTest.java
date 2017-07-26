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
package io.fabric8.che.starter.client.keycloak;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.fabric8.che.starter.TestConfig;

/**
 * PAYLOAD:DATA used in the test:
 *
 * {
        "sub": "1234567890",
        "name": "John Doe",
        "admin": true
    }
 * @see <a href="https://jwt.io/">https://jwt.io/</a>
 */
public class KeycloakTokenParserTest extends TestConfig {
    private static final Logger LOG = LoggerFactory.getLogger(KeycloakTokenParserTest.class);
    private static final String AUTH_HEADER = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";
    private static final String SUB = "1234567890";

    @Autowired
    KeycloakTokenParser parser;

    @Test
    public void getIdentityId() throws JsonProcessingException, IOException {
        String identityId = parser.getIdentityId(AUTH_HEADER);
        LOG.info("Identity ID: {}", identityId);
        assertEquals(identityId, SUB);
    }
}