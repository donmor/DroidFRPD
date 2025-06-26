package top.donmor.droidfrpd;

import static top.donmor.droidfrpd.Utils.KEY_EULA;
import static top.donmor.droidfrpd.Utils.KEY_IS_SVR;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import top.donmor.droidfrpd.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

	private AppBarConfiguration mAppBarConfiguration;
	private ActivityMainBinding binding;
	private NavController navController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		setSupportActionBar(binding.appBarMain.toolbar);
		DrawerLayout drawer = binding.drawerLayout;
		// Passing each menu ID as a set of Ids because each
		// menu should be considered as top level destinations.
		mAppBarConfiguration = new AppBarConfiguration.Builder(
				R.id.nav_client, R.id.nav_server)
				.setOpenableLayout(drawer)
				.build();
	}

	@Override
	protected void onStart() {
		super.onStart();
		NavigationView navigationView = binding.navView;
		navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
		NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
		NavigationUI.setupWithNavController(navigationView, navController);
		NavGraph graph = navController.getGraph();
		navController.addOnDestinationChangedListener((navController1, navDestination, bundle) -> {
			if (navDestination.getId() == R.id.nav_settings) return;
			if (navController1.getPreviousBackStackEntry() != null) {
				navController1.navigate(navDestination.getId(), null, new NavOptions.Builder().setPopUpTo(graph.getStartDestinationId(), true).build());
				graph.setStartDestination(navDestination.getId());
			}
		});
		Intent intent = getIntent();
		if (Intent.ACTION_MAIN.equalsIgnoreCase(intent.getAction()) && intent.hasExtra(KEY_IS_SVR)) {
			int id = intent.getBooleanExtra(KEY_IS_SVR, false) ? R.id.nav_server : R.id.nav_client;
			// Known bug:	Tabs in navigation menu may need clicking twice, because it removes
			// 				stacked fragments in a hacky way.
			navController.navigate(id, null, new NavOptions.Builder().setPopUpTo(id, true).build());
			graph.setStartDestination(id);
		}
		SharedPreferences preferences = Utils.getPreferences(this);
		if (!preferences.getBoolean(KEY_EULA, false)) Utils.showAppInfo(this, accepted -> {
			if (accepted) preferences.edit().putBoolean(KEY_EULA, true).apply(); else finishAffinity();
		});
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (navController != null && Intent.ACTION_MAIN.equalsIgnoreCase(intent.getAction()) && intent.hasExtra(KEY_IS_SVR)) {
			int id = intent.getBooleanExtra(KEY_IS_SVR, false) ? R.id.nav_server : R.id.nav_client;
			navController.navigate(id, null, new NavOptions.Builder().setPopUpTo(id, true).build());
			navController.getGraph().setStartDestination(id);
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
		return NavigationUI.navigateUp(navController, mAppBarConfiguration)
				|| super.onSupportNavigateUp();
	}
}