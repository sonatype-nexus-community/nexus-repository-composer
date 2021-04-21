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
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;

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

  private static final String MIME_TYPE_ZIP = "application/zip";

  private static final String NAME_VENDOR = "rjkip";

  private static final String NAME_PROJECT = "ftp-php";

  private static final String NAME_VERSION = "v1.1.0";

  private static final String NAME_PACKAGES = "packages";

  private static final String NAME_LIST = "list";

  private static final String EXTENSION_JSON = ".json";

  private static final String EXTENSION_ZIP = ".zip";

  private static final String FILE_PROVIDER = NAME_PROJECT + EXTENSION_JSON;

  private static final String FILE_PACKAGES = NAME_PACKAGES + EXTENSION_JSON;

  private static final String FILE_PACKAGES_CHANGED = NAME_PACKAGES + "-changed" + EXTENSION_JSON;

  private static final String FILE_LIST = NAME_LIST + EXTENSION_JSON;

  private static final String FILE_ZIPBALL = NAME_VENDOR + "-" + NAME_PROJECT + "-" + NAME_VERSION + EXTENSION_ZIP;

  private static final String PACKAGE_BASE_PATH = "p/" + NAME_VENDOR + "/";

  private static final String LIST_BASE_PATH = "packages/";

  private static final String BAD_PATH = "/this/path/is/not/valid";

  private static final String VALID_PROVIDER_URL = PACKAGE_BASE_PATH + FILE_PROVIDER;

  private static final String VALID_LIST_URL = LIST_BASE_PATH + FILE_LIST;

  private static final String VALID_ZIPBALL_URL = NAME_VENDOR + "/" + NAME_PROJECT + "/" + NAME_VERSION + "/" + FILE_ZIPBALL;

  private static final String ZIPBALL_FILE_NAME = "rjkip-ftp-php-v1.1.0.zip";

  private static final String COMPONENT_NAME = "ftp-php";

  private static final String PACKAGE_NAME = COMPONENT_NAME + EXTENSION_JSON;

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
        .serve("/" + FILE_PACKAGES)
        .withBehaviours(Behaviours.file(testData.resolveFile(FILE_PACKAGES)))
        .serve("/" + VALID_LIST_URL)
        .withBehaviours(Behaviours.file(testData.resolveFile(FILE_LIST)))
        .serve("/" + VALID_PROVIDER_URL)
        .withBehaviours(Behaviours.file(testData.resolveFile(FILE_PROVIDER)))
        .serve("/" + VALID_ZIPBALL_URL)
        .withBehaviours(Behaviours.file(testData.resolveFile(ZIPBALL_FILE_NAME)))
        .start();

    proxyRepo = repos.createComposerProxy("composer-test-proxy", server.getUrl().toExternalForm());
    proxyClient = composerClient(proxyRepo);
  }

  @Test
  public void unresponsiveRemoteProduces404() throws Exception {
    assertThat(status(proxyClient.get(BAD_PATH)), is(HttpStatus.NOT_FOUND));
  }

  public void retrievePackagesJSONFromProxyWhenRemoteOnline() throws Exception {
    assertThat(status(proxyClient.get(FILE_PACKAGES)), is(HttpStatus.OK));

    final Asset asset = findAsset(proxyRepo, FILE_PACKAGES);
    assertThat(asset.name(), is(equalTo(FILE_PACKAGES)));
    assertThat(asset.contentType(), is(equalTo(MIME_TYPE_JSON)));
    assertThat(asset.format(), is(equalTo(FORMAT_NAME)));
  }

  @Test
  public void providersURLChangedToNXRM() throws Exception {
    assertThat(status(proxyClient.get(FILE_PACKAGES)), is(HttpStatus.OK));

    try (CloseableHttpResponse response = proxyClient.get(FILE_PACKAGES)) {
      HttpEntity entity = response.getEntity();
      JsonElement element = new JsonParser().parse(IOUtils.toString(entity.getContent()));
      JsonObject json = element.getAsJsonObject();

      assertThat(json.get("providers-url").toString(), is(equalTo("\"http://localhost:10000/repository/composer-test-proxy/p/%package%.json\"")));
    }
  }

  public void retrieveListJSONFromProxyWhenRemoteOnline() throws Exception {
    assertThat(status(proxyClient.get(VALID_LIST_URL)), is(HttpStatus.OK));

    final Asset asset = findAsset(proxyRepo, FILE_LIST);
    assertThat(asset.name(), is(equalTo(FILE_LIST)));
    assertThat(asset.contentType(), is(equalTo(MIME_TYPE_JSON)));
    assertThat(asset.format(), is(equalTo(FORMAT_NAME)));
  }

  @Test
  public void retrieveProviderJSONFromProxyWhenRemoteOnline() throws Exception {
    assertThat(status(proxyClient.get(VALID_PROVIDER_URL)), is(HttpStatus.OK));

    final Asset asset = findAsset(proxyRepo, VALID_PROVIDER_URL);
    assertThat(asset.name(), is(equalTo(VALID_PROVIDER_URL)));
    assertThat(asset.contentType(), is(equalTo(MIME_TYPE_JSON)));
    assertThat(asset.format(), is(equalTo(FORMAT_NAME)));
  }

  // TODO: Dude, this test, what the heck! It's completely wacky, someone needs to look at this
  //@Test
  //public void retrieveZipballFromProxyWhenRemoteOnline() throws Exception {
  //  assertThat(status(proxyClient.get(VALID_ZIPBALL_URL)), is(HttpStatus.OK));
  //
  //  final Asset asset = findAsset(proxyRepo, VALID_ZIPBALL_URL);
  //  assertThat(asset.name(), is(equalTo(VALID_ZIPBALL_URL)));
  //  assertThat(asset.contentType(), is(equalTo(MIME_TYPE_ZIP)));
  //  assertThat(asset.format(), is(equalTo(FORMAT_NAME)));
  //}
  
  @After
  public void tearDown() throws Exception {
    server.stop();
  }
}
