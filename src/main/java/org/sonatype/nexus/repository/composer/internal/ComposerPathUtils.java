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

import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import org.eclipse.sisu.Nullable;

import static org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport.NAME_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport.PROJECT_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport.VENDOR_TOKEN;
import static org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport.VERSION_TOKEN;

/**
 * Utility class containing methods for working with Composer routes and paths.
 */
public final class ComposerPathUtils
{
  private static final String ZIPBALL_PATH = "%s/%s/%s/%s.zip";

  private static final String NAME_PATTERN = "%s-%s-%s";

  /**
   * Builds the path to a zipball based on the path contained in a particular context. For download routes the full
   * path including the name token will be present and will be constructed accordingly. For upload routes the full
   * path will not be known because the filename will not be present, so the name portion will be constructed from
   * the vendor, project, and version information contained in the other path segments.
   */
  public static String buildZipballPath(final Context context) {
    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    Map<String, String> tokens = state.getTokens();
    return buildZipballPath(
        tokens.get(VENDOR_TOKEN),
        tokens.get(PROJECT_TOKEN),
        tokens.get(VERSION_TOKEN),
        tokens.get(NAME_TOKEN));
  }

  private static String buildZipballPath(final String vendor,
                                         final String project,
                                         final String version,
                                         @Nullable final String name)
  {
    return String.format(ZIPBALL_PATH, vendor, project, version,
        name == null ? String.format(NAME_PATTERN, vendor, project, version) : name);
  }

  private ComposerPathUtils() {
    // empty
  }
}
