package com.sentiance.sdkstarter.triggeredtrips;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.sentiance.sdk.SdkStatus;
import com.sentiance.sdk.Sentiance;
import com.sentiance.sdk.trip.StartTripCallback;
import com.sentiance.sdk.trip.StopTripCallback;
import com.sentiance.sdk.trip.TripType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final TripStartStopCallback mTripStartStopCallback = new TripStartStopCallback();
    private final BroadcastReceiver statusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive (Context context, Intent intent) {
            refreshStatus();
            updateButtonTexts();
        }
    };

    private Button controlTripButton;
    private ListView statusList;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // We need to ask the user to grant permission. We've offloaded that to a different activity for clarity.
            startActivity(new Intent(this, PermissionCheckActivity.class));
        }

        setContentView(R.layout.activity_main);

        controlTripButton = findViewById(R.id.controlTripButton);
        statusList = findViewById(R.id.statusList);
    }

    @Override
    protected void onResume () {
        super.onResume();

        // Register a receiver so we are notified by MyApplication when the Sentiance SDK status was updated.
        LocalBroadcastManager.getInstance(this).registerReceiver(statusUpdateReceiver, new IntentFilter(MyApplication.ACTION_SENTIANCE_STATUS_UPDATE));

        refreshStatus();

        if (Sentiance.getInstance(getApplicationContext()).getInitState() != InitState.INITIALIZED) {
            controlTripButton.setEnabled(false);
        }
        updateButtonTexts();
    }

    @Override
    protected void onPause () {
        super.onPause();

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(statusUpdateReceiver);
    }

    private void refreshStatus () {
        List<String> statusItems = new ArrayList<>();

        if (Sentiance.getInstance(this).getInitState() == InitState.INITIALIZED) {
            controlTripButton.setEnabled(true);

            statusItems.add("SDK version: " + Sentiance.getInstance(this).getVersion());
            statusItems.add("User ID: " + Sentiance.getInstance(this).getUserId());

            SdkStatus sdkStatus = Sentiance.getInstance(this).getSdkStatus();

            statusItems.add("Start status: " + sdkStatus.startStatus.name());
            statusItems.add("Can detect: " + String.valueOf(sdkStatus.canDetect));
            statusItems.add("Remote enabled: " + String.valueOf(sdkStatus.isRemoteEnabled));
            statusItems.add("Location perm granted: " + String.valueOf(sdkStatus.isLocationPermGranted));
            statusItems.add("Location setting: " + sdkStatus.locationSetting.name());

            statusItems.add(formatQuota("Wi-Fi", sdkStatus.wifiQuotaStatus, Sentiance.getInstance(this).getWiFiQuotaUsage(), Sentiance.getInstance(this).getWiFiQuotaLimit()));
            statusItems.add(formatQuota("Mobile data", sdkStatus.mobileQuotaStatus, Sentiance.getInstance(this).getMobileQuotaUsage(), Sentiance.getInstance(this).getMobileQuotaLimit()));
            statusItems.add(formatQuota("Disk", sdkStatus.diskQuotaStatus, Sentiance.getInstance(this).getDiskQuotaUsage(), Sentiance.getInstance(this).getDiskQuotaLimit()));
        }
        else {
            statusItems.add("SDK not initialized");
        }

        statusList.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_status, R.id.textView, statusItems));
    }

    public void onControlTripButtonClicked (View view) {
        Sentiance sentiance = Sentiance.getInstance(getApplicationContext());
        if (sentiance.getInitState() != InitState.INITIALIZED) {
            return;
        }

        if (sentiance.isTripOngoing(TripType.EXTERNAL_TRIP)) {
            stopTrip();
        }
        else {
            startTrip();
        }
        controlTripButton.setEnabled(false);
        updateButtonTexts();
    }

    private void startTrip () {
        Sentiance.getInstance(getApplicationContext()).startTrip(null, null, mTripStartStopCallback);
    }

    private void stopTrip () {
        Sentiance.getInstance(getApplicationContext()).stopTrip(mTripStartStopCallback);
    }

    private String formatQuota (String name, SdkStatus.QuotaStatus status, long bytesUsed, long bytesLimit) {
        return String.format(Locale.US, "%s quota: %s / %s (%s)",
                name,
                Formatter.formatShortFileSize(this, bytesUsed),
                Formatter.formatShortFileSize(this, bytesLimit),
                status.name());
    }

    private void updateButtonTexts () {
        Sentiance sentiance = Sentiance.getInstance(getApplicationContext());

        if (sentiance.isTripOngoing(TripType.EXTERNAL_TRIP)) {
            controlTripButton.setText(R.string.stop_trip);
        }
        else {
            controlTripButton.setText(R.string.start_trip);
        }
    }

    private class TripStartStopCallback implements StopTripCallback, StartTripCallback {

        @Override
        public void onSuccess () {
            handleTripStartStop();
        }

        @Override
        public void onFailure (@Nullable SdkStatus sdkStatus) {
            handleTripStartStop();
        }

        private void handleTripStartStop () {
            updateButtonTexts();
            controlTripButton.setEnabled(true);
        }
    }
}
