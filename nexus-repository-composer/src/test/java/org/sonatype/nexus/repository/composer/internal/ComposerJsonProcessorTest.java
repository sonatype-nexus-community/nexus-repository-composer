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

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.io.CharStreams;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.ComposerContentFacet;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.*;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

public class ComposerJsonProcessorTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private Content payload1;

  @Mock
  private Payload payload2;

  @Mock
  private Payload payload3;

  @Mock
  private FluentComponent component1;

  @Mock
  private FluentComponent component2;

  @Mock
  private FluentComponent component3;

  @Mock
  private FluentComponent component4;

  @Mock
  private FluentAsset asset1;

  @Mock
  private FluentAsset asset2;

  @Mock
  private FluentAsset asset3;

  @Mock
  private FluentAsset asset4;

  @Mock
  private FluentBlobs fluentBlobs;

  @Mock
  private AssetBlob assetBlob1;

  @Mock
  private AssetBlob assetBlob2;

  @Mock
  private AssetBlob assetBlob3;

  @Mock
  private AssetBlob assetBlob4;

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
  private ComposerContentFacet composerContentFacet;

  @Mock
  private ComposerJsonExtractor composerJsonExtractor;

  @Mock
  private ComposerJsonMinifier composerJsonMinifier;

  @Test
  public void rewritePackagesJson() throws Exception {
    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");
    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor, composerJsonMinifier);

    // test 1: packagist.org style
    String original = readStreamToString(getClass().getResourceAsStream("rewritePackagesJson.input1.json"));
    String expected = readStreamToString(getClass().getResourceAsStream("rewritePackagesJson.output1.json"));
    when(payload1.openInputStream()).thenReturn(new ByteArrayInputStream(original.getBytes(UTF_8)));

    Payload output = underTest.rewritePackagesJson(repository, payload1);
    assertEquals(expected, readStreamToString(output.openInputStream()), true);

    // test 2: Drupal 8 style
    original = readStreamToString(getClass().getResourceAsStream("rewritePackagesJson.input2.json"));
    expected = readStreamToString(getClass().getResourceAsStream("rewritePackagesJson.output2.json"));
    when(payload1.openInputStream()).thenReturn(new ByteArrayInputStream(original.getBytes(UTF_8)));

    output = underTest.rewritePackagesJson(repository, payload1);
    assertEquals(expected, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void generatePackagesFromComponents() throws Exception {
    String packagesJson = readStreamToString(getClass().getResourceAsStream("generatePackagesFromComponents.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");

    when(component1.namespace()).thenReturn("vendor1");
    when(component1.name()).thenReturn("project1");
    when(component1.version()).thenReturn("version1");

    when(component2.namespace()).thenReturn("vendor2");
    when(component2.name()).thenReturn("project2");
    when(component2.version()).thenReturn("version2");

    when(component3.namespace()).thenReturn("vendor1");
    when(component3.name()).thenReturn("project1");
    when(component3.version()).thenReturn("version2");

    FluentComponents components = mock(FluentComponents.class);
    when(components.count()).thenReturn(2);
    when(components.browse(anyInt(), isNull())).thenReturn(new ContinuationList("con-tkn-001", component1, component2));
    when(components.browse(anyInt(), eq("con-tkn-001"))).thenReturn(new ContinuationList("con-tkn-002", component3));
    when(components.browse(anyInt(), eq("con-tkn-002"))).thenReturn(new ContinuationList(""));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor, composerJsonMinifier);

    Content output = underTest.generatePackagesFromComponents(repository, components);

    assertEquals(packagesJson, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void rewriteProviderJson() throws Exception {
    String inputJson = readStreamToString(getClass().getResourceAsStream("rewriteProviderJson.input.json"));
    String outputJson = readStreamToString(getClass().getResourceAsStream("rewriteProviderJson.output.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");
    when(payload1.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor, composerJsonMinifier);
    Payload output = underTest.rewriteProviderJson(repository, payload1);

    assertEquals(outputJson, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void mergeProviderJson() throws Exception {
    OffsetDateTime time = OffsetDateTime.of(2008, 5, 15, 12, 30, 0, 0, ZoneOffset.ofHours(-4));

    String inputJson1 = readStreamToString(getClass().getResourceAsStream("mergeProviderJson.input1.json"));
    String inputJson2 = readStreamToString(getClass().getResourceAsStream("mergeProviderJson.input2.json"));
    String outputJson = readStreamToString(getClass().getResourceAsStream("mergeProviderJson.output.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");
    when(payload1.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson1.getBytes(UTF_8)));
    when(payload2.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson2.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor, composerJsonMinifier);
    Payload output = underTest.mergeProviderJson(repository, Arrays.asList(payload1, payload2), time);

    assertEquals(outputJson, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void buildProviderJson() throws Exception {
    String outputJson = readStreamToString(getClass().getResourceAsStream("buildProviderJson.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");

    when(component1.namespace()).thenReturn("vendor1");
    when(component1.name()).thenReturn("project1");
    when(component1.version()).thenReturn("1.0.0");
    when(component1.lastUpdated()).thenReturn(OffsetDateTime.of(1982, 6, 4, 12, 30, 0, 0, ZoneOffset.ofHours(-4)));
    when(component1.assets()).thenReturn(singletonList(asset1));
    when(asset1.hasBlob()).thenReturn(true);
    when(asset1.blob()).thenReturn(Optional.of(assetBlob1));
    when(assetBlob1.blobRef()).thenReturn(blobRef1);
    when(composerContentFacet.blobs()).thenReturn(fluentBlobs);
    when(fluentBlobs.blob(blobRef1)).thenReturn(Optional.of(blob1));
    when(assetBlob1.checksums()).thenReturn(singletonMap(SHA1.name(), HashCode.fromLong(1L).toString()));
    when(asset1.attributes()).thenReturn(mock(NestedAttributesMap.class));
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

    when(component2.namespace()).thenReturn("vendor1");
    when(component2.name()).thenReturn("project1");
    when(component2.version()).thenReturn("2.0.0");
    when(component2.lastUpdated()).thenReturn(OffsetDateTime.of(2008, 5, 15, 12, 30, 0, 0, ZoneOffset.ofHours(-4)));
    when(component2.assets()).thenReturn(singletonList(asset2));
    when(asset2.hasBlob()).thenReturn(true);
    when(asset2.blob()).thenReturn(Optional.of(assetBlob2));
    when(assetBlob2.blobRef()).thenReturn(blobRef2);
    when(fluentBlobs.blob(blobRef2)).thenReturn(Optional.of(blob2));
    when(assetBlob2.checksums()).thenReturn(singletonMap(SHA1.name(), HashCode.fromLong(2L).toString()));
    when(asset2.attributes()).thenReturn(mock(NestedAttributesMap.class));
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

    when(component3.namespace()).thenReturn("vendor2");
    when(component3.name()).thenReturn("project2");
    when(component3.version()).thenReturn("3.0.0");
    when(component3.lastUpdated()).thenReturn(OffsetDateTime.of(1979, 7, 11, 12, 30, 0, 0, ZoneOffset.ofHours(-4)));
    when(component3.assets()).thenReturn(singletonList(asset3));
    when(asset3.hasBlob()).thenReturn(true);
    when(asset3.blob()).thenReturn(Optional.of(assetBlob3));
    when(assetBlob3.blobRef()).thenReturn(blobRef3);
    when(fluentBlobs.blob(blobRef3)).thenReturn(Optional.of(blob3));
    when(assetBlob3.checksums()).thenReturn(singletonMap(SHA1.name(), HashCode.fromLong(3L).toString()));
    when(asset3.attributes()).thenReturn(mock(NestedAttributesMap.class));
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

    when(component4.namespace()).thenReturn("vendor2");
    when(component4.name()).thenReturn("project2");
    when(component4.version()).thenReturn("4.0.0");
    when(component4.lastUpdated()).thenReturn(OffsetDateTime.of(2008, 5, 15, 12, 30, 0, 0, ZoneOffset.ofHours(-4)));
    when(component4.assets()).thenReturn(singletonList(asset4));
    when(asset4.hasBlob()).thenReturn(true);
    when(asset4.blob()).thenReturn(Optional.of(assetBlob4));
    when(assetBlob4.blobRef()).thenReturn(blobRef4);
    when(fluentBlobs.blob(blobRef4)).thenReturn(Optional.of(blob4));
    when(assetBlob4.checksums()).thenReturn(singletonMap(SHA1.name(), HashCode.fromLong(4L).toString()));
    when(asset4.attributes()).thenReturn(mock(NestedAttributesMap.class));
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

    FluentQuery<FluentComponent> components = mock(FluentComponents.class);
    when(components.browse(anyInt(), isNull())).thenReturn(new ContinuationList("con-tkn-001", component1, component2));
    when(components.browse(anyInt(), eq("con-tkn-001"))).thenReturn(new ContinuationList("con-tkn-002", component3, component4));
    when(components.browse(anyInt(), eq("con-tkn-002"))).thenReturn(new ContinuationList(""));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor, composerJsonMinifier);
    Optional<Content> output = underTest.buildProviderJson(repository, composerContentFacet, components);

    assertTrue(output.isPresent());
    assertEquals(outputJson, readStreamToString(output.get().openInputStream()), true);
  }

  @Test
  public void rewritePackageJson() throws Exception {
    String inputJson = readStreamToString(getClass().getResourceAsStream("rewritePackageJson.input.json"));
    String outputJson = readStreamToString(getClass().getResourceAsStream("rewritePackageJson.output.json"));

    when(repository.getUrl()).thenReturn("http://nexus.repo/base/repo");
    when(payload1.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor, composerJsonMinifier);
    Payload output = underTest.rewritePackageJson(repository, payload1);

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

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor, composerJsonMinifier);
    Payload output = underTest.mergePackagesJson(repository, Arrays.asList(payload1, payload2));

    assertEquals(outputJson, readStreamToString(output.openInputStream()), true);

    // Add another repository without "available-packages"
    String inputJson3 = readStreamToString(getClass().getResourceAsStream("mergePackagesJson.input3.json"));
    String outputJson2 = readStreamToString(getClass().getResourceAsStream("mergePackagesJson.output2.json"));

    when(payload1.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson1.getBytes(UTF_8)));
    when(payload2.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson2.getBytes(UTF_8)));
    when(payload3.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson3.getBytes(UTF_8)));
    output = underTest.mergePackagesJson(repository, Arrays.asList(payload1, payload2, payload3));
    assertEquals(outputJson2, readStreamToString(output.openInputStream()), true);
  }

  @Test
  public void getDistUrl() throws Exception {
    String inputJson = readStreamToString(getClass().getResourceAsStream("getDistUrl.json"));
    when(payload1.openInputStream()).thenReturn(new ByteArrayInputStream(inputJson.getBytes(UTF_8)));

    ComposerJsonProcessor underTest = new ComposerJsonProcessor(composerJsonExtractor, composerJsonMinifier);
    String distUrl = underTest.getDistUrl("vendor1", "project1", "2.0.0", payload1);

    assertThat(distUrl, is("https://git.example.com/zipball/418e708b379598333d0a48954c0fa210437795be"));
  }

  private String readStreamToString(final InputStream in) throws IOException {
    try {
      return CharStreams.toString(new InputStreamReader(in, UTF_8));
    } finally {
      in.close();
    }
  }

  private static class ContinuationList
      extends ArrayList<FluentComponent>
      implements Continuation<FluentComponent>
  {
    private final String continuationToken;

    private ContinuationList(Collection<FluentComponent> elements, String continuationToken) {
      super(elements);
      this.continuationToken = continuationToken;
    }

    private ContinuationList(String continuationToken, FluentComponent... elements) {
      super(Arrays.asList(elements));
      this.continuationToken = continuationToken;
    }

    @Override
    public String nextContinuationToken() {
      checkState(!isEmpty(), "No more results");
      return continuationToken;
    }
  }
}
