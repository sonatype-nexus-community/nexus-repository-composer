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

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.getProjectToken;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.getVendorToken;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.getVersionToken;

/**
 * Upload handler for Composer hosted repositories.
 */
@Named
@Singleton
public class ComposerHostedUploadHandler
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    String vendor = getVendorToken(context);
    String project = getProjectToken(context);
    String version = getVersionToken(context);

    Request request = checkNotNull(context.getRequest());
    Payload payload = checkNotNull(request.getPayload());

    Repository repository = context.getRepository();
    ComposerHostedFacet hostedFacet = repository.facet(ComposerHostedFacet.class);

    hostedFacet.upload(vendor, project, version, payload);
    return HttpResponses.ok();
  }
}
