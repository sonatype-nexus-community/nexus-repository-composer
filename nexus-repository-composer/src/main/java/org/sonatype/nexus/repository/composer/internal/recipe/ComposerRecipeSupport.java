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
package org.sonatype.nexus.repository.composer.internal.recipe;

import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.composer.AssetKind;
import org.sonatype.nexus.repository.composer.ComposerContentFacet;
import org.sonatype.nexus.repository.composer.internal.ComposerMaintenanceFacet;
import org.sonatype.nexus.repository.composer.internal.ComposerSecurityFacet;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.search.index.SearchIndexFacet;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Route.Builder;
import org.sonatype.nexus.repository.view.handlers.*;
import org.sonatype.nexus.repository.view.matchers.ActionMatcher;
import org.sonatype.nexus.repository.view.matchers.LiteralMatcher;
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import javax.inject.Inject;
import javax.inject.Provider;

import static org.sonatype.nexus.repository.http.HttpMethods.*;

/**
 * Abstract superclass containing methods and constants common to most Composer repository recipes.
 */
public abstract class ComposerRecipeSupport
    extends RecipeSupport
{
  public static final String VENDOR_TOKEN = "vendor";

  public static final String PROJECT_TOKEN = "project";

  public static final String VERSION_TOKEN = "version";

  public static final String NAME_TOKEN = "name";

  public static final String PACKAGE_FIELD_NAME = "package";

  public static final String SOURCE_TYPE_FIELD_NAME = "src-type";

  public static final String SOURCE_URL_FIELD_NAME = "src-url";

  public static final String SOURCE_REFERENCE_FIELD_NAME = "src-ref";

  private DatabaseCheck databaseCheck;

  @Inject
  protected Provider<ComposerContentFacet> contentFacet;

  @Inject
  protected Provider<ComposerMaintenanceFacet> maintenanceFacet;

  @Inject
  protected Provider<ComposerSecurityFacet> securityFacet;

  @Inject
  protected Provider<ConfigurableViewFacet> viewFacet;

  @Inject
  protected Provider<SearchIndexFacet> searchFacet;

  @Inject
  protected Provider<BrowseFacet> browseFacet;

  @Inject
  protected ExceptionHandler exceptionHandler;

  @Inject
  protected TimingHandler timingHandler;

  @Inject
  protected SecurityHandler securityHandler;

  @Inject
  protected PartialFetchHandler partialFetchHandler;

  @Inject
  protected ConditionalRequestHandler conditionalRequestHandler;

  @Inject
  protected ContentHeadersHandler contentHeadersHandler;

  @Inject
  protected HandlerContributor handlerContributor;

  protected ComposerRecipeSupport(final Type type, final Format format) {
    super(type, format);
  }

  protected static Handler assetKindHandler(AssetKind assetKind) {
    return (context -> {
      context.getAttributes().set(AssetKind.class, assetKind);
      return context.proceed();
    });
  }

  protected static Builder packagesMatcher() {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET, HEAD),
            new LiteralMatcher("/packages.json")
        ));
  }

  protected static Builder listMatcher() {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET, HEAD),
            new LiteralMatcher("/packages/list.json")
        ));
  }

  protected static Builder providerMatcher() {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET, HEAD),
            new TokenMatcher("/p/{vendor:.+}/{project:.+}.json")
        ));
  }

  protected static Builder packageMatcher() {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET, HEAD),
            new TokenMatcher("/p2/{vendor:.+}/{project:.+}.json")
        ));
  }

  protected static Builder zipballMatcher() {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET, HEAD),
            new TokenMatcher("/{vendor:.+}/{project:.+}/{version:.+}/{name:.+}.zip")
        ));
  }

  protected static Builder uploadMatcher() {
    return new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(PUT),
            new TokenMatcher("/packages/upload/{vendor:.+}/{project:.+}/{version:.+}")
        ));
  }

  @Inject
  public void setDatabaseCheck(final DatabaseCheck databaseCheck) {
    this.databaseCheck = databaseCheck;
  }

  @Override
  public boolean isFeatureEnabled() {
    return databaseCheck == null || databaseCheck.isAllowedByVersion(getClass());
  }
}
