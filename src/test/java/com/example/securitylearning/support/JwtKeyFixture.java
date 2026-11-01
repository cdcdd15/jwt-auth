package com.example.securitylearning.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPair;
import java.util.Base64;

import com.example.securitylearning.security.RsaKeyGenerator;

public final class JwtKeyFixture implements AutoCloseable {

    private final KeyPair keys = RsaKeyGenerator.generateDevelopmentKeyPair();
    private final Path directory;
    private final Path publicPath;
    private final Path privatePath;

    public JwtKeyFixture() {
        try {
            directory = Files.createTempDirectory("jwt-test-keys-",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            publicPath = directory.resolve("public.pem");
            privatePath = directory.resolve("private.pem");
            writePem(publicPath, "PUBLIC KEY", keys.getPublic().getEncoded());
            writePem(privatePath, "PRIVATE KEY", keys.getPrivate().getEncoded());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot prepare local test keys", exception);
        }
    }

    public KeyPair keys() {
        return keys;
    }

    public Path publicPath() {
        return publicPath;
    }

    public Path privatePath() {
        return privatePath;
    }

    private static void writePem(Path path, String type, byte[] encoded) throws IOException {
        Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        String body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
        Files.writeString(path, "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n");
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(privatePath);
        Files.deleteIfExists(publicPath);
        Files.deleteIfExists(directory);
    }
}
