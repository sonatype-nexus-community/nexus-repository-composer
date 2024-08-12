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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import java.io.InputStream;
import java.util.*;

import static java.util.Collections.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.composer.internal.ComposerAttributes.*;

public class ComposerFormatAttributesExtractorTest
    extends TestSupport
{
  private static final String EXPECTED_FIELDS[] = new String[]{
      P_NAME, P_VERSION, P_DESCRIPTION, P_TYPE, P_KEYWORDS, P_HOMEPAGE, P_TIME, P_LICENSE, P_AUTHORS, P_SUPPORT_EMAIL,
      P_SUPPORT_ISSUES, P_SUPPORT_FORUM, P_SUPPORT_WIKI, P_SUPPORT_SOURCE, P_SUPPORT_DOCS, P_SUPPORT_RSS
  };

  @Mock
  private Blob blob;

  @Mock
  private TempBlob tempBlob;

  @Mock
  private ComposerJsonExtractor composerJsonExtractor;

  private ComposerFormatAttributesExtractor underTest;

  private FluentComponent destination;
  private NestedAttributesMap attributesMap;

  @Before
  public void setUp() {
    underTest = new ComposerFormatAttributesExtractor(composerJsonExtractor);
    destination = mock(FluentComponent.class);
    attributesMap = new NestedAttributesMap("backing", new LinkedHashMap<>());
    when(destination.attributes()).thenReturn(attributesMap);
    when(destination.attributes(anyString()))
        .thenAnswer((inv) -> attributesMap.get(inv.getArgument(0, String.class)));
    when(destination.withAttribute(anyString(), any()))
        .thenAnswer((inv) -> {
          attributesMap.set(inv.getArgument(0, String.class), inv.getArgument(1));
          return destination;
        });
  }

  @Test
  public void extractInfoFromZipballWithJson() throws Exception {
    Map<String, Object> contents;
    try (InputStream in = getClass().getResourceAsStream("extractInfoFromZipballWithJson.composer.json")) {
      contents = new ObjectMapper().readValue(in, new TypeReference<Map<String, Object>>()
      {
      });
    }

    when(tempBlob.getBlob()).thenReturn(blob);
    when(composerJsonExtractor.extractFromZip(blob)).thenReturn(contents);

    underTest.extractFromZip(tempBlob, destination);

    assertThat(attributesMap.keys(), containsInAnyOrder(EXPECTED_FIELDS));
    assertThat(attributesMap.get(P_NAME), is("vendor/project"));
    assertThat(attributesMap.get(P_VERSION), is("1.2.3"));
    assertThat(attributesMap.get(P_DESCRIPTION), is("Test description"));
    assertThat(attributesMap.get(P_TYPE), is("library"));
    assertThat((List<String>) attributesMap.get(P_KEYWORDS), containsInAnyOrder("keyword1", "keyword2", "keyword3"));
    assertThat(attributesMap.get(P_HOMEPAGE), is("http://www.example.com/"));
    assertThat(attributesMap.get(P_TIME), is("2008-05-15"));
    assertThat(attributesMap.get(P_LICENSE), is("MIT"));
    assertThat((List<String>) attributesMap.get(P_AUTHORS),
        containsInAnyOrder(
            "Author1 <author1@example.com> (http://www.example.com/author1)",
            "Author2 <author2@example.com> (http://www.example.com/author2)"));
    assertThat(attributesMap.get(P_SUPPORT_EMAIL), is("email@example.com"));
    assertThat(attributesMap.get(P_SUPPORT_ISSUES), is("irc://irc.example.com/irc"));
    assertThat(attributesMap.get(P_SUPPORT_FORUM), is("http://www.example.com/forum"));
    assertThat(attributesMap.get(P_SUPPORT_WIKI), is("http://www.example.com/wiki"));
    assertThat(attributesMap.get(P_SUPPORT_SOURCE), is("http://www.example.com/source"));
    assertThat(attributesMap.get(P_SUPPORT_DOCS), is("http://www.example.com/docs"));
    assertThat(attributesMap.get(P_SUPPORT_RSS), is("http://www.example.com/rss"));
  }

  @Test
  public void extractInfoFromZipballWithoutJson() throws Exception {
    when(tempBlob.getBlob()).thenReturn(blob);
    when(composerJsonExtractor.extractFromZip(blob)).thenReturn(Collections.emptyMap());

    NestedAttributesMap attributesMap = new NestedAttributesMap("composer", new LinkedHashMap<>());
    FluentComponent component = mock(FluentComponent.class);
    when(component.attributes()).thenReturn(attributesMap);
    underTest.extractFromZip(tempBlob, component);

    assertThat(attributesMap.keys(), is(empty()));
  }

  @Test
  public void extractStringsMissing() {
    underTest.extractStrings(emptyMap(), destination, singletonMap("inkey", "outkey"));
    assertThat(attributesMap.keys(), is(empty()));
  }

  @Test
  public void extractStringsNull() {
    underTest.extractStrings(singletonMap("inkey", null), destination, singletonMap("inkey", "outkey"));
    assertThat(attributesMap.keys(), is(empty()));
  }

  @Test
  public void extractStringsNonStringValue() {
    underTest.extractStrings(singletonMap("inkey", Integer.MAX_VALUE), destination, singletonMap("inkey", "outkey"));
    assertThat(attributesMap.keys(), is(empty()));
  }

  @Test
  public void extractStringsSingleString() {
    underTest.extractStrings(singletonMap("inkey", "value"), destination, singletonMap("inkey", "outkey"));
    assertThat(attributesMap.keys(), contains("outkey"));
    assertThat(attributesMap.get("outkey"), is("value"));
  }

  @Test
  public void extractStringsEmptyCollection() {
    underTest.extractStrings(singletonMap("inkey", emptyList()), destination, singletonMap("inkey", "outkey"));
    assertThat(attributesMap.keys(), is(empty()));
  }

  @Test
  public void extractStringsCollectionWithNonStringValue() {
    underTest.extractStrings(singletonMap("inkey", Integer.MAX_VALUE), destination, singletonMap("inkey", "outkey"));
    assertThat(attributesMap.keys(), is(empty()));
  }

  @Test
  public void extractStringsCollectionWithString() {
    underTest
        .extractStrings(singletonMap("inkey", singletonList("value")), destination, singletonMap("inkey", "outkey"));
    assertThat(attributesMap.keys(), contains("outkey"));
    assertThat((List<String>) attributesMap.get("outkey"), contains("value"));
  }

  @Test
  public void extractAuthorPartMissing() {
    List<String> parts = new ArrayList<>();
    underTest.extractAuthorPart(emptyMap(), parts, "part", "%s");
    assertThat(parts, is(empty()));
  }

  @Test
  public void extractAuthorPartNull() {
    List<String> parts = new ArrayList<>();
    underTest.extractAuthorPart(Collections.singletonMap("part", null), parts, "part", "%s");
    assertThat(parts, is(empty()));
  }

  @Test
  public void extractAuthorPartNonStringValue() {
    List<String> parts = new ArrayList<>();
    underTest.extractAuthorPart(Collections.singletonMap("part", Integer.MAX_VALUE), parts, "part", "%s");
    assertThat(parts, is(empty()));
  }

  @Test
  public void extractAuthorPartString() {
    List<String> parts = new ArrayList<>();
    underTest.extractAuthorPart(Collections.singletonMap("part", "value"), parts, "part", "%s");
    assertThat(parts, contains("value"));
  }

  @Test
  public void extractAuthorsMissing() {
    underTest.extractAuthors(emptyMap(), destination);
    assertThat(attributesMap.keys(), is(empty()));
  }

  @Test
  public void extractAuthorsNull() {
    underTest.extractAuthors(singletonMap("authors", null), destination);
    assertThat(attributesMap.keys(), is(empty()));
  }

  @Test
  public void extractAuthorsEmptyCollection() {
    underTest.extractAuthors(singletonMap("authors", emptyList()), destination);
    assertThat(attributesMap.keys(), is(empty()));
  }

  @Test
  public void extractAuthorsEmptyAuthor() {
    underTest.extractAuthors(singletonMap("authors", singletonList(emptyMap())), destination);
    assertThat(attributesMap.keys(), is(empty()));
  }

  @Test
  public void extractAuthorsAuthorName() {
    underTest.extractAuthors(singletonMap("authors", singletonList(singletonMap("name", "value"))), destination);
    assertThat(attributesMap.keys(), contains(P_AUTHORS));
    assertThat((List<String>) attributesMap.get(P_AUTHORS), contains("value"));
  }

  @Test
  public void extractAuthorsAuthorEmail() {
    underTest.extractAuthors(singletonMap("authors", singletonList(singletonMap("email", "value"))), destination);
    assertThat(attributesMap.keys(), contains(P_AUTHORS));
    assertThat((List<String>) attributesMap.get(P_AUTHORS), contains("<value>"));
  }

  @Test
  public void extractAuthorsAuthorHomepage() {
    underTest.extractAuthors(singletonMap("authors", singletonList(singletonMap("homepage", "value"))), destination);
    assertThat(attributesMap.keys(), contains(P_AUTHORS));
    assertThat((List<String>) attributesMap.get(P_AUTHORS), contains("(value)"));
  }
}
