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
package org.sonatype.nexus.repository.composer;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;

/**
 * Interface defining the features supported by Composer repository hosted facets.
 */
@Facet.Exposed
public interface ComposerHostedFacet
    extends Facet
{
  FluentAsset upload(String vendor, String project, String version, String sourceType, String sourceUrl,
                     String sourceReference, Payload payload) throws IOException;

  Content getPackagesJson() throws IOException;

  Content getListJson(String filter) throws IOException;

  Content getProviderJson(String vendor, String project) throws IOException;

  Content getPackageJson(String vendor, String project) throws IOException;

  Optional<Content> rebuildPackageJson(String vendor, String project) throws IOException;

  Optional<Content> rebuildProviderJson(String vendor, String project) throws IOException;

  @Nullable
  Content getZipball(String path) throws IOException;
}
