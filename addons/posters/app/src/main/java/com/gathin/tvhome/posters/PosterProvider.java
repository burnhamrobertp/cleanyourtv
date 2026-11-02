package com.gathin.tvhome.posters;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Fetches the https poster a Watch Next row names for the launcher, which has no INTERNET permission, and hands
 * it back as a file: content://com.gathin.tvhome.posters/poster?url=<https url>. Only apps signed like this one
 * may ask, and only images from public https addresses, MAX_BYTES at most, are fetched.
 */
public final class PosterProvider extends ContentProvider {
    private static final String TAG = "TvPosters";
    private static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final int MAX_FILES = 40;
    private static final int MAX_REDIRECTS = 3;
    private static final int TIMEOUT_MS = 8000;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("read only");
        Context context = getContext();
        String caller = getCallingPackage();
        if (caller == null || context.getPackageManager().checkSignatures(caller, context.getPackageName())
                != PackageManager.SIGNATURE_MATCH) {
            throw new SecurityException(caller + " is not signed like " + context.getPackageName());
        }
        String url = uri.getQueryParameter("url");
        if (url == null || !url.startsWith("https://")) throw new FileNotFoundException("not an https url");

        File dir = new File(context.getCacheDir(), "posters");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new FileNotFoundException("no cache folder");
        File file = new File(dir, sha1(url));
        if (!file.exists()) {
            fetch(url, file);
            prune(dir);
        }
        file.setLastModified(System.currentTimeMillis());

        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private static void fetch(String url, File dest) throws FileNotFoundException {
        File part = null;
        try {
            HttpURLConnection c = open(new URL(url));
            try {
                String type = c.getContentType();
                if (type == null || !type.startsWith("image/")) throw new IOException("not an image: " + type);
                part = File.createTempFile("poster", ".part", dest.getParentFile());
                try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(part)) {
                    byte[] buf = new byte[16384];
                    int total = 0;
                    for (int n; (n = in.read(buf)) > 0; ) {
                        total += n;
                        if (total > MAX_BYTES) throw new IOException("over " + MAX_BYTES + " bytes");
                        out.write(buf, 0, n);
                    }
                }
            } finally {
                c.disconnect();
            }
            if (!part.renameTo(dest)) throw new IOException("could not keep the poster");
            part = null;
        } catch (IOException | RuntimeException e) {
            // No URL in the log: it names what was watched.
            Log.w(TAG, "poster not fetched: " + e);
            throw new FileNotFoundException(e.toString());
        } finally {
            if (part != null) part.delete();
        }
    }

    /** Follows redirects itself, so each hop is checked for https and a public address before it connects. */
    private static HttpURLConnection open(URL url) throws IOException {
        for (int hop = 0; ; hop++) {
            if (!"https".equals(url.getProtocol())) throw new IOException("not https");
            for (InetAddress a : InetAddress.getAllByName(url.getHost())) {
                if (!isPublic(a)) throw new IOException("not a public address");
            }
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(TIMEOUT_MS);
            c.setReadTimeout(TIMEOUT_MS);
            int code = c.getResponseCode();
            if (code / 100 == 3 && hop < MAX_REDIRECTS) {
                String next = c.getHeaderField("Location");
                c.disconnect();
                if (next == null) throw new IOException("redirect without a Location");
                url = new URL(url, next);
                continue;
            }
            if (code != HttpURLConnection.HTTP_OK) {
                c.disconnect();
                throw new IOException("HTTP " + code);
            }

            return c;
        }
    }

    static boolean isPublic(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()
                || a.isMulticastAddress()) {
            return false;
        }
        byte[] b = a.getAddress();

        return !isUniqueLocalV6(b) && !isSharedAddressSpace(b);
    }

    private static boolean isUniqueLocalV6(byte[] b) {
        return b.length == 16 && (b[0] & 0xFE) == 0xFC;
    }

    private static boolean isSharedAddressSpace(byte[] b) {
        return b.length == 4 && (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 64;
    }

    private static void prune(File dir) {
        File[] files = dir.listFiles((d, name) -> !name.endsWith(".part"));
        if (files == null || files.length <= MAX_FILES) return;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (int i = MAX_FILES; i < files.length; i++) files[i].delete();
    }

    private static String sha1(String s) {
        try {
            StringBuilder hex = new StringBuilder();
            for (byte x : MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8))) {
                hex.append(String.format("%02x", x));
            }

            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
