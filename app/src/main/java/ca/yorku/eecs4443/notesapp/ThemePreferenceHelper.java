package ca.yorku.eecs4443.notesapp;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemePreferenceHelper {
    private static final String PREFS = "app_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final String MODE_LIGHT = "LIGHT";
    public static final String MODE_DARK = "DARK";
    public static final String MODE_SYSTEM = "SYSTEM";

    public static void saveThemeMode(Context context, String mode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME_MODE, mode)
                .apply();
    }

    public static String getThemeMode(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_THEME_MODE, MODE_SYSTEM);
    }

    public static void applyTheme(Context context) {
        String mode = getThemeMode(context);

        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;

            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;

            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}