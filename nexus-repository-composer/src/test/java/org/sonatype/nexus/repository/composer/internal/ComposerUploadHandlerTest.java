package org.sonatype.nexus.repository.composer.internal;

import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ComposerUploadHandlerTest
        extends TestSupport {

    @Mock
    private ContentPermissionChecker contentPermissionChecker;

    @Mock
    private VariableResolverAdapter variableResolverAdapter;

    private final Set<UploadDefinitionExtension> uploadDefinitionExtensions = new LinkedHashSet<>();

    private final ComposerUploadHandler underTest = new ComposerUploadHandler(
            contentPermissionChecker,
            variableResolverAdapter,
            uploadDefinitionExtensions
    );

    @Test
    public void testGetResponseContents() throws IOException {
        // given
        Repository hostedRepo = mock(Repository.class);
        ComposerHostedFacet mockFacet = mock(ComposerHostedFacet.class);
        when(hostedRepo.facet(ComposerHostedFacet.class)).thenReturn(mockFacet);
        StorageFacet mockStorage = mock(StorageFacet.class);
        Supplier<StorageTx> mockSupplier = mock(Supplier.class);
        when(mockStorage.txSupplier()).thenReturn(mockSupplier);
        when(hostedRepo.facet(StorageFacet.class)).thenReturn(mockStorage);
        Map<String, PartPayload> pathToPayload = new LinkedHashMap<>();
        PartPayload payload = mock(PartPayload.class);
        pathToPayload.put("path", payload);
        doNothing().when(mockFacet).upload(eq("vendor"), eq("name"), eq("1.2.3"), eq(null), eq(null), eq(null), any(PartPayload.class));

        // when
        List<Content> contentList = underTest.getResponseContents("vendor", "name", "1.2.3", hostedRepo, pathToPayload);

        // then
        verify(mockFacet, times(1)).upload(eq("vendor"), eq("name"), eq("1.2.3"), eq(null), eq(null), eq(null), any(PartPayload.class));
        assertThat(contentList, is(notNullValue()));
        assertThat(contentList.size(), is(1));
    }

    // test normalizePath function
    @Test
    public void testNormalizePath() {
        // given
        String path = "/vendor//path/compli//cated//";
        String expected = "vendor/path/compli/cated";

        // when
        String result = underTest.normalizePath(path);

        // then
        assertThat(result, is(expected));
    }

}