package com.philiplarosa.iconpack;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The app's only screen. Lawnchair never opens it: it reads res/xml/appfilter.xml
 * and res/xml/drawable.xml directly. This screen just shows which icons will
 * auto-apply on this phone and lets you copy component names for new icons.
 */
public class MainActivity extends Activity {

    private static final int GREEN = 0xFF16A34A;

    /** One icon listed in res/xml/drawable.xml, plus the apps mapped to it in appfilter.xml. */
    private static final class PackIcon {
        final String drawable;
        final String name;
        final List<ComponentName> components = new ArrayList<>();

        PackIcon(String drawable, String name) {
            this.drawable = drawable;
            this.name = name;
        }
    }

    /** A launchable app installed on this phone. */
    private static final class InstalledApp {
        ComponentName component;
        String label;
        Drawable icon;
        String packIcon; // drawable name from appfilter.xml, or null
    }

    private LinearLayout content;
    private boolean night;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;

        ScrollView scroll = new ScrollView(this);
        scroll.setFitsSystemWindows(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        content.setPadding(pad, pad, pad, pad);
        scroll.addView(content);
        setContentView(scroll);

        addText(content, getString(R.string.app_name),
                android.R.style.TextAppearance_Material_Display1, dp(4));
        addText(content, "Custom icon pack for Lawnchair.",
                android.R.style.TextAppearance_Material_Subhead, dp(20));

        LinearLayout how = addCard();
        addText(how, "How to use", android.R.style.TextAppearance_Material_Title, dp(8));
        addText(how, "Whole pack: Lawnchair Settings › General › Icon pack › Android Icons. "
                        + "Apps marked ✓ below switch over automatically.",
                android.R.style.TextAppearance_Material_Body1, dp(8));
        addText(how, "One app at a time: long-press the app on your home screen, tap the edit "
                        + "(pencil) option, tap its icon, choose Android Icons and pick any icon.",
                android.R.style.TextAppearance_Material_Body1, dp(8));
        addText(how, "Updated this app? Lawnchair keeps showing the old icon list until it "
                        + "restarts. Tap below, then Force stop, then go back to your home screen.",
                android.R.style.TextAppearance_Material_Body1, dp(8));
        Button restart = new Button(this);
        restart.setText("Open launcher app info");
        restart.setOnClickListener(v -> openLauncherInfo());
        how.addView(restart);

        final TextView loading = addText(content, "Loading…",
                android.R.style.TextAppearance_Material_Body1, 0);

        new Thread(() -> {
            Map<String, PackIcon> icons = loadPackIcons();
            List<InstalledApp> apps = loadInstalledApps(icons);
            runOnUiThread(() -> {
                content.removeView(loading);
                showPackIcons(icons, apps);
                showInstalledApps(apps);
            });
        }).start();
    }

    // ---------------------------------------------------------------- data

