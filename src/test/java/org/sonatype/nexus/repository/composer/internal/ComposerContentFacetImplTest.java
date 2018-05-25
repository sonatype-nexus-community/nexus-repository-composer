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

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.hash.HashCode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.composer.internal.AssetKind.LIST;
import static org.sonatype.nexus.repository.composer.internal.AssetKind.PACKAGES;
import static org.sonatype.nexus.repository.composer.internal.AssetKind.PROVIDER;
import static org.sonatype.nexus.repository.composer.internal.AssetKind.ZIPBALL;
import static org.sonatype.nexus.repository.composer.internal.ComposerAttributes.P_PROJECT;
import static org.sonatype.nexus.repository.composer.internal.ComposerAttributes.P_VENDOR;
import static org.sonatype.nexus.repository.composer.internal.ComposerAttributes.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

public class ComposerContentFacetImplTest
    extends TestSupport
{
  private static final String CONTENT_TYPE = "content-type";

  private static final Format COMPOSER_FORMAT = new ComposerFormat();

  private static final Date LAST_MODIFIED = new Date();

  private static final Date LAST_VERIFIED = new Date();

  private static final String ETAG = "etag";

  private static final List<HashAlgorithm> HASH_ALGORITHMS = asList(MD5, SHA1, SHA256);

  private static final Map<HashAlgorithm, HashCode> CHECKSUMS = singletonMap(SHA256, HashCode.fromInt(1));

  private static final String LIST_PATH = "packages/list.json";

  private static final String PACKAGES_PATH = "packages.json";

  private static final String PROVIDER_PATH = "p/vendor/project.json";

  private static final String ZIPBALL_PATH = "vendor/project/version/project-version.zip";

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
  private StorageFacet storageFacet;

  @Mock
  private Bucket bucket;

  @Mock
  private StorageTx tx;

  @Mock
  private Content upload;

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
    underTest = new ComposerContentFacetImpl(COMPOSER_FORMAT, composerFormatAttributesExtractor);
    underTest.attach(repository);

    when(tx.findBucket(repository)).thenReturn(bucket);
    when(tx.requireBlob(blobRef)).thenReturn(blob);
    when(tx.createAsset(bucket, COMPOSER_FORMAT)).thenReturn(asset);
    when(tx.createAsset(bucket, component)).thenReturn(asset);
    when(tx.findComponents(any(Query.class), eq(singletonList(repository)))).thenReturn(emptyList());
    when(tx.createComponent(bucket, COMPOSER_FORMAT)).thenReturn(component);

    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);

    when(asset.attributes()).thenReturn(assetAttributes);
    when(asset.formatAttributes()).thenReturn(formatAttributes);
    when(asset.requireBlobRef()).thenReturn(blobRef);
    when(asset.requireContentType()).thenReturn(CONTENT_TYPE);
    when(asset.getChecksums(HASH_ALGORITHMS)).thenReturn(CHECKSUMS);

    when(assetAttributes.child("cache")).thenReturn(cacheAttributes);
    when(assetAttributes.child("composer")).thenReturn(formatAttributes);
    when(assetAttributes.child("content")).thenReturn(contentAttributes);

    when(assetBlob.getBlob()).thenReturn(blob);
    when(assetBlob.getBlobRef()).thenReturn(blobRef);

    when(contentAttributes.get("last_modified", Date.class)).thenReturn(LAST_MODIFIED);
    when(contentAttributes.get("etag")).thenReturn(ETAG);

    when(cacheAttributes.get("last_verified", Date.class)).thenReturn(LAST_VERIFIED);

    when(storageFacet.createTempBlob(upload, HASH_ALGORITHMS)).thenReturn(tempBlob);

    when(blob.getInputStream()).thenReturn(blobInputStream);

    when(upload.getContentType()).thenReturn(CONTENT_TYPE);

    when(component.group(any(String.class))).thenReturn(component);
    when(component.name(any(String.class))).thenReturn(component);
    when(component.version(any(String.class))).thenReturn(component);

    doThrow(new RuntimeException("Test")).when(composerFormatAttributesExtractor)
        .extractFromZip(tempBlob, formatAttributes);

    UnitOfWork.beginBatch(tx);
  }

  @After
  public void tearDown() throws Exception {
    UnitOfWork.end();
  }

  @Test
  public void getAssetNotFound() throws Exception {
    assertThat(underTest.get("path"), is(nullValue()));
  }

  @Test
  public void getAssetFoundWithoutUpdate() throws Exception {
    testGet(ZIPBALL_PATH, false);
  }

  @Test
  public void getAssetFoundWithUpdate() throws Exception {
    testGet(ZIPBALL_PATH, true);
  }

  @Test
  public void putListJson() throws Exception {
    testPutOrUpdate(LIST, LIST_PATH, false);
  }

  @Test
  public void putPackagesJson() throws Exception {
    testPutOrUpdate(PACKAGES, PACKAGES_PATH, false);
  }

  @Test
  public void putProviderJson() throws Exception {
    testPutOrUpdate(PROVIDER, PROVIDER_PATH, false);
  }

  @Test
  public void putZipball() throws Exception {
    testPutOrUpdate(ZIPBALL, ZIPBALL_PATH, false);
  }

  @Test
  public void updateListJson() throws Exception {
    testPutOrUpdate(LIST, LIST_PATH, true);
  }

  @Test
  public void updatePackagesJson() throws Exception {
    testPutOrUpdate(PACKAGES, PACKAGES_PATH, true);
  }

  @Test
  public void updateProviderJson() throws Exception {
    testPutOrUpdate(PROVIDER, PROVIDER_PATH, true);
  }

  @Test
  public void updateZipball() throws Exception {
    testPutOrUpdate(ZIPBALL, ZIPBALL_PATH, true);
  }

  private void testGet(final String path, final boolean markAsDownloaded) throws Exception {
    when(tx.findAssetWithProperty(P_NAME, path, bucket)).thenReturn(asset);
    when(asset.markAsDownloaded()).thenReturn(markAsDownloaded);

    Content content = underTest.get(path);
    assertThat(content, is(notNullValue()));
    assertThat(content.openInputStream(), is(blobInputStream));
    assertThat(content.getContentType(), is(CONTENT_TYPE));

    if (markAsDownloaded) {
      verify(tx).saveAsset(asset);
    }
    else {
      verify(tx, never()).saveAsset(asset);
    }
  }

  private void testPutOrUpdate(final AssetKind assetKind, final String path, final boolean update) throws Exception {
    when(tx.setBlob(asset, path, tempBlob, null, CONTENT_TYPE, false)).thenReturn(assetBlob);
    if (update) {
      when(tx.findComponents(any(Query.class), eq(singletonList(repository)))).thenReturn(singletonList(component));
      when(tx.findAssetWithProperty(P_NAME, path, bucket)).thenReturn(asset);
    }

    Content content = underTest.put(path, upload, assetKind);
    assertThat(content, is(notNullValue()));
    assertThat(content.openInputStream(), is(blobInputStream));
    assertThat(content.getContentType(), is(CONTENT_TYPE));

    if (ZIPBALL.equals(assetKind)) {
      verify(formatAttributes).clear();
      verify(formatAttributes).set(P_VENDOR, "vendor");
      verify(formatAttributes).set(P_PROJECT, "project");
      verify(formatAttributes).set(P_VERSION, "version");
      verify(composerFormatAttributesExtractor).extractFromZip(tempBlob, formatAttributes);
    }

    verify(tx).saveAsset(asset);
    if (!update && ZIPBALL.equals(assetKind)) {
      verify(component).group("vendor");
      verify(component).name("project");
      verify(component).version("version");
    }
  }
}
