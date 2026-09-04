package com.fest.visuals.api.utils.jarvis;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Plays the TTS reply the server sends back.
 *
 * <p>Goes through {@code javax.sound.sampled} rather than Minecraft's OpenAL context: the reply
 * is a plain stereo/mono clip with no position in the world to render from, and reading the WAV
 * header ourselves means the server is free to pick whatever sample rate its TTS engine outputs
 * (Piper's default voices are 22050Hz, not the 16kHz the microphone capture uses).
 */
public class JarvisAudioPlayer {
    private volatile SourceDataLine current;

    /** Parses a standard RIFF/WAVE byte array and plays it on a background thread. */
    public void play(byte[] wav, float volume) {
        AudioFormat format = parseWavFormat(wav);
        if (format == null) return;

        int dataOffset = findDataChunk(wav);
        if (dataOffset < 0) return;

        Thread thread = new Thread(() -> playBlocking(format, wav, dataOffset, volume), "festvisuals-jarvis-playback");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        SourceDataLine line = current;
        if (line != null) line.stop();
    }

    private void playBlocking(AudioFormat format, byte[] wav, int dataOffset, float volume) {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) return;

            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            current = line;
            line.open(format);
            applyVolume(line, volume);
            line.start();

            line.write(wav, dataOffset, wav.length - dataOffset);
            line.drain();
            line.close();
        } catch (LineUnavailableException e) {
            // No output device available right now; nothing sensible to do about it.
        } finally {
            current = null;
        }
    }

    private void applyVolume(SourceDataLine line, float volume) {
        try {
            var control = (javax.sound.sampled.FloatControl) line.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
            float clamped = Math.max(0.0001f, Math.min(1f, volume));
            float gain = (float) (Math.log10(clamped) * 20.0);
            control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), gain)));
        } catch (IllegalArgumentException ignored) {
            // Some mixers do not expose a gain control; the clip just plays at full volume.
        }
    }

    private AudioFormat parseWavFormat(byte[] wav) {
        int fmt = findChunk(wav, "fmt ");
        if (fmt < 0) return null;

        ByteBuffer buf = ByteBuffer.wrap(wav, fmt, 16).order(ByteOrder.LITTLE_ENDIAN);
        buf.getShort(); // audio format tag, assumed PCM
        int channels = buf.getShort();
        int sampleRate = buf.getInt();
        buf.getInt(); // byte rate
        buf.getShort(); // block align
        int bitsPerSample = buf.getShort();

        return new AudioFormat(sampleRate, bitsPerSample, channels, true, false);
    }

    private int findDataChunk(byte[] wav) {
        int chunk = findChunk(wav, "data");
        return chunk < 0 ? -1 : chunk;
    }

    /** Returns the offset of a chunk's payload (just past its 8-byte id+size header), or -1. */
    private int findChunk(byte[] wav, String id) {
        int pos = 12; // past "RIFF" + size + "WAVE"
        byte[] target = id.getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        while (pos + 8 <= wav.length) {
            boolean match = true;
            for (int i = 0; i < 4; i++) {
                if (wav[pos + i] != target[i]) {
                    match = false;
                    break;
                }
            }

            int size = ByteBuffer.wrap(wav, pos + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (match) return pos + 8;

            pos += 8 + size + (size % 2);
        }
        return -1;
    }
}
