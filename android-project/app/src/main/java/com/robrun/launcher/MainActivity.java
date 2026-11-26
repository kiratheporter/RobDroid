package com.robrun.launcher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {
    private EditText etAppId, etSteamUser, etPackageUrl, etExpectedSha256;
    private CheckBox cbOnlineMode, cbOptimize;
    private Button btnDownloadAndInstall, btnLaunchGame;
    private ProgressBar progressBar;
    private TextView tvStatus;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etAppId = findViewById(R.id.etAppId);
        etSteamUser = findViewById(R.id.etSteamUser);
        etPackageUrl = findViewById(R.id.etPackageUrl);
        etExpectedSha256 = findViewById(R.id.etExpectedSha256);
        cbOnlineMode = findViewById(R.id.cbOnlineMode);
        cbOptimize = findViewById(R.id.cbOptimize);
        btnDownloadAndInstall = findViewById(R.id.btnDownloadAndInstall);
        btnLaunchGame = findViewById(R.id.btnLaunchGame);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);

        loadSavedInputs();
        verifyExistingInstallation();

        btnDownloadAndInstall.setOnClickListener(v -> startRealInstallation());
        btnLaunchGame.setOnClickListener(v -> launchGame());
    }

    private void loadSavedInputs() {
        SharedPreferences prefs = getSharedPreferences("robrun_prefs", MODE_PRIVATE);
        etAppId.setText(prefs.getString("app_id", ""));
        etSteamUser.setText(prefs.getString("steam_user", ""));
        etPackageUrl.setText(prefs.getString("package_url", ""));
        etExpectedSha256.setText(prefs.getString("expected_sha256", ""));
    }

    private void saveInputs(String appId, String user, String url, String sha256) {
        SharedPreferences prefs = getSharedPreferences("robrun_prefs", MODE_PRIVATE);
        prefs.edit()
            .putString("app_id", appId)
            .putString("steam_user", user)
            .putString("package_url", url)
            .putString("expected_sha256", sha256)
            .apply();
    }

    private void verifyExistingInstallation() {
        File gameDir = new File(getExternalFilesDir(null), "RobRun");
        File readyMarker = new File(gameDir, "install_verified.flag");
        if (readyMarker.exists()) {
            btnLaunchGame.setEnabled(true);
            tvStatus.setText("Durum: Dogrulanmis Rob Run kurulumu hazir.");
        }
    }

    private void startRealInstallation() {
        String appId = etAppId.getText().toString().trim();
        String user = etSteamUser.getText().toString().trim();
        String urlStr = etPackageUrl.getText().toString().trim();
        String expectedSha = etExpectedSha256.getText().toString().trim().toLowerCase(Locale.ROOT);

        if (appId.isEmpty() || urlStr.isEmpty()) {
            tvStatus.setText("Hata: Steam App ID ve paket URL zorunludur.");
            return;
        }

        if (!urlStr.startsWith("https://") && !urlStr.startsWith("http://")) {
            tvStatus.setText("Hata: Gecerli bir http:// veya https:// adresi girin.");
            return;
        }

        saveInputs(appId, user, urlStr, expectedSha);
        btnDownloadAndInstall.setEnabled(false);
        btnLaunchGame.setEnabled(false);
        progressBar.setProgress(0);

        executor.execute(() -> {
            File gameDir = new File(getExternalFilesDir(null), "RobRun");
            File tempZip = new File(getCacheDir(), "robrun_package_" + appId + ".zip");

            try {
                if (!gameDir.exists() && !gameDir.mkdirs()) {
                    throw new IOException("Hedef oyun klasoru olusturulamadi.");
                }

                updateStatus("Sunucuya baglaniliyor (AppID: " + appId + ")...", 5);
                downloadFile(urlStr, tempZip);

                if (!expectedSha.isEmpty()) {
                    updateStatus("SHA-256 butunluk dogrulamasi yapiliyor...", 75);
                    String actualSha = computeSha256(tempZip);
                    if (!actualSha.equals(expectedSha)) {
                        throw new IOException("SHA-256 uyusmadi! Beklenen: " + expectedSha + " | Gelen: " + actualSha);
                    }
                }

                updateStatus("Paket dosyalari cikariliyor...", 82);
                extractZipSafely(tempZip, gameDir);

                writeConfigFiles(gameDir, appId, user, cbOnlineMode.isChecked(), cbOptimize.isChecked());

                File marker = new File(gameDir, "install_verified.flag");
                try (FileWriter fw = new FileWriter(marker)) {
                    fw.write("VERIFIED_APP_ID=" + appId + "\n");
                }

                if (tempZip.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    tempZip.delete();
                }

                runOnUiThread(() -> {
                    progressBar.setProgress(100);
                    tvStatus.setText("Durum: Gercek paket indirildi, dogrulandi ve kuruldu.");
                    btnDownloadAndInstall.setEnabled(true);
                    btnLaunchGame.setEnabled(true);
                });
            } catch (Exception e) {
                if (tempZip.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    tempZip.delete();
                }
                runOnUiThread(() -> {
                    tvStatus.setText("Kurulum Basarisiz: " + e.getMessage());
                    btnDownloadAndInstall.setEnabled(true);
                });
            }
        });
    }

    private void downloadFile(String urlStr, File outputFile) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Steam/DesktopClient RobDroidInstaller/1.0");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP Hatasi: " + responseCode);
            }

            long totalBytes = conn.getContentLengthLong();
            long downloaded = 0;

            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 FileOutputStream out = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                    downloaded += count;
                    if (totalBytes > 0) {
                        int pct = 5 + (int) ((downloaded * 70L) / totalBytes);
                        updateStatus("Indiriliyor: " + (downloaded / 1024) + " KB / " + (totalBytes / 1024) + " KB", Math.min(75, pct));
                    }
                }
                out.flush();
            }

            if (outputFile.length() == 0) {
                throw new IOException("Indirilen dosya bos.");
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String computeSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[8192];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void extractZipSafely(File zipFile, File targetDir) throws IOException {
        String canonicalTarget = targetDir.getCanonicalPath() + File.separator;

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(targetDir, entry.getName());
                String canonicalOut = outFile.getCanonicalPath();

                if (!canonicalOut.startsWith(canonicalTarget)) {
                    throw new IOException("Guvenlik engeli (Zip Slip): " + entry.getName());
                }

                if (entry.isDirectory()) {
                    if (!outFile.exists() && !outFile.mkdirs()) {
                        throw new IOException("Klasor olusturulamadi: " + outFile.getAbsolutePath());
                    }
                } else {
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new IOException("Ust klasor olusturulamadi: " + parent.getAbsolutePath());
                    }
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.flush();
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void writeConfigFiles(File gameDir, String appId, String user, boolean online, boolean optimize) throws IOException {
        File cfgFile = new File(gameDir, "android_graphics.cfg");
        String content = optimize
            ? "targetFrameRate=60\nresolutionScale=0.75\nshadows=off\nvsync=0\nonline=" + online + "\nappId=" + appId + "\nuser=" + user + "\n"
            : "targetFrameRate=60\nresolutionScale=1.0\nshadows=low\nvsync=1\nonline=" + online + "\nappId=" + appId + "\nuser=" + user + "\n";
        try (FileWriter fw = new FileWriter(cfgFile)) {
            fw.write(content);
        }
    }

    private void updateStatus(String message, int progress) {
        runOnUiThread(() -> {
            tvStatus.setText("Durum: " + message);
            progressBar.setProgress(progress);
        });
    }

    private void launchGame() {
        File gameDir = new File(getExternalFilesDir(null), "RobRun");
        Intent intent = new Intent(MainActivity.this, GameActivity.class);
        intent.putExtra("game_path", gameDir.getAbsolutePath());
        intent.putExtra("app_id", etAppId.getText().toString().trim());
        intent.putExtra("steam_user", etSteamUser.getText().toString().trim());
        intent.putExtra("online_mode", cbOnlineMode.isChecked());
        intent.putExtra("optimize_mode", cbOptimize.isChecked());
        startActivity(intent);
    }
}
