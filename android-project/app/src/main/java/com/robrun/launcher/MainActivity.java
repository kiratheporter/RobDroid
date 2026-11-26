package com.robrun.launcher;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "RobDroid_Auth";
    private static final String KEY_STEAM_USER = "steam_user";
    private static final String KEY_STEAM_PASS = "steam_pass";
    private static final String DEFAULT_APP_ID = "4262460";

    // Arka uç (CDN/API) tarafından sağlanacak gerçek paket URL'si
    // Şimdilik bir yer tutucu olarak bırakıldı.
    private static final String BASE_PACKAGE_CDN = "https://c.port.steam.host/depot/4262460/pack";

    private TextInputEditText etAppId;
    private TextInputEditText etPackageUrl;
    private CheckBox cbOnlineMode;
    private CheckBox cbOptimize;
    private Button btnDownloadInstall;
    private Button btnLaunchGame;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private Button btnOpenSteamLogin;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String currentSteamUser = "";
    private String currentSteamPass = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etAppId = findViewById(R.id.etAppId);
        etPackageUrl = findViewById(R.id.etPackageUrl);
        cbOnlineMode = findViewById(R.id.cbOnlineMode);
        cbOptimize = findViewById(R.id.cbOptimize);
        btnDownloadInstall = findViewById(R.id.btnDownloadInstall);
        btnLaunchGame = findViewById(R.id.btnLaunchGame);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        btnOpenSteamLogin = findViewById(R.id.btnOpenSteamLogin);

        loadAuth();
        updateManifestUrl();

        if (etAppId != null && TextUtils.isEmpty(etAppId.getText().toString().trim())) {
            etAppId.setText(DEFAULT_APP_ID);
        }

        btnOpenSteamLogin.setOnClickListener(v -> showSteamLoginDialog());
        btnDownloadInstall.setOnClickListener(v -> startDownloadAndInstall());
        btnLaunchGame.setOnClickListener(v -> launchGame());

        updateLaunchButtonState();
    }

    private void loadAuth() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentSteamUser = prefs.getString(KEY_STEAM_USER, "");
        currentSteamPass = prefs.getString(KEY_STEAM_PASS, "");
        updateSteamLoginButtonState();
    }

    private void saveAuth(String user, String pass) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        if (user == null) {
            editor.remove(KEY_STEAM_USER);
            editor.remove(KEY_STEAM_PASS);
        } else {
            editor.putString(KEY_STEAM_USER, user);
            editor.putString(KEY_STEAM_PASS, pass);
        }
        editor.apply();
        currentSteamUser = user == null ? "" : user;
        currentSteamPass = pass == null ? "" : pass;
        updateSteamLoginButtonState();
    }

    private void updateSteamLoginButtonState() {
        if (TextUtils.isEmpty(currentSteamUser)) {
            btnOpenSteamLogin.setText("STEAM HESABI: GİRİŞ YAPILMADI");
            btnOpenSteamLogin.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
        } else {
            btnOpenSteamLogin.setText("HESAP: " + currentSteamUser);
            btnOpenSteamLogin.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent)); // Kırmızı
        }
    }

    private void updateManifestUrl() {
        String appId = etAppId != null ? etAppId.getText().toString().trim() : DEFAULT_APP_ID;
        if (TextUtils.isEmpty(appId)) appId = DEFAULT_APP_ID;

        // Arka uç sunucusu, AppID'ye ve muhtemelen kullanıcı oturumuna (güvenli) göre en güncel manifest'i/URL'yi döndürür.
        String finalUrl = BASE_PACKAGE_CDN + "?appid=" + appId;
        if (etPackageUrl != null) {
            etPackageUrl.setText(finalUrl);
        }
    }

    private void showSteamLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_steam_login, null);

        TextInputEditText etUser = view.findViewById(R.id.etSteamUser);
        TextInputEditText etPass = view.findViewById(R.id.etSteamPass);
        Button btnLogin = view.findViewById(R.id.btnDialogLogin);
        Button btnLogout = view.findViewById(R.id.btnDialogLogout);
        TextView tvUserStatus = view.findViewById(R.id.tvDialogSteamUser);

        if (!TextUtils.isEmpty(currentSteamUser)) {
            etUser.setText(currentSteamUser);
            etUser.setEnabled(false); // Düzenlemeyi engellemek için
            etPass.setVisibility(View.GONE);
            view.findViewById(R.id.tilSteamPass).setVisibility(View.GONE);
            btnLogin.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
            tvUserStatus.setVisibility(View.VISIBLE);
            tvUserStatus.setText("Giriş yapıldı: " + currentSteamUser);
        }

        builder.setView(view);
        AlertDialog dialog = builder.create();

        btnLogin.setOnClickListener(v -> {
            String user = etUser.getText().toString().trim();
            String pass = etPass.getText().toString();
            if (TextUtils.isEmpty(user) || TextUtils.isEmpty(pass)) {
                Toast.makeText(this, "Kullanıcı adı ve şifre gereklidir.", Toast.LENGTH_SHORT).show();
                return;
            }
            saveAuth(user, pass);
            Toast.makeText(this, "Giriş kaydedildi.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            updateManifestUrl();
        });

        btnLogout.setOnClickListener(v -> {
            saveAuth(null, null);
            dialog.dismiss();
            updateManifestUrl();
            Toast.makeText(this, "Çıkış yapıldı.", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    // ... (Önceki startDownloadAndInstall, unzipSafely, vb. fonksiyonlar burada kalacak,
    //      startDownloadAndInstall içinde `rawUrl` kontrolü basitleştirilecek.)

    private File getGameDir() {
        File base = getExternalFilesDir(null);
        if (base == null) base = getFilesDir();
        return new File(base, "RobRun");
    }

    private boolean isGameInstalled() {
        File verifiedFlag = new File(getGameDir(), "install_verified.flag");
        return verifiedFlag.exists() && verifiedFlag.isFile();
    }

    private void updateLaunchButtonState() {
        btnLaunchGame.setEnabled(isGameInstalled());
        if (isGameInstalled()) {
            btnLaunchGame.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
            btnLaunchGame.setTextColor(getResources().getColor(android.R.color.white));
        }
    }

    private void setBusyState(boolean busy) {
        btnDownloadInstall.setEnabled(!busy);
        btnLaunchGame.setEnabled(!busy && isGameInstalled());
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        progressBar.setIndeterminate(busy);
    }

    private void startDownloadAndInstall() {
        if (TextUtils.isEmpty(currentSteamUser) || TextUtils.isEmpty(currentSteamPass)) {
            Toast.makeText(this, "Lütfen önce Steam hesabına giriş yapın.", Toast.LENGTH_SHORT).show();
            showSteamLoginDialog();
            return;
        }

        updateManifestUrl(); // Kurulum anında URL'yi tazele
        String rawUrl = etPackageUrl.getText().toString().trim();
        String appId = etAppId != null ? etAppId.getText().toString().trim() : DEFAULT_APP_ID;
        final boolean optimize = cbOptimize.isChecked();

        setBusyState(true);
        tvStatus.setText("Steam oturumu doğrulanıyor ve paket CDN'den alınıyor... (AppID: " + appId + ")");

        executor.execute(() -> {
            File cacheDir = getCacheDir();
            File zipFile = new File(cacheDir, "robrun_package_" + appId + ".zip");
            File gameDir = getGameDir();

            HttpURLConnection conn = null;
            try {
                URL url = new URL(rawUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                // Arka uç (CDN) kimlik doğrulaması için temel yetkilendirme (örneğin JWT/Session)
                // Bu kısım arka uç mimarisine göre değişir.
                // conn.setRequestProperty("Authorization", "Bearer " + generateFakeToken(currentSteamUser, currentSteamPass));
                conn.connect();

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("CDN Bağlantı Hatası: " + conn.getResponseCode());
                }

                long totalBytes = conn.getContentLengthLong();
                long downloaded = 0L;

                try (InputStream in = new BufferedInputStream(conn.getInputStream());
                     FileOutputStream out = new FileOutputStream(zipFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        downloaded += read;
                        if (totalBytes > 0) {
                            int pct = (int) Math.min(100L, (downloaded * 100L) / totalBytes);
                            runOnUiThread(() -> {
                                progressBar.setProgress(pct);
                                tvStatus.setText("İndiriliyor: %" + pct);
                            });
                        }
                    }
                    out.flush();
                }

                if (zipFile.length() == 0) {
                    throw new Exception("İndirilen dosya boş.");
                }

                runOnUiThread(() -> tvStatus.setText("Paket doğrulanıyor ve açılıyor..."));
                // ... (unzipSafely, deleteRecursive, writeGraphicsConfig, writeVerifiedFlag fonksiyonlarını çağırın)
                // (Önceki kodlarda bunlar mevcuttu.)

                runOnUiThread(() -> {
                    setBusyState(false);
                    tvStatus.setText("Kurulum tamamlandı. Oyunu başlatabilirsiniz.");
                    updateLaunchButtonState();
                });

            } catch (Exception ex) {
                if (zipFile.exists()) zipFile.delete();
                runOnUiThread(() -> {
                    setBusyState(false);
                    tvStatus.setText("Kurulum Hatası: " + ex.getMessage());
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void launchGame() {
        if (!isGameInstalled()) {
            tvStatus.setText("Hata: Oyun henüz kurulmadı.");
            return;
        }
        String appId = etAppId != null ? etAppId.getText().toString().trim() : DEFAULT_APP_ID;
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("steam_user", currentSteamUser); // Kayıtlı kullanıcıyı gönder
        intent.putExtra("app_id", appId);
        intent.putExtra("online_mode", cbOnlineMode.isChecked());
        intent.putExtra("optimize_mode", cbOptimize.isChecked());
        intent.putExtra("game_path", getGameDir().getAbsolutePath());
        startActivity(intent);
    }
}
