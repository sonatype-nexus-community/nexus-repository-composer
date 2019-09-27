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
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;

/**
 * Utility class for extracting the contents of a package's {@code composer.json} file and returning it as a map.
 */
@Named
@Singleton
public class ComposerJsonExtractor
    extends ComponentSupport
{
  private final TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String, Object>>() { };

  private final ObjectMapper mapper = new ObjectMapper();

  private final ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();

  /**
   * Extracts the contents for the first matching {@code composer.json} file (of which there should only be one) as a
   * map representing the parsed JSON content. If no such file is found then an empty map is returned.
   */
  public Map<String, Object> extractFromZip(final Blob blob) throws IOException {
    try (InputStream is = blob.getInputStream()) {
      try (ArchiveInputStream ais = archiveStreamFactory.createArchiveInputStream(ArchiveStreamFactory.ZIP, is)) {
        ArchiveEntry entry = ais.getNextEntry();
        while (entry != null) {
          Map<String, Object> contents = processEntry(ais, entry);
          if (!contents.isEmpty()) {
            return contents;
          }
          entry = ais.getNextEntry();
        }
      }
      return Collections.emptyMap();
    }
    catch (ArchiveException e) {
      throw new IOException("Error reading from archive", e);
    }
  }

  /**
   * Processes a single entry in the archive. If the entry is the composer.json then the attributes will be extracted.
   * If not, the entry is skipped.
   */
  private Map<String, Object> processEntry(final ArchiveInputStream stream, final ArchiveEntry entry) throws IOException
  {
    if (isComposerJsonFilename(entry.getName())) {
      return mapper.readValue(stream, typeReference);
    }
    return Collections.emptyMap();
  }

  /**
   * Returns a boolean indicating if the associated file path (from an archive file) represents the {@code
   * composer.json} file.
   */
  private boolean isComposerJsonFilename(final String entryName) {
    int filenameIndex = entryName.indexOf("/composer.json");
    int separatorIndex = entryName.indexOf("/");
    return entryName.equals("composer.json") || (filenameIndex >= 0 && filenameIndex == separatorIndex);
  }
}
