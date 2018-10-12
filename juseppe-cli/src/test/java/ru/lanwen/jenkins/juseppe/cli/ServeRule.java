package ru.lanwen.jenkins.juseppe.cli;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.lanwen.jenkins.juseppe.props.Props;

import java.io.IOException;
import java.io.File;
import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class ServeRule extends ExternalResource {
    private static final Logger LOG = LoggerFactory.getLogger(ServeRule.class);
    
    private CompletableFuture<Void> future;
    private int port;

    @Override
    protected void before() throws Throwable {
        ClassLoader classloader = getClass().getClassLoader();
        File plugins = new File(classloader.getResource("serve/plugins").getFile());
        File key = new File(classloader.getResource("serve/cert/uc.key").getFile());
        File crt = new File(classloader.getResource("serve/cert/uc.crt").getFile());
        port = findRandomOpenPortOnAllLocalInterfaces();

        future = CompletableFuture.runAsync(() -> {
            try {
                new ServeCommand().unsafeRun(Props.populated()
                        .withBaseurl(uri())
                        .withPluginsDir(plugins.toString())
                        .withCert(crt.toString())
                        .withKey(key.toString())
                        .withPort(port));
            } catch (Exception e) {
                LOG.error("Can't start juseppe", e);
            }
        });
    }

    @Override
    protected void after() {
        future.cancel(true);
    }

    public URI uri() {
        return URI.create(format("http://localhost:%s", port));
    }

    private Integer findRandomOpenPortOnAllLocalInterfaces() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
