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

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * Default implementation of a Composer hosted facet.
 */
@Named
public class ComposerHostedFacetImpl
    extends FacetSupport
    implements ComposerHostedFacet
{
  private final ComposerJsonProcessor composerJsonProcessor;

  @Inject
  public ComposerHostedFacetImpl(final ComposerJsonProcessor composerJsonProcessor) {
    this.composerJsonProcessor = checkNotNull(composerJsonProcessor);
  }

  @Override
  @TransactionalStoreBlob
  public void upload(final String vendor, final String project, final String version, final String sourceType,
                     final String sourceUrl, final String sourceReference, final Payload payload)
      throws IOException {
    content().put(
        ComposerPathUtils.buildZipballPath(vendor, project, version),
        payload,
        sourceType,
        sourceUrl,
        sourceReference
    );
  }

  @Override
  public Content getZipball(final String path) throws IOException {
    return content().get(path);
  }

  @Override
  @TransactionalTouchMetadata
  public Content getPackagesJson() throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    return composerJsonProcessor
        .generatePackagesFromComponents(getRepository(), tx.browseComponents(tx.findBucket(getRepository())));
  }

  @Override
  @TransactionalTouchMetadata
  public Content getProviderJson(final String vendor, final String project) throws IOException {
    return content().get(ComposerPathUtils.buildProviderPath(vendor, project));
  }

  @Override
  @TransactionalStoreBlob
  public void rebuildProviderJson(final String vendor, final String project) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Content content = composerJsonProcessor.buildProviderJson(getRepository(), tx,
        tx.findComponents(buildQuery(vendor, project), singletonList(getRepository())));
    content().put(ComposerPathUtils.buildProviderPath(vendor, project), content, AssetKind.PROVIDER);
  }

  @VisibleForTesting
  protected Query buildQuery(final String vendor, final String project) {
    return Query.builder().where(P_GROUP).eq(vendor).and(P_NAME).eq(project).build();
  }

  private ComposerContentFacet content() {
    return getRepository().facet(ComposerContentFacet.class);
  }
}
