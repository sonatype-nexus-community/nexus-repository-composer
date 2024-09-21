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
package org.sonatype.nexus.repository.composer.internal.hosted;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.sonatype.goodies.common.Loggers;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.AssetKind;
import org.sonatype.nexus.repository.composer.ComposerContentFacet;
import org.sonatype.nexus.repository.composer.ComposerFormat;
import org.sonatype.nexus.repository.composer.ComposerHostedFacet;
import org.sonatype.nexus.repository.composer.internal.ComposerContentFacetImpl;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.importtask.ImportFileConfiguration;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.upload.*;
import org.sonatype.nexus.repository.view.*;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.repository.view.payloads.TempBlobPayload;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.*;
import static org.sonatype.nexus.repository.composer.internal.recipe.ComposerRecipeSupport.*;

/**
 * Upload handler for Composer hosted repositories.
 */
@Named(ComposerFormat.NAME)
@Singleton
public class ComposerHostedUploadHandler
    extends UploadHandlerSupport
    implements Handler
{

  protected static final Logger log = Preconditions.checkNotNull(Loggers.getLogger(ComposerHostedUploadHandler.class));

  private final VariableResolverAdapter variableResolverAdapter;
  private final ContentPermissionChecker contentPermissionChecker;

  private UploadDefinition definition;

  @Inject
  public ComposerHostedUploadHandler(
      final Set<UploadDefinitionExtension> uploadDefinitionExtensions,
      final VariableResolverAdapter variableResolverAdapter,
      final ContentPermissionChecker contentPermissionChecker
  )
  {
    super(uploadDefinitionExtensions);
    this.variableResolverAdapter = variableResolverAdapter;
    this.contentPermissionChecker = contentPermissionChecker;
  }

  @Override
  public @Nonnull Response handle(@Nonnull Context context) throws Exception {
    String vendor = getVendorToken(context);
    String project = getProjectToken(context);
    String version = getVersionToken(context);
    String sourceType = null;
    String sourceUrl = null;
    String sourceRef = null;
    Payload payload = null;

    Request request = checkNotNull(context.getRequest());
    Repository repository = context.getRepository();
    // if we have also the source url and reference which have been sent in data
    if (request.isMultipart() && request.getMultiparts() != null) {
      for (PartPayload part : request.getMultiparts()) {
        log.trace("Part with fieldName: {}, name: {}, type: {}, isFormField: {} and with type: {}",
            part.getFieldName(),
            part.getName(),
            part.getContentType(),
            part.isFormField(),
            part.getClass().getName()
        );
        if (SOURCE_TYPE_FIELD_NAME.equals(part.getFieldName())) {
          sourceType = checkNotNull(readPartStreamToString(part));
        } else if (SOURCE_URL_FIELD_NAME.equals(part.getFieldName())) {
          sourceUrl = checkNotNull(readPartStreamToString(part));
        } else if (SOURCE_REFERENCE_FIELD_NAME.equals(part.getFieldName())) {
          sourceRef = checkNotNull(readPartStreamToString(part));
        } else if (PACKAGE_FIELD_NAME.equals(part.getFieldName())) {
          payload = readPartStreamToBytePayload(part);
        }
      }
      log.trace("Upload with source data: {} with url {} and reference {} and data exists: {}",
          sourceType,
          sourceUrl,
          sourceRef,
          payload != null
      );
    } else {
      payload = checkNotNull(request.getPayload());
      log.trace("Payload for single file is of type: {} with content type: {}",
          payload.getClass().getName(),
          payload.getContentType()
      );
    }

    ComposerHostedFacet hostedFacet = repository.facet(ComposerHostedFacet.class);

    hostedFacet.upload(vendor, project, version, sourceType, sourceUrl, sourceRef, payload);

    hostedFacet.rebuildProviderJson(vendor, project);
    hostedFacet.rebuildPackageJson(vendor, project);

    return HttpResponses.ok();
  }

  @Override
  public UploadResponse handle(final Repository repository, final ComponentUpload upload) throws IOException {
    AssetUpload assetUpload = upload.getAssetUploads().get(0);
    String vendor = checkNotNull(assetUpload.getField(VENDOR_TOKEN));
    String project = checkNotNull(assetUpload.getField(PROJECT_TOKEN));
    String version = checkNotNull(assetUpload.getField(VERSION_TOKEN));

    Payload payload = checkNotNull(upload.getAssetUploads().get(0).getPayload());
    log.trace("Payload for single file is of type: {} with content type: {}",
        payload.getClass().getName(),
        payload.getContentType()
    );

    ComposerHostedFacet hostedFacet = repository.facet(ComposerHostedFacet.class);

    FluentAsset asset = hostedFacet.upload(vendor, project, version, null, null, null, payload);

    hostedFacet.rebuildProviderJson(vendor, project);
    hostedFacet.rebuildPackageJson(vendor, project);

    return new UploadResponse(singletonList(asset.path()));
  }

  @Override
  public Content handle(final Repository repository, final File content, final String path) throws IOException {
    ImportFileConfiguration configuration = new ImportFileConfiguration(repository, content, path);
    return handle(configuration);
  }

  @Override
  public Content handle(final ImportFileConfiguration configuration) throws IOException {
    Repository repository = configuration.getRepository();
    String path = configuration.getAssetName();
    File content = configuration.getFile();
    Path contentPath = content.toPath();

    ComposerContentFacet contentFacet = repository.facet(ComposerContentFacet.class);
    String contentType = Files.probeContentType(contentPath);
    try (TempBlob blob = contentFacet.blobs().ingest(contentPath, contentType, ComposerContentFacetImpl.hashAlgorithms,
        configuration.isHardLinkingEnabled())) {
      ComposerContentFacet composerFacet = repository.facet(ComposerContentFacet.class);
      return composerFacet.put(path, new TempBlobPayload(blob, contentType), AssetKind.ZIPBALL);
    }
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      definition = getDefinition(
          ComposerFormat.NAME,
          false,
          emptyList(),
          Arrays.asList(
              new UploadFieldDefinition(VENDOR_TOKEN, false, UploadFieldDefinition.Type.STRING),
              new UploadFieldDefinition(PROJECT_TOKEN, false, UploadFieldDefinition.Type.STRING),
              new UploadFieldDefinition(VERSION_TOKEN, false, UploadFieldDefinition.Type.STRING)
          ),
          new UploadRegexMap("([^/]+)/([^/]+)/(Method...)", VENDOR_TOKEN, PROJECT_TOKEN, VERSION_TOKEN))
      ;
    }
    return definition;
  }

  @Override
  public VariableResolverAdapter getVariableResolverAdapter() {
    return variableResolverAdapter;
  }

  @Override
  public ContentPermissionChecker contentPermissionChecker() {
    return contentPermissionChecker;
  }


  private BytesPayload readPartStreamToBytePayload(final PartPayload in) throws IOException {
    try (InputStream is = in.openInputStream()) {
      return new BytesPayload(ByteStreams.toByteArray(is), in.getContentType());
    } finally {
      in.close();
    }
  }

  private String readPartStreamToString(final PartPayload in) throws IOException {
    try {
      return CharStreams.toString(new InputStreamReader(in.openInputStream(), UTF_8));
    } finally {
      in.close();
    }
  }
}
