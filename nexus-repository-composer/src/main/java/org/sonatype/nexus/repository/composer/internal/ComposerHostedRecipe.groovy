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
package org.sonatype.nexus.repository.composer.internal

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet

import static org.sonatype.nexus.repository.composer.internal.AssetKind.PACKAGES
import static org.sonatype.nexus.repository.composer.internal.AssetKind.PROVIDER
import static org.sonatype.nexus.repository.composer.internal.AssetKind.ZIPBALL

/**
 * Recipe for creating a Composer hosted repository.
 */
@Named(ComposerHostedRecipe.NAME)
@Singleton
class ComposerHostedRecipe
    extends ComposerRecipeSupport
{
  public static final String NAME = 'composer-hosted'

  @Inject
  Provider<ComposerHostedFacet> hostedFacet

  @Inject
  Provider<ComposerHostedMetadataFacet> hostedMetadataFacet

  @Inject
  ComposerHostedUploadHandler uploadHandler

  @Inject
  ComposerHostedDownloadHandler downloadHandler

  @Inject
  ComposerHostedRecipe(@Named(HostedType.NAME) final Type type, @Named(ComposerFormat.NAME) final Format format) {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(storageFacet.get())
    repository.attach(contentFacet.get())
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(componentMaintenanceFacet.get())
    repository.attach(hostedFacet.get())
    repository.attach(hostedMetadataFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(attributesFacet.get())
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    builder.route(packagesMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(PACKAGES))
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(downloadHandler)
        .create())

    builder.route(providerMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(PROVIDER))
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(downloadHandler)
        .create())

    builder.route(zipballMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(ZIPBALL))
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(downloadHandler)
        .create())

    builder.route(uploadMatcher()
        .handler(timingHandler)
        .handler(assetKindHandler.rcurry(ZIPBALL))
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(uploadHandler)
        .create())

    addBrowseUnsupportedRoute(builder)

    builder.defaultHandlers(HttpHandlers.notFound())

    facet.configure(builder.create())

    return facet
  }
}
