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

import com.google.common.hash.HashCode;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.*;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.AssetKind;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.fluent.*;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.*;
import static org.sonatype.nexus.common.hash.HashAlgorithm.*;
import static org.sonatype.nexus.repository.composer.AssetKind.*;

public class ComposerContentFacetImplTest
    extends TestSupport
{
  private static final String CONTENT_TYPE = "content-type";

  private static final Date LAST_MODIFIED = new Date();

  private static final Date LAST_VERIFIED = new Date();

  private static final String ETAG = "etag";

  private static final List<HashAlgorithm> HASH_ALGORITHMS = asList(MD5, SHA1, SHA256);

  private static final Map<String, String> CHECKSUMS = singletonMap(SHA256.name(), HashCode.fromInt(1).toString());

  private static final String LIST_PATH = "/packages/list.json";

  private static final String PACKAGES_PATH = "/packages.json";

  private static final String PROVIDER_PATH = "/p/vendor/project.json";

  private static final String ZIPBALL_PATH = "/vendor/project/version/project-version.zip";

  @Mock
  private Component component;

  @Mock
  private Asset asset;

  @Mock
  private BlobRef blobRef;

  @Mock
  private Blob blob;

  @Mock
  private InputStream blobInputStream;

  @Mock
  private Repository repository;

  @Mock
  private FluentBlobs fluentBlobs;

  @Mock
  private Content upload;

  @Mock
  private FluentComponents fluentComponents;

  @Mock
  private FluentComponentBuilder fluentComponentBuilder;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private FluentAssetBuilder fluentAssetBuilder;

  @Mock
  private FluentAsset fluentAsset;

  private Content content;

  @Mock
  private FluentComponent fluentComponent;

  @Mock
  private NestedAttributesMap assetAttributes;

  @Mock
  private NestedAttributesMap contentAttributes;

  @Mock
  private NestedAttributesMap cacheAttributes;

  @Mock
  private NestedAttributesMap formatAttributes;

  @Mock
  private AssetBlob assetBlob;

  @Mock
  private TempBlob tempBlob;

  @Mock
  private ComposerFormatAttributesExtractor composerFormatAttributesExtractor;

  private ComposerContentFacetImpl underTest;

  @Before
  public void setUp() throws Exception {
    underTest = spy(
        new ComposerContentFacetImpl(
            mock(FormatStoreManager.class),
            composerFormatAttributesExtractor
        )
    );
    underTest.attach(repository);


    BlobStoreManager blobStoreManager = mock(BlobStoreManager.class);
    BlobStore blobStore = mock(BlobStore.class);
    when(blobStoreManager.get("my-blobs")).thenReturn(blobStore);

    ContentFacetStores contentFacetStores = new ContentFacetStores(
        blobStoreManager,
        "my-blobs",
        mock(FormatStoreManager.class),
        "my-content"
    );
    when(underTest.stores()).thenReturn(contentFacetStores);
    when(underTest.contentRepositoryId()).thenReturn(1);

    when(underTest.assets()).thenReturn(fluentAssets);
    FluentAssetBuilder emptyFAB = mock(FluentAssetBuilder.class);
    when(fluentAssets.path(anyString())).thenReturn(emptyFAB);
    when(emptyFAB.find()).thenReturn(Optional.empty());
    when(fluentAssetBuilder.find()).thenReturn(Optional.of(fluentAsset));

    content = new Content(new BlobPayload(blob, null));
    when(fluentAsset.markAsCached(any(Payload.class))).thenReturn(fluentAsset);
    when(fluentAsset.download()).thenReturn(content);

    when(underTest.blobs()).thenReturn(fluentBlobs);
    when(fluentBlobs.ingest(any(Payload.class), any(List.class))).thenReturn(tempBlob);

    when(underTest.components()).thenReturn(fluentComponents);

    when(asset.attributes()).thenReturn(assetAttributes);
    BlobId blobId = new BlobId("my-blob");
    when(blobRef.getBlobId()).thenReturn(blobId);
    when(blobStore.get(blobId)).thenReturn(blob);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));
    when(assetBlob.blobRef()).thenReturn(blobRef);
    when(assetBlob.contentType()).thenReturn(CONTENT_TYPE);
    when(assetBlob.checksums()).thenReturn(CHECKSUMS);

    when(assetAttributes.child("cache")).thenReturn(cacheAttributes);
    when(assetAttributes.child("composer")).thenReturn(formatAttributes);
    when(assetAttributes.child("content")).thenReturn(contentAttributes);

    when(contentAttributes.get("last_modified", Date.class)).thenReturn(LAST_MODIFIED);
    when(contentAttributes.get("etag")).thenReturn(ETAG);

    when(cacheAttributes.get("last_verified", Date.class)).thenReturn(LAST_VERIFIED);

    when(blob.getInputStream()).thenReturn(blobInputStream);

    when(upload.getContentType()).thenReturn(CONTENT_TYPE);

    doThrow(new RuntimeException("Test")).when(composerFormatAttributesExtractor)
        .extractFromZip(tempBlob, fluentComponent);
  }

  @Test
  public void getAssetNotFound() {
    assertThat(underTest.get("/path"), is(Optional.empty()));
  }

  @Test
  public void getAssetFound() {
    testGet(ZIPBALL_PATH);
  }

  @Test
  public void putListJson() throws Exception {
    testPutOrUpdate(LIST, LIST_PATH);
  }

  @Test
  public void putPackagesJson() throws Exception {
    testPutOrUpdate(PACKAGES, PACKAGES_PATH);
  }

  @Test
  public void putProviderJson() throws Exception {
    testPutOrUpdate(PROVIDER, PROVIDER_PATH);
  }

  @Test
  @Ignore("versionNormalizerService() not mocked")
  public void putZipball() throws Exception {
    testPutOrUpdate(ZIPBALL, ZIPBALL_PATH);
  }

  private void testGet(final String path) {
    when(fluentAssets.path(path)).thenReturn(fluentAssetBuilder);

    Content content = underTest.get(path).orElse(null);
    assertThat(content, is(notNullValue()));
  }

  private void testPutOrUpdate(final AssetKind assetKind, final String path) throws Exception {
    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> kindCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<TempBlob> tempBlobCaptor = ArgumentCaptor.forClass(TempBlob.class);
    ArgumentCaptor<Component> componentCaptor = ArgumentCaptor.forClass(Component.class);

    ArgumentCaptor<String> vendor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> project = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> version = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> normalizedVersion = ArgumentCaptor.forClass(String.class);

    when(fluentAssets.path(pathCaptor.capture())).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.kind(kindCaptor.capture())).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.blob(tempBlobCaptor.capture())).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.blob(tempBlobCaptor.capture())).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.save()).thenReturn(fluentAsset);

    if (ZIPBALL.equals(assetKind)) {
      when(fluentAssetBuilder.component(componentCaptor.capture())).thenReturn(fluentAssetBuilder);

      when(fluentComponents.name(project.capture())).thenReturn(fluentComponentBuilder);
      when(fluentComponentBuilder.version(version.capture())).thenReturn(fluentComponentBuilder);
      when(fluentComponentBuilder.normalizedVersion(normalizedVersion.capture())).thenReturn(fluentComponentBuilder);
      when(fluentComponentBuilder.namespace(vendor.capture())).thenReturn(fluentComponentBuilder);
      when(fluentComponentBuilder.getOrCreate()).thenReturn(fluentComponent);
    }

    Content res = underTest.put(path, upload, assetKind);
    assertThat(res, is(content));
    assertThat(pathCaptor.getValue(), is(path));
    assertThat(kindCaptor.getValue(), is(assetKind.name()));
    assertThat(tempBlobCaptor.getValue(), is(tempBlob));

    if (ZIPBALL.equals(assetKind)) {
      assertThat(componentCaptor.getValue(), is(component));
      assertThat(vendor.getValue(), is("vendor"));
      assertThat(project.getValue(), is("project"));
      assertThat(version.getValue(), is("version"));
    }
  }
}
