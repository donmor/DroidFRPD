package top.donmor.droidfrpd.ui.client;

import static top.donmor.droidfrpd.Utils.badConfig;
import static top.donmor.droidfrpd.Utils.getConf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.io.File;

import top.donmor.droidfrpd.FRPCDaemon;
import top.donmor.droidfrpd.R;
import top.donmor.droidfrpd.databinding.FragmentClientBinding;

public class ClientFragment extends Fragment {

	private FragmentClientBinding binding;
	private FRPCDaemon.LocalBinder service;
	private ToggleButton toggleClient;
	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder svc) {
			service = (FRPCDaemon.LocalBinder) svc;
			toggleClient.setChecked(true);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			service = null;
			toggleClient.setChecked(false);
		}
	};

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
							 ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentClientBinding.inflate(inflater, container, false);
		View root = binding.getRoot();

		final FragmentActivity activity = getActivity();
		assert activity != null;
		Intent intent = new Intent(activity, FRPCDaemon.class);

		toggleClient = binding.toggleClient;
		toggleClient.setOnClickListener(view -> {
			view.setEnabled(false);

			new Thread(() -> {
				runnable:
				{
					if (toggleClient.isChecked()) {
						File confFile = getConf(activity, false);
						if (badConfig(activity, confFile, false)) {
							activity.runOnUiThread(() -> {
								Toast.makeText(activity, R.string.ui_stopped_err, Toast.LENGTH_SHORT).show();
								toggleClient.setChecked(false);
							});
							break runnable;
						}
						if (activity.startService(intent) == null)
							toggleClient.setChecked(false);
						if (!activity.bindService(intent, connection, Context.BIND_ABOVE_CLIENT))
							toggleClient.setChecked(false);
					} else {
						activity.stopService(intent);
					}
				}
				activity.runOnUiThread(() -> view.setEnabled(true));
			}).start();
		});
		activity.bindService(intent, connection, Context.BIND_ABOVE_CLIENT);
		return root;
	}

	@Override
	public void onResume() {
		if (service != null && toggleClient != null && service.isDead())
			toggleClient.setChecked(false);
		super.onResume();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		final FragmentActivity activity = getActivity();
		assert activity != null;
		activity.unbindService(connection);
		binding = null;
	}
}