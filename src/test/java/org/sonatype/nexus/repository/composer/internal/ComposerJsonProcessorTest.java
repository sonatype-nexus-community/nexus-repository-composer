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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import com.google.common.io.CharStreams;
import org.junit.Test;
import org.mockito.Mock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

public class ComposerJsonProcessorTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private Payload payload;

  @Test
  public void generatePackagesFromList() throws Exception {
    String listJson = readStreamToString(getClass().getResourceAsStream("generatePackagesFromList.list.json"));
    String packagesJson = readStreamToString(getClass().getResourceAsStream("generatePackagesFromList.packages.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");
    when(payload.openInputStream()).thenReturn(new ByteArrayInputStream(listJson.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor();
    Content output = underTest.generatePackagesJson(repository, payload);

    assertEquals(readStreamToString(output.openInputStream()), packagesJson, false);
  }

  @Test
  public void rewriteProviderJson() throws Exception {
    String inputJson = readStreamToString(getClass().getResourceAsStream("rewriteProviderJson.input.json"));
    String outputJson = readStreamToString(getClass().getResourceAsStream("rewriteProviderJson.output.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");
    when(payload.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor();
    Payload output = underTest.rewriteProviderJson(repository, payload);

    assertEquals(readStreamToString(output.openInputStream()), outputJson, false);
  }

  @Test
  public void getDistUrl() throws Exception {
    String inputJson = readStreamToString(getClass().getResourceAsStream("getDistUrl.json"));
    when(payload.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor();
    String distUrl = underTest.getDistUrl("vendor1", "project1", "2.0.0", payload);

    assertThat(distUrl, is("https://git.example.com/zipball/418e708b379598333d0a48954c0fa210437795be"));
  }

  private String readStreamToString(final InputStream in) throws IOException {
    try {
      return CharStreams.toString(new InputStreamReader(in, UTF_8));
    }
    finally {
      in.close();
    }
  }
}
