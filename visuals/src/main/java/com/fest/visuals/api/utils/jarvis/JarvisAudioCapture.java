package com.fest.visuals.api.utils.jarvis;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;

/**
 * Records the default microphone into raw 16kHz mono PCM while held, which is the format
 * faster-whisper wants and small enough to ship over a websocket without any encoding step.
 *
 * <p>Capture runs on its own thread: {@link TargetDataLine#read} blocks, and stalling the game
 * tick or the render thread on it would freeze the client for as long as the button is held.
 */
public class JarvisAudioCapture {
    private static final AudioFormat FORMAT = new AudioFormat(16_000f, 16, 1, true, false);

    private TargetDataLine line;
    private Thread thread;
    private volatile ByteArrayOutputStream buffer;

    public boolean start() {
        if (line != null) return true;

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            if (!AudioSystem.isLineSupported(info)) return false;

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(FORMAT);
            line.start();
        } catch (LineUnavailableException | IllegalArgumentException e) {
            line = null;
            return false;
        }

        buffer = new ByteArrayOutputStream();
        thread = new Thread(this::pump, "festvisuals-jarvis-capture");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    private void pump() {
        byte[] chunk = new byte[2048];
        TargetDataLine active = line;
        ByteArrayOutputStream target = buffer;

        while (active != null && active.isOpen()) {
            int read = active.read(chunk, 0, chunk.length);
            if (read <= 0) break;
            synchronized (target) {
                target.write(chunk, 0, read);
            }
        }
    }

    /** Stops the line and returns everything captured since {@link #start()}. */
    public byte[] stop() {
        TargetDataLine active = line;
        ByteArrayOutputStream target = buffer;
        line = null;
        buffer = null;

        if (active == null) return new byte[0];

        active.stop();
        active.close();

        if (thread != null) {
            try {
                thread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (target == null) return new byte[0];
        synchronized (target) {
            return target.toByteArray();
        }
    }

    public boolean isCapturing() {
        return line != null;
    }

    public static int sampleRate() {
        return (int) FORMAT.getSampleRate();
    }
}
