package com.synopsys.integration.blackduck.imageinspector.name;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.synopsys.integration.blackduck.imageinspector.name.ImageNameResolver;

public class ImageNameResolverTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testSimple() {
        final ImageNameResolver resolver = new ImageNameResolver("alpine:latest");
        assertEquals("alpine", resolver.getNewImageRepo().get());
        assertEquals("latest", resolver.getNewImageTag().get());
    }

    @Test
    public void testNull() {
        final ImageNameResolver resolver = new ImageNameResolver("null:null");
        assertEquals("null", resolver.getNewImageRepo().get());
        assertEquals("null", resolver.getNewImageTag().get());
    }

    @Test
    public void testWithoutTag() {
        final ImageNameResolver resolver = new ImageNameResolver("alpine");
        assertEquals("alpine", resolver.getNewImageRepo().get());
        assertEquals("latest", resolver.getNewImageTag().get());
    }

    @Test
    public void testWithUrlPortTag() {
        final ImageNameResolver resolver = new ImageNameResolver("https://artifactory.team.domain.com:5002/repo:tag");
        assertEquals("https://artifactory.team.domain.com:5002/repo", resolver.getNewImageRepo().get());
        assertEquals("tag", resolver.getNewImageTag().get());
    }

    @Test
    public void testWithUrlPortNoTag() {
        final ImageNameResolver resolver = new ImageNameResolver("https://artifactory.team.domain.com:5002/repo");
        assertEquals("https://artifactory.team.domain.com:5002/repo", resolver.getNewImageRepo().get());
        assertEquals("latest", resolver.getNewImageTag().get());
    }

    @Test
    public void testWithUrlTag() {
        final ImageNameResolver resolver = new ImageNameResolver("https://artifactory.team.domain.com/repo:tag");
        assertEquals("https://artifactory.team.domain.com/repo", resolver.getNewImageRepo().get());
        assertEquals("tag", resolver.getNewImageTag().get());
    }

    @Test
    public void testWithUrlNoTag() {
        final ImageNameResolver resolver = new ImageNameResolver("https://artifactory.team.domain.com/repo");
        assertEquals("https://artifactory.team.domain.com/repo", resolver.getNewImageRepo().get());
        assertEquals("latest", resolver.getNewImageTag().get());
    }

    @Test
    public void testWithSha() {
        final ImageNameResolver resolver = new ImageNameResolver("solsson/kafka-prometheus-jmx-exporter@sha256:a23062396cd5af1acdf76512632c20ea6be76885dfc20cd9ff40fb23846557e8");
        assertEquals("solsson/kafka-prometheus-jmx-exporter", resolver.getNewImageRepo().get());
        assertEquals("@sha256:a23062396cd5af1acdf76512632c20ea6be76885dfc20cd9ff40fb23846557e8", resolver.getNewImageTag().get());
    }
}
