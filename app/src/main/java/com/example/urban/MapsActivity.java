package com.example.urban;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.urban.DataStore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    public String TAG = "position";
    public Button back;
    private GoogleMap map;
    private Marker mBrisbane;
    private Marker mdublin;
    TextView tv;
    private static final LatLng BRISBANE = new LatLng(55, -6);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        back = findViewById(R.id.back);
        tv = findViewById(R.id.tv);
        EventBus.getDefault().register(this); //初始化EventBus
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MapsActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        Double gn = event.getMessage1();
        Toast.makeText(this, "The latitude of the received message is：" + event.getMessage1(), Toast.LENGTH_SHORT).show();
        Double la = event.getMessage2();
        Toast.makeText(this, "The received message has the longitude of：" + event.getMessage2(), Toast.LENGTH_SHORT).show();
        tv.setText("current location latitude is:"+la.toString() + "    longitude is:" + gn.toString());
//        LatLng own = new LatLng(53, -6);
//        mBrisbane = map.addMarker(new MarkerOptions().position(own).title("Marker").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this); //释放
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        Intent R=this.getIntent();
        Bundle b=R.getExtras();
        String lat = b.getString("lat");
        String lng = b.getString("lng");
        Log.d(TAG,lat);
        LatLng dublin = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
        mdublin= map.addMarker(new MarkerOptions()
                .position(dublin)
                .title("Marker in lat is"+lat+"  lng is"+lng));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)), 7));
    }
}




