// GyroscopeActivity.java
package org.tensorflow.lite.examples.objectdetection;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GyroscopeActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private LocationManager locationManager;

    private GpsTracker gpsTracker;

    private long lastTimestamp;
    private float angleX;
    private float threshold = 1.0f; // 기준 기울기 값 지정

    private TextView textViewX;
    private TextView textViewY;
    private TextView textViewZ;
    private TextView textViewThreshold;
    private TextView textViewOrientation;
    private TextView textViewLatitude;
    private TextView textViewLongitude;

    //현재 위도 경도
    private double currentLatitude;
    private double currentLongitude;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gyroscope);

        // 센서 매니저 초기화
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // 자이로스코프 센서 가져오기
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (!checkLocationServicesStatus()) {

            showDialogForLocationServiceSetting();
        }else {

            checkRunTimePermission();
        }

        final TextView textview_address = (TextView)findViewById(R.id.textview);

        Button ShowLocationButton = (Button) findViewById(R.id.button);
        ShowLocationButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {

                gpsTracker = new GpsTracker(GyroscopeActivity.this);

                double latitude = gpsTracker.getLatitude();
                double longitude = gpsTracker.getLongitude();

                String address = getCurrentAddress(latitude, longitude);
                textview_address.setText(address);

                Toast.makeText(GyroscopeActivity.this, "현재위치 \n위도 " + latitude + "\n경도 " + longitude, Toast.LENGTH_LONG).show();
            }
        });


        // 센서 리스너 등록
        if (gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // TextView 초기화
        textViewX = findViewById(R.id.textViewX);
        textViewY = findViewById(R.id.textViewY);
        textViewZ = findViewById(R.id.textViewZ);
        textViewThreshold = findViewById(R.id.textViewThreshold);
        textViewOrientation = findViewById(R.id.textViewOrientation);
        textViewLatitude = findViewById(R.id.textViewLatitude);
        textViewLongitude = findViewById(R.id.textViewLongitude);



        // 초기화
        lastTimestamp = 0;
        angleX = 0.0f;

        // 위치 매니저 초기화
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        checkLocationPermission();

        // 위치 업데이트 요청
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int permsRequestCode,
//                                           @NonNull String[] permissions,
//                                           @NonNull int[] grandResults) {
//
//        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {
//
//            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
//
//            boolean check_result = true;
//
//
//            // 모든 퍼미션을 허용했는지 체크합니다.
//
//            for (int result : grandResults) {
//                if (result != PackageManager.PERMISSION_GRANTED) {
//                    check_result = false;
//                    break;
//                }
//            }
//
//
//            if ( check_result ) {
//
//                //위치 값을 가져올 수 있음
//                ;
//            }
//            else {
//                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.
//
//                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
//                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {
//
//                    Toast.makeText(GyroscopeActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
//                    finish();
//
//
//                }else {
//
//                    Toast.makeText(GyroscopeActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();
//
//                }
//            }
//
//        }
//    }

    void checkRunTimePermission(){

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(GyroscopeActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(GyroscopeActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            // 3.  위치 값을 가져올 수 있음



        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(GyroscopeActivity.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(GyroscopeActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(GyroscopeActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(GyroscopeActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }


    public String getCurrentAddress( double latitude, double longitude) {

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }



        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString()+"\n";

    }


    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(GyroscopeActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 권한 요청
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            // 이미 권한이 있는 경우 위치 업데이트 요청
            requestLocationUpdates();
        }
    }

    private void requestLocationUpdates() {
        // 위치 업데이트 요청
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            // 권한 요청 결과 확인
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // 권한이 허용된 경우 위치 업데이트 요청
//                requestLocationUpdates();
//            } else {
//                // 권한이 거부된 경우 사용자에게 메시지 표시 또는 다른 처리 수행
//                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // 자이로스코프 데이터 처리
            if (lastTimestamp != 0) {
                float dt = (event.timestamp - lastTimestamp) * 1.0e-9f; // 시간 간격 (초 단위)
                float gyroX = event.values[0];
                float gyroY = event.values[1];
                float gyroZ = event.values[2];

                // 각속도를 적분하여 각도로 변환
                angleX += gyroX * dt;

                // TextView에 값을 업데이트
                textViewX.setText("X: " + gyroX);
                textViewY.setText("Y: " + gyroY);
                textViewZ.setText("Z: " + gyroZ);
                textViewThreshold.setText("기준 기울기 값: " + threshold);

                // 각도 표시 TextView 업데이트
                textViewOrientation.setText("현재 기울기 값: " + angleX);

                // 여기에서 일정 기울기 값 이상인지 확인
                if (Math.abs(angleX) > threshold) {
                    showFallenAlert();
                }
            }

            lastTimestamp = event.timestamp;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 센서 정확도 변경 시 호출되는 메서드
        // 센서의 정확도가 변경될 때 호출되는 메서드
        String accuracyString;
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                accuracyString = "UNRELIABLE";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                accuracyString = "LOW";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                accuracyString = "MEDIUM";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                accuracyString = "HIGH";
                break;
            default:
                accuracyString = "Unknown";
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // 위치가 변경될 때 호출되는 메서드
//        double latitude = location.getLatitude();
//        double longitude = location.getLongitude();

//        Log.d("Location", "Latitude: " + latitude + ", Longitude: " + longitude);
//
//        // TextView에 값을 업데이트
//        textViewLatitude.setText("위도: " + latitude);
//        textViewLongitude.setText("경도: " + longitude);
//
//        currentLatitude = latitude;
//        currentLongitude = longitude;

        gpsTracker = new GpsTracker(GyroscopeActivity.this);

        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();

        String address = getCurrentAddress(latitude, longitude);

        textViewLatitude.setText("위도: " + latitude);
        textViewLongitude.setText("경도: " + longitude);

        currentLatitude = latitude;
        currentLongitude = longitude;
        Toast.makeText(GyroscopeActivity.this, "현재위치 \n위도 " + latitude + "\n경도 " + longitude, Toast.LENGTH_LONG).show();

        checkFallen();

    }

    @Override
    protected void onPause() {
        super.onPause();
        // 화면이 onPause 상태로 전환되면 센서 리스너 및 위치 업데이트 리스너 해제
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
    }

    private void checkFallen() {
        // 일정 기울기 값 이상인지 확인
        if (Math.abs(angleX) > threshold) {
            // 사용자가 넘어졌다면 경고 표시
            showFallenAlert();
        }
    }
    private void showFallenAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("아야!")
                .setMessage("사용자가 넘어졌습니다 ㅠㅠ \n")
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        gpsTracker = new GpsTracker(GyroscopeActivity.this);

                        double latitude = gpsTracker.getLatitude();
                        double longitude = gpsTracker.getLongitude();

                        String address = getCurrentAddress(latitude, longitude);

                        final TextView textview_address = (TextView)findViewById(R.id.textview);
                        textViewLatitude.setText("위도: " + latitude);
                        textViewLongitude.setText("경도: " + longitude);
                        textview_address.setText(address);

                        currentLatitude = latitude;
                        currentLongitude = longitude;
                        Toast.makeText(GyroscopeActivity.this, "현재위치 \n위도 " + latitude + "\n경도 " + longitude, Toast.LENGTH_LONG).show();

                    }
                })
                .setCancelable(false) // 사용자가 뒤로가기 버튼으로 창을 닫지 못하게 설정
                .show();
    }
}
