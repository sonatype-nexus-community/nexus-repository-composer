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
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.sonatype.goodies.common.Loggers;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.*;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.getProjectToken;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.getVendorToken;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.getVersionToken;
import static org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport.*;

/**
 * Upload handler for Composer hosted repositories.
 */
@Named
@Singleton
public class ComposerHostedUploadHandler
    implements Handler {

  protected static final Logger log = Preconditions.checkNotNull(Loggers.getLogger(ComposerHostedUploadHandler.class));

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
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
    return HttpResponses.ok();
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
