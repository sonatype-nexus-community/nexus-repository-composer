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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.storage.Asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;
import static org.testcontainers.shaded.org.hamcrest.text.MatchesPattern.matchesPattern;

public class ComposerProxyIT
    extends ComposerITSupport
{
  private static final String FORMAT_NAME = "composer";

  private static final String COMPOSER_TEST_PROXY = "composer-test-proxy";

  private ComposerClient proxyClient;

  private Repository proxyRepo;

  @Before
  public void setup() throws Exception {
    startServer();

    proxyRepo = repos.createComposerProxy(COMPOSER_TEST_PROXY, server.getUrl().toExternalForm());
    proxyClient = composerClient(proxyRepo);
  }

  @Test
  public void unresponsiveRemoteProduces404() throws Exception {
    assertThat(status(proxyClient.get(BAD_PATH)), is(HttpStatus.NOT_FOUND));
  }

  @Test
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
      assertThat(matchesPattern("http://localhost:[0-9]*/repository/composer-test-proxy/p/%package%.json").matches(json.get("providers-url").getAsString()), is(true));
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

  @Test
  public void checkRestAPI() throws Exception {
    assertThat(status(proxyClient.get("/service/rest/v1/repositories")), is(HttpStatus.OK));
  }

  @Test
  public void badPutProxyConfigurationByAPI() throws Exception {
    assertThat(proxyClient.put("/service/rest/v1/repositories/composer/proxy/" + COMPOSER_TEST_PROXY, "bad request"), is(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void getAndUpdateProxyConfigurationByAPI() throws Exception {
    // given
    int port = server.getPort();
    JsonObject expected = new JsonParser().parse("{\"name\":\"composer-test-proxy\",\"format\":\"composer\",\"online\":true,\"storage\":{\"blobStoreName\":\"default\",\"strictContentTypeValidation\":true},\"cleanup\":null,\"proxy\":{\"remoteUrl\":\"http://localhost:" + port + "\",\"contentMaxAge\":1440,\"metadataMaxAge\":1440},\"negativeCache\":{\"enabled\":true,\"timeToLive\":1440},\"httpClient\":{\"blocked\":false,\"autoBlock\":false,\"connection\":{\"retries\":null,\"userAgentSuffix\":null,\"timeout\":null,\"enableCircularRedirects\":false,\"enableCookies\":false,\"useTrustStore\":false},\"authentication\":null},\"routingRuleName\":null,\"type\":\"proxy\"}").getAsJsonObject();

    getAndUpdateConfig(expected, COMPOSER_TEST_PROXY, "proxy", proxyClient);
  }

}
