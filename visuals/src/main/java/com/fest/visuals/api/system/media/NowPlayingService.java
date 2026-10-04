package com.fest.visuals.api.system.media;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.fest.visuals.api.system.backend.ClientInfo;

/**
 * Bridges the Windows System Media Transport Controls into the client.
 *
 * <p>Windows only exposes SMTC through WinRT, which the JVM cannot call. A tiny C# helper is
 * compiled once with the framework's {@code csc.exe} against the split WinMetadata files and
 * then run as a child process. It streams one tab separated line per sample:
 * <pre>
 *   M \t title \t artist \t status \t positionMs \t durationMs \t source \t artPath
 *   P \t peak
 * </pre>
 * Playback commands travel the other way through a small command file the helper polls.
 * Everything degrades to a no-op when the platform, compiler or metadata is unavailable.
 */
public class NowPlayingService {
    @Getter private static final NowPlayingService instance = new NowPlayingService();

    /** Number of retained peak samples. The card renders the tail of this ring as a waveform. */
    public static final int WAVE_SAMPLES = 64;

    private static final String WIN_METADATA = "C:\\Windows\\System32\\WinMetadata";
    private static final String FRAMEWORK = "C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319";

    private final AtomicReference<NowPlaying> snapshot = new AtomicReference<>(NowPlaying.EMPTY);
    private final float[] wave = new float[WAVE_SAMPLES];
    private int waveHead;
    private volatile float peak;

    private volatile String artPath = "";
    private volatile int artVersion;

    /** Wall clock (nanos) at which {@link NowPlaying#positionMs()} was captured. */
    private volatile long positionStampNanos = System.nanoTime();

    /** Assigned from the bootstrap thread, read from the render thread. */
    private volatile Path commandFile;
    private volatile Process process;

    public NowPlaying current() {
        return snapshot.get();
    }

    public float peak() {
        return peak;
    }

    /** Absolute path of the cover the helper last wrote, or empty when the track has none. */
    public String artPath() {
        return artPath;
    }

    /** Bumped whenever {@link #artPath()} changes, so the texture is uploaded once per track. */
    public int artVersion() {
        return artVersion;
    }

    /**
     * Peak sample {@code index} steps back from the newest, where 0 is the newest.
     * Returns 0 for indices outside the retained window.
     */
    public float waveSample(int index) {
        if (index < 0 || index >= WAVE_SAMPLES) return 0f;
        synchronized (wave) {
            return wave[Math.floorMod(waveHead - 1 - index, WAVE_SAMPLES)];
        }
    }

    /**
     * Playback position extrapolated to now. Media sessions only report position about once a
     * second (and some browsers never advance it at all), so the elapsed wall time since the
     * last sample is added back while the track is playing.
     */
    public int positionMs() {
        NowPlaying np = snapshot.get();
        if (!np.playing()) return np.positionMs();

        long elapsed = (System.nanoTime() - positionStampNanos) / 1_000_000L;
        int projected = np.positionMs() + (int) elapsed;
        return np.durationMs() > 0 ? Math.min(projected, np.durationMs()) : projected;
    }

    public void togglePlayPause() { sendCommand("TOGGLE"); }
    public void skipNext()        { sendCommand("NEXT"); }
    public void skipPrevious()    { sendCommand("PREV"); }

