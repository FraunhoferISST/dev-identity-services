package org.eclipse.tractusx.identityhub.fsvault;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.nio.file.Files;
import java.nio.file.Path;

@Extension(value = FSVaultExtension.NAME)
public class FSVaultExtension implements ServiceExtension {

    static final String NAME = "LocalFilesystemVaultExtension";
    private Path path;


    @Provider
    public Vault provideFSVaultExtension(ServiceExtensionContext context) {
        String fileDirectoryPath = System.getProperty("user.dir") + "/vaultdir";
        path = Path.of(fileDirectoryPath);
        Monitor monitor = context.getMonitor().withPrefix("FSVaultExtension");
        try {
            if (!Files.exists(path)) {
                monitor.info("Trying to create vault directory: " + fileDirectoryPath);
                Files.createDirectory(path);
            } else {
                monitor.info("Vault directory already exists: " + fileDirectoryPath);
            }
        } catch (Exception ex) {}
        return new LocalFileSystemVault(path, "defaultPartition", context);
    }

}
