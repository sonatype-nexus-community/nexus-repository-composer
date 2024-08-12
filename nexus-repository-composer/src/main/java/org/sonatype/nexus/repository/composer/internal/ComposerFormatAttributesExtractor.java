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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.repository.composer.internal.ComposerAttributes.*;

/**
 * Extracts format attributes from a Composer archive. Currently only zip archives are supported.
 */
@Named
@Singleton
public class ComposerFormatAttributesExtractor
    extends ComponentSupport
{
  private static final String NAME = "name";

  private static final String DESCRIPTION = "description";

  private static final String VERSION = "version";

  private static final String TYPE = "type";

  private static final String KEYWORDS = "keywords";

  private static final String HOMEPAGE = "homepage";

  private static final String TIME = "time";

  private static final String LICENSE = "license";

  private static final String AUTHORS = "authors";

  private static final String AUTHOR_NAME = "name";

  private static final String AUTHOR_EMAIL = "email";

  private static final String AUTHOR_HOMEPAGE = "homepage";

  private static final String SUPPORT = "support";

  private static final String SUPPORT_EMAIL = "email";

  private static final String SUPPORT_ISSUES = "issues";

  private static final String SUPPORT_FORUM = "forum";

  private static final String SUPPORT_WIKI = "wiki";

  private static final String SUPPORT_IRC = "irc";

  private static final String SUPPORT_SOURCE = "source";

  private static final String SUPPORT_DOCS = "docs";

  private static final String SUPPORT_RSS = "rss";

  private static final Map<String, String> STRINGS_MAPPING = new ImmutableMap.Builder<String, String>()
      .put(NAME, P_NAME)
      .put(DESCRIPTION, P_DESCRIPTION)
      .put(VERSION, P_VERSION)
      .put(TYPE, P_TYPE)
      .put(KEYWORDS, P_KEYWORDS)
      .put(HOMEPAGE, P_HOMEPAGE)
      .put(TIME, P_TIME)
      .put(LICENSE, P_LICENSE)
      .build();

  private static final Map<String, String> SUPPORT_MAPPING = new ImmutableMap.Builder<String, String>()
      .put(SUPPORT_EMAIL, P_SUPPORT_EMAIL)
      .put(SUPPORT_ISSUES, P_SUPPORT_ISSUES)
      .put(SUPPORT_FORUM, P_SUPPORT_FORUM)
      .put(SUPPORT_WIKI, P_SUPPORT_WIKI)
      .put(SUPPORT_IRC, P_SUPPORT_IRC)
      .put(SUPPORT_SOURCE, P_SUPPORT_SOURCE)
      .put(SUPPORT_DOCS, P_SUPPORT_DOCS)
      .put(SUPPORT_RSS, P_SUPPORT_RSS)
      .build();

  private ComposerJsonExtractor composerJsonExtractor;

  @Inject
  public ComposerFormatAttributesExtractor(final ComposerJsonExtractor composerJsonExtractor) {
    this.composerJsonExtractor = requireNonNull(composerJsonExtractor);
  }

  /**
   * Populates an asset's format attributes with the content contained in a composer.json file in the zip archive. This
   * does not extract all JSON entries, but does try to extract those that could be viewed as more "interesting" from
   * the standpoint of the repository manager.
   */
  public FluentComponent extractFromZip(final TempBlob tempBlob, FluentComponent component) throws IOException {
    Map<String, Object> contents = composerJsonExtractor.extractFromZip(tempBlob.getBlob());
    if (!contents.isEmpty()) {
      component = extractStrings(contents, component, STRINGS_MAPPING);
      component = extractAuthors(contents, component);
      component = extractSupport(contents, component);
    }
    return component;
  }

  /**
   * Extracts zero or more string-only fields from the source map into the destination attribute map. If a collection
   * is encountered, any string items within the collection are added to a list and stored as a collection of strings.
   */
  @VisibleForTesting
  FluentComponent extractStrings(final Map<String, Object> source,
                      FluentComponent component,
                      final Map<String, String> mappings)
  {
    for (Map.Entry<String, String> mapping : mappings.entrySet()) {
      Object sourceValue = source.get(mapping.getKey());
      if (sourceValue instanceof String) {
        component = component.withAttribute(mapping.getValue(), sourceValue);
      }
      else if (sourceValue instanceof Collection) {
        List<String> entries = new ArrayList<>();
        for (Object entryValue : (Collection<?>) sourceValue) {
          if (entryValue instanceof String) {
            entries.add((String) entryValue);
          }
        }
        if (!entries.isEmpty()) {
          component = component.withAttribute(mapping.getValue(), entries);
        }
      }
    }

    return component;
  }

  /**
   * Extracts author contact information (except for the role) into a collection of strings.
   */
  @VisibleForTesting
  FluentComponent extractAuthors(final Map<String, Object> contents, FluentComponent component) {
    Object sourceValue = contents.get(AUTHORS);
    if (sourceValue instanceof Collection) {
      List<String> authors = new ArrayList<>();
      for (Object author : (Collection) sourceValue) {
        if (author instanceof Map) {
          List<String> parts = new ArrayList<>();
          extractAuthorPart((Map<String, Object>) author, parts, AUTHOR_NAME, "%s");
          extractAuthorPart((Map<String, Object>) author, parts, AUTHOR_EMAIL, "<%s>");
          extractAuthorPart((Map<String, Object>) author, parts, AUTHOR_HOMEPAGE, "(%s)");
          if (!parts.isEmpty()) {
            authors.add(String.join(" ", parts));
          }
        }
      }
      if (!authors.isEmpty()) {
        component = component.withAttribute(P_AUTHORS, authors);
      }
    }
    return component;
  }

  /**
   * Extracts one part of the author information into a collection for later joining, applying the specified format
   * string if a string entry with that key is present.
   */
  @VisibleForTesting
  void extractAuthorPart(final Map<String, Object> author,
                         final List<String> parts,
                         final String key,
                         final String format)
  {
    Object part = author.get(key);
    if (part instanceof String) {
      parts.add(String.format(format, part));
    }
  }

  /**
   * Extracts the subkeys for the support entry into their own top-level format attributes.
   */
  private FluentComponent extractSupport(final Map<String, Object> contents, final FluentComponent component) {
    Object sourceValue = contents.get(SUPPORT);
    if (sourceValue instanceof Map) {
      return extractStrings((Map<String, Object>) sourceValue, component, SUPPORT_MAPPING);
    }
    return component;
  }
}
