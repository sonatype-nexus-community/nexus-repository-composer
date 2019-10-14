/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2018-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.composer.internal;

import org.sonatype.goodies.httpfixture.server.fluent.Behaviours;
import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

public class ComposerProxyIT
    extends ComposerITSupport
{
  private static final String FORMAT_NAME = "composer";

  private static final String MIME_TYPE_JSON = "application/json";

  private static final String COMPONENT_NAME = "ftp-php";

  private static final String EXTENSION_JSON = ".json";

  private static final String PACKAGE_NAME = COMPONENT_NAME + EXTENSION_JSON;

  private static final String PACKAGE_BASE_PATH = "p/rjkip/";

  private static final String BAD_PATH = "/this/path/is/not/valid";

  private static final String VALID_PACKAGE_URL = PACKAGE_BASE_PATH + PACKAGE_NAME;

  private ComposerClient proxyClient;

  private Repository proxyRepo;

  private Server server;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-composer")
    );
  }

  @Before
  public void setup() throws Exception {
    server = Server.withPort(0)
        .serve("/" + VALID_PACKAGE_URL)
        .withBehaviours(Behaviours.file(testData.resolveFile(PACKAGE_NAME)))
        .start();

    proxyRepo = repos.createComposerProxy("composer-test-proxy", server.getUrl().toExternalForm());
    proxyClient = composerClient(proxyRepo);
  }

  @Test
  public void unresponsiveRemoteProduces404() throws Exception {
    assertThat(status(proxyClient.get(BAD_PATH)), is(HttpStatus.NOT_FOUND));
  }

  @Test
  public void retrieveProjectJSONFromProxyWhenRemoteOnline() throws Exception {
    assertThat(status(proxyClient.get(VALID_PACKAGE_URL)), is(HttpStatus.OK));

    final Asset asset = findAsset(proxyRepo, VALID_PACKAGE_URL);
    assertThat(asset.name(), is(equalTo(VALID_PACKAGE_URL)));
    assertThat(asset.contentType(), is(equalTo(MIME_TYPE_JSON)));
    assertThat(asset.format(), is(equalTo(FORMAT_NAME)));
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }
}
