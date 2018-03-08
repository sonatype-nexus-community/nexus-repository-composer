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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.TempBlob;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;

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

  private final TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String, Object>>() { };

  private final ObjectMapper mapper = new ObjectMapper();

  private final ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();

  /**
   * Populates an asset's format attributes with the content contained in a composer.json file in the zip archive. This
   * does not extract all JSON entries, but does try to extract those that could be viewed as more "interesting" from
   * the standpoint of the repository manager.
   */
  public void extractFromZip(final TempBlob tempBlob, final NestedAttributesMap formatAttributes) throws IOException {
    try (InputStream is = tempBlob.getBlob().getInputStream()) {
      try (ArchiveInputStream ais = archiveStreamFactory.createArchiveInputStream(ArchiveStreamFactory.ZIP, is)) {
        ArchiveEntry entry = ais.getNextEntry();
        while (entry != null) {
          if (processEntry(ais, entry, formatAttributes)) {
            return;
          }
          entry = ais.getNextEntry();
        }
      }
    }
    catch (ArchiveException e) {
      throw new IOException("Error reading from archive", e);
    }
  }

  /**
   * Processes a single entry in the archive. If the entry is the composer.json then the attributes will be extracted.
   * If not, the entry is skipped.
   */
  private boolean processEntry(final ArchiveInputStream stream,
                               final ArchiveEntry entry,
                               final NestedAttributesMap formatAttributes) throws IOException
  {
    String name = entry.getName();
    int filenameIndex = name.indexOf("/composer.json");
    int separatorIndex = name.indexOf("/");
    if (filenameIndex >= 0 && filenameIndex == separatorIndex) {
      Map<String, Object> contents = mapper.readValue(stream, typeReference);
      extractStrings(contents, formatAttributes, STRINGS_MAPPING);
      extractAuthors(contents, formatAttributes);
      extractSupport(contents, formatAttributes);
      return true;
    }
    return false;
  }

  /**
   * Extracts zero or more string-only fields from the source map into the destination attribute map. If a collection
   * is encountered, any string items within the collection are added to a list and stored as a collection of strings.
   */
  @VisibleForTesting
  void extractStrings(final Map<String, Object> source,
                      final NestedAttributesMap destination,
                      final Map<String, String> mappings)
  {
    for (Map.Entry<String, String> mapping : mappings.entrySet()) {
      Object sourceValue = source.get(mapping.getKey());
      if (sourceValue instanceof String) {
        destination.set(mapping.getValue(), sourceValue);
      }
      else if (sourceValue instanceof Collection) {
        List<String> entries = new ArrayList<>();
        for (Object entryValue : (Collection) sourceValue) {
          if (entryValue instanceof String) {
            entries.add((String) entryValue);
          }
        }
        if (!entries.isEmpty()) {
          destination.set(mapping.getValue(), entries);
        }
      }
    }
  }

  /**
   * Extracts author contact information (except for the role) into a collection of strings.
   */
  @VisibleForTesting
  void extractAuthors(final Map<String, Object> contents,
                      final NestedAttributesMap formatAttributes)
  {
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
        formatAttributes.set(P_AUTHORS, authors);
      }
    }
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
  private void extractSupport(final Map<String, Object> contents, final NestedAttributesMap formatAttributes) {
    Object sourceValue = contents.get(SUPPORT);
    if (sourceValue instanceof Map) {
      extractStrings((Map<String, Object>) sourceValue, formatAttributes, SUPPORT_MAPPING);
    }
  }
}
