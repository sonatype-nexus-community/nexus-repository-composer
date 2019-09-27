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
import java.io.UncheckedIOException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.buildProviderPath;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.buildZipballPath;
import static org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport.PROJECT_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport.VENDOR_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport.VERSION_TOKEN;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;

/**
 * Proxy facet for a Composer repository.
 */
@Named
public class ComposerProxyFacetImpl
    extends ProxyFacetSupport
{
  private static final String PACKAGES_JSON = "packages.json";

  private static final String LIST_JSON = "packages/list.json";

  private final ComposerJsonProcessor composerJsonProcessor;

  @Inject
  public ComposerProxyFacetImpl(final ComposerJsonProcessor composerJsonProcessor) {
    this.composerJsonProcessor = checkNotNull(composerJsonProcessor);
  }

  @Nullable
  @Override
  protected Content fetch(Context context, Content stale) throws IOException {
    try {
      return super.fetch(context, stale);
    }
    catch (NonResolvableProviderJsonException e) {
      log.debug("Composer provider URL not resolvable: {}", e.getMessage());
      return null;
    }
  }

  // HACK: Workaround for known CGLIB issue, forces an Import-Package for org.sonatype.nexus.repository.config
  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    super.doValidate(configuration);
  }

  @Nullable
  @Override
  protected Content getCachedContent(final Context context) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case PACKAGES:
        return content().get(PACKAGES_JSON);
      case LIST:
        return content().get(LIST_JSON);
      case PROVIDER:
        return content().get(buildProviderPath(context));
      case ZIPBALL:
        return content().get(buildZipballPath(context));
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case PACKAGES:
        return content().put(PACKAGES_JSON, generatePackagesJson(context), assetKind);
      case LIST:
        return content().put(LIST_JSON, content, assetKind);
      case PROVIDER:
        return content().put(buildProviderPath(context), content, assetKind);
      case ZIPBALL:
        return content().put(buildZipballPath(context), content, assetKind);
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo)
      throws IOException
  {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case PACKAGES:
        content().setCacheInfo(PACKAGES_JSON, content, cacheInfo);
        break;
      case LIST:
        content().setCacheInfo(LIST_JSON, content, cacheInfo);
        break;
      case PROVIDER:
        content().setCacheInfo(buildProviderPath(context), content, cacheInfo);
        break;
      case ZIPBALL:
        content().setCacheInfo(buildZipballPath(context), content, cacheInfo);
        break;
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case ZIPBALL:
        return getZipballUrl(context);
      default:
        return context.getRequest().getPath().substring(1);
    }
  }

  @Nonnull
  @Override
  protected CacheController getCacheController(@Nonnull final Context context) {
    final AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    return cacheControllerHolder.require(assetKind.getCacheType());
  }

  private Content generatePackagesJson(final Context context) throws IOException {
    try {
      // TODO: Better logging and error checking on failure/non-200 scenarios
      Request request = new Request.Builder().action(GET).path("/" + LIST_JSON).build();
      Response response = getRepository().facet(ViewFacet.class).dispatch(request, context);
      Payload payload = checkNotNull(response.getPayload());
      return composerJsonProcessor.generatePackagesFromList(getRepository(), payload);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  private String getZipballUrl(final Context context) {
    try {
      TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
      Map<String, String> tokens = state.getTokens();
      String vendor = tokens.get(VENDOR_TOKEN);
      String project = tokens.get(PROJECT_TOKEN);
      String version = tokens.get(VERSION_TOKEN);

      Request request = new Request.Builder().action(GET).path("/" + buildProviderPath(vendor, project))
          .attribute(ComposerProviderHandler.DO_NOT_REWRITE, "true").build();
      Response response = getRepository().facet(ViewFacet.class).dispatch(request, context);
      Payload payload = response.getPayload();
      if (payload == null) {
        throw new NonResolvableProviderJsonException(
            String.format("No provider found for vendor %s, project %s, version %s", vendor, project, version));
      }
      else {
        return composerJsonProcessor.getDistUrl(vendor, project, version, payload);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  private ComposerContentFacet content() {
    return getRepository().facet(ComposerContentFacet.class);
  }

  @VisibleForTesting
  static class NonResolvableProviderJsonException
      extends RuntimeException
  {
    public NonResolvableProviderJsonException(final String message) {
      super(message);
    }
  }
}
