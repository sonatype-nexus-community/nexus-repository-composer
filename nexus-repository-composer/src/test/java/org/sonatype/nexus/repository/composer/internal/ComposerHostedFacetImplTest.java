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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.composer.internal.AssetKind.PROVIDER;
import static org.sonatype.nexus.repository.composer.internal.AssetKind.ZIPBALL;

public class ComposerHostedFacetImplTest
    extends TestSupport
{
  private static final String VENDOR = "vendor";

  private static final String PROJECT = "project";

  private static final String VERSION = "version";

  private static final String SRC_TYPE = "src-type";

  private static final String SRC_URL = "src-url";

  private static final String SRC_REF = "src-ref";

  private static final String ZIPBALL_PATH = "vendor/project/version/vendor-project-version.zip";

  private static final String PROVIDER_PATH = "p/vendor/project.json";

  @Mock
  private Repository repository;

  @Mock
  private Bucket bucket;

  @Mock
  private ComposerContentFacet composerContentFacet;

  @Mock
  private ComposerJsonProcessor composerJsonProcessor;

  @Mock
  private Payload payload;

  @Mock
  private Content content;

  @Mock
  private StorageTx tx;

  @Mock
  private Iterable<Component> components;

  @Mock
  private Query query;

  private ComposerHostedFacetImpl underTest;

  @Before
  public void setUp() throws Exception {
    when(repository.facet(ComposerContentFacet.class)).thenReturn(composerContentFacet);
    when(tx.findBucket(repository)).thenReturn(bucket);

    underTest = spy(new ComposerHostedFacetImpl(composerJsonProcessor));
    underTest.attach(repository);

    UnitOfWork.beginBatch(tx);
  }

  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test
  public void testUpload() throws Exception {
    underTest.upload(VENDOR, PROJECT, VERSION, SRC_TYPE, SRC_URL, SRC_REF, payload);
    verify(composerContentFacet).put(ZIPBALL_PATH, payload, SRC_TYPE, SRC_URL, SRC_REF);
  }

  @Test
  public void testGetZipball() throws Exception {
    when(composerContentFacet.get(ZIPBALL_PATH)).thenReturn(content);
    assertThat(underTest.getZipball(ZIPBALL_PATH), is(content));
  }

  @Test
  public void testGetPackagesJson() throws Exception {
    when(tx.browseComponents(bucket)).thenReturn(components);
    when(composerJsonProcessor.generatePackagesFromComponents(repository, components)).thenReturn(content);
    assertThat(underTest.getPackagesJson(), is(content));
  }

  @Test
  public void testGetProviderJson() throws Exception {
    when(composerContentFacet.get(PROVIDER_PATH)).thenReturn(content);
    assertThat(underTest.getProviderJson(VENDOR, PROJECT), is(content));
  }

  @Test
  public void testBuildProviderJson() throws Exception {
    when(underTest.buildQuery(VENDOR, PROJECT)).thenReturn(query);
    when(tx.findComponents(eq(query), eq(singletonList(repository)))).thenReturn(components);
    when(composerJsonProcessor.buildProviderJson(repository, tx, components)).thenReturn(content);
    underTest.rebuildProviderJson(VENDOR, PROJECT);
    verify(composerContentFacet).put(PROVIDER_PATH, content, PROVIDER);
  }

  @Test
  public void testBuildQuery() throws Exception {
    Query result = underTest.buildQuery(VENDOR, PROJECT);
    assertThat(result.getWhere(), is("group = :p0 AND name = :p1"));
    assertThat(result.getParameters(), hasEntry("p0", VENDOR));
    assertThat(result.getParameters(), hasEntry("p1", PROJECT));
  }
}
