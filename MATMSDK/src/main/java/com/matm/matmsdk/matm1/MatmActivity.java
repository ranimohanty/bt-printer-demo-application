package com.matm.matmsdk.matm1;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.matm.matmsdk.CustomThemes;
import com.matm.matmsdk.Utils.SdkConstants;
import com.matm.matmsdk.aepsmodule.utils.Session;

import org.json.JSONException;
import org.json.JSONObject;

import isumatm.androidsdk.equitas.R;

import static com.matm.matmsdk.Utils.SdkConstants.Matm1BluetoothFlag;

public class MatmActivity extends AppCompatActivity implements View.OnClickListener, MicroAtmContract.View {

    Session session;
    MicroAtmPresenter microAtmPresenter;
    ProgressDialog pd;
    String encData;
    String authentication;
    MicroAtmTransactionModel microAtmTransactionModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new CustomThemes(this);
        setContentView(R.layout.activity_matm);
        session = new Session(MatmActivity.this);
        microAtmPresenter = new MicroAtmPresenter(this);
        pd = new ProgressDialog(MatmActivity.this);

        if (SdkConstants.Matm1BluetoothFlag.equalsIgnoreCase("1")) {
            pairBluetooth();

        } else if (SdkConstants.applicationType.equalsIgnoreCase("CORE")) {
            session.setUserToken(SdkConstants.tokenFromCoreApp);
            session.setUsername(SdkConstants.userNameFromCoreApp);

        } else {
            if (SdkConstants.encryptedData.trim().length() != 0) {
                getUserAuthToken();
            } else {
                showAlert("Request parameters are missing. Please check and try again..");
            }
        }
    }

    private void pairBluetooth() {
        Matm1BluetoothFlag = "0";
        boolean installed  =   appInstalledOrNot("com.matm.matmservice_1");
        try {
            if (installed) {
                Intent intent = new Intent(Intent.ACTION_DATE_CHANGED);
                PackageManager manager = getPackageManager();
                intent = manager.getLaunchIntentForPackage("com.matm.matmservice_1");
                intent.putExtra("RequestData", "");
                intent.putExtra("HeaderData", "");
                intent.putExtra("ReturnTime", 5);
                intent.putExtra("IS_PAIR_DEVICE", true);
                intent.putExtra("Flag", "bluetooth");
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                startActivityForResult(intent, 3);
                System.out.println("App already installed om your phone");
                finish();
            } else {
                showAlert("App is not installed on your phone");
                System.out.println("App is not installed on your phone");
            }
        }catch (Exception e){
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



    private void getUserAuthToken(){
        showLoader();
        String url = SdkConstants.BASE_URL+"api/getAuthenticateData" ;
        JSONObject obj = new JSONObject();
        try {
            obj.put("encryptedData", SdkConstants.encryptedData);
            obj.put("retailerUserName", SdkConstants.loginID);

            AndroidNetworking.post(url)
                    .setPriority(Priority.HIGH)
                    .addJSONObjectBody(obj)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONObject obj = new JSONObject(response.toString());
                                String status = obj.getString("status");

                                if(status.equalsIgnoreCase("success")) {
                                    String userName = obj.getString("username");
                                    String userToken = obj.getString("usertoken");
                                    session.setUsername(userName);
                                    session.setUserToken(userToken);
                                    //hideLoader();
                                    CallMatm1Api();

                                }else {
                                    showAlert(status);
                                    hideLoader();
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                hideLoader();
                                showAlert("Invalid Encrypted Data");
                            }
                        }
                        @Override
                        public void onError(ANError anError) {
                            hideLoader();

                        }

                    });
        }catch ( Exception e){
            e.printStackTrace();
        }
    }

    private void CallMatm1Api() {
           if(SdkConstants.transactionType.equalsIgnoreCase(SdkConstants.balanceEnquiry)){
                balanceEnquiryApiCalling();
            }else{
                apiCalling();
            }
    }

    public void showAlert(String msg){

        AlertDialog.Builder builder = new AlertDialog.Builder(MatmActivity.this);
        builder.setTitle("Alert!!");
        builder.setMessage(msg);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void checkRequestCode(String status, String message, MicroAtmResponse microAtmResponse) {

        if(status != null && !status.matches("")) {
            encData = microAtmResponse.getEncData();
            authentication = microAtmResponse.getAuthentication();
            String trans_type = "";
            if (SdkConstants.transactionType.equalsIgnoreCase(SdkConstants.cashWithdrawal)) {
                trans_type = "cash";
            }else{
                trans_type = "balance";
            }

            PackageManager manager = getPackageManager();
            Intent intent = manager.getLaunchIntentForPackage("com.matm.matmservice");
            intent.putExtra("RequestData", encData);
            intent.putExtra("HeaderData", authentication);
            intent.putExtra("ReturnTime", 5);
            intent.putExtra("IS_PAIR_DEVICE", false);
            intent.putExtra("Flag", "transaction");
            intent.putExtra("TransactionType", trans_type);
            intent.putExtra("client_id", "");
            intent.setFlags(0);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivityForResult(intent, 1);
        }else{
            showAlert(message);
        }


    }
    @Override
    public void checkEmptyFields() {

    }

    @Override
    public void showLoader() {
        if (pd !=null){
            pd = new ProgressDialog(MatmActivity.this);
            pd.setCancelable(false);
            pd.setMessage("Please Wait..");
            pd.show();
        }

    }

    @Override
    public void hideLoader() {
        if (pd!=null){
            pd.dismiss();
        }

    }

    public void apiCalling()
    {
        MicroAtmRequestModel microAtmRequestModel = new MicroAtmRequestModel(SdkConstants.transactionAmount,"MATMcashWithdrawal","mobile");
        microAtmPresenter.performRequestData(session.getUserName(),session.getUserToken(), microAtmRequestModel);
    }

    public void balanceEnquiryApiCalling()
    {
        MicroAtmRequestModel microAtmRequestModel = new MicroAtmRequestModel("0","MATMbalanceEnquiry","mobile");
        microAtmPresenter.performRequestData(session.getUserName(),session.getUserToken(), microAtmRequestModel);

    }
}
