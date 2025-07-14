package top.donmor.droidfrpd;

import static top.donmor.droidfrpd.Utils.KEY_EULA;
import static top.donmor.droidfrpd.Utils.KEY_NOTIFICATION;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;

import top.donmor.droidfrpd.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

	private ActivityMainBinding binding;
	private NavController navController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		setSupportActionBar(binding.appBarMain.toolbar);
	}

	@Override
	protected void onStart() {
		super.onStart();
		BottomNavigationView navigationView = binding.navView;
		navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
		NavigationUI.setupWithNavController(navigationView, navController);
		navController.addOnDestinationChangedListener((navController1, navDestination, bundle) -> {
			if (navDestination.getId() == R.id.nav_settings) return;
			navController1.popBackStack(R.id.nav_daemons, false);
		});
		SharedPreferences preferences = Utils.getPreferences(this);
		if (!preferences.getBoolean(KEY_EULA, false)) Utils.showAppInfo(this, accepted -> {
			if (accepted) preferences.edit().putBoolean(KEY_EULA, true).apply();
			else finishAffinity();
		});
		if (preferences.getBoolean(getString(R.string.pk_global_hide_activity), false))
			((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getAppTasks()
					.forEach(appTask -> appTask.setExcludeFromRecents(true));
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (navController != null && Intent.ACTION_MAIN.equalsIgnoreCase(intent.getAction()) && intent.getBooleanExtra(KEY_NOTIFICATION, false))
			navController.navigateUp();
	}
}