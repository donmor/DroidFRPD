package top.donmor.droidfrpd;

import static top.donmor.droidfrpd.BuildConfig.BINARY_FRP_C;
import static top.donmor.droidfrpd.BuildConfig.BINARY_FRP_S;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;

public abstract class Utils {
	static final String E_FNF = "File not found at %s";
	public static final String CONF_TOML_C = "frpc.toml";
	public static final String CONF_TOML_S = "frps.toml";
	private static final String CONF_JSON_C = "frpc.json";
	private static final String CONF_INI_C = "frpc.ini";
	private static final String CONF_JSON_S = "frps.json";
	private static final String CONF_INI_S = "frps.ini";
	private static final String CERT_F_DROID_DN = "CN=FDroid, OU=FDroid, O=fdroid.org, L=ORG, ST=ORG, C=UK";
	private static final String CERT_F_DROID_TYPE = "X.509";
	static final String DIR_LOG = "log";
	static final String KEY_EULA = "eula";
	static final String KEY_NOTIFICATION = "notification";
	private static final String LIC_ASSET_URL = "file:///android_asset/LICENSE";
	static final String LOG_PTN_C = "frpc.%tY%tm%td%tH%tM%tS%tL.log";
	static final String LOG_PTN_S = "frps.%tY%tm%td%tH%tM%tS%tL.log";
	static final String PARAM_CONF = "-c";
	static final String S0 = "";
	private static final String KEY_HDR_LOC = "Location";
	private static final String PARAM_VERIFY = "verify";
	private static final String PREF_POST = "_preferences";
	public static final String SCH_PACKAGE = "package:";


	public static Boolean isFDroidBuild = null;

	record CloseableProcess(Process process) implements AutoCloseable {
		@Override
		public void close() {
			process.destroy();
		}
	}

