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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.buildZipballPath;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.getProjectToken;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.getVendorToken;

/**
 * Download handler for Composer hosted repositories.
 */
@Named
@Singleton
public class ComposerHostedDownloadHandler
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    Repository repository = context.getRepository();
    ComposerHostedFacet hostedFacet = repository.facet(ComposerHostedFacet.class);
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case PACKAGES:
        return HttpResponses.ok(hostedFacet.getPackagesJson());
      case LIST:
        throw new IllegalStateException("Unsupported assetKind: " + assetKind);
      case PROVIDER:
        return HttpResponses.ok(hostedFacet.getProviderJson(getVendorToken(context), getProjectToken(context)));
      case ZIPBALL:
        return responseFor(hostedFacet.getZipball(buildZipballPath(context)));
      default:
        throw new IllegalStateException("Unexpected assetKind: " + assetKind);
    }
  }

  private Response responseFor(@Nullable final Content content) {
    if (content == null) {
      return HttpResponses.notFound();
    }
    return HttpResponses.ok(content);
  }
}
