/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.Lists;

/**
 * Support for uploading components via UI & API
 *
 */
@Named(ComposerFormat.NAME)
@Singleton
public class ComposerUploadHandler
    extends ComposerUploadHandlerSupport
{
  @Inject
  public ComposerUploadHandler(
      final ContentPermissionChecker contentPermissionChecker,
      @Named("simple") final VariableResolverAdapter variableResolverAdapter,
      final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
  {
    super(contentPermissionChecker, variableResolverAdapter, uploadDefinitionExtensions, false);
  }

  @Override
  protected List<Content> getResponseContents(String vendor, String name, String version, final Repository repository, final Map<String, PartPayload> pathToPayload)
      throws IOException
  {
    ComposerHostedFacet hostedFacet = repository.facet(ComposerHostedFacet.class);

    List<Content> responseContents = Lists.newArrayList();
    UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
    try {
      for (Entry<String, PartPayload> entry : pathToPayload.entrySet()) {
        PartPayload payload = entry.getValue();
        hostedFacet.upload(vendor, name, version, null, null, null, payload);
        Content content = new Content(payload);
        responseContents.add(content);
      }
    }
    finally {
      UnitOfWork.end();
    }
    return responseContents;
  }
}
