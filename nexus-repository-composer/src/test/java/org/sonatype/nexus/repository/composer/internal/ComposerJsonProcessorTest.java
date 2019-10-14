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
import java.util.Arrays;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.io.CharStreams;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

public class ComposerJsonProcessorTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private Payload payload1;

  @Mock
  private Payload payload2;

  @Mock
  private Component component1;

  @Mock
  private Component component2;

  @Mock
  private Component component3;

  @Mock
  private Component component4;

  @Mock
  private Asset asset1;

  @Mock
  private Asset asset2;

  @Mock
  private Asset asset3;

  @Mock
  private Asset asset4;

  @Mock
  private BlobRef blobRef1;

  @Mock
  private BlobRef blobRef2;

  @Mock
  private BlobRef blobRef3;

  @Mock
  private BlobRef blobRef4;

  @Mock
  private Blob blob1;

  @Mock
  private Blob blob2;

  @Mock
  private Blob blob3;

  @Mock
  private Blob blob4;

  @Mock
  private StorageTx storageTx;

  @Mock
  private ComposerJsonExtractor composerJsonExtractor;

  @Test
  public void generatePackagesFromList() throws Exception {
    String listJson = readStreamToString(getClass().getResourceAsStream("generatePackagesFromList.list.json"));
    String packagesJson = readStreamToString(getClass().getResourceAsStream("generatePackagesFromList.packages.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");
    when(payload1.openInputStream()).thenReturn(new ByteArrayInputStream(listJson.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor);
    Content output = underTest.generatePackagesFromList(repository, payload1);

    assertEquals(packagesJson, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void generatePackagesFromComponents() throws Exception {
    String packagesJson = readStreamToString(getClass().getResourceAsStream("generatePackagesFromComponents.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");

    when(component1.group()).thenReturn("vendor1");
    when(component1.name()).thenReturn("project1");
    when(component1.version()).thenReturn("version1");

    when(component2.group()).thenReturn("vendor2");
    when(component2.name()).thenReturn("project2");
    when(component2.version()).thenReturn("version2");

    when(component3.group()).thenReturn("vendor1");
    when(component3.name()).thenReturn("project1");
    when(component3.version()).thenReturn("version2");

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor);
    Content output = underTest.generatePackagesFromComponents(repository, asList(component1, component2, component3));

    assertEquals(packagesJson, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void rewriteProviderJson() throws Exception {
    String inputJson = readStreamToString(getClass().getResourceAsStream("rewriteProviderJson.input.json"));
    String outputJson = readStreamToString(getClass().getResourceAsStream("rewriteProviderJson.output.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");
    when(payload1.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor);
    Payload output = underTest.rewriteProviderJson(repository, payload1);

    assertEquals(outputJson, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void mergeProviderJson() throws Exception {
    DateTime time = new DateTime(1210869000000L, DateTimeZone.forOffsetHours(-4));

    String inputJson1 = readStreamToString(getClass().getResourceAsStream("mergeProviderJson.input1.json"));
    String inputJson2 = readStreamToString(getClass().getResourceAsStream("mergeProviderJson.input2.json"));
    String outputJson = readStreamToString(getClass().getResourceAsStream("mergeProviderJson.output.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");
    when(payload1.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson1.getBytes(UTF_8)));
    when(payload2.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson2.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor);
    Payload output = underTest.mergeProviderJson(repository, Arrays.asList(payload1, payload2), time);

    assertEquals(outputJson, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void buildProviderJson() throws Exception {
    String outputJson = readStreamToString(getClass().getResourceAsStream("buildProviderJson.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");

    when(component1.group()).thenReturn("vendor1");
    when(component1.name()).thenReturn("project1");
    when(component1.version()).thenReturn("1.0.0");
    when(component1.requireLastUpdated()).thenReturn(new DateTime(392056200000L, DateTimeZone.forOffsetHours(-4)));
    when(storageTx.firstAsset(component1)).thenReturn(asset1);
    when(asset1.requireBlobRef()).thenReturn(blobRef1);
    when(asset1.getChecksum(SHA1)).thenReturn(HashCode.fromLong(1L));
    when(storageTx.requireBlob(blobRef1)).thenReturn(blob1);
    when(composerJsonExtractor.extractFromZip(blob1)).thenReturn(new ImmutableMap.Builder<String, Object>()
        .put("autoload", singletonMap("psr-4", singletonMap("psr-1-key", "psr-1-value")))
        .put("autoload-dev", singletonMap("psr-4", singletonMap("psr-1-key", "psr-1-value")))
        .put("require", singletonMap("dependency-1", "version-1"))
        .put("require-dev", singletonMap("dev-dependency-1", "dev-version-1"))
        .put("suggest", singletonMap("suggest-1", "description-1"))
        .put("authors", asList(singletonMap("name", "author-1")))
        .put("bin", asList("bin-1"))
        .put("conflict", singletonMap("conflict-1", "version-1"))
        .put("extra", singletonMap("branch-alias", singletonMap("branch-1", "version-1")))
        .put("license", asList("license-1"))
        .put("description", "description-1")
        .put("homepage", "homepage-1")
        .put("include-path", asList("include-path-1"))
        .put("replace", singletonMap("replace-1", "replace-1-value"))
        .put("provide", singletonMap("provide-1", "version-1"))
        .put("target-dir", "target-dir-1")
        .put("scripts", singletonMap("scripts-1", asList("script-1")))
        .put("support", singletonMap("support-1", "support-1-value"))
        .put("type", "type-1-value")
        .put("foo", singletonMap("foo-key", "foo-value"))
        .build());

    when(component2.group()).thenReturn("vendor1");
    when(component2.name()).thenReturn("project1");
    when(component2.version()).thenReturn("2.0.0");
    when(component2.requireLastUpdated()).thenReturn(new DateTime(1210869000000L, DateTimeZone.forOffsetHours(-4)));
    when(storageTx.firstAsset(component2)).thenReturn(asset2);
    when(asset2.requireBlobRef()).thenReturn(blobRef2);
    when(asset2.getChecksum(SHA1)).thenReturn(HashCode.fromLong(2L));
    when(storageTx.requireBlob(blobRef2)).thenReturn(blob2);
    when(composerJsonExtractor.extractFromZip(blob2)).thenReturn(new ImmutableMap.Builder<String, Object>()
        .put("autoload", singletonMap("psr-0", singletonMap("psr-2-key", "psr-2-value")))
        .put("autoload-dev", singletonMap("psr-4", singletonMap("psr-2-key", "psr-2-value")))
        .put("require", singletonMap("dependency-2", "version-2"))
        .put("require-dev", singletonMap("dev-dependency-2", "dev-version-2"))
        .put("suggest", singletonMap("suggest-2", "description-2"))
        .put("authors", asList(singletonMap("name", "author-2")))
        .put("bin", asList("bin-2"))
        .put("conflict", singletonMap("conflict-2", "version-2"))
        .put("extra", singletonMap("branch-alias", singletonMap("branch-2", "version-2")))
        .put("license", asList("license-2"))
        .put("description", "description-2")
        .put("homepage", "homepage-2")
        .put("include-path", asList("include-path-2"))
        .put("replace", singletonMap("replace-2", "replace-2-value"))
        .put("provide", singletonMap("provide-2", "version-2"))
        .put("target-dir", "target-dir-2")
        .put("scripts", singletonMap("scripts-2", asList("script-2")))
        .put("support", singletonMap("support-2", "support-2-value"))
        .put("type", "type-2-value")
        .put("foo", singletonMap("foo-key", "foo-value"))
        .build());

    when(component3.group()).thenReturn("vendor2");
    when(component3.name()).thenReturn("project2");
    when(component3.version()).thenReturn("3.0.0");
    when(component3.requireLastUpdated()).thenReturn(new DateTime(300558600000L, DateTimeZone.forOffsetHours(-4)));
    when(storageTx.firstAsset(component3)).thenReturn(asset3);
    when(asset3.requireBlobRef()).thenReturn(blobRef3);
    when(asset3.getChecksum(SHA1)).thenReturn(HashCode.fromLong(3L));
    when(storageTx.requireBlob(blobRef3)).thenReturn(blob3);
    when(composerJsonExtractor.extractFromZip(blob3)).thenReturn(new ImmutableMap.Builder<String, Object>()
        .put("autoload", singletonMap("psr-4", singletonMap("psr-3-key", "psr-3-value")))
        .put("autoload-dev", singletonMap("psr-4", singletonMap("psr-3-key", "psr-3-value")))
        .put("require", singletonMap("dependency-3", "version-3"))
        .put("require-dev", singletonMap("dev-dependency-3", "dev-version-3"))
        .put("suggest", singletonMap("suggest-3", "description-3"))
        .put("authors", asList(singletonMap("name", "author-3")))
        .put("bin", asList("bin-3"))
        .put("conflict", singletonMap("conflict-3", "version-3"))
        .put("extra", singletonMap("branch-alias", singletonMap("branch-3", "version-3")))
        .put("license", asList("license-3"))
        .put("description", "description-3")
        .put("homepage", "homepage-3")
        .put("include-path", asList("include-path-3"))
        .put("replace", singletonMap("replace-3", "replace-3-value"))
        .put("provide", singletonMap("provide-3", "version-3"))
        .put("target-dir", "target-dir-3")
        .put("scripts", singletonMap("scripts-3", asList("script-3")))
        .put("support", singletonMap("support-3", "support-3-value"))
        .put("type", "type-3-value")
        .put("foo", singletonMap("foo-key", "foo-value"))
        .build());

    when(component4.group()).thenReturn("vendor2");
    when(component4.name()).thenReturn("project2");
    when(component4.version()).thenReturn("4.0.0");
    when(component4.requireLastUpdated()).thenReturn(new DateTime(1210869000000L, DateTimeZone.forOffsetHours(-4)));
    when(storageTx.firstAsset(component4)).thenReturn(asset4);
    when(asset4.requireBlobRef()).thenReturn(blobRef4);
    when(asset4.getChecksum(SHA1)).thenReturn(HashCode.fromLong(4L));
    when(storageTx.requireBlob(blobRef4)).thenReturn(blob4);
    when(composerJsonExtractor.extractFromZip(blob4)).thenReturn(new ImmutableMap.Builder<String, Object>()
        .put("autoload", singletonMap("psr-0", singletonMap("psr-4-key", "psr-4-value")))
        .put("autoload-dev", singletonMap("psr-4", singletonMap("psr-4-key", "psr-4-value")))
        .put("require", singletonMap("dependency-4", "version-4"))
        .put("require-dev", singletonMap("dev-dependency-4", "dev-version-4"))
        .put("suggest", singletonMap("suggest-4", "description-4"))
        .put("authors", asList(singletonMap("name", "author-4")))
        .put("bin", asList("bin-4"))
        .put("conflict", singletonMap("conflict-4", "version-4"))
        .put("extra", singletonMap("branch-alias", singletonMap("branch-4", "version-4")))
        .put("license", asList("license-4"))
        .put("description", "description-4")
        .put("homepage", "homepage-4")
        .put("include-path", asList("include-path-4"))
        .put("replace", singletonMap("replace-4", "replace-4-value"))
        .put("provide", singletonMap("provide-4", "version-4"))
        .put("target-dir", "target-dir-4")
        .put("scripts", singletonMap("scripts-4", asList("script-4")))
        .put("support", singletonMap("support-4", "support-4-value"))
        .put("type", "type-4-value")
        .put("foo", singletonMap("foo-key", "foo-value"))
        .build());

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor);
    Content output = underTest
        .buildProviderJson(repository, storageTx, asList(component1, component2, component3, component4));

    assertEquals(outputJson, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void mergePackagesJson() throws Exception {
    String inputJson1 = readStreamToString(getClass().getResourceAsStream("mergePackagesJson.input1.json"));
    String inputJson2 = readStreamToString(getClass().getResourceAsStream("mergePackagesJson.input2.json"));
    String outputJson = readStreamToString(getClass().getResourceAsStream("mergePackagesJson.output.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");
    when(payload1.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson1.getBytes(UTF_8)));
    when(payload2.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson2.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor);
    Payload output = underTest.mergePackagesJson(repository, Arrays.asList(payload1, payload2));

    assertEquals(outputJson, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void getDistUrl() throws Exception {
    String inputJson = readStreamToString(getClass().getResourceAsStream("getDistUrl.json"));
    when(payload1.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor);
    String distUrl = underTest.getDistUrl("vendor1", "project1", "2.0.0", payload1);

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
