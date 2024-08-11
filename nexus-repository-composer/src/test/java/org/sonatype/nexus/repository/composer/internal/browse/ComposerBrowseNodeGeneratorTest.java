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

import org.junit.Test;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.ComponentData;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ComposerBrowseNodeGeneratorTest
    extends TestSupport
{
  private final ComposerBrowseNodeGenerator underTest = new ComposerBrowseNodeGenerator();

  @Test
  public void testComputeAssetPaths() {
    AssetData asset = new AssetData();
    asset.setPath("/v3nd0r/p4ck4g3/13.3.7/v3nd0r-p4ck4g3-13.3.7.zip");
    asset.setComponent(createComponent("v3nd0r", "p4ck4g3", "13.3.7"));
    List<BrowsePath> browsePaths = underTest.computeAssetPaths(asset);
    assertEquals(
        Arrays.asList(
            new BrowsePath("v3nd0r", "/v3nd0r/"),
            new BrowsePath("p4ck4g3", "/v3nd0r/p4ck4g3/"),
            new BrowsePath("13.3.7", "/v3nd0r/p4ck4g3/13.3.7/"),
            new BrowsePath("v3nd0r-p4ck4g3-13.3.7.zip", "/v3nd0r/p4ck4g3/13.3.7/v3nd0r-p4ck4g3-13.3.7.zip")
        ),
        browsePaths
    );
  }
  @Test
  public void computeComponentPaths() {
    AssetData asset = new AssetData();
    asset.setPath("/v3nd0r/p4ck4g3/13.3.7/v3nd0r-p4ck4g3-13.3.7.zip");
    asset.setComponent(createComponent("v3nd0r", "p4ck4g3", "13.3.7"));
    List<BrowsePath> browsePaths = underTest.computeAssetPaths(asset);
    assertEquals(
        Arrays.asList(
            new BrowsePath("v3nd0r", "/v3nd0r/"),
            new BrowsePath("p4ck4g3", "/v3nd0r/p4ck4g3/"),
            new BrowsePath("13.3.7", "/v3nd0r/p4ck4g3/13.3.7/"),
            new BrowsePath("v3nd0r-p4ck4g3-13.3.7.zip", "/v3nd0r/p4ck4g3/13.3.7/v3nd0r-p4ck4g3-13.3.7.zip")
        ),
        browsePaths
    );
  }

  private static Component createComponent(String vendor, String packageName, String version) {
    ComponentData componentData = new ComponentData();
    componentData.setRepositoryId(1);
    componentData.setComponentId(1);
    componentData.setNamespace(vendor);
    componentData.setName(packageName);
    componentData.setVersion(version);
    return componentData;
  }
}
