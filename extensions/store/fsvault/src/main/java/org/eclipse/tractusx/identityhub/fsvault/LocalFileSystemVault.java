package org.eclipse.tractusx.identityhub.fsvault;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;

public class LocalFileSystemVault implements Vault {

    private final Path path;
    private final Path defaultPartitionPath;
    private final Monitor monitor;

    public LocalFileSystemVault(Path path, String defaultPartition, ServiceExtensionContext context) {
        this.path = path;
        this.defaultPartitionPath = path.resolve(defaultPartition);
        this.monitor = context.getMonitor().withPrefix(this.getClass().getSimpleName());
        monitor.info("Local FileSystemVault created");
        monitor.info("Local FileSystemVault default partition: " + defaultPartition);
    }

    @Override
    public @Nullable String resolveSecret(String key) {
        return getFile(defaultPartitionPath.resolve(key));
    }

    @Override
    public Result<Void> storeSecret(String key, String value) {
        if (storeFile(defaultPartitionPath, key, value)) {
            return Result.success();
        }  else {
            return Result.failure("Failed to store secret");
        }
    }

    @Override
    public Result<Void> deleteSecret(String key) {
        if (deleteFile(defaultPartitionPath.resolve(key))) {
            return Result.success();
        } else {
            return Result.failure("Failed to delete secret");
        }
    }

    @Override
    public String resolveSecret(@Nullable String vaultPartition, String key) {
        Path actualPath = vaultPartition == null ? defaultPartitionPath : path.resolve(vaultPartition);
        return getFile(path.resolve(actualPath).resolve(key));
    }

    @Override
    public Result<Void> storeSecret(@Nullable String vaultPartition, String key, String value) {
        Path actualPath = vaultPartition == null ? defaultPartitionPath : path.resolve(vaultPartition);
        if (storeFile(actualPath, key, value)) {
            return Result.success();
        } else  {
            return Result.failure("Failed to store secret");
        }
    }

    @Override
    public Result<Void> deleteSecret(@Nullable String vaultPartition, String key) {
        Path actualPath = vaultPartition == null ? defaultPartitionPath : path.resolve(vaultPartition);
        if (deleteFile(actualPath.resolve(key))) {
            return Result.success();
        } else {
            return Result.failure("Failed to delete secret");
        }
    }

    private String getFile(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception ex) {
            monitor.warning("Local FileSystemVault getFile failed for path: " + path.toString());
            return null;
        }
    }

    private boolean storeFile(Path path, String name, String value) {
        try {
            if (!Files.isDirectory(path)) {
                monitor.info("Trying to create dir: " + path);
                Files.createDirectories(path);
            }

            Files.write(path.resolve(name), value.getBytes());
            return true;
        } catch (Exception ex) {
            monitor.warning("Local FileSystemVault storeFile failed for path: " + path.resolve(name));
            monitor.severe("Message: " + ex.getMessage(), ex);
        }
        return false;
    }

    private boolean deleteFile(Path path) {
        try {
            Files.delete(path);
            return true;
        } catch (Exception ex) {
            monitor.warning("Local FileSystemVault deleteFile failed for path: " + path.toString());
        }
        return false;
    }
}
