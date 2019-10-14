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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetCreatedEvent;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.AssetUpdatedEvent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.joda.time.DateTime;

import static java.util.Objects.requireNonNull;
import static org.joda.time.DateTime.now;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.composer.internal.ComposerAttributes.P_PROJECT;
import static org.sonatype.nexus.repository.composer.internal.ComposerAttributes.P_VENDOR;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * Concrete implementation of {@code ComposerHostedMetadataFacet} using events. Essentially a stripped-down form of the
 * mechanism behind the Yum createrepo implementation in our internal codebase.
 */
@Named
public class ComposerHostedMetadataFacetImpl
    extends FacetSupport
    implements ComposerHostedMetadataFacet
{
  private final EventManager eventManager;

  @Inject
  public ComposerHostedMetadataFacetImpl(final EventManager eventManager)
  {
    this.eventManager = requireNonNull(eventManager);
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetDeletedEvent deleted) {
    if (matchesRepository(deleted) && isEventRelevant(deleted)) {
      invalidateMetadata(deleted);
    }
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetCreatedEvent created) {
    if (matchesRepository(created) && isEventRelevant(created)) {
      invalidateMetadata(created);
    }
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetUpdatedEvent updated) {
    if (matchesRepository(updated) && isEventRelevant(updated) && hasBlobBeenUpdated(updated)) {
      invalidateMetadata(updated);
    }
  }

  @Subscribe
  @Guarded(by = STARTED)
  public void on(final ComposerHostedMetadataInvalidationEvent event) throws IOException {
    if (getRepository().getName().equals(event.getRepositoryName())) {
      ComposerHostedFacet hostedFacet = getRepository().facet(ComposerHostedFacet.class);
      UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
      try {
        hostedFacet.rebuildProviderJson(event.getVendor(), event.getProject());
      }
      finally {
        UnitOfWork.end();
      }
    }
  }

  private void invalidateMetadata(final AssetEvent assetEvent) {
    Asset asset = assetEvent.getAsset();
    String vendor = asset.formatAttributes().require(P_VENDOR, String.class);
    String project = asset.formatAttributes().require(P_PROJECT, String.class);
    eventManager.post(new ComposerHostedMetadataInvalidationEvent(getRepository().getName(), vendor, project));
  }

  private boolean matchesRepository(final AssetEvent assetEvent) {
    return assetEvent.isLocal() && getRepository().getName().equals(assetEvent.getRepositoryName());
  }

  private boolean isEventRelevant(final AssetEvent event) {
    return AssetKind.ZIPBALL.name().equals(event.getAsset().formatAttributes().get(P_ASSET_KIND, String.class));
  }

  private boolean hasBlobBeenUpdated(final AssetUpdatedEvent updated) {
    DateTime blobUpdated = updated.getAsset().blobUpdated();
    DateTime oneMinuteAgo = now().minusMinutes(1);
    return blobUpdated == null || blobUpdated.isAfter(oneMinuteAgo);
  }
}
