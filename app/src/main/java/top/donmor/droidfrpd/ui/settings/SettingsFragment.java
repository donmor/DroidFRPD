package top.donmor.droidfrpd.ui.settings;

import static top.donmor.droidfrpd.Utils.checkUpdate;
import static top.donmor.droidfrpd.Utils.getConf;
import static top.donmor.droidfrpd.Utils.showAppInfo;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.stream.Collectors;

import top.donmor.droidfrpd.R;
import top.donmor.droidfrpd.Utils;

public class SettingsFragment extends PreferenceFragmentCompat {
	@Override
	public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
		setPreferencesFromResource(R.xml.preference, rootKey);
		FragmentActivity activity = getActivity();
		Preference preferenceConfigClient = findPreference(getString(R.string.pk_client_edit_config)),
				preferenceConfigServer = findPreference(getString(R.string.pk_server_edit_config)),
				preferenceUpdate = findPreference(getString(R.string.pk_global_check_update)),
				preferenceAbout = findPreference(getString(R.string.pk_global_about));
		assert preferenceConfigClient != null
				&& preferenceConfigServer != null
				&& preferenceUpdate != null
				&& preferenceAbout != null
				&& activity != null;
		// Create config editor dialog
		for (boolean isServer : new boolean[]{false, true}) {
			Preference p = isServer ? preferenceConfigServer : preferenceConfigClient;
			p.setOnPreferenceClickListener(preference -> {
				EditText editor = new EditText(activity);
				editor.setSingleLine(false);
				editor.setGravity(Gravity.TOP);
				editor.setTextSize(16);
				final File[] f = {getConf(activity, isServer)};
				try (FileInputStream is = new FileInputStream(f[0]);
									   InputStreamReader reader = new InputStreamReader(is);
									   BufferedReader bufferedReader = new BufferedReader(reader)) {
					String s = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
					editor.setText(s, TextView.BufferType.EDITABLE);
				} catch (IOException e) {
					e.printStackTrace();
				}
				new AlertDialog.Builder(activity)
						.setView(editor)
						.setNegativeButton(android.R.string.cancel, null)
						.setPositiveButton(R.string.ui_save, (dialog, which) -> {
							try (FileOutputStream os = new FileOutputStream(f[0]);
								 OutputStreamWriter writer = new OutputStreamWriter(os)) {
								writer.write(editor.getText().toString());
							} catch (IOException e) {
								e.printStackTrace();
							}
						}).show();
				return true;
			});
		}
		preferenceUpdate.setOnPreferenceClickListener(preference -> {
			preferenceUpdate.setEnabled(false);
			new Thread(() -> {
				String newVersion = checkUpdate(activity);
				if (newVersion != null) activity.runOnUiThread(() -> {
					preferenceUpdate.setTitle(activity.getString(R.string.preference_global_update, newVersion));
					preferenceUpdate.setOnPreferenceClickListener(preference1 -> {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(
								Utils.isFDroidPackage(activity) ? R.string.update_url_f_droid : R.string.update_url)));
						try {
							startActivity(intent);
						} catch (RuntimeException e) {
							e.printStackTrace();
						}
						return true;
					});
					preferenceUpdate.setEnabled(true);
				});
				else activity.runOnUiThread(() -> {
					Toast.makeText(activity, R.string.ui_already_latest, Toast.LENGTH_SHORT).show();
					preferenceUpdate.setEnabled(true);
				});
			}).start();
			return true;
		});
		preferenceAbout.setOnPreferenceClickListener(preference -> {
			showAppInfo(activity, null);
			return true;
		});
	}
}
