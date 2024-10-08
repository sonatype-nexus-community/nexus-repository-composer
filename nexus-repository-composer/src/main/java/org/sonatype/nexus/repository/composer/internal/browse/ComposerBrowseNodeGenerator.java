/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2024-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.composer.internal.browse;

import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.composer.ComposerFormat;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.browse.ComponentPathBrowseNodeGenerator;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.text.Strings2.isBlank;
import static org.sonatype.nexus.repository.browse.node.BrowsePathBuilder.appendPath;
import static org.sonatype.nexus.repository.browse.node.BrowsePathBuilder.fromPaths;

/**
 * Browse node generator for Composer.
 */
@Singleton
@Named(ComposerFormat.NAME)
public class ComposerBrowseNodeGenerator
    extends ComponentPathBrowseNodeGenerator {

  @Override
  public List<BrowsePath> computeAssetPaths(final Asset asset) {
    checkNotNull(asset);

    return asset.component().map(component -> {

      // place asset under component, but use its true path as the request path for permission checks
      List<BrowsePath> assetPaths = computeComponentPaths(asset);
      appendPath(assetPaths, lastSegment(asset.path()), asset.path());
      return assetPaths;

    }).orElseGet(() -> super.computeAssetPaths(asset));
  }

  /**
   * Generates browse nodes to the component using the standard Maven layout.
   */
  @Override
  public List<BrowsePath> computeComponentPaths(final Asset asset) {
    checkNotNull(asset);

    Component component = asset.component().get(); // NOSONAR: caller guarantees this

    List<String> componentPath = pathToArtifactFolder(component);

    String version = component.version();
    if (!isBlank(version) && !version.equals(componentPath.get(componentPath.size() - 1))) {
      componentPath.add(version);
    }

    return fromPaths(componentPath, true);
  }

  /**
   * Generates a path to the artifact folder using the standard Composer layout.
   * <ul>
   * <li>Schema: /vendor/project/version/
   * </ul>
   */
  private List<String> pathToArtifactFolder(Component component) {
    List<String> paths = new ArrayList<>();

    String vendor = component.namespace();
    String project = component.name();
    String version = component.version();

    paths.add(vendor);
    paths.add(project);
    paths.add(version);

    return paths;
  }
}