	public static boolean badConfig(Context context, @NonNull File confFile, boolean isServer) {
		SharedPreferences preferences = getPreferences(context);
		if (!preferences.getBoolean(KEY_EULA, false)) return true;
		String conf = confFile.getName();
		try (CloseableProcess closeableProcess = new CloseableProcess(new ProcessBuilder(
				context.getApplicationInfo().nativeLibraryDir + '/' + (isServer ? BINARY_FRP_S : BINARY_FRP_C),
				PARAM_VERIFY,
				PARAM_CONF,
				conf)
				.directory(context.getFilesDir())
				.start())) {
			return closeableProcess.process().waitFor() != 0;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}

	@NonNull
	public static File getConf(@NonNull Context context, boolean isServer) {
		File ff = context.getFilesDir();
		if (!ff.isDirectory()) throw new RuntimeException();
		File[] ls = ff.listFiles((dir, name) -> (isServer ? CONF_TOML_S : CONF_TOML_C).equals(name));
		if (ls == null || ls.length == 0)
			ls = ff.listFiles((dir, name) -> (isServer ? CONF_JSON_S : CONF_JSON_C).equals(name));
		if (ls == null || ls.length == 0)
			ls = ff.listFiles((dir, name) -> (isServer ? CONF_INI_S : CONF_INI_C).equals(name));
		if (ls == null || ls.length == 0) {
			ls = new File[]{new File(ff, isServer ? CONF_TOML_S : CONF_TOML_C)};
			try {
				if (!ls[0].exists())
					//noinspection ResultOfMethodCallIgnored
					ls[0].createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return ls[0];
	}

	public static SharedPreferences getPreferences(Context context) {
		return context.getSharedPreferences(context.getPackageName() + PREF_POST, Context.MODE_PRIVATE);
	}

	public interface LicenseAcceptDeclineListener {
		void exec(boolean accepted);
	}

	public static void showAppInfo(Context context, LicenseAcceptDeclineListener licenseAcceptDeclineListener) {
		boolean confirming = licenseAcceptDeclineListener != null;

		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		int dp4 = (int) (4 * context.getResources().getDisplayMetrics().density);
		layout.setPaddingRelative(dp4 * 4, dp4 * 6, dp4 * 4, 0);
		if (!confirming) {
			TextView desc = new TextView(context);
			desc.setText(R.string.ui_about_desc);
			desc.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);
			desc.setGravity(Gravity.CENTER);
			layout.addView(desc);
			TextView ver = new TextView(context);
			ver.setText(context.getString(
					isFDroidPackage(context) ? R.string.ui_ver_f_droid : R.string.ui_ver, BuildConfig.VERSION_NAME));
			ver.setGravity(Gravity.CENTER);
			layout.addView(ver);
		}
		TextView licLbl = new TextView(context);
		licLbl.setText(R.string.ui_eula_lic);
		licLbl.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Widget_TextView);
		layout.addView(licLbl);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp4 * 30);
		WebView licView = new WebView(context);
		licView.setLayoutParams(params);
		licView.setPadding(dp4, 0, dp4, 0);
		licView.setBackgroundColor(ContextCompat.getColor(context, R.color.field_color));
		licView.getSettings().setUseWideViewPort(true);
		licView.loadUrl(LIC_ASSET_URL);
		layout.addView(licView);
		TextView pmLbl = new TextView(context);
		pmLbl.setText(R.string.ui_eula_permissions);
		pmLbl.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Widget_TextView);
		layout.addView(pmLbl);
		TextView pmView = new TextView(context);
		pmView.setPadding(4, 0, 4, 0);
		pmView.setText(R.string.ui_eula_permissions_text);
		pmView.setHorizontallyScrolling(true);
		pmView.setMovementMethod(ScrollingMovementMethod.getInstance());
		pmView.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Widget_TextView);
		pmView.setTypeface(Typeface.MONOSPACE);
		pmView.setTextSize(12);
		pmView.setBackgroundColor(ContextCompat.getColor(context, R.color.field_color));
		layout.addView(pmView);

		AlertDialog dialog = new AlertDialog.Builder(context)
				.setCancelable(!confirming)
				.setTitle(confirming ? R.string.ui_eula : R.string.ui_about)
				.setView(layout)
				.setPositiveButton(confirming ? R.string.ui_accept : R.string.ui_close,
						confirming ? (dialog2, which) -> licenseAcceptDeclineListener.exec(true) : null)
				.create();
		if (confirming)
			dialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.ui_decline),
					(dialog1, which) -> licenseAcceptDeclineListener.exec(false));
		dialog.show();
		if (!confirming)
			dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnLongClickListener(v -> {
				new AlertDialog.Builder(context)
						.setTitle(android.R.string.dialog_alert_title)
						.setMessage(R.string.ui_revoke)
						.setNegativeButton(android.R.string.cancel, null)
						.setPositiveButton(android.R.string.ok, (dialog3, which) -> {
							if (getPreferences(context).edit().putBoolean(KEY_EULA, false).commit()) {
								Activity activity = (Activity) context;
								activity.stopService(new Intent(activity, FRPCDaemon.class));
								activity.stopService(new Intent(activity, FRPSDaemon.class));
								activity.finishAffinity();
							}
						}).show();
				return false;
			});
	}

	@Nullable
	public static String checkUpdate(Context context) {
		try {
			URL url = new URL(context.getString(R.string.update_url));
			HttpsURLConnection httpURLConnection = (HttpsURLConnection) url.openConnection();
			httpURLConnection.setReadTimeout(10000);
			httpURLConnection.setInstanceFollowRedirects(false);
			httpURLConnection.connect();
			int status = httpURLConnection.getResponseCode();
			if (status != HttpsURLConnection.HTTP_MOVED_TEMP
					&& status != HttpsURLConnection.HTTP_MOVED_PERM
					&& status != HttpsURLConnection.HTTP_SEE_OTHER)
				return null;
			String latestVersion = Uri.parse(httpURLConnection.getHeaderField(KEY_HDR_LOC)).getLastPathSegment();
			if (!BuildConfig.VERSION_NAME.equals(latestVersion)) return latestVersion;
		} catch (IOException | NullPointerException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean isFDroidPackage(Context context) {
		if (isFDroidBuild != null) return isFDroidBuild;
		PackageManager pm = context.getPackageManager();
		try {
			PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
			Signature[] signatures = info != null ? info.signatures : null;
			isFDroidBuild = signatures != null && Arrays.stream(signatures).anyMatch(
					signature -> {
						try {
							CertificateFactory certFactory = CertificateFactory.getInstance(CERT_F_DROID_TYPE);
							X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(signature.toByteArray()));
							return CERT_F_DROID_DN.equals(certificate.getIssuerX500Principal().toString());
						} catch (CertificateException e) {
							return false;
						}
					});
			return isFDroidBuild;
		} catch (PackageManager.NameNotFoundException ignored) {
		}
		return false;
	}
}
