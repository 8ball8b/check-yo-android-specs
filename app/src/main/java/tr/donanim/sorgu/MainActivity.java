package tr.donanim.sorgu;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(23, 19, 31);
    private static final int PANEL = Color.rgb(34, 29, 43);
    private static final int PURPLE = Color.rgb(132, 91, 220);
    private static final int GREEN = Color.rgb(105, 226, 145);
    private static final int MUTED = Color.rgb(177, 169, 190);

    private LinearLayout results;
    private ScrollView scrollView;
    private int selectionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        showWelcome();
    }

    private void buildScreen() {
        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BACKGROUND);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            scrollView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
                    view.setPadding(systemBars.left, systemBars.top,
                            systemBars.right, systemBars.bottom);
                    return insets;
                }
            });
        }

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(28), dp(22), dp(28));
        scrollView.addView(page);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        page.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.subtitle);
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(14);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(6);
        subtitleParams.bottomMargin = dp(22);
        page.addView(subtitle, subtitleParams);

        addMenuButton(page, "İşlemci (CPU)", new Runnable() {
            @Override public void run() { showCpu(); }
        });
        addMenuButton(page, "Bellek (RAM)", new Runnable() {
            @Override public void run() { showMemory(); }
        });
        addMenuButton(page, "Grafik işlemcisi (GPU)", new Runnable() {
            @Override public void run() { showGpu(); }
        });
        addMenuButton(page, "Cihaz / anakart", new Runnable() {
            @Override public void run() { showDevice(); }
        });
        addMenuButton(page, "Tüm bilgileri göster", new Runnable() {
            @Override public void run() { showAll(); }
        });

        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setPadding(dp(18), dp(16), dp(18), dp(18));
        results.setBackground(rounded(PANEL, dp(18)));
        LinearLayout.LayoutParams resultParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        resultParams.topMargin = dp(10);
        page.addView(results, resultParams);

        setContentView(scrollView);
    }

    private void addMenuButton(LinearLayout parent, String text, final Runnable action) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(18), dp(15), dp(18), dp(15));
        button.setBackground(rounded(PURPLE, dp(14)));
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { action.run(); }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        parent.addView(button, params);
    }

    private void showWelcome() {
        selectionId++;
        results.removeAllViews();
        addSectionTitle("Bilgi seçin");
        addLine("Menüden bir donanım bölümüne dokunun.");
        addLine("Bilgiler bu Android cihazdan okunur; Windows bilgisayar taranmaz.");
    }

    private void showCpu() {
        selectionId++;
        results.removeAllViews();
        addSectionTitle("İşlemci (CPU)");
        addInfo("Model", readCpuModel());
        addInfo("Üretici", cpuManufacturer());
        addInfo("Çekirdek sayısı", String.valueOf(Runtime.getRuntime().availableProcessors()));
        addInfo("Mimariler", join(Build.SUPPORTED_ABIS));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            addInfo("SoC modeli", value(Build.SOC_MODEL, null));
        }
    }

    private void showMemory() {
        selectionId++;
        results.removeAllViews();
        addSectionTitle("Bellek (RAM)");
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            addInfo("Durum", "RAM bilgisi alınamadı.");
            return;
        }

        ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
        manager.getMemoryInfo(memory);
        addInfo("Toplam RAM", formatBytes(memory.totalMem));
        addInfo("Kullanılabilir RAM", formatBytes(memory.availMem));
        addInfo("Düşük bellek durumu", memory.lowMemory ? "Evet" : "Hayır");
    }

    private void showGpu() {
        final int requestId = ++selectionId;
        results.removeAllViews();
        addSectionTitle("Grafik işlemcisi (GPU)");
        addLine("GPU bilgisi okunuyor…");

        new Thread(new Runnable() {
            @Override public void run() {
                final String[] gpu = readGpu();
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (isFinishing() || selectionId != requestId) {
                            return;
                        }
                        results.removeAllViews();
                        addSectionTitle("Grafik işlemcisi (GPU)");
                        addInfo("Üretici", gpu[0]);
                        addInfo("Model", gpu[1]);
                        addInfo("OpenGL sürümü", gpu[2]);
                    }
                });
            }
        }, "gpu-info").start();
    }

    private void showAll() {
        final int requestId = ++selectionId;
        results.removeAllViews();
        showCpuSection();
        showMemorySection();
        addSectionTitle("Grafik işlemcisi (GPU)");
        addLine("GPU bilgisi okunuyor…");
        addSectionTitle("Cihaz / anakart");
        addDeviceInfo();

        new Thread(new Runnable() {
            @Override public void run() {
                final String[] gpu = readGpu();
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (!isFinishing() && selectionId == requestId) {
                            showAllResults(gpu);
                        }
                    }
                });
            }
        }, "gpu-info").start();
    }

    private void showAllResults(String[] gpu) {
        results.removeAllViews();
        showCpuSection();
        showMemorySection();
        addSectionTitle("Grafik işlemcisi (GPU)");
        addInfo("Üretici", gpu[0]);
        addInfo("Model", gpu[1]);
        addInfo("OpenGL sürümü", gpu[2]);
        addSectionTitle("Cihaz / anakart");
        addDeviceInfo();
    }

    private void showDevice() {
        selectionId++;
        results.removeAllViews();
        addSectionTitle("Cihaz / anakart");
        addDeviceInfo();
    }

    private void showCpuSection() {
        addSectionTitle("İşlemci (CPU)");
        addInfo("Model", readCpuModel());
        addInfo("Üretici", cpuManufacturer());
        addInfo("Çekirdek sayısı", String.valueOf(Runtime.getRuntime().availableProcessors()));
        addInfo("Mimariler", join(Build.SUPPORTED_ABIS));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            addInfo("SoC modeli", value(Build.SOC_MODEL, null));
        }
    }

    private void showMemorySection() {
        addSectionTitle("Bellek (RAM)");
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            addInfo("Durum", "RAM bilgisi alınamadı.");
            return;
        }

        ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
        manager.getMemoryInfo(memory);
        addInfo("Toplam RAM", formatBytes(memory.totalMem));
        addInfo("Kullanılabilir RAM", formatBytes(memory.availMem));
        addInfo("Düşük bellek durumu", memory.lowMemory ? "Evet" : "Hayır");
    }

    private void addDeviceInfo() {
        addInfo("Üretici", Build.MANUFACTURER);
        addInfo("Marka", Build.BRAND);
        addInfo("Model", Build.MODEL);
        addInfo("Cihaz kodu", Build.DEVICE);
        addInfo("Anakart kodu", Build.BOARD);
        addInfo("Donanım", Build.HARDWARE);
        addInfo("Ürün", Build.PRODUCT);
    }

    private String cpuManufacturer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return value(Build.SOC_MANUFACTURER, Build.MANUFACTURER);
        }
        return value(Build.MANUFACTURER, null);
    }

    private void addSectionTitle(String title) {
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextColor(GREEN);
        heading.setTextSize(18);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setPadding(0, dp(8), 0, dp(8));
        results.addView(heading);
    }

    private void addInfo(String label, String value) {
        TextView row = new TextView(this);
        row.setText(getString(R.string.info_row, label, value(value, "Kullanılamıyor")));
        row.setTextColor(GREEN);
        row.setTextSize(14);
        row.setPadding(0, dp(5), 0, dp(5));
        results.addView(row);
    }

    private void addLine(String text) {
        TextView line = new TextView(this);
        line.setText(text);
        line.setTextColor(GREEN);
        line.setTextSize(14);
        line.setPadding(0, dp(5), 0, dp(5));
        results.addView(line);
    }

    private String readCpuModel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !TextUtils.isEmpty(Build.SOC_MODEL)) {
            return Build.SOC_MODEL;
        }

        String hardware = null;
        String processor = null;
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int separator = line.indexOf(':');
                if (separator < 0) {
                    continue;
                }
                String key = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
                String content = line.substring(separator + 1).trim();
                if (key.equals("model name") && !content.isEmpty()) {
                    return content;
                }
                if ((key.equals("hardware") || key.equals("processor")) && !content.isEmpty()) {
                    if (key.equals("hardware")) {
                        hardware = content;
                    } else if (processor == null) {
                        processor = content;
                    }
                }
            }
        } catch (IOException exception) {
            return "Okunamadı: " + exception.getMessage();
        }
        return value(hardware, value(processor, Build.HARDWARE));
    }

    private String[] readGpu() {
        String vendor = null;
        String renderer = null;
        String version = null;
        EGLDisplay display = EGL14.EGL_NO_DISPLAY;
        EGLContext context = EGL14.EGL_NO_CONTEXT;
        EGLSurface surface = EGL14.EGL_NO_SURFACE;

        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (display == EGL14.EGL_NO_DISPLAY) {
                throw new IllegalStateException("EGL ekranı açılamadı.");
            }
            int[] versions = new int[2];
            if (!EGL14.eglInitialize(display, versions, 0, versions, 1)) {
                throw new IllegalStateException("EGL başlatılamadı.");
            }

            int[] configAttributes = {
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] configCount = new int[1];
            if (!EGL14.eglChooseConfig(display, configAttributes, 0, configs, 0, 1, configCount, 0)
                    || configCount[0] == 0) {
                throw new IllegalStateException("Uygun EGL yapılandırması bulunamadı.");
            }

            int[] contextAttributes = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            context = EGL14.eglCreateContext(
                    display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttributes, 0);
            if (context == EGL14.EGL_NO_CONTEXT) {
                throw new IllegalStateException("OpenGL bağlamı oluşturulamadı.");
            }

            int[] surfaceAttributes = {
                    EGL14.EGL_WIDTH, 1,
                    EGL14.EGL_HEIGHT, 1,
                    EGL14.EGL_NONE
            };
            surface = EGL14.eglCreatePbufferSurface(display, configs[0], surfaceAttributes, 0);
            if (surface == EGL14.EGL_NO_SURFACE
                    || !EGL14.eglMakeCurrent(display, surface, surface, context)) {
                throw new IllegalStateException("OpenGL yüzeyi açılamadı.");
            }

            vendor = GLES20.glGetString(GLES20.GL_VENDOR);
            renderer = GLES20.glGetString(GLES20.GL_RENDERER);
            version = GLES20.glGetString(GLES20.GL_VERSION);
        } catch (RuntimeException exception) {
            String error = "Okunamadı: " + exception.getMessage();
            return new String[]{error, error, error};
        } finally {
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                if (surface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(display, surface);
                }
                if (context != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(display, context);
                }
                EGL14.eglTerminate(display);
            }
        }

        return new String[]{
                value(vendor, "Kullanılamıyor"),
                value(renderer, "Kullanılamıyor"),
                value(version, "Kullanılamıyor")
        };
    }

    private String formatBytes(long bytes) {
        return String.format(Locale.getDefault(), "%.2f GiB",
                bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private String join(String[] values) {
        List<String> nonEmpty = new ArrayList<>();
        for (String item : values) {
            if (!TextUtils.isEmpty(item)) {
                nonEmpty.add(item);
            }
        }
        return nonEmpty.isEmpty() ? "Kullanılamıyor" : TextUtils.join(", ", nonEmpty);
    }

    private String value(String primary, String fallback) {
        if (!TextUtils.isEmpty(primary)) {
            return primary;
        }
        return TextUtils.isEmpty(fallback) ? "Kullanılamıyor" : fallback;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
