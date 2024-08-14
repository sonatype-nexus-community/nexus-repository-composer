package org.sonatype.nexus.repository.composer.internal.proxy;

import org.sonatype.nexus.repository.composer.internal.ComposerJsonProcessor;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.http.HttpStatus;
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
                response = HttpResponses
                        .ok(composerJsonProcessor.rewritePackageJson(context.getRepository(), response.getPayload()));
            }
        }
        return response;
    }
}
