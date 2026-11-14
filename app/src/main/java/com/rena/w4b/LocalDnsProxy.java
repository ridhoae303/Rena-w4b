package com.rena.w4b;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LocalDnsProxy {
    private static final ExecutorService POOL = Executors.newCachedThreadPool();
    private static final HashMap<String, Object[]> CACHE = new HashMap<String, Object[]>();
    private static ServerSocket server;
    private static volatile DnsManager.Provider provider;

    private LocalDnsProxy() {
    }

    public static synchronized int start(DnsManager.Provider next) {
        provider = next;
        synchronized (CACHE) {
            CACHE.clear();
        }
        if (server != null && !server.isClosed()) {
            return server.getLocalPort();
        }
        try {
            server = new ServerSocket(0, 64, InetAddress.getByName("127.0.0.1"));
            final ServerSocket local = server;
            Thread acceptor = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!local.isClosed()) {
                        try {
                            final Socket client = local.accept();
                            POOL.execute(new Runnable() {
                                @Override
                                public void run() {
                                    handle(client);
                                }
                            });
                        } catch (Throwable error) {
                            break;
                        }
                    }
                }
            }, "rena-dns-proxy");
            acceptor.setDaemon(true);
            acceptor.start();
            return server.getLocalPort();
        } catch (Throwable error) {
            server = null;
            return -1;
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            try {
                server.close();
            } catch (Throwable ignored) {
            }
            server = null;
        }
    }

    private static void handle(Socket client) {
        Socket remote = null;
        try {
            client.setSoTimeout(30000);
            client.setTcpNoDelay(true);
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            ByteArrayOutputStream head = new ByteArrayOutputStream();
            int state = 0;
            int value;
            while (head.size() < 32768 && (value = in.read()) != -1) {
                head.write(value);
                if (value == '\r' && (state == 0 || state == 2)) {
                    state++;
                } else if (value == '\n' && (state == 1 || state == 3)) {
                    state++;
                    if (state == 4) {
                        break;
                    }
                } else {
                    state = 0;
                }
            }
            String header = head.toString("ISO-8859-1");
            int lineEnd = header.indexOf("\r\n");
            if (lineEnd < 0) {
                client.close();
                return;
            }
            String requestLine = header.substring(0, lineEnd);
            String[] parts = requestLine.split(" ");
            if (parts.length < 3) {
                client.close();
                return;
            }
            String method = parts[0];
            String target = parts[1];
            if ("CONNECT".equalsIgnoreCase(method)) {
                int colon = target.lastIndexOf(':');
                String host = colon > 0 ? target.substring(0, colon) : target;
                int port = colon > 0 ? Integer.parseInt(target.substring(colon + 1)) : 443;
                InetAddress address = resolve(host);
                remote = new Socket();
                remote.connect(new InetSocketAddress(address, port), 15000);
                remote.setTcpNoDelay(true);
                out.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes("ISO-8859-1"));
                out.flush();
                pipe(client, remote);
            } else {
                URL url = new URL(target);
                int port = url.getPort() > 0 ? url.getPort() : 80;
                InetAddress address = resolve(url.getHost());
                remote = new Socket();
                remote.connect(new InetSocketAddress(address, port), 15000);
                OutputStream remoteOut = remote.getOutputStream();
                String path = url.getFile().length() == 0 ? "/" : url.getFile();
                StringBuilder forward = new StringBuilder();
                forward.append(method).append(' ').append(path).append(' ').append(parts[2]).append("\r\n");
                String[] lines = header.substring(lineEnd + 2).split("\r\n");
                for (String line : lines) {
                    if (line.length() == 0) {
                        continue;
                    }
                    String lower = line.toLowerCase();
                    if (lower.startsWith("proxy-connection:") || lower.startsWith("connection:") || lower.startsWith("proxy-authorization:")) {
                        continue;
                    }
                    forward.append(line).append("\r\n");
                }
                forward.append("Connection: close\r\n\r\n");
                remoteOut.write(forward.toString().getBytes("ISO-8859-1"));
                remoteOut.flush();
                pipe(client, remote);
            }
        } catch (Throwable error) {
            try {
                client.close();
            } catch (Throwable ignored) {
            }
            if (remote != null) {
                try {
                    remote.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void pipe(final Socket a, final Socket b) {
        Thread forward = new Thread(new Runnable() {
            @Override
            public void run() {
                copy(a, b);
            }
        });
        forward.setDaemon(true);
        forward.start();
        copy(b, a);
        try {
            forward.join(1000);
        } catch (Throwable ignored) {
        }
        close(a);
        close(b);
    }

    private static void copy(Socket from, Socket to) {
        byte[] buffer = new byte[16384];
        try {
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream();
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                out.flush();
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                to.shutdownOutput();
            } catch (Throwable ignored) {
            }
        }
    }

    private static void close(Socket socket) {
        try {
            socket.close();
        } catch (Throwable ignored) {
        }
    }

    private static boolean isLiteral(String host) {
        return host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$") || host.indexOf(':') >= 0;
    }

    static InetAddress resolve(String host) throws IOException {
        if (host == null || host.length() == 0) {
            throw new IOException("host");
        }
        if (isLiteral(host)) {
            return InetAddress.getByName(host);
        }
        String key = host.toLowerCase();
        long now = System.currentTimeMillis();
        synchronized (CACHE) {
            Object[] cached = CACHE.get(key);
            if (cached != null && (Long) cached[1] > now) {
                return (InetAddress) cached[0];
            }
        }
        DnsManager.Provider current = provider;
        if (current != null) {
            for (String endpoint : current.endpoints) {
                for (int type : new int[]{1, 28}) {
                    try {
                        Object[] answer = query(endpoint, host, type);
                        if (answer != null) {
                            InetAddress address = (InetAddress) answer[0];
                            long ttl = Math.max(30L, Math.min(600L, (Long) answer[1]));
                            synchronized (CACHE) {
                                CACHE.put(key, new Object[]{address, now + ttl * 1000L});
                            }
                            return address;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return InetAddress.getByName(host);
    }

    private static Object[] query(String endpoint, String host, int type) throws Exception {
        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        int id = (int) (System.nanoTime() & 0xffff);
        packet.write(0);
        packet.write(0);
        packet.write(0x01);
        packet.write(0x00);
        packet.write(0);
        packet.write(1);
        for (int i = 0; i < 6; i++) {
            packet.write(0);
        }
        for (String label : host.split("\\.")) {
            byte[] bytes = label.getBytes("US-ASCII");
            if (bytes.length == 0 || bytes.length > 63) {
                return null;
            }
            packet.write(bytes.length);
            packet.write(bytes);
        }
        packet.write(0);
        packet.write(0);
        packet.write(type);
        packet.write(0);
        packet.write(1);
        if (id < 0) {
            return null;
        }
        String encoded = Base64.encodeToString(packet.toByteArray(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        String url = endpoint + (endpoint.contains("?") ? "&" : "?") + "dns=" + encoded;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(6000);
            connection.setRequestProperty("Accept", "application/dns-message");
            if (connection.getResponseCode() != 200) {
                return null;
            }
            InputStream in = connection.getInputStream();
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int read;
            while ((read = in.read(buffer)) != -1) {
                body.write(buffer, 0, read);
            }
            return parse(body.toByteArray(), type);
        } finally {
            connection.disconnect();
        }
    }

    private static Object[] parse(byte[] data, int wanted) throws Exception {
        if (data.length < 12) {
            return null;
        }
        int qd = ((data[4] & 0xff) << 8) | (data[5] & 0xff);
        int an = ((data[6] & 0xff) << 8) | (data[7] & 0xff);
        int pos = 12;
        for (int i = 0; i < qd; i++) {
            pos = skipName(data, pos) + 4;
        }
        for (int i = 0; i < an; i++) {
            pos = skipName(data, pos);
            if (pos + 10 > data.length) {
                return null;
            }
            int type = ((data[pos] & 0xff) << 8) | (data[pos + 1] & 0xff);
            long ttl = ((long) (data[pos + 4] & 0xff) << 24) | ((data[pos + 5] & 0xff) << 16) | ((data[pos + 6] & 0xff) << 8) | (data[pos + 7] & 0xff);
            int length = ((data[pos + 8] & 0xff) << 8) | (data[pos + 9] & 0xff);
            pos += 10;
            if (pos + length > data.length) {
                return null;
            }
            if (type == wanted && ((type == 1 && length == 4) || (type == 28 && length == 16))) {
                byte[] raw = new byte[length];
                System.arraycopy(data, pos, raw, 0, length);
                return new Object[]{InetAddress.getByAddress(raw), ttl};
            }
            pos += length;
        }
        return null;
    }

    private static int skipName(byte[] data, int pos) {
        while (pos < data.length) {
            int length = data[pos] & 0xff;
            if (length == 0) {
                return pos + 1;
            }
            if ((length & 0xc0) == 0xc0) {
                return pos + 2;
            }
            pos += length + 1;
        }
        return pos;
    }
}
