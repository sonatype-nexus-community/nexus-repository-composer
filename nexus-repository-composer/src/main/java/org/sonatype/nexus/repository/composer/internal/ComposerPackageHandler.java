package org.sonatype.nexus.repository.composer.internal;

import org.joda.time.DateTime;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

public class ComposerPackageHandler
        implements Handler
{
    public static final String DO_NOT_REWRITE = "ComposerProviderHandler.doNotRewrite";

    private final ComposerJsonProcessor composerJsonProcessor;

    @Inject
    public ComposerPackageHandler(final ComposerJsonProcessor composerJsonProcessor) {
        this.composerJsonProcessor = checkNotNull(composerJsonProcessor);
    }

    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception {
        Response response = context.proceed();
        if (!Boolean.parseBoolean(context.getRequest().getAttributes().get(DO_NOT_REWRITE, String.class))) {
            if (response.getStatus().getCode() == HttpStatus.OK && response.getPayload() != null) {
                final Content content = composerJsonProcessor.rewritePackageJson(context.getRepository(), response.getPayload());
                if (response.getPayload() instanceof Content) {
                    final DateTime lastModified = ((Content) response.getPayload()).getAttributes().get(Content.CONTENT_LAST_MODIFIED, DateTime.class);
                    final String eTag = ((Content) response.getPayload()).getAttributes().get(Content.CONTENT_ETAG, String.class);

                    content.getAttributes().set(Content.CONTENT_LAST_MODIFIED, lastModified);
                    content.getAttributes().set(Content.CONTENT_ETAG, eTag);
                }

                response = HttpResponses.ok(content);
            }
        }
        return response;
    }
}
