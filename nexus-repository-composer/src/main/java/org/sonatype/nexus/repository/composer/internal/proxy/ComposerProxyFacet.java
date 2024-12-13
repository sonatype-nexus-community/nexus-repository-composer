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
package org.sonatype.nexus.repository.composer.internal.proxy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.composer.AssetKind;
import org.sonatype.nexus.repository.composer.ComposerContentFacet;
import org.sonatype.nexus.repository.composer.internal.ComposerJsonProcessor;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.facet.ContentProxyFacetSupport;
import org.sonatype.nexus.repository.view.*;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.*;
import static org.sonatype.nexus.repository.composer.internal.recipe.ComposerRecipeSupport.*;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;

/**
 * Proxy facet for a Composer repository.
 */
@Named
public class ComposerProxyFacet
    extends ContentProxyFacetSupport
{
  private static final String PACKAGES_JSON = "/packages.json";

  private static final String LIST_JSON = "/packages/list.json";

  private final ComposerJsonProcessor composerJsonProcessor;

  @Inject
  public ComposerProxyFacet(final ComposerJsonProcessor composerJsonProcessor) {
    this.composerJsonProcessor = checkNotNull(composerJsonProcessor);
  }

  @Nullable
  @Override
  protected Content fetch(Context context, Content stale) throws IOException {
    try {
      return super.fetch(context, stale);
    } catch (NonResolvableProviderJsonException e) {
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
    Optional<Content> content;
    switch (assetKind) {
      case PACKAGES:
        content = content().get(PACKAGES_JSON);
        break;
      case LIST:
        content = content().get(LIST_JSON);
        break;
      case PROVIDER:
        content = content().get(buildProviderPath(context));
        break;
      case PACKAGE:
        content = content().get(buildPackagePath(context));
        break;
      case ZIPBALL:
        content = content().get(buildZipballPath(context));
        break;
      default:
        throw new IllegalStateException();
    }

    return content.orElse(null);
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    Content res;
    switch (assetKind) {
      case PACKAGES:
        res = content().put(PACKAGES_JSON, generatePackagesJson(content), assetKind);
        break;
      case LIST:
        res = content().put(LIST_JSON, content, assetKind);
        break;
      case PROVIDER:
        res = content().put(buildProviderPath(context), content, assetKind);
        break;
      case PACKAGE:
        res = content().put(buildPackagePath(context), content, assetKind);
        break;
      case ZIPBALL:
        res = content().put(buildZipballPath(context), content, assetKind);
        break;
      default:
        throw new IllegalStateException();
    }

    return res;
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
      case PACKAGE:
        content().setCacheInfo(buildPackagePath(context), content, cacheInfo);
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

  private Content generatePackagesJson(final Content original) {
    try {
      Payload rewritten = composerJsonProcessor.rewritePackagesJson(getRepository(), original.getPayload());
      return new Content(original, rewritten);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String getZipballUrl(final Context context) {
    try {
      TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
      Map<String, String> tokens = state.getTokens();
      String vendor = tokens.get(VENDOR_TOKEN);
      String project = tokens.get(PROJECT_TOKEN);
      String version = tokens.get(VERSION_TOKEN);

      // try v2 package
      try {
        String path = buildPackagePath(vendor, project);
        Payload payload = getPackagePayload(context, path);
        if (payload != null) {
          return composerJsonProcessor.getDistUrlFromPackage(vendor, project, version, payload);
        }
      } catch (Exception e) {
        // ignored because we have a fallback
      }

      // try v2 package (dev versions)
      try {
        String path = buildPackagePathForDevVersions(vendor, project);
        Payload payload = getPackagePayload(context, path);
        if (payload != null) {
          return composerJsonProcessor.getDistUrlFromPackage(vendor, project, version, payload);
        }
      } catch (Exception e) {
        // ignored because we have a fallback
      }

      // try v1 provider
      String path = buildProviderPath(vendor, project);
      Payload payload = getProviderPayload(context, path);
      if (payload == null) {
        throw new NonResolvableProviderJsonException(
            String.format("No provider found for vendor %s, project %s, version %s", vendor, project, version));
      } else {
        return composerJsonProcessor.getDistUrl(vendor, project, version, payload);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  private ComposerContentFacet content() {
    return getRepository().facet(ComposerContentFacet.class);
  }

  private Payload getPackagePayload(final Context context, String path) throws Exception {
    Request request = new Request.Builder().action(GET).path(path)
        .attribute(ComposerProviderHandler.DO_NOT_REWRITE, "true").build();
    Response response = getRepository().facet(ViewFacet.class).dispatch(request, context);
    return response.getPayload();
  }

  private Payload getProviderPayload(final Context context, String path) throws Exception {
    return getPackagePayload(context, path);
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
