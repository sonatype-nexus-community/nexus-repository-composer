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

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.junit.Rule;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.internal.fixtures.RepositoryRuleComposer;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.testsuite.testsupport.NexusBaseITSupport;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class ComposerITSupport
    extends NexusBaseITSupport
{

  @Inject
  protected RepositoryManager repositoryManager;

  @Rule
  public RepositoryRuleComposer repos = new RepositoryRuleComposer(() -> repositoryManager);

  public ComposerITSupport() {
    testData.addDirectory(NexusPaxExamSupport.resolveBaseFile("target/it-resources/composer"));
  }

  @Nonnull
  protected ComposerClient composerClient(final Repository repository) throws Exception {
    checkNotNull(repository);

    return composerClient(repositoryBaseUrl(repository));
  }

  protected ComposerClient composerClient(final URL repositoryUrl) throws Exception {
    return new ComposerClient(
        clientBuilder(repositoryUrl).build(),
        clientContext(),
        repositoryUrl.toURI()
    );
  }

  @Nonnull
  protected URL repositoryBaseUrl(final Repository repository) {
    return resolveUrl(nexusUrl(), "/repository/" + repository.getName() + "/");
  }

  protected HttpClientBuilder clientBuilder(final URL nexusUrl) throws Exception {
    AuthScope scope = new AuthScope(nexusUrl.getHost(), -1);
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(scope, new UsernamePasswordCredentials("admin", "admin123"));

    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (FileInputStream instream = new FileInputStream(resolveBaseFile("target/it-resources/ssl/client.jks"))) {
      trustStore.load(instream, "password".toCharArray());
    }
    SSLContext context = SSLContexts.custom().loadTrustMaterial(trustStore, new TrustSelfSignedStrategy()).build();

    return HttpClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build())
        .setDefaultCredentialsProvider(credentialsProvider)
        .setSSLSocketFactory(new SSLConnectionSocketFactory(context, NoopHostnameVerifier.INSTANCE));
  }

  /**
   * @return Context with preemptive auth enabled for Nexus
   */
  protected HttpClientContext clientContext() {
    String hostname = nexusUrl().getHost();
    AuthCache authCache = new BasicAuthCache();
    HttpHost hostHttp = new HttpHost(hostname, nexusUrl().getPort(), "http");
    HttpHost hostHttps = new HttpHost(hostname, nexusUrl().getPort(), "https");
    authCache.put(hostHttp, new BasicScheme());
    authCache.put(hostHttps, new BasicScheme());

    HttpClientContext context = HttpClientContext.create();
    context.setAuthCache(authCache);

    return context;
  }

  public static Optional<FluentAsset> findAsset(Repository repository, String path) {
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    return repository.facet(ContentFacet.class).assets().path(path).find();
  }
}
