package com.elastisys.scale.cloudpool.commons.testutils.sshserver;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Command} that runs on the SSH server as an external {@link Process}.
 *
 * @see PermissiveSshServer
 */
public class ExternalProcessCommand implements Command {
    static Logger LOG = LoggerFactory.getLogger(ExternalProcessCommand.class);

    private String command;
    private InputStream inStream;
    private OutputStream outStream;
    private OutputStream errorStream;
    private ExitCallback callback;

    public ExternalProcessCommand(String command) {
        this.command = command;
    }

    @Override
    public void setInputStream(InputStream in) {
        this.inStream = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.outStream = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.errorStream = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void start(Environment env) throws IOException {
        List<String> commandParts = Arrays.asList(this.command.split("\\s+"));
        ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
        processBuilder.environment().putAll(env.getEnv());
        LOG.debug("running command: " + processBuilder.command());
        try {
            Process process = processBuilder.start();
            // close stdin
            process.getOutputStream().close();
            readProcessOutput(process);

            LOG.debug("waiting for process to quit");
            int exitCode = process.waitFor();
            LOG.debug("process exited with code: " + exitCode);
            if (this.callback != null) {
                this.callback.onExit(exitCode);
            }
        } catch (Exception e) {
            String errorMessage = format("failed to run command: %s", e.getMessage());
            this.errorStream.write((errorMessage + "\n").getBytes());
            this.errorStream.flush();
            this.callback.onExit(1, errorMessage);
        }
    }

    private void readProcessOutput(Process process) throws IOException {
        byte[] buffer = new byte[1024];

        try (InputStream stdout = process.getInputStream(); InputStream stderr = process.getErrorStream()) {
            while (!done(process) || stdout.available() > 0 || stderr.available() > 0) {
                while (stdout.available() > 0) {
                    int bytesRead = stdout.read(buffer);
                    this.outStream.write(buffer, 0, bytesRead);
                    LOG.debug("stdout >> " + new String(buffer, 0, bytesRead));
                    this.outStream.flush();
                }
                while (stderr.available() > 0) {
                    int bytesRead = stderr.read(buffer);
                    this.errorStream.write(buffer, 0, bytesRead);
                    LOG.debug("stderr >> " + new String(buffer, 0, bytesRead));
                    this.errorStream.flush();
                }
            }
        }
    }

    private boolean done(Process process) {
        try {
            process.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            return false;
        }
    }

    @Override
    public void destroy() {
    }
}