    private Map<String, PackIcon> loadPackIcons() {
        Map<String, PackIcon> icons = new LinkedHashMap<>();
        try (XmlResourceParser p = getResources().getXml(R.xml.drawable)) {
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                if (p.getEventType() == XmlPullParser.START_TAG && "item".equals(p.getName())) {
                    String drawable = p.getAttributeValue(null, "drawable");
                    String name = p.getAttributeValue(null, "name");
                    if (drawable != null) {
                        icons.put(drawable, new PackIcon(drawable, name != null ? name : drawable));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try (XmlResourceParser p = getResources().getXml(R.xml.appfilter)) {
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                if (p.getEventType() == XmlPullParser.START_TAG && "item".equals(p.getName())) {
                    String drawable = p.getAttributeValue(null, "drawable");
                    ComponentName cn = parseComponent(p.getAttributeValue(null, "component"));
                    if (drawable == null || cn == null) continue;
                    PackIcon icon = icons.get(drawable);
                    if (icon == null) {
                        icon = new PackIcon(drawable, drawable);
                        icons.put(drawable, icon);
                    }
                    icon.components.add(cn);
                }
            }
        } catch (Exception ignored) {
        }
        return icons;
    }

    private List<InstalledApp> loadInstalledApps(Map<String, PackIcon> icons) {
        Map<ComponentName, String> mapped = new HashMap<>();
        for (PackIcon icon : icons.values()) {
            for (ComponentName cn : icon.components) mapped.put(cn, icon.drawable);
        }

        PackageManager pm = getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<InstalledApp> apps = new ArrayList<>();
        for (ResolveInfo ri : pm.queryIntentActivities(launcher, 0)) {
            ActivityInfo ai = ri.activityInfo;
            if (getPackageName().equals(ai.packageName)) continue;
            InstalledApp app = new InstalledApp();
            app.component = new ComponentName(ai.packageName, ai.name);
            app.label = ri.loadLabel(pm).toString();
            app.icon = ri.loadIcon(pm);
            app.packIcon = mapped.get(app.component);
            apps.add(app);
        }
        Collator collator = Collator.getInstance();
        Collections.sort(apps, (a, b) -> collator.compare(a.label, b.label));
        return apps;
    }

    private static ComponentName parseComponent(String value) {
        if (value == null) return null;
        String s = value.trim();
        if (s.startsWith("ComponentInfo{") && s.endsWith("}")) {
            s = s.substring("ComponentInfo{".length(), s.length() - 1);
        }
        return ComponentName.unflattenFromString(s);
    }

    // ---------------------------------------------------------------- sections

    private void showPackIcons(Map<String, PackIcon> icons, List<InstalledApp> apps) {
        addSectionTitle("Icons in this pack (" + icons.size() + ")");
        LinearLayout card = addCard();
        for (PackIcon icon : icons.values()) {
            String matchedLabel = null;
            for (InstalledApp app : apps) {
                if (icon.drawable.equals(app.packIcon)) {
                    matchedLabel = app.label;
                    break;
                }
            }
            String status = matchedLabel != null
                    ? "✓ Auto-applies to " + matchedLabel
                    : "No matching app found on this phone. You can still pick it manually.";
            addRow(card, packDrawable(icon.drawable), icon.name, status,
                    matchedLabel != null ? GREEN : 0, null, null);
        }
    }

    private void showInstalledApps(List<InstalledApp> apps) {
        addSectionTitle("Installed apps (" + apps.size() + ")");
        addText(content, "Tap an app to copy its appfilter.xml line, for adding a new icon.",
                android.R.style.TextAppearance_Material_Body1, dp(12));
        LinearLayout card = addCard();
        for (InstalledApp app : apps) {
            String flat = app.component.flattenToShortString();
            String status = app.packIcon != null ? "✓ Has an icon in this pack" : null;
            String line = "<item component=\"ComponentInfo{" + app.component.flattenToString()
                    + "}\" drawable=\"" + suggestName(app.label) + "\" />";
            addRow(card, app.icon, app.label, status, GREEN, flat, () -> copy(line));
        }
    }

    // ---------------------------------------------------------------- helpers

    private Drawable packDrawable(String name) {
        int id = getResources().getIdentifier(name, "drawable", getPackageName());
        return id != 0 ? getDrawable(id) : null;
    }

    /**
     * Lawnchair caches each icon pack for as long as its process lives, so after
     * this app updates it needs a force stop to pick up new icons.
     */
    private void openLauncherInfo() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        String pkg = ri != null && ri.activityInfo != null ? ri.activityInfo.packageName : null;
        if (pkg == null || "android".equals(pkg)) {
            Toast.makeText(this, "Couldn't find your default launcher", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", pkg, null)));
    }

    private void copy(String text) {
        ClipboardManager cm = getSystemService(ClipboardManager.class);
        cm.setPrimaryClip(ClipData.newPlainText("appfilter line", text));
        if (Build.VERSION.SDK_INT < 33) {
            Toast.makeText(this, "Copied: " + text, Toast.LENGTH_SHORT).show();
        }
    }

    /** "Google Wallet" -> "google_wallet": a valid Android resource name. */
    static String suggestName(String label) {
        String s = label.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (s.isEmpty() || Character.isDigit(s.charAt(0))) s = "app_" + s;
        return s;
    }

    private void addSectionTitle(String title) {
        TextView tv = addText(content, title, android.R.style.TextAppearance_Material_Title, dp(12));
        ((LinearLayout.LayoutParams) tv.getLayoutParams()).topMargin = dp(12);
    }

    private LinearLayout addCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(night ? 0xFF1C1C1E : 0xFFF2F2F5);
        bg.setCornerRadius(dp(20));
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(16);
        content.addView(card, lp);
        return card;
    }

    private TextView addText(LinearLayout parent, String text, int appearance, int bottomMargin) {
        TextView tv = new TextView(this);
        tv.setTextAppearance(appearance);
        tv.setText(text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = bottomMargin;
        parent.addView(tv, lp);
        return tv;
    }

    private void addRow(LinearLayout parent, Drawable icon, String title, String status,
                        int statusColor, String detail, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        ImageView iv = new ImageView(this);
        iv.setImageDrawable(icon);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(48), dp(48));
        ilp.rightMargin = dp(16);
        row.addView(iv, ilp);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        addText(text, title, android.R.style.TextAppearance_Material_Subhead, 0);
        if (status != null) {
            TextView st = addText(text, status, android.R.style.TextAppearance_Material_Body1, 0);
            if (statusColor != 0) st.setTextColor(statusColor);
        }
        if (detail != null) {
            TextView dt = addText(text, detail, android.R.style.TextAppearance_Material_Caption, 0);
            dt.setTypeface(Typeface.MONOSPACE);
        }

        if (onClick != null) {
            TypedValue tv = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
            row.setBackgroundResource(tv.resourceId);
            row.setOnClickListener(v -> onClick.run());
        }
        parent.addView(row);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
