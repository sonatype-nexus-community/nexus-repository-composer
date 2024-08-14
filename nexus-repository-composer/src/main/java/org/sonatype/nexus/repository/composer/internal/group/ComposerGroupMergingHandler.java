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
package org.sonatype.nexus.repository.composer.internal.group;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;

import static org.sonatype.nexus.repository.http.HttpConditions.makeConditional;
import static org.sonatype.nexus.repository.http.HttpConditions.makeUnconditional;

/**
 * Abstract handler for merging in the context of a Composer group repository, with merging left to concrete
 * implementations of the class.
 */
public abstract class ComposerGroupMergingHandler
    extends GroupHandler
{
  @Override
  protected final Response doGet(@Nonnull final Context context,
                                 @Nonnull final GroupHandler.DispatchedRepositories dispatched)
      throws Exception
  {
    Repository repository = context.getRepository();
    GroupFacet groupFacet = repository.facet(GroupFacet.class);

    makeUnconditional(context.getRequest());
    Map<Repository, Response> responses;
    try {
      responses = getAll(context, groupFacet.members(), dispatched);
    }
    finally {
      makeConditional(context.getRequest());
    }

    List<Payload> payloads = responses.values().stream()
        .filter(response -> response.getStatus().getCode() == HttpStatus.OK && response.getPayload() != null)
        .map(Response::getPayload)
        .collect(Collectors.toList());
    if (payloads.isEmpty()) {
      return notFoundResponse(context);
    }
    return HttpResponses.ok(merge(repository, payloads));
  }

  protected abstract Content merge(final Repository repository, final List<Payload> payloads) throws Exception;
}
