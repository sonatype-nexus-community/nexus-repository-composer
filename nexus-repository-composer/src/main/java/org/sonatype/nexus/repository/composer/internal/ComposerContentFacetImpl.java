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

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.composer.AssetKind;
import org.sonatype.nexus.repository.composer.ComposerContentFacet;
import org.sonatype.nexus.repository.composer.ComposerFormat;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.*;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.normalizeAssetPath;
import static org.sonatype.nexus.repository.composer.internal.recipe.ComposerRecipeSupport.*;

/**
 * Default (and currently only) implementation of {@code ComposerContentFacet}.
 */
@Named
public class ComposerContentFacetImpl
    extends ContentFacetSupport
    implements ComposerContentFacet
{
  public static final List<HashAlgorithm> hashAlgorithms = Arrays.asList(MD5, SHA1, SHA256);

  private final ComposerFormatAttributesExtractor composerFormatAttributesExtractor;

  @Inject
  public ComposerContentFacetImpl(@Named(ComposerFormat.NAME) final FormatStoreManager formatStoreManager,
                                  final ComposerFormatAttributesExtractor composerFormatAttributesExtractor)
  {
    super(formatStoreManager);
    this.composerFormatAttributesExtractor = composerFormatAttributesExtractor;
  }

  @Override
  public Optional<FluentAsset> getAsset(final String path) {
    return assets().path(path).find();
  }

  @Override
  public Optional<Content> get(final String assetPath) {
    return assets().path(assetPath).find().map(FluentAsset::download);
  }

  @Override
  public Content put(final String path, final Payload payload, final AssetKind assetKind) throws IOException {
    try (TempBlob tempBlob = getTempBlob(payload)) {
      FluentAsset asset;
      switch (assetKind) {
        case ZIPBALL:
          asset = findOrCreateContentAsset(path, tempBlob, assetKind, null, null, null);
          break;
        case PACKAGES:
        case PACKAGE:
        case LIST:
        case PROVIDER:
          asset = findOrCreateMetadataAsset(path, tempBlob, assetKind);
          break;
        default:
          throw new IllegalStateException("Unexpected asset kind: " + assetKind);
      }

      return asset
          .markAsCached(payload)
          .download();
    }
  }

  @Override
  public FluentAsset put(final String path, final Payload payload, final String sourceType, final String sourceUrl,
                         final String sourceReference) throws IOException
  {
    try (TempBlob tempBlob = blobs().ingest(payload, hashAlgorithms)) {
      return findOrCreateContentAsset(path, tempBlob, AssetKind.ZIPBALL, sourceType, sourceUrl, sourceReference);
    }
  }

  @Override
  public TempBlob getTempBlob(final Payload payload) {
    checkNotNull(payload);
    return blobs().ingest(payload, hashAlgorithms);
  }

  @Override
  public TempBlob getTempBlob(final InputStream in, @Nullable final String contentType) {
    checkNotNull(in);
    return blobs().ingest(in, contentType, hashAlgorithms);
  }

  @Override
  public void setCacheInfo(final String path, final Content content, final CacheInfo cacheInfo) {
    Asset asset = content.getAttributes().get(Asset.class);
    if (asset == null) {
      log.debug("Attempting to set cache info for non-existent Composer asset {}", path);
      return;
    }

    assets().with(asset).markAsCached(cacheInfo);
  }

  protected FluentAsset findOrCreateMetadataAsset(final String path, final TempBlob tempBlob, final AssetKind assetKind) {
    return assets()
        .path(path)
        .kind(assetKind.name())
        .blob(tempBlob)
        .save();
  }

  protected FluentAsset findOrCreateContentAsset(final String path,
                                                 final TempBlob tempBlob,
                                                 final AssetKind assetKind,
                                                 final String sourceType,
                                                 final String sourceUrl,
                                                 final String sourceReference) throws IOException
  {
    String[] parts = path.split("/");
    String group = parts[1];
    String name = parts[2];
    String version = parts[3];

    FluentComponent component = findOrCreateComponent(group, name, version);
    if (sourceType != null) {
      component = component.withAttribute(SOURCE_TYPE_FIELD_NAME, sourceType);
    }
    if (sourceUrl != null) {
      component = component.withAttribute(SOURCE_URL_FIELD_NAME, sourceUrl);
    }
    if (sourceReference != null) {
      component = component.withAttribute(SOURCE_REFERENCE_FIELD_NAME, sourceReference);
    }

    component = composerFormatAttributesExtractor.extractFromZip(tempBlob, component);

    return assets()
        .path(normalizeAssetPath(path))
        .kind(assetKind.name())
        .component(component)
        .blob(tempBlob)
        .save();

  }

  private FluentComponent findOrCreateComponent(final String vendor, final String project, final String version) {
    return components()
        .name(project)
        .version(version)
        .normalizedVersion(versionNormalizerService().getNormalizedVersionByFormat(version, repository().getFormat()))
        .namespace(vendor)
        .getOrCreate();
  }
}