    private void sendCommand(String command) {
        if (commandFile == null) return;
        try {
            Files.writeString(commandFile, command, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    // ------------------------------------------------------------------ lifecycle

    public void start() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("windows")) return;
        if (process != null && process.isAlive()) return;

        Thread boot = new Thread(this::bootstrap, "FestVisuals-MediaBoot");
        boot.setDaemon(true);
        boot.start();
    }

    public void stop() {
        if (process != null) {
            process.destroyForcibly();
            process = null;
        }
        snapshot.set(NowPlaying.EMPTY);
        peak = 0f;
    }

    /** Compiles the helper if needed and launches it. Runs off-thread; failures are silent. */
    private void bootstrap() {
        try {
            Path dir = Paths.get(ClientInfo.CONFIG_PATH_OTHER, "media");
            Files.createDirectories(dir);

            Path artDir = dir.resolve("art");
            Files.createDirectories(artDir);

            Path exe = dir.resolve("MediaBridge.exe");
            Path source = dir.resolve("MediaBridge.cs");

            // Bridges started by older builds outlived a crashed or killed game; one still running
            // would also lock the exe and make the recompile below fail.
            stopLeftoverBridges(exe);

            // Recompile whenever the embedded source changes.
            boolean stale = !Files.exists(exe)
                    || !Files.exists(source)
                    || !Files.readString(source, StandardCharsets.UTF_8).equals(BRIDGE_SOURCE);

            if (stale) {
                Files.writeString(source, BRIDGE_SOURCE, StandardCharsets.UTF_8);
                if (!compile(source, exe)) return;
            }

            commandFile = dir.resolve("command");
            Files.deleteIfExists(commandFile);

            ProcessBuilder pb = new ProcessBuilder(
                    exe.toAbsolutePath().toString(),
                    commandFile.toAbsolutePath().toString(),
                    artDir.toAbsolutePath().toString(),
                    // The bridge exits by itself once this process is gone.
                    String.valueOf(ProcessHandle.current().pid())
            );
            pb.redirectErrorStream(true);
            process = pb.start();

            Thread reader = new Thread(this::pump, "FestVisuals-NowPlaying");
            reader.setDaemon(true);
            reader.start();
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private static void stopLeftoverBridges(Path exe) {
        Path target = exe.toAbsolutePath().normalize();
        ProcessHandle.allProcesses()
                .filter(p -> runs(p, target))
                .forEach(p -> {
                    p.destroyForcibly();
                    p.onExit().completeOnTimeout(p, 2, TimeUnit.SECONDS).join();
                });
    }

    private static boolean runs(ProcessHandle process, Path exe) {
        try {
            return process.info().command()
                    .map(command -> Paths.get(command).toAbsolutePath().normalize().equals(exe))
                    .orElse(false);
        } catch (RuntimeException e) {
            return false; // unreadable or odd command line: not ours
        }
    }

    private boolean compile(Path source, Path exe) {
        Path csc = Paths.get(FRAMEWORK, "csc.exe");
        Path metadata = Paths.get(WIN_METADATA);
        if (!Files.isRegularFile(csc) || !Files.isDirectory(metadata)) return false;

        List<String> command = new ArrayList<>(List.of(
                csc.toString(),
                "-nologo",
                "-optimize+",
                // winexe, not exe: a console subsystem child launched from javaw pops up its own
                // window, which players took for a virus. Stdout still reaches our redirected pipe.
                "-target:winexe",
                "-out:" + exe.toAbsolutePath()
        ));

        // The merged Windows.winmd is gone on modern Windows; reference the split files instead.
        for (String winmd : new String[]{"Windows.Foundation.winmd", "Windows.Media.winmd", "Windows.Storage.winmd"}) {
            Path path = metadata.resolve(winmd);
            if (!Files.isReadable(path)) return false;
            command.add("-reference:" + path);
        }
        command.add("-reference:" + Paths.get(FRAMEWORK, "System.Runtime.dll"));
        command.add(source.toAbsolutePath().toString());

        try {
            Process compiler = new ProcessBuilder(command).redirectErrorStream(true).start();
            compiler.getInputStream().readAllBytes();
            return compiler.waitFor() == 0 && Files.exists(exe);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // ------------------------------------------------------------------ stream parsing

    private void pump() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("P\t")) {
                    pushPeak(parseFloat(line.substring(2)));
                } else if (line.startsWith("M\t")) {
                    applyMedia(line.substring(2));
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void pushPeak(float value) {
        float clamped = Math.max(0f, Math.min(1f, value));
        peak = clamped;
        synchronized (wave) {
            wave[waveHead] = clamped;
            waveHead = (waveHead + 1) % WAVE_SAMPLES;
        }
    }

    private void applyMedia(String payload) {
        String[] parts = payload.split("\t", -1);
        if (parts.length < 6) {
            snapshot.set(NowPlaying.EMPTY);
            return;
        }

        String title = parts[0];
        String artist = parts[1];
        String status = parts[2];
        int position = parseInt(parts[3]);
        int duration = parseInt(parts[4]);
        String source = parts[5];
        String art = parts.length > 6 ? parts[6] : "";

        if ("NONE".equals(status) || (title.isEmpty() && artist.isEmpty())) {
            snapshot.set(NowPlaying.EMPTY);
            return;
        }

        if (!art.equals(artPath)) {
            artPath = art;
            artVersion++;
        }

        positionStampNanos = System.nanoTime();
        snapshot.set(new NowPlaying(title, artist, "Playing".equalsIgnoreCase(status), position, duration, source));
    }

    private static int parseInt(String value) {
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private static float parseFloat(String value) {
        try { return Float.parseFloat(value.trim().replace(',', '.')); } catch (NumberFormatException e) { return 0f; }
    }

    public record NowPlaying(String title, String artist, boolean playing, int positionMs, int durationMs, String source) {
        public static final NowPlaying EMPTY = new NowPlaying("", "", false, 0, 0, "");

        public boolean hasTrack() { return !title.isEmpty() || !artist.isEmpty(); }
    }

    /**
     * Helper compiled at runtime. Kept free of System.Runtime.WindowsRuntime so it only needs the
     * split WinMetadata files: async results are awaited through IAsyncOperation.Completed and the
     * thumbnail is read with a WinRT DataReader rather than the AsStreamForRead extension.
     */
    private static final String BRIDGE_SOURCE = """
            // FestVisuals media bridge (windowless build).
            using System;
            using System.Diagnostics;
            using System.Globalization;
            using System.IO;
            using System.Runtime.InteropServices;
            using System.Threading;
            using Windows.Foundation;
            using Windows.Media.Control;
            using Windows.Storage.Streams;

            [ComImport, Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")]
            internal class MMDeviceEnumeratorComObject { }

            [ComImport, Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
            internal interface IMMDeviceEnumerator {
                int NotImpl1();
                int GetDefaultAudioEndpoint(int dataFlow, int role, out IMMDevice device);
            }

            [ComImport, Guid("D666063F-1587-4E43-81F1-B948E807363F"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
            internal interface IMMDevice {
                int Activate(ref Guid iid, int clsCtx, IntPtr activationParams, [MarshalAs(UnmanagedType.IUnknown)] out object instance);
            }

            [ComImport, Guid("C02216F6-8C67-4B5B-9D00-D008E73E0064"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
            internal interface IAudioMeterInformation {
                int GetPeakValue(out float peak);
            }

            internal static class AudioPeak {
                private static IAudioMeterInformation meter;

                public static float Read() {
                    try {
                        if (meter == null) {
                            var enumerator = (IMMDeviceEnumerator)(new MMDeviceEnumeratorComObject());
                            IMMDevice device;
                            enumerator.GetDefaultAudioEndpoint(0, 1, out device);
                            Guid iid = typeof(IAudioMeterInformation).GUID;
                            object instance;
                            device.Activate(ref iid, 1, IntPtr.Zero, out instance);
                            meter = (IAudioMeterInformation)instance;
                        }
                        float value;
                        meter.GetPeakValue(out value);
                        return value;
                    } catch {
                        meter = null;
                        return 0f;
                    }
                }
            }

            public static class MediaBridge {
                const string TAB = "\\t";

                static T Await<T>(IAsyncOperation<T> operation) {
                    var done = new ManualResetEventSlim(false);
                    operation.Completed = (op, status) => done.Set();
                    done.Wait();
                    return operation.GetResults();
                }

                static string Clean(string value) {
                    if (string.IsNullOrEmpty(value)) return "";
                    return value.Replace('\\t', ' ').Replace('\\r', ' ').Replace('\\n', ' ');
                }

                static string SaveThumbnail(IRandomAccessStreamReference reference, string artDir, int counter) {
                    try {
                        if (reference == null) return "";
                        var stream = Await(reference.OpenReadAsync());
                        if (stream == null || stream.Size == 0) return "";

                        uint size = (uint)stream.Size;
                        var reader = new DataReader(stream.GetInputStreamAt(0));
                        Await(reader.LoadAsync(size));

                        byte[] bytes = new byte[size];
                        reader.ReadBytes(bytes);

                        string path = Path.Combine(artDir, "art_" + counter + ".img");
                        File.WriteAllBytes(path, bytes);

                        string stale = Path.Combine(artDir, "art_" + (counter - 2) + ".img");
                        if (File.Exists(stale)) { try { File.Delete(stale); } catch { } }

                        return path;
                    } catch { return ""; }
                }

                public static int Main(string[] args) {
                    Console.OutputEncoding = System.Text.Encoding.UTF8;

                    string commandFile = args.Length > 0 ? args[0] : null;
                    string artDir = args.Length > 1 ? args[1] : Path.GetTempPath();

                    // Writes to a closed stdout pipe fail silently in .NET, so a crashed or killed
                    // game would leave this loop running forever; watch the game process instead.
                    Process game = null;
                    if (args.Length > 2) {
                        try { game = Process.GetProcessById(int.Parse(args[2], CultureInfo.InvariantCulture)); }
                        catch { return 0; }
                    }

                    GlobalSystemMediaTransportControlsSessionManager manager;
                    try {
                        manager = Await(GlobalSystemMediaTransportControlsSessionManager.RequestAsync());
                    } catch { return 1; }

                    string lastTrack = "";
                    string artFile = "";
                    int artCounter = 0;
                    int tick = 0;

                    while (true) {
                        if (game != null && tick % 20 == 0) {
                            try { if (game.HasExited) return 0; } catch { }
                        }

                        GlobalSystemMediaTransportControlsSession session = null;
                        try { session = manager.GetCurrentSession(); } catch { }

                        if (commandFile != null && File.Exists(commandFile)) {
                            string command = "";
                            try {
                                command = File.ReadAllText(commandFile);
                                File.Delete(commandFile);
                            } catch { }

                            if (session != null) {
                                try {
                                    if (command.Contains("TOGGLE")) session.TryTogglePlayPauseAsync();
                                    else if (command.Contains("NEXT")) session.TrySkipNextAsync();
                                    else if (command.Contains("PREV")) session.TrySkipPreviousAsync();
                                } catch { }
                            }
                        }

                        Console.WriteLine("P" + TAB + AudioPeak.Read().ToString("F4", CultureInfo.InvariantCulture));

                        // Metadata changes far slower than the meter; sample it once a second.
                        if (tick % 20 == 0) {
                            if (session == null) {
                                lastTrack = "";
                                artFile = "";
                                Console.WriteLine("M" + TAB + TAB + TAB + "NONE" + TAB + "0" + TAB + "0" + TAB + TAB);
                            } else {
                                try {
                                    var props = Await(session.TryGetMediaPropertiesAsync());
                                    var timeline = session.GetTimelineProperties();
                                    var playback = session.GetPlaybackInfo();

                                    string title = Clean(props.Title);
                                    string artist = Clean(props.Artist);
                                    string status = playback.PlaybackStatus.ToString();
                                    string source = Clean(session.SourceAppUserModelId);

                                    long position = (long)timeline.Position.TotalMilliseconds;
                                    long duration = (long)(timeline.EndTime - timeline.StartTime).TotalMilliseconds;

                                    // Position is stamped when the session last reported it, and
                                    // browsers can go many seconds between updates. Replay the gap
                                    // so the clock does not stall and then jump.
                                    if (status == "Playing") {
                                        double age = (DateTimeOffset.Now - timeline.LastUpdatedTime).TotalMilliseconds;
                                        if (age > 0 && age < 60000) position += (long)age;
                                    }

                                    // Sources that do not know the length report junk; call it unknown.
                                    if (duration <= 0 || duration > 24L * 60 * 60 * 1000) duration = 0;
                                    if (position < 0) position = 0;
                                    if (duration > 0 && position > duration) position = duration;

                                    string track = title + "|" + artist;
                                    if (track != lastTrack) {
                                        lastTrack = track;
                                        artCounter++;
                                        artFile = SaveThumbnail(props.Thumbnail, artDir, artCounter);
                                    }

                                    Console.WriteLine("M" + TAB + title + TAB + artist + TAB + status + TAB
                                            + position + TAB + duration + TAB + source + TAB + artFile);
                                } catch {
                                    Console.WriteLine("M" + TAB + TAB + TAB + "NONE" + TAB + "0" + TAB + "0" + TAB + TAB);
                                }
                            }
                        }

                        tick++;
                        Thread.Sleep(50);
                    }
                }
            }
            """;
}
