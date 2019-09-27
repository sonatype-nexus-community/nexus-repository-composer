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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

public class ComposerJsonExtractorTest
    extends TestSupport
{
  @Mock
  private Blob blob;

  private ComposerJsonExtractor underTest;

  @Before
  public void setUp() {
    underTest = new ComposerJsonExtractor();
  }

  @Test
  public void extractInfoFromZipballWithJson() throws Exception {
    String expected;
    try (InputStream in = getClass().getResourceAsStream("extractInfoFromZipballWithJson.composer.json")) {
      expected = CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
    String actual;
    try (InputStream in = getClass().getResourceAsStream("extractInfoFromZipballWithJson.zip")) {
      when(blob.getInputStream()).thenReturn(in);
      actual = new ObjectMapper().writeValueAsString(underTest.extractFromZip(blob));
    }
    assertEquals(expected, actual, true);
  }

  @Test
  public void extractInfoFromZipballWithJsonComposerArchived() throws Exception {
    String expected;
    try (InputStream in = getClass().getResourceAsStream("extractInfoFromZipballWithJsonComposerArchived.composer.json")) {
      expected = CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
    String actual;
    try (InputStream in = getClass().getResourceAsStream("extractInfoFromZipBallWithJsonComposerArchived.zip")) {
      when(blob.getInputStream()).thenReturn(in);
      actual = new ObjectMapper().writeValueAsString(underTest.extractFromZip(blob));
    }
    assertEquals(expected, actual, true);
  }

  @Test
  public void extractInfoFromZipballWithoutJson() throws Exception {
    Map<String, Object> results;
    try (InputStream in = getClass().getResourceAsStream("extractInfoFromZipballWithoutJson.zip")) {
      when(blob.getInputStream()).thenReturn(in);
      results = underTest.extractFromZip(blob);
    }
    assertThat(results.isEmpty(), is(true));
  }
}
