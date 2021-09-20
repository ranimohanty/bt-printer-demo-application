package com.matm.matmsdk.MPOS;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.matm.matmsdk.Error.Error2Activity;
import com.matm.matmsdk.Error.ErrorActivity;
import com.matm.matmsdk.GpsTracker;
import com.matm.matmsdk.Utils.SdkConstants;
import com.matm.matmsdk.aepsmodule.AEPS2HomeActivity;
import com.matm.matmsdk.aepsmodule.utils.Util;
import com.matm.matmsdk.transaction_report.TransactionStatusActivity;

import static com.matm.matmsdk.Utils.SdkConstants.REQUEST_CODE;


public class PosServiceActivity extends AppCompatActivity {
    private static final String TAG = PosActivity.class.getSimpleName();
    private GpsTracker gpsTracker;
    String latLong = "";
    Double latitude,longitude;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

        getLocation();
        boolean installed = appInstalledOrNot("com.linkmatm.service");
        try {
            if (installed) {
                PackageManager manager = getPackageManager();
                Intent sIntent = manager.getLaunchIntentForPackage("com.linkmatm.service");
                sIntent.setFlags(0);
                sIntent.putExtra("ActivityName", "mATM2");


                if (SdkConstants.applicationType.equals("CORE")) {
                    sIntent.putExtra("UserName", SdkConstants.userNameFromCoreApp);
                    sIntent.putExtra("UserToken", SdkConstants.tokenFromCoreApp);
                    sIntent.putExtra("ApplicationType", "CORE");
                } else {
                    sIntent.putExtra("LoginID", SdkConstants.loginID);
                    sIntent.putExtra("EncryptedData", SdkConstants.encryptedData);
                    sIntent.putExtra("ApplicationType", "");
                }

                sIntent.putExtra("ParamA", SdkConstants.paramA);
                sIntent.putExtra("ParamB", SdkConstants.paramB);
                sIntent.putExtra("ParamC", SdkConstants.paramC);
                sIntent.putExtra("latitude", latitude);
                sIntent.putExtra("longitude", longitude);

                sIntent.putExtra("Amount", SdkConstants.transactionAmount);
                sIntent.putExtra("TransactionType", SdkConstants.transactionType);


                sIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                startActivityForResult(sIntent, REQUEST_CODE);

            } else {
                /*It will show if the app is not install in your phone*/
                showAlert(PosServiceActivity.this);
            }
        } catch (Exception e) {
        }

    }

    public void getLocation(){
        gpsTracker = new GpsTracker(PosServiceActivity.this);
        if(gpsTracker.canGetLocation()){
             latitude = gpsTracker.getLatitude();
             longitude = gpsTracker.getLongitude();

            latLong = latitude + "," + longitude;

        }else{
            gpsTracker.showSettingsAlert();
        }
    }

    private boolean appInstalledOrNot(String uri) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);



        if (data != null && resultCode == RESULT_OK) {
            if (data.hasExtra("error1Response")) {
                Intent intent = new Intent(PosServiceActivity.this, ErrorActivity.class);
                intent.putExtra("errorResponse", data.getIntExtra("error1Response", 0));
                startActivity(intent);
                finish();
            } else if (data.hasExtra("errorResponse")) {
                Intent intent = new Intent(PosServiceActivity.this, Error2Activity.class);
                intent.putExtra("errorResponse", data.getStringExtra("errorResponse"));
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(PosServiceActivity.this, TransactionStatusActivity.class);

                //Bundle b = data.getExtras();
                //intent.putExtras(b);

                intent.putExtra("flag", data.getStringExtra("flag"));
                intent.putExtra("TRANSACTION_ID",data.getStringExtra("TRANSACTION_ID"));
                intent.putExtra("TRANSACTION_TYPE", data.getStringExtra("TRANSACTION_TYPE"));
                intent.putExtra("TRANSACTION_AMOUNT", data.getStringExtra("TRANSACTION_AMOUNT"));
                intent.putExtra("RRN_NO", data.getStringExtra("RRN_NO"));
                intent.putExtra("RESPONSE_CODE", data.getStringExtra("RESPONSE_CODE"));
                intent.putExtra("APP_NAME", data.getStringExtra("APP_NAME"));
                intent.putExtra("AID", data.getStringExtra("AID"));
                intent.putExtra("AMOUNT", data.getStringExtra("AMOUNT"));
                intent.putExtra("MID", data.getStringExtra("MID"));
                intent.putExtra("TID", data.getStringExtra("TID"));
                intent.putExtra("TXN_ID", data.getStringExtra("TXN_ID"));
                intent.putExtra("INVOICE", data.getStringExtra("INVOICE"));
                intent.putExtra("CARD_TYPE", data.getStringExtra("CARD_TYPE"));
                intent.putExtra("APPR_CODE", data.getStringExtra("APPR_CODE"));
                intent.putExtra("CARD_NUMBER", data.getStringExtra("CARD_NUMBER"));

                startActivity(intent);
                finish();
            }
        } else {
            finish();
        }
    }

    public void showAlert(Context context) {
        try {

            AlertDialog.Builder alertbuilderupdate;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alertbuilderupdate = new AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog_Alert);
            } else {
                alertbuilderupdate = new AlertDialog.Builder(context);
            }
            alertbuilderupdate.setCancelable(false);
            String message = "Please download the MATM SERVICE-2 app from the playstore.";
            alertbuilderupdate.setTitle("Alert")
                    .setMessage(message)
                    .setPositiveButton("Download Now", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            redirectToPlayStore();
                            finish();
                        }
                    })
                    .setNegativeButton("Not Now", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    });
            AlertDialog alert11 = alertbuilderupdate.create();
            alert11.show();

        } catch (Exception e) {

        }
    }

    public void redirectToPlayStore() {
        Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=com.linkmatm.service&hl=en_US");
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.linkmatm.service&hl=en_US")));
        }
    }

}
