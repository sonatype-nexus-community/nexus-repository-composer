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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.google.common.base.Preconditions.checkState;

/**
 * Class encapsulating JSON processing for Composer-format repositories, including operations for parsing JSON indexes
 * and rewriting them to be compatible with a proxy repository.
 */
@Named
@Singleton
public class ComposerJsonProcessor
{
  private static final String REWRITE_URL = "%s/%s/%s/%s-%s.zip";

  private static final String VENDOR_AND_PROJECT = "%s/%s";

  private static final String DIST_KEY = "dist";

  private static final String PROVIDERS_URL_KEY = "providers-url";

  private static final String PROVIDERS_KEY = "providers";

  private static final String PACKAGES_KEY = "packages";

  private static final String PACKAGE_NAMES_KEY = "packageNames";

  private static final String SHA256_KEY = "sha256";

  private static final String SOURCE_KEY = "source";

  private static final String TYPE_KEY = "type";

  private static final String URL_KEY = "url";

  private static final String ZIP_TYPE = "zip";

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Generates a packages.json file (inclusive of all projects) based on the list.json provided as a payload. Expected
   * usage is to "go remote" on the current repository to fetch a list.json copy, then pass it to this method to build
   * the packages.json for the client to use.
   */
  public Content generatePackagesFromList(final Repository repository, final Payload payload) throws IOException {
    // TODO: Parse using JSON tokens rather than loading all this into memory, it "should" work but I'd be careful.
    Map<String, Object> listJson = parseJson(payload);
    return buildPackagesJson(repository, (Collection<String>) listJson.get(PACKAGE_NAMES_KEY));
  }

  /**
   * Generates a packages.json file (inclusive of all projects) based on the components provided. Expected usage is
   * for a hosted repository to be queried for its components, which are then provided to this method to build the
   * packages.json for the client to use.
   */
  public Content generatePackagesFromComponents(final Repository repository, final Iterable<Component> components)
      throws IOException
  {
    return buildPackagesJson(repository, StreamSupport.stream(components.spliterator(), false)
        .map(component -> component.group() + "/" + component.name()).collect(Collectors.toList()));
  }

  /**
   * Builds a packages.json file as a {@code Content} instance containing the actual JSON for the given providers.
   */
  private Content buildPackagesJson(final Repository repository, final Collection<String> names) throws IOException {
    Map<String, Object> packagesJson = new LinkedHashMap<>();
    packagesJson.put(PROVIDERS_URL_KEY, repository.getUrl() + "/p/%package%.json");
    packagesJson.put(PROVIDERS_KEY, names.stream()
        .collect(Collectors.toMap((each) -> each, (each) -> Collections.singletonMap(SHA256_KEY, null))));
    return new Content(new StringPayload(mapper.writeValueAsString(packagesJson), ContentTypes.APPLICATION_JSON));
  }

  /**
   * Rewrites the provider JSON so that source entries are removed and dist entries are pointed back to Nexus.
   */
  public Payload rewriteProviderJson(final Repository repository, final Payload payload) throws IOException {
    Map<String, Object> json = parseJson(payload);
    if (json.get(PACKAGES_KEY) instanceof Map) {
      Map<String, Object> packagesMap = (Map<String, Object>) json.get(PACKAGES_KEY);
      for (String packageName : packagesMap.keySet()) {
        Map<String, Object> packageVersions = (Map<String, Object>) packagesMap.get(packageName);
        for (String packageVersion : packageVersions.keySet()) {
          // TODO: Make this more robust, right now it makes a lot of assumptions and doesn't deal with bad things well
          Map<String, Object> versionInfo = (Map<String, Object>) packageVersions.get(packageVersion);
          versionInfo.remove(SOURCE_KEY); // TODO: For now don't allow sources, probably should make this configurable?

          Map<String, Object> distInfo = (Map<String, Object>) versionInfo.get(DIST_KEY);
          if (distInfo != null) {
            String distType = (String) distInfo.get(TYPE_KEY);
            checkState(ZIP_TYPE.equals(distInfo.get(TYPE_KEY)), "Invalid dist type %s for package %s version %s",
                distType, packageName, packageVersion);
            distInfo.put(URL_KEY, String
                .format(REWRITE_URL, repository.getUrl(), packageName, packageVersion, packageName.replace('/', '-'),
                    packageVersion));
          }
        }
      }
    }
    return new StringPayload(mapper.writeValueAsString(json), payload.getContentType());
  }

  /**
   * Obtains the dist URL for a particular vendor/project and version within a provider JSON payload.
   */
  public String getDistUrl(final String vendor, final String project, final String version, final Payload payload)
      throws IOException
  {
    String vendorAndProject = String.format(VENDOR_AND_PROJECT, vendor, project);
    Map<String, Object> json = parseJson(payload);
    Map<String, Object> packagesMap = (Map<String, Object>) json.get(PACKAGES_KEY);
    Map<String, Object> packageInfo = (Map<String, Object>) packagesMap.get(vendorAndProject);
    Map<String, Object> versionInfo = (Map<String, Object>) packageInfo.get(version);
    Map<String, Object> distInfo = (Map<String, Object>) versionInfo.get(DIST_KEY);
    return (String) distInfo.get(URL_KEY);
  }

  private Map<String, Object> parseJson(final Payload payload) throws IOException {
    try (InputStream in = payload.openInputStream()) {
      TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String, Object>>() { };
      return mapper.readValue(in, typeReference);
    }
  }
}
