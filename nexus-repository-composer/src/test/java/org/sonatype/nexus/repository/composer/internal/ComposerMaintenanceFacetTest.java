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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.ComposerHostedFacet;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.view.Content;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class ComposerMaintenanceFacetTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "test-repository-name";

  private static final String VENDOR = "test-vendor";

  private static final String PROJECT = "test-project";
  private static final String ZIPBALL_PATH = "/" + VENDOR + "/" + PROJECT + "/1.0.0/" + VENDOR + "-" + PROJECT + "-1.0.0.zip";
  private static final String PROVIDER_PATH = "/p/" + VENDOR + "/" + PROJECT + ".json";
  private static final String PACKAGE_PATH = "/p2/" + VENDOR + "/" + PROJECT + ".json";


  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private ComposerHostedFacet hostedFacet;

  @Mock
  private Component component;

  @Mock
  private FluentComponents fluentComponents;

  @Mock
  private FluentComponent fluentComponent;

  @Mock
  private FluentAsset fluentAsset;

  @Mock
  private Content content;

  private ComposerMaintenanceFacet underTest;

  @Before
  public void setUp() throws Exception {
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.optionalFacet(ComposerHostedFacet.class)).thenReturn(Optional.of(hostedFacet));

    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    when(fluentComponent.assets()).thenReturn(singleton(fluentAsset));
    when(fluentAsset.delete()).thenReturn(true);
    when(fluentAsset.path()).thenReturn(ZIPBALL_PATH);

    when(component.namespace()).thenReturn(VENDOR);
    when(component.name()).thenReturn(PROJECT);

    underTest = new ComposerMaintenanceFacet();
    underTest.attach(repository);
  }

  @Test
  public void testDeleteComponent() throws IOException {
    when(hostedFacet.rebuildPackageJson(VENDOR, PROJECT)).thenReturn(Optional.of(content));
    when(hostedFacet.rebuildProviderJson(VENDOR, PROJECT)).thenReturn(Optional.of(content));

    Set<String> deletedPaths = underTest.deleteComponent(component);
    assertEquals(singleton(ZIPBALL_PATH), deletedPaths);
  }

  @Test
  public void testDeleteComponentLast() throws IOException {
    when(hostedFacet.rebuildPackageJson(VENDOR, PROJECT)).thenReturn(Optional.empty());
    when(hostedFacet.rebuildProviderJson(VENDOR, PROJECT)).thenReturn(Optional.empty());

    Set<String> deletedPaths = underTest.deleteComponent(component);
    assertEquals(new HashSet<>(Arrays.asList(ZIPBALL_PATH, PROVIDER_PATH, PACKAGE_PATH)), deletedPaths);
  }

  @Test
  public void testDeleteProxyComponent() {
    when(repository.optionalFacet(ComposerHostedFacet.class)).thenReturn(Optional.empty());
    Set<String> deletedPaths = underTest.deleteComponent(component);
    assertEquals(singleton(ZIPBALL_PATH), deletedPaths);
  }
}
