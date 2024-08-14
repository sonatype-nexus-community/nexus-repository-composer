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

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.AssetKind;
import org.sonatype.nexus.repository.composer.ComposerHostedFacet;
import org.sonatype.nexus.repository.composer.internal.hosted.ComposerHostedDownloadHandler;
import org.sonatype.nexus.repository.view.*;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.composer.AssetKind.*;
import static org.sonatype.nexus.repository.composer.internal.recipe.ComposerRecipeSupport.NAME_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.recipe.ComposerRecipeSupport.PROJECT_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.recipe.ComposerRecipeSupport.VENDOR_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.recipe.ComposerRecipeSupport.VERSION_TOKEN;

public class ComposerHostedDownloadHandlerTest
    extends TestSupport
{
  private static final String VENDOR = "testvendor";

  private static final String PROJECT = "testproject";

  private static final String VERSION = "testversion";

  private static final String NAME = "testvendor-testproject-testversion";

  private static final String ZIPBALL_PATH = "/testvendor/testproject/testversion/testvendor-testproject-testversion.zip";

  @Mock
  private Map<String, String> tokens;

  @Mock
  private TokenMatcher.State state;

  @Mock
  private ComposerHostedFacet composerHostedFacet;

  @Mock
  private Repository repository;

  @Mock
  private Context context;

  @Mock
  private Content content;

  @Mock
  private Request request;

  @Mock
  private Parameters parameters;

  @Mock
  private AttributesMap attributes;

  private ComposerHostedDownloadHandler underTest = new ComposerHostedDownloadHandler();

  @Before
  public void setUp() {
    when(repository.facet(ComposerHostedFacet.class)).thenReturn(composerHostedFacet);
    when(context.getRepository()).thenReturn(repository);
    when(context.getAttributes()).thenReturn(attributes);
    when(context.getRequest()).thenReturn(request);
    when(request.getParameters()).thenReturn(parameters);
    when(attributes.require(TokenMatcher.State.class)).thenReturn(state);
    when(state.getTokens()).thenReturn(tokens);
  }

  @Test
  public void testHandlePackages() throws Exception {
    when(attributes.require(AssetKind.class)).thenReturn(PACKAGES);
    when(composerHostedFacet.getPackagesJson()).thenReturn(content);
    Response response = underTest.handle(context);
    assertThat(response.getStatus().getCode(), is(200));
    assertThat(response.getPayload(), is(content));
  }

  @Test
  public void testHandleProvider() throws Exception {
    when(attributes.require(AssetKind.class)).thenReturn(PROVIDER);
    when(tokens.get(VENDOR_TOKEN)).thenReturn(VENDOR);
    when(tokens.get(PROJECT_TOKEN)).thenReturn(PROJECT);
    when(composerHostedFacet.getProviderJson(VENDOR, PROJECT)).thenReturn(content);
    Response response = underTest.handle(context);
    assertThat(response.getStatus().getCode(), is(200));
    assertThat(response.getPayload(), is(content));
  }

  @Test
  public void testHandleProviderAbsent() throws Exception {
    when(attributes.require(AssetKind.class)).thenReturn(PROVIDER);
    when(tokens.get(VENDOR_TOKEN)).thenReturn(VENDOR);
    when(tokens.get(PROJECT_TOKEN)).thenReturn(PROJECT);
    when(composerHostedFacet.getProviderJson(VENDOR, PROJECT)).thenReturn(null);
    Response response = underTest.handle(context);
    assertThat(response.getStatus().getCode(), is(404));
    assertThat(response.getPayload(), is(nullValue()));
  }

  @Test
  public void testHandlePackage() throws Exception {
    when(attributes.require(AssetKind.class)).thenReturn(PACKAGE);
    when(tokens.get(VENDOR_TOKEN)).thenReturn(VENDOR);
    when(tokens.get(PROJECT_TOKEN)).thenReturn(PROJECT);
    when(composerHostedFacet.getPackageJson(VENDOR, PROJECT)).thenReturn(content);
    Response response = underTest.handle(context);
    assertThat(response.getStatus().getCode(), is(200));
    assertThat(response.getPayload(), is(content));
  }

  @Test
  public void testHandlePackageAbsent() throws Exception {
    when(attributes.require(AssetKind.class)).thenReturn(PACKAGE);
    when(tokens.get(VENDOR_TOKEN)).thenReturn(VENDOR);
    when(tokens.get(PROJECT_TOKEN)).thenReturn(PROJECT);
    when(composerHostedFacet.getProviderJson(VENDOR, PROJECT)).thenReturn(null);
    Response response = underTest.handle(context);
    assertThat(response.getStatus().getCode(), is(404));
    assertThat(response.getPayload(), is(nullValue()));
  }


  @Test
  public void testHandleList() throws Exception {
    when(attributes.require(AssetKind.class)).thenReturn(LIST);

    // No filter
    when(parameters.get("filter")).thenReturn(null);
    when(composerHostedFacet.getListJson(null)).thenReturn(content);
    Response response = underTest.handle(context);
    assertThat(response.getStatus().getCode(), is(200));
    assertThat(response.getPayload(), is(content));

    // With filter
    when(parameters.get("filter")).thenReturn("test/*");
    when(composerHostedFacet.getListJson("test/*")).thenReturn(content);
    response = underTest.handle(context);
    assertThat(response.getStatus().getCode(), is(200));
    assertThat(response.getPayload(), is(content));
  }

  @Test
  public void testHandleZipballPresent() throws Exception {
    when(attributes.require(AssetKind.class)).thenReturn(ZIPBALL);
    when(tokens.get(VENDOR_TOKEN)).thenReturn(VENDOR);
    when(tokens.get(PROJECT_TOKEN)).thenReturn(PROJECT);
    when(tokens.get(VERSION_TOKEN)).thenReturn(VERSION);
    when(tokens.get(NAME_TOKEN)).thenReturn(NAME);
    when(composerHostedFacet.getZipball(ZIPBALL_PATH)).thenReturn(content);
    Response response = underTest.handle(context);
    assertThat(response.getStatus().getCode(), is(200));
    assertThat(response.getPayload(), is(content));
  }

  @Test
  public void testHandleZipballAbsent() throws Exception {
    when(attributes.require(AssetKind.class)).thenReturn(ZIPBALL);
    when(tokens.get(VENDOR_TOKEN)).thenReturn(VENDOR);
    when(tokens.get(PROJECT_TOKEN)).thenReturn(PROJECT);
    when(tokens.get(VERSION_TOKEN)).thenReturn(VERSION);
    when(tokens.get(NAME_TOKEN)).thenReturn(NAME);
    when(composerHostedFacet.getZipball(ZIPBALL_PATH)).thenReturn(null);
    Response response = underTest.handle(context);
    assertThat(response.getStatus().getCode(), is(404));
    assertThat(response.getPayload(), is(nullValue()));
  }
}
