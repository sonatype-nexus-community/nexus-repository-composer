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
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import com.google.common.io.CharStreams;
import org.junit.Test;
import org.mockito.Mock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.sonatype.nexus.repository.composer.internal.ComposerAttributes.P_NAME;
import static org.sonatype.nexus.repository.composer.internal.ComposerAttributes.P_VERSION;

public class ComposerJsonProcessorTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private Payload payload;

  @Mock
  private Component component1;

  @Mock
  private Component component2;

  @Mock
  private Asset asset1;

  @Mock
  private Asset asset2;

  @Mock
  private Asset asset3;

  @Mock
  private Asset asset4;

  @Mock
  private NestedAttributesMap assetAttributes1;

  @Mock
  private NestedAttributesMap assetAttributes2;

  @Mock
  private NestedAttributesMap assetAttributes3;

  @Mock
  private NestedAttributesMap assetAttributes4;

  @Test
  public void generatePackagesFromList() throws Exception {
    String listJson = readStreamToString(getClass().getResourceAsStream("generatePackagesFromList.list.json"));
    String packagesJson = readStreamToString(getClass().getResourceAsStream("generatePackagesFromList.packages.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");
    when(payload.openInputStream()).thenReturn(new ByteArrayInputStream(listJson.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor();
    Content output = underTest.generatePackagesFromList(repository, payload);

    assertEquals(packagesJson, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void generatePackagesFromComponents() throws Exception {
    String packagesJson = readStreamToString(getClass().getResourceAsStream("generatePackagesFromComponents.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");

    when(component1.group()).thenReturn("vendor1");
    when(component1.name()).thenReturn("project1");

    when(component2.group()).thenReturn("vendor2");
    when(component2.name()).thenReturn("project2");

    ComposerJsonProcessor underTest = new ComposerJsonProcessor();
    Content output = underTest.generatePackagesFromComponents(repository, asList(component1, component2));

    assertEquals(packagesJson, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void rewriteProviderJson() throws Exception {
    String inputJson = readStreamToString(getClass().getResourceAsStream("rewriteProviderJson.input.json"));
    String outputJson = readStreamToString(getClass().getResourceAsStream("rewriteProviderJson.output.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");
    when(payload.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor();
    Payload output = underTest.rewriteProviderJson(repository, payload);

    assertEquals(outputJson, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void buildProviderJson() throws Exception {
    String outputJson = readStreamToString(getClass().getResourceAsStream("buildProviderJson.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");

    when(asset1.name()).thenReturn("vendor1/project1/1.0.0/vendor1-project1-1.0.0.zip");
    when(asset1.formatAttributes()).thenReturn(assetAttributes1);

    when(asset2.name()).thenReturn("vendor1/project1/2.0.0/vendor1-project1-2.0.0.zip");
    when(asset2.formatAttributes()).thenReturn(assetAttributes2);

    when(asset3.name()).thenReturn("vendor2/project2/3.0.0/vendor2-project2-3.0.0.zip");
    when(asset3.formatAttributes()).thenReturn(assetAttributes3);

    when(asset4.name()).thenReturn("vendor2/project2/4.0.0/vendor2-project2-4.0.0.zip");
    when(asset4.formatAttributes()).thenReturn(assetAttributes4);

    when(assetAttributes1.require(P_NAME, String.class)).thenReturn("vendor1/project1");
    when(assetAttributes1.require(P_VERSION, String.class)).thenReturn("1.0.0");

    when(assetAttributes2.require(P_NAME, String.class)).thenReturn("vendor1/project1");
    when(assetAttributes2.require(P_VERSION, String.class)).thenReturn("2.0.0");

    when(assetAttributes3.require(P_NAME, String.class)).thenReturn("vendor2/project2");
    when(assetAttributes3.require(P_VERSION, String.class)).thenReturn("3.0.0");

    when(assetAttributes4.require(P_NAME, String.class)).thenReturn("vendor2/project2");
    when(assetAttributes4.require(P_VERSION, String.class)).thenReturn("4.0.0");

    ComposerJsonProcessor underTest = new ComposerJsonProcessor();
    Content output = underTest.buildProviderJson(repository, asList(asset1, asset2, asset3, asset4));

    assertEquals(outputJson, readStreamToString(output.openInputStream()), true);
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
