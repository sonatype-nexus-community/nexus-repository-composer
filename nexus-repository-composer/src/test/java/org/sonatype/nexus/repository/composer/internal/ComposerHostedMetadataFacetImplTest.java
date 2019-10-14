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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetCreatedEvent;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;
import org.sonatype.nexus.repository.storage.AssetUpdatedEvent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.google.common.base.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.composer.internal.AssetKind.PACKAGES;
import static org.sonatype.nexus.repository.composer.internal.AssetKind.ZIPBALL;
import static org.sonatype.nexus.repository.composer.internal.ComposerAttributes.P_PROJECT;
import static org.sonatype.nexus.repository.composer.internal.ComposerAttributes.P_VENDOR;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

public class ComposerHostedMetadataFacetImplTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "test-repository-name";

  private static final String VENDOR = "test-vendor";

  private static final String PROJECT = "test-project";

  @Mock
  private Repository repository;

  @Mock
  private EventManager eventManager;

  @Mock
  private ComposerHostedFacet hostedFacet;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  Supplier<StorageTx> txSupplier;

  @Mock
  private StorageTx storageTx;

  @Mock
  private Asset asset;

  @Mock
  private NestedAttributesMap formatAttributes;

  @Mock
  private AssetDeletedEvent assetDeletedEvent;

  @Mock
  private AssetUpdatedEvent assetUpdatedEvent;

  @Mock
  private AssetCreatedEvent assetCreatedEvent;

  @Mock
  private ComposerHostedMetadataInvalidationEvent composerHostedMetadataInvalidationEvent;

  private ComposerHostedMetadataFacetImpl underTest;

  @Before
  public void setUp() throws Exception {
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.facet(ComposerHostedFacet.class)).thenReturn(hostedFacet);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);

    when(assetDeletedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetDeletedEvent.getAsset()).thenReturn(asset);
    when(assetDeletedEvent.isLocal()).thenReturn(true);

    when(assetUpdatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetUpdatedEvent.getAsset()).thenReturn(asset);
    when(assetUpdatedEvent.isLocal()).thenReturn(true);

    when(assetCreatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetCreatedEvent.getAsset()).thenReturn(asset);
    when(assetCreatedEvent.isLocal()).thenReturn(true);

    when(composerHostedMetadataInvalidationEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(composerHostedMetadataInvalidationEvent.getVendor()).thenReturn(VENDOR);
    when(composerHostedMetadataInvalidationEvent.getProject()).thenReturn(PROJECT);

    when(asset.formatAttributes()).thenReturn(formatAttributes);

    when(formatAttributes.require(P_VENDOR, String.class)).thenReturn(VENDOR);
    when(formatAttributes.require(P_PROJECT, String.class)).thenReturn(PROJECT);
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(ZIPBALL.name());

    when(storageFacet.txSupplier()).thenReturn(txSupplier);
    when(txSupplier.get()).thenReturn(storageTx);

    underTest = new ComposerHostedMetadataFacetImpl(eventManager);
    underTest.attach(repository);
  }

  @Test
  public void testAssetDeletedEventWithDifferentRepository() {
    when(assetDeletedEvent.getRepositoryName()).thenReturn("other-repository");

    underTest.on(assetDeletedEvent);

    verify(eventManager, never()).post(any(ComposerHostedMetadataInvalidationEvent.class));
  }

  @Test
  public void testAssetDeletedEventWithDifferentNode() {
    when(assetDeletedEvent.isLocal()).thenReturn(false);

    underTest.on(assetDeletedEvent);

    verify(eventManager, never()).post(any(ComposerHostedMetadataInvalidationEvent.class));
  }

  @Test
  public void testAssetDeletedEventWithNonZipballAsset() {
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(PACKAGES.name());

    underTest.on(assetDeletedEvent);

    verify(eventManager, never()).post(any(ComposerHostedMetadataInvalidationEvent.class));
  }

  @Test
  public void testAssetDeletedEvent() {
    underTest.on(assetDeletedEvent);

    ArgumentCaptor<ComposerHostedMetadataInvalidationEvent> captor = ArgumentCaptor
        .forClass(ComposerHostedMetadataInvalidationEvent.class);

    verify(eventManager).post(captor.capture());

    ComposerHostedMetadataInvalidationEvent event = captor.getValue();

    assertThat(event.getProject(), is(PROJECT));
    assertThat(event.getVendor(), is(VENDOR));
    assertThat(event.getRepositoryName(), is(REPOSITORY_NAME));
  }

  @Test
  public void testAssetUpdatedEventWithDifferentRepository() {
    when(assetUpdatedEvent.getRepositoryName()).thenReturn("other-repository");

    underTest.on(assetUpdatedEvent);

    verify(eventManager, never()).post(any(ComposerHostedMetadataInvalidationEvent.class));
  }

  @Test
  public void testAssetUpdatedEventWithDifferentNode() {
    when(assetUpdatedEvent.isLocal()).thenReturn(false);

    underTest.on(assetUpdatedEvent);

    verify(eventManager, never()).post(any(ComposerHostedMetadataInvalidationEvent.class));
  }

  @Test
  public void testAssetUpdatedEventWithNonZipballAsset() {
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(PACKAGES.name());

    underTest.on(assetUpdatedEvent);

    verify(eventManager, never()).post(any(ComposerHostedMetadataInvalidationEvent.class));
  }

  @Test
  public void testAssetUpdatedEvent() {
    underTest.on(assetUpdatedEvent);

    ArgumentCaptor<ComposerHostedMetadataInvalidationEvent> captor = ArgumentCaptor
        .forClass(ComposerHostedMetadataInvalidationEvent.class);

    verify(eventManager).post(captor.capture());

    ComposerHostedMetadataInvalidationEvent event = captor.getValue();

    assertThat(event.getProject(), is(PROJECT));
    assertThat(event.getVendor(), is(VENDOR));
    assertThat(event.getRepositoryName(), is(REPOSITORY_NAME));
  }

  @Test
  public void testAssetCreatedEventWithDifferentRepository() {
    when(assetCreatedEvent.getRepositoryName()).thenReturn("other-repository");

    underTest.on(assetCreatedEvent);

    verify(eventManager, never()).post(any(ComposerHostedMetadataInvalidationEvent.class));
  }

  @Test
  public void testAssetCreatedEventWithDifferentNode() {
    when(assetCreatedEvent.isLocal()).thenReturn(false);

    underTest.on(assetCreatedEvent);

    verify(eventManager, never()).post(any(ComposerHostedMetadataInvalidationEvent.class));
  }

  @Test
  public void testAssetCreatedEventWithNonZipballAsset() {
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(PACKAGES.toString());

    underTest.on(assetCreatedEvent);

    verify(eventManager, never()).post(any(ComposerHostedMetadataInvalidationEvent.class));
  }

  @Test
  public void testAssetCreatedEvent() {
    underTest.on(assetCreatedEvent);

    ArgumentCaptor<ComposerHostedMetadataInvalidationEvent> captor = ArgumentCaptor
        .forClass(ComposerHostedMetadataInvalidationEvent.class);

    verify(eventManager).post(captor.capture());

    ComposerHostedMetadataInvalidationEvent event = captor.getValue();

    assertThat(event.getProject(), is(PROJECT));
    assertThat(event.getVendor(), is(VENDOR));
    assertThat(event.getRepositoryName(), is(REPOSITORY_NAME));
  }

  @Test
  public void testComposerHostedMetadataInvalidationEventWithDifferentRepository() throws Exception {
    when(composerHostedMetadataInvalidationEvent.getRepositoryName()).thenReturn("other-repository");

    underTest.on(composerHostedMetadataInvalidationEvent);

    verify(hostedFacet, never()).rebuildProviderJson(any(String.class), any(String.class));
  }

  @Test
  public void testComposerHostedMetadataInvalidationEvent() throws Exception {
    underTest.on(composerHostedMetadataInvalidationEvent);

    verify(hostedFacet).rebuildProviderJson(VENDOR, PROJECT);
  }
}
