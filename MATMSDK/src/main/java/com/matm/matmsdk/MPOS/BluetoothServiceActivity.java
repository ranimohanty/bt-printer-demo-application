package com.matm.matmsdk.MPOS;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.matm.matmsdk.Utils.SdkConstants;


public class BluetoothServiceActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_bluetooth);

        boolean installed = appInstalledOrNot("com.linkmatm.service");
        try {
            if (installed) {
                PackageManager manager = getPackageManager();
                Intent sIntent = manager.getLaunchIntentForPackage("com.linkmatm.service");
                sIntent.putExtra("ActivityName", "Bluetooth");

                if (SdkConstants.applicationType.equals("CORE")){
                    sIntent.putExtra("UserName", getIntent().getStringExtra("userName"));
                    sIntent.putExtra("UserToken", getIntent().getStringExtra("user_token"));
                    sIntent.putExtra("ApplicationType", "CORE");
                } else {
                    sIntent.putExtra("LoginID", SdkConstants.loginID);
                    sIntent.putExtra("EncryptedData", SdkConstants.encryptedData);
                    sIntent.putExtra("ApplicationType", "");
                }

                sIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                startActivity(sIntent);
                finish();

            } else {
                showAlert(BluetoothServiceActivity.this);
            }
        } catch (Exception e) {
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
                    .setNegativeButton("Not Now", (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    });
//                    .show();
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
