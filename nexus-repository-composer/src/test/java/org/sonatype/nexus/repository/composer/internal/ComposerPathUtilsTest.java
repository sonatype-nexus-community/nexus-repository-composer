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
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport.NAME_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport.PROJECT_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport.VENDOR_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport.VERSION_TOKEN;

public class ComposerPathUtilsTest
    extends TestSupport
{
  @Mock
  private Context context;

  @Mock
  private AttributesMap contextAttributes;

  @Mock
  private TokenMatcher.State state;

  @Mock
  private Map<String, String> tokens;

  @Test
  public void buildZipballPathFromContextWithNameToken() {
    when(context.getAttributes()).thenReturn(contextAttributes);
    when(contextAttributes.require(TokenMatcher.State.class)).thenReturn(state);
    when(state.getTokens()).thenReturn(tokens);

    when(tokens.get(VENDOR_TOKEN)).thenReturn("testvendor");
    when(tokens.get(PROJECT_TOKEN)).thenReturn("testproject");
    when(tokens.get(VERSION_TOKEN)).thenReturn("1.2.3");
    when(tokens.get(NAME_TOKEN)).thenReturn("name");

    assertThat(ComposerPathUtils.buildZipballPath(context), is("testvendor/testproject/1.2.3/name.zip"));
  }

  @Test
  public void buildZipballPathFromContextWithoutNameToken() {
    when(context.getAttributes()).thenReturn(contextAttributes);
    when(contextAttributes.require(TokenMatcher.State.class)).thenReturn(state);
    when(state.getTokens()).thenReturn(tokens);

    when(tokens.get(VENDOR_TOKEN)).thenReturn("testvendor");
    when(tokens.get(PROJECT_TOKEN)).thenReturn("testproject");
    when(tokens.get(VERSION_TOKEN)).thenReturn("1.2.3");

    assertThat(ComposerPathUtils.buildZipballPath(context),
        is("testvendor/testproject/1.2.3/testvendor-testproject-1.2.3.zip"));
  }

  @Test
  public void buildZipballPathFromValues() {
    when(context.getAttributes()).thenReturn(contextAttributes);
    when(contextAttributes.require(TokenMatcher.State.class)).thenReturn(state);
    when(state.getTokens()).thenReturn(tokens);

    assertThat(ComposerPathUtils.buildZipballPath("testvendor", "testproject", "1.2.3"),
        is("testvendor/testproject/1.2.3/testvendor-testproject-1.2.3.zip"));
  }

  @Test
  public void buildProviderPathFromTokens() {
    when(context.getAttributes()).thenReturn(contextAttributes);
    when(contextAttributes.require(TokenMatcher.State.class)).thenReturn(state);
    when(state.getTokens()).thenReturn(tokens);

    when(tokens.get(VENDOR_TOKEN)).thenReturn("testvendor");
    when(tokens.get(PROJECT_TOKEN)).thenReturn("testproject");

    assertThat(ComposerPathUtils.buildProviderPath(context), is("p/testvendor/testproject.json"));
  }

  @Test
  public void buildProviderPathFromValues() {
    assertThat(ComposerPathUtils.buildProviderPath("testvendor", "testproject"), is("p/testvendor/testproject.json"));
  }
}
