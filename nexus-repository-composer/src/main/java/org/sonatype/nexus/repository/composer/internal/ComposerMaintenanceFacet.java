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

import com.google.common.collect.ImmutableSet;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.composer.ComposerHostedFacet;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet;

import javax.inject.Named;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Facet for maintenance of Composer artifacts.
 */
@Facet.Exposed
@Named
public class ComposerMaintenanceFacet
    extends LastAssetMaintenanceFacet
{
  @Override
  public Set<String> deleteComponent(final Component component) {
    ImmutableSet.Builder<String> deletedPaths = ImmutableSet.builder();
    deletedPaths.addAll(super.deleteComponent(component));

    String vendor = component.namespace();
    String project = component.name();

    Optional<ComposerHostedFacet> hostedFacet = composerHosted();
    if (hostedFacet.isPresent()) {
      try {
        if (!hostedFacet.get().rebuildPackageJson(vendor, project).isPresent()) {
          deletedPaths.add(ComposerPathUtils.buildPackagePath(vendor, project));
        }
      } catch (IOException e) {
        // update failed
      }
      try {
        if (!hostedFacet.get().rebuildProviderJson(vendor, project).isPresent()) {
          deletedPaths.add(ComposerPathUtils.buildProviderPath(vendor, project));
        }
      } catch (IOException e) {
        // update failed
      }
    }

    return deletedPaths.build();
  }

  private Optional<ComposerHostedFacet> composerHosted() {
    return optionalFacet(ComposerHostedFacet.class);
  }
}
