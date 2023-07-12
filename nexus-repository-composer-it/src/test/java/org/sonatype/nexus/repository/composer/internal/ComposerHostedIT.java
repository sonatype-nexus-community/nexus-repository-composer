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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

public class ComposerHostedIT
    extends ComposerITSupport
{
  private static final String COMPOSER_TEST_HOSTED = "composer-test-hosted";

  private ComposerClient hostedClient;

  @Before
  public void setup() throws Exception {
    startServer();

    Repository hostedRepo = repos.createComposerHosted(COMPOSER_TEST_HOSTED);
    hostedClient = composerClient(hostedRepo);
  }

  @Test
  public void nonExistingPackageProduces404() throws Exception {
    assertThat(status(hostedClient.get(BAD_PATH)), is(HttpStatus.NOT_FOUND));
  }

  @Test
  public void putAndGetOk() throws Exception {
    // given
    assertThat(status(hostedClient.get(VALID_ZIPBALL_URL)), is(HttpStatus.NOT_FOUND));

    // when
    assertThat(hostedClient.put(NAME_PACKAGES + "/upload/" + VALID_ZIPBALL_BASE_URL, testData.resolveFile(ZIPBALL_FILE_NAME)), is(200));

    // then
    assertThat(status(hostedClient.get(VALID_ZIPBALL_URL)), is(HttpStatus.OK));
  }

  @Test
  public void checkRestAPI() throws Exception {
    assertThat(status(hostedClient.get("/service/rest/v1/repositories")), is(HttpStatus.OK));
  }

  @Test
  public void badPutHostedConfigurationByAPI() throws Exception {
    assertThat(hostedClient.put("/service/rest/v1/repositories/composer/hosted/" + COMPOSER_TEST_HOSTED, "bad request"), is(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void getAndUpdateHostedConfigurationByAPI() throws Exception {
    JsonObject expected = new JsonParser().parse("{\"name\":\"composer-test-hosted\",\"format\":\"composer\",\"online\":true,\"storage\":{\"blobStoreName\":\"default\",\"strictContentTypeValidation\":true,\"writePolicy\":\"ALLOW\"},\"cleanup\":null,\"component\":{\"proprietaryComponents\":false},\"type\":\"hosted\"}").getAsJsonObject();
    getAndUpdateConfig(expected, COMPOSER_TEST_HOSTED, "hosted", hostedClient);
  }
}
