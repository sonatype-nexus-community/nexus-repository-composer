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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event fired when metadata (the provider.json) for a particular vendor/project in a Composer hosted repository is no
 * longer valid. This happens when an asset is created, updated, or deleted and the corresponding provider.json must be
 * rebuilt.
 */
public class ComposerHostedMetadataInvalidationEvent
{
  private final String repositoryName;

  private final String vendor;

  private final String project;

  public ComposerHostedMetadataInvalidationEvent(final String repositoryName,
                                                 final String vendor,
                                                 final String project)
  {
    this.repositoryName = checkNotNull(repositoryName);
    this.vendor = checkNotNull(vendor);
    this.project = checkNotNull(project);
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getVendor() {
    return vendor;
  }

  public String getProject() {
    return project;
  }
}
