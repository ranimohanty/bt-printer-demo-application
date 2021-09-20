  package com.matm.matmsdk.aepsmodule;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.JsonReader;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.matm.matmsdk.CustomThemes;
import com.matm.matmsdk.FileUtils;
import com.matm.matmsdk.GpsTracker;
import com.matm.matmsdk.Utils.SdkConstants;
import com.matm.matmsdk.aepsmodule.balanceenquiry.BalanceEnquiryAEPS2RequestModel;
import com.matm.matmsdk.aepsmodule.balanceenquiry.BalanceEnquiryContract;
import com.matm.matmsdk.aepsmodule.balanceenquiry.BalanceEnquiryPresenter;
import com.matm.matmsdk.aepsmodule.balanceenquiry.BalanceEnquiryResponse;
import com.matm.matmsdk.aepsmodule.bankspinner.BankNameListActivity;
import com.matm.matmsdk.aepsmodule.bankspinner.BankNameModel;
import com.matm.matmsdk.aepsmodule.cashwithdrawal.AepsResponse;
import com.matm.matmsdk.aepsmodule.cashwithdrawal.CashWithDrawalContract;
import com.matm.matmsdk.aepsmodule.cashwithdrawal.CashWithdrawalAEPS2RequestModel;
import com.matm.matmsdk.aepsmodule.cashwithdrawal.CashWithdrawalPresenter;
import com.matm.matmsdk.aepsmodule.cashwithdrawal.CashWithdrawalResponse;

import com.matm.matmsdk.aepsmodule.ministatement.StatementResponse;
import com.matm.matmsdk.aepsmodule.ministatement.StatementTransactionActivity;
import com.matm.matmsdk.aepsmodule.transactionstatus.TransactionStatusAeps2Activity;
import com.matm.matmsdk.aepsmodule.transactionstatus.TransactionStatusModel;
import com.matm.matmsdk.aepsmodule.utils.Constants;
import com.matm.matmsdk.aepsmodule.utils.Session;
import com.matm.matmsdk.aepsmodule.utils.Util;
import com.matm.matmsdk.notification.NotificationHelper;
import com.moos.library.HorizontalProgressView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringReader;

import isumatm.androidsdk.equitas.R;

public class AEPS2HomeActivity extends AppCompatActivity implements BalanceEnquiryContract.View, CashWithDrawalContract.View {

    Boolean adharbool = true;
    Boolean virtualbool = false;
    private EditText aadharNumber, aadharVirtualID;
    private TextView balanceEnquiryExpandButton, cashWithdrawalButton, fingerprintStrengthDeposit, depositNote;
    private EditText mobileNumber, bankspinner, amountEnter;
    private ImageView fingerprint, virtualID, aadhaar;
    private HorizontalProgressView depositBar;
    private Button submitButton;
    private BalanceEnquiryPresenter balanceEnquiryPresenter;
    private CashWithdrawalPresenter cashWithdrawalPresenter;
    Session session;
    BalanceEnquiryAEPS2RequestModel balanceEnquiryaeps2RequestModel;
    CashWithdrawalAEPS2RequestModel cashWithdrawalaeps2RequestModel;
    String bankIINNumber = "";
    ProgressDialog loadingView;
    String flagNameRdService = "";
    Class driverActivity;
    String balanaceInqueryAadharNo = "";
    Boolean flagFromDriver = false;
    String vid = "", uid = "";
    TextView virtualidText, aadharText;
    boolean mInside = false;
    boolean mWannaDeleteHyphen = false;
    boolean mKeyListenerSet = false;
    final static String MARKER = "|"; // filtered in layout not to be in the string
    private GpsTracker gpsTracker;
    String latLong = "";
    SharedPreferences settings;
    SharedPreferences.Editor editor;
    //notification 22 july
    private NotificationHelper notificationHelper;
    TypedValue typedValue;
    Resources.Theme theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
         * For CustomThemes
         * @Author - RashmiRanjan
         * */

       new CustomThemes(this);
       // setTheme(R.style.MediumSlateBlue);

        if (SdkConstants.dashboardLayout == 0) {
            setContentView(R.layout.activity_aeps_home);
        } else {
            setContentView(SdkConstants.dashboardLayout);
        }

        session = new Session(AEPS2HomeActivity.this);
        getRDServiceClass();
        typedValue = new TypedValue();
        theme = this.getTheme();

        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        getLocation();
        //notification 22 july
        notificationHelper = new NotificationHelper(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        SdkConstants.RECEIVE_DRIVER_DATA = "";

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        if(SdkConstants.applicationType.equalsIgnoreCase("CORE")){
            session.setUserToken(SdkConstants.tokenFromCoreApp);
            session.setUsername(SdkConstants.userNameFromCoreApp);
            SdkConstants.isSL = false;

        }else {
            SdkConstants.isSL = true;
            getUserAuthToken();

        }
        fingerprintStrengthDeposit = findViewById(R.id.fingerprintStrengthDeposit);
        depositNote = findViewById(R.id.depositNote);
        depositNote.setVisibility(View.GONE);
        fingerprintStrengthDeposit.setVisibility(View.GONE);
        aadharVirtualID = (EditText) findViewById(R.id.aadharVirtualID);
        virtualID = findViewById(R.id.virtualID);
        aadhaar = findViewById(R.id.aadhaar);
        virtualidText = findViewById(R.id.virtualidText);
        aadharText = findViewById(R.id.aadharText);
        aadharNumber = findViewById(R.id.aadharNumber);
        mobileNumber = findViewById(R.id.mobileNumber);
        bankspinner = findViewById(R.id.bankspinner);
        amountEnter = findViewById(R.id.amountEnter);
        fingerprint = findViewById(R.id.fingerprint);
        fingerprint.setEnabled(false);
        submitButton = findViewById(R.id.submitButton);
        depositBar = findViewById(R.id.depositBar);
        depositBar.setVisibility(View.GONE);


        cashWithdrawalButton = findViewById(R.id.cashWithdrawalButton);
        balanceEnquiryExpandButton = findViewById(R.id.balanceEnquiryExpandButton);



        if (SdkConstants.transactionType.equalsIgnoreCase(SdkConstants.balanceEnquiry)) {
            balanceEnquiryExpandButton.setVisibility(View.VISIBLE);
            cashWithdrawalButton.setVisibility(View.GONE);
            amountEnter.setVisibility(View.GONE);

        } else if (SdkConstants.transactionType.equalsIgnoreCase(SdkConstants.ministatement)) {
            balanceEnquiryExpandButton.setVisibility(View.VISIBLE);
            balanceEnquiryExpandButton.setText("Mini Statement");
            cashWithdrawalButton.setVisibility(View.GONE);
            amountEnter.setVisibility(View.GONE);

        } else if (SdkConstants.transactionType.equalsIgnoreCase(SdkConstants.cashWithdrawal)) {

            balanceEnquiryExpandButton.setVisibility(View.GONE);
            cashWithdrawalButton.setVisibility(View.VISIBLE);
            amountEnter.setText(SdkConstants.transactionAmount);
        }

        amountEnter.setEnabled(SdkConstants.editable);




        virtualID.setBackgroundResource(R.drawable.ic_language);
        virtualidText.setTextColor(getResources().getColor(R.color.grey));

        aadhaar.setBackgroundResource(R.drawable.ic_fingerprint_blue);
       // aadharText.setTextColor(getResources().getColor(R.color.light_blue));


        bankspinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                showLoader();
                Intent in = new Intent(AEPS2HomeActivity.this, BankNameListActivity.class);
                if (SdkConstants.transactionType.equalsIgnoreCase(SdkConstants.balanceEnquiry)) {
                    startActivityForResult(in, SdkConstants.REQUEST_FOR_ACTIVITY_BALANCE_ENQUIRY_CODE);

                } else if (SdkConstants.transactionType.equalsIgnoreCase(SdkConstants.cashWithdrawal)) {
                    startActivityForResult(in, SdkConstants.REQUEST_FOR_ACTIVITY_CASH_WITHDRAWAL_CODE);

                } else if (SdkConstants.transactionType.equalsIgnoreCase(SdkConstants.ministatement)) {
                    startActivityForResult(in, SdkConstants.REQUEST_FOR_ACTIVITY_BALANCE_ENQUIRY_CODE);
                }

            }
        });


        fingerprint.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                showLoader();
                fingerprint.setEnabled(false);
                //fingerprint.setImageDrawable(getResources().getDrawable(R.drawable.ic_scanner_grey));
                fingerprint.setColorFilter(R.color.colorGrey);
                flagFromDriver = true;

                Intent launchIntent = new Intent(AEPS2HomeActivity.this, driverActivity);
                launchIntent.putExtra("driverFlag", flagNameRdService);
                launchIntent.putExtra("freshnesFactor", session.getFreshnessFactor());
                launchIntent.putExtra("AadharNo", balanaceInqueryAadharNo);
                startActivityForResult(launchIntent, 1);
            }
        });
        //-------------------------

        aadharNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!mKeyListenerSet) {
                    aadharNumber.setOnKeyListener(new View.OnKeyListener() {
                        @Override
                        public boolean onKey(View v, int keyCode, KeyEvent event) {
                            try {
                                mWannaDeleteHyphen = (keyCode == KeyEvent.KEYCODE_DEL
                                        && aadharNumber.getSelectionEnd() - aadharNumber.getSelectionStart() <= 1
                                        && aadharNumber.getSelectionStart() > 0
                                        && aadharNumber.getText().toString().charAt(aadharNumber.getSelectionEnd() - 1) == '-');
                            } catch (IndexOutOfBoundsException e) {
                                // never to happen because of checks
                            }
                            return false;
                        }
                    });
                    mKeyListenerSet = true;
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mInside) // to avoid recursive calls
                    return;
                mInside = true;

                int currentPos = aadharNumber.getSelectionStart();
                String string = aadharNumber.getText().toString().toUpperCase();
                String newString = makePrettyString(string);

                if (count == 14) {

                    fingerprint.setEnabled(true);
                    //fingerprint.setImageDrawable(getResources().getDrawable(R.drawable.ic_scanner));
                    theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
                    int color = typedValue.data;
                    fingerprint.setColorFilter(color);
                    submitButton.setEnabled(true);
                    submitButton.setBackground(getResources().getDrawable(R.drawable.button_submit_blue));
                } else {

                }

                aadharNumber.setText(newString);
                try {
                    aadharNumber.setSelection(getCursorPos(string, newString, currentPos, mWannaDeleteHyphen));
                } catch (IndexOutOfBoundsException e) {
                    aadharNumber.setSelection(aadharNumber.length()); // last resort never to happen
                }

                mWannaDeleteHyphen = false;
                mInside = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() < 1) {
                    aadharNumber.setError(getResources().getString(R.string.aadhaarnumber));

                }

                if (s.length() > 0) {
                    aadharNumber.setError(null);
                    String aadharNo = aadharNumber.getText().toString();
                    if (aadharNo.contains("-")) {
                        aadharNo = aadharNo.replaceAll("-", "").trim();
                        balanaceInqueryAadharNo = aadharNo;
                        if (balanaceInqueryAadharNo.length() >= 12) {
                            fingerprint.setEnabled(true);
                            //fingerprint.setImageDrawable(getResources().getDrawable(R.drawable.ic_scanner));
                            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
                            int color = typedValue.data;
                            fingerprint.setColorFilter(color);
                            aadharNumber.clearFocus();
                            mobileNumber.requestFocus();
                        }
                    }

                    if (Util.validateAadharNumber(aadharNo) == false) {
                        aadharNumber.setError(getResources().getString(R.string.valid_aadhar_error));
                    }
                }
            }

        });

        aadharVirtualID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!mKeyListenerSet) {
                    aadharVirtualID.setOnKeyListener(new View.OnKeyListener() {
                        @Override
                        public boolean onKey(View v, int keyCode, KeyEvent event) {
                            try {
                                mWannaDeleteHyphen = (keyCode == KeyEvent.KEYCODE_DEL
                                        && aadharVirtualID.getSelectionEnd() - aadharVirtualID.getSelectionStart() <= 1
                                        && aadharVirtualID.getSelectionStart() > 0
                                        && aadharVirtualID.getText().toString().charAt(aadharVirtualID.getSelectionEnd() - 1) == '-');
                            } catch (IndexOutOfBoundsException e) {
                                // never to happen because of checks
                            }
                            return false;
                        }
                    });
                    mKeyListenerSet = true;
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mInside) // to avoid recursive calls
                    return;
                mInside = true;

                Log.v("SUBHA", "count == " + count);
                int currentPos = aadharVirtualID.getSelectionStart();
                String string = aadharVirtualID.getText().toString().toUpperCase();
                String newString = makePrettyString(string);

                Log.v("SUBHA", "count == " + string.length());
                if (count == 19) {
                    fingerprint.setEnabled(true);
                    //fingerprint.setImageDrawable(getResources().getDrawable(R.drawable.ic_scanner));
                    theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
                    int color = typedValue.data;
                    fingerprint.setColorFilter(color);
                }
                aadharVirtualID.setText(newString);
                try {
                    aadharVirtualID.setSelection(getCursorPos(string, newString, currentPos, mWannaDeleteHyphen));
                } catch (IndexOutOfBoundsException e) {
                    aadharVirtualID.setSelection(aadharVirtualID.length()); // last resort never to happen
                }

                mWannaDeleteHyphen = false;
                mInside = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() < 1) {
                    aadharVirtualID.setError(getResources().getString(R.string.aadhaarVID));

                }

                if (s.length() > 0) {
                    aadharVirtualID.setError(null);
                    String aadharNo = aadharVirtualID.getText().toString();
                    if (aadharNo.contains("-")) {
                        aadharNo = aadharNo.replaceAll("-", "").trim();
                        balanaceInqueryAadharNo = aadharNo;
                        if (balanaceInqueryAadharNo.length() >= 12) {
                            fingerprint.setEnabled(true);
                            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
                            int color = typedValue.data;
                            fingerprint.setColorFilter(color);
                            aadharVirtualID.clearFocus();
                            mobileNumber.requestFocus();
                            //fingerprint.setImageDrawable(getResources().getDrawable(R.drawable.ic_scanner));
                        }
                    }
                    if (Util.validateAadharVID(aadharNo) == false) {
                        aadharVirtualID.setError(getResources().getString(R.string.valid_aadhar__uid_error));
                    }
                }
            }

        });


        bankspinner.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() < 1) {
                    bankspinner.setError(getResources().getString(R.string.select_bank_error));
                }
                if (s.length() > 0) {
                    bankspinner.setError(null);
                }
            }
        });


        amountEnter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() < 1) {
                    amountEnter.setError(getResources().getString(R.string.amount_error));
                }
                if (s.length() > 0) {
                    amountEnter.setError(null);
                }
            }
        });


        mobileNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                submitButton.setEnabled(true);
                submitButton.setBackgroundResource(R.drawable.button_submit_blue);
            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() > 0 || s.length() < 10) {
                    mobileNumber.setError(null);
                    String x = s.toString();
                    if (x.startsWith("0") || Util.isValidMobile(mobileNumber.getText().toString().trim()) == false) {
                        mobileNumber.setError(getResources().getString(R.string.mobilevaliderror));
                    }
                }else{
                    submitButton.setEnabled(true);
                    submitButton.setBackgroundResource(R.drawable.button_submit_blue);
                }
            }
        });


        virtualID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Will implement when VID will used*/

                aadharNumber.setVisibility(View.GONE);
                aadharVirtualID.setVisibility(View.VISIBLE);
                virtualID.setEnabled(false);
                aadhaar.setEnabled(true);
                virtualbool = true;
                adharbool = false;
                virtualID.setBackgroundResource(R.drawable.ic_language_blue);
                //virtualidText.setTextColor(getResources().getColor(R.color.colorPrimary));
                theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
                int color = typedValue.data;
                virtualidText.setTextColor(color);
                aadhaar.setBackground(getResources().getDrawable(R.drawable.ic_fingerprint_grey));
                aadharText.setTextColor(getResources().getColor(R.color.grey));


            }
        });

        aadhaar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aadharNumber.setVisibility(View.VISIBLE);
                aadharVirtualID.setVisibility(View.GONE);
                virtualID.setEnabled(true);
                aadhaar.setEnabled(false);
                virtualID.setBackgroundResource(R.drawable.ic_language);
                virtualidText.setTextColor(getResources().getColor(R.color.grey));
                adharbool = true;
                virtualbool = false;
                aadhaar.setBackgroundResource(R.drawable.ic_fingerprint_blue);
                theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
                int color = typedValue.data;
                aadharText.setTextColor(color);

                //  aadharText.setTextColor(getResources().getColor(R.color.colorPrimary));
            }
        });



        //--------RAJESH------------------

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //showLoader();
                String balanceaadharNo = "";
                String balanceaadharVid = "";
                balanceaadharNo = aadharNumber.getText().toString();
                if (adharbool == true) {
                    if (balanceaadharNo.contains("-")) {
                        balanceaadharNo = balanceaadharNo.replaceAll("-", "").trim();
                    }
                    if (balanceaadharNo == null || balanceaadharNo.matches("")) {
                        aadharNumber.setError(getResources().getString(R.string.valid_aadhar_error));
                        return;
                    }
                    if (Util.validateAadharNumber(balanceaadharNo) == false) {
                        aadharNumber.setError(getResources().getString(R.string.valid_aadhar_error));
                        return;
                    }
                    if (aadharNumber.getText().toString().length() < 14) {
                        aadharNumber.setError("Enter valid aadhaar no.");
                        return;
                    }
                } else if (virtualbool == true) {
                    balanceaadharVid = aadharVirtualID.getText().toString().trim();
                    if (balanceaadharVid.contains("-")) {
                        balanceaadharVid = balanceaadharVid.replaceAll("-", "").trim();
                    }
                    if (balanceaadharVid == null || balanceaadharVid.matches("")) {
                        aadharVirtualID.setError(getResources().getString(R.string.valid_vid_error));
                        return;
                    }
                    if (Util.validateAadharNumber(balanceaadharVid) == false) {
                        aadharVirtualID.setError(getResources().getString(R.string.valid_aadhar_error));
                        return;
                    }

                }
                if (!flagFromDriver) {
                    Toast.makeText(AEPS2HomeActivity.this, "Please do Biometric Varification", Toast.LENGTH_LONG).show();
                    return;
                } else {

                    try {
                        JSONObject respObj = new JSONObject(SdkConstants.RECEIVE_DRIVER_DATA);
                        String scoreStr = respObj.getString("pidata_qscore");

                        if (Float.parseFloat(scoreStr) <= 40) {
                            showAlert("Bad Fingerprint Strength, Please try Again !");
                            return;
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                if (mobileNumber.getText() == null || mobileNumber.getText().toString().trim().matches("") || Util.isValidMobile(mobileNumber.getText().toString().trim()) == false) {
                    mobileNumber.setError(getResources().getString(R.string.mobileerror));
                    return;
                }
                String panaaadhaar = mobileNumber.getText().toString().trim();
                if (!panaaadhaar.contains(" ") && panaaadhaar.length() == 10) {
                } else {
                    mobileNumber.setError(getResources().getString(R.string.mobileerror));
                    return;
                }
                if (bankspinner.getText() == null || bankspinner.getText().toString().trim().matches("")) {
                    bankspinner.setError(getResources().getString(R.string.select_bank_error));
                    return;
                }
                if (SdkConstants.transactionType.equalsIgnoreCase(SdkConstants.balanceEnquiry)) {
                    showLoader();

                    try {
                        JSONObject respObj = new JSONObject(SdkConstants.RECEIVE_DRIVER_DATA);
                        String CI = respObj.getString("CI");
                        String DC = respObj.getString("DC");
                        String DPID = respObj.getString("DPID");
                        String DATAVALUE = respObj.getString("DATAVALUE");
                        String HMAC = respObj.getString("HMAC");
                        String MI = respObj.getString("MI");
                        String MC = respObj.getString("MC");
                        String RDSID = respObj.getString("RDSID");
                        String RDSVER = respObj.getString("RDSVER");
                        String value = respObj.getString("value");
                        String hashString = FileUtils.getSha256Hash(balanaceInqueryAadharNo);
                        balanceEnquiryaeps2RequestModel = new BalanceEnquiryAEPS2RequestModel("", balanaceInqueryAadharNo, vid, CI, DC, "", DPID, DATAVALUE, session.getFreshnessFactor(), HMAC, bankIINNumber, MC, MI, mobileNumber.getText().toString().trim(), "", RDSID, RDSVER, value, "MOBUSER", session.getUserName(), SdkConstants.isSL, SdkConstants.paramA, SdkConstants.paramB, SdkConstants.paramC, latLong,hashString);
                        balanceEnquiryPresenter = new BalanceEnquiryPresenter(AEPS2HomeActivity.this);
                        checkVPNstatusForTransaction("Balance");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else if (SdkConstants.transactionType.equalsIgnoreCase(SdkConstants.ministatement)) {
                    showLoader();

                    try {
                        JSONObject respObj = new JSONObject(SdkConstants.RECEIVE_DRIVER_DATA);
                        String CI = respObj.getString("CI");
                        String DC = respObj.getString("DC");
                        String DPID = respObj.getString("DPID");
                        String DATAVALUE = respObj.getString("DATAVALUE");
                        String HMAC = respObj.getString("HMAC");
                        String MI = respObj.getString("MI");
                        String MC = respObj.getString("MC");
                        String RDSID = respObj.getString("RDSID");
                        String RDSVER = respObj.getString("RDSVER");
                        String value = respObj.getString("value");
                        String hashString = FileUtils.getSha256Hash(balanaceInqueryAadharNo);
                        balanceEnquiryaeps2RequestModel = new BalanceEnquiryAEPS2RequestModel("", balanaceInqueryAadharNo, vid, CI, DC, "", DPID, DATAVALUE, session.getFreshnessFactor(), HMAC, bankIINNumber, MC, MI, mobileNumber.getText().toString().trim(), "", RDSID, RDSVER, value, "WEBUSER", session.getUserName(), SdkConstants.isSL, SdkConstants.paramA, SdkConstants.paramB, SdkConstants.paramC, latLong,hashString);
                        balanceEnquiryPresenter = new BalanceEnquiryPresenter(AEPS2HomeActivity.this);
                        checkVPNstatusForTransaction("Balance");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {
                    showLoader();
                    if (amountEnter.getText() == null || amountEnter.getText().toString().trim().matches("")) {
                        amountEnter.setError(getResources().getString(R.string.amount_error));
                        return;
                    }
                    try {
                        JSONObject respObj = new JSONObject(SdkConstants.RECEIVE_DRIVER_DATA);
                        String CI = respObj.getString("CI");
                        String DC = respObj.getString("DC");
                        String DPID = respObj.getString("DPID");
                        String DATAVALUE = respObj.getString("DATAVALUE");
                        String HMAC = respObj.getString("HMAC");
                        String MI = respObj.getString("MI");
                        String MC = respObj.getString("MC");
                        String RDSID = respObj.getString("RDSID");
                        String RDSVER = respObj.getString("RDSVER");
                        String value = respObj.getString("value");
                        cashWithdrawalaeps2RequestModel = new CashWithdrawalAEPS2RequestModel(amountEnter.getText().toString().trim(), balanaceInqueryAadharNo, vid, CI, DC, "WITHDRAW", DPID, DATAVALUE, session.getFreshnessFactor(), HMAC, bankIINNumber, MC, MI, mobileNumber.getText().toString().trim(), "WITHDRAW", RDSID, RDSVER, value, "WEBUSER", session.getUserName(), SdkConstants.isSL, SdkConstants.paramA, SdkConstants.paramB, SdkConstants.paramC, latLong);
                        cashWithdrawalPresenter = new CashWithdrawalPresenter(AEPS2HomeActivity.this);
                        checkVPNstatusForTransaction("Cash");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    private void getRDServiceClass() {
        //String accessClassName =  getIntent().getStringExtra("activity");
        //flagNameRdService = getIntent().getStringExtra("driverFlag");
        String accessClassName = SdkConstants.DRIVER_ACTIVITY;//getIntent().getStringExtra("activity");
        flagNameRdService = SdkConstants.MANUFACTURE_FLAG;//getIntent().getStringExtra("driverFlag");


        try {
            Class<? extends Activity> targetActivity = Class.forName(accessClassName).asSubclass(Activity.class);
            driverActivity = targetActivity;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void getLocation(){
        gpsTracker = new GpsTracker(AEPS2HomeActivity.this);
        if(gpsTracker.canGetLocation()){
            double latitude = gpsTracker.getLatitude();
            double longitude = gpsTracker.getLongitude();

            latLong = latitude + "," + longitude;

        }else{
            gpsTracker.showSettingsAlert();
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SdkConstants.REQUEST_FOR_ACTIVITY_BALANCE_ENQUIRY_CODE) {
            hideLoader();
            if (resultCode == RESULT_OK) {
                BankNameModel bankIINValue = (BankNameModel) data.getSerializableExtra(SdkConstants.IIN_KEY);
                bankspinner.setText(bankIINValue.getBankName());
                bankIINNumber = bankIINValue.getIin();
                SdkConstants.bankIIN = bankIINNumber;
                checkBalanceEnquiryValidation();

            }

            checkBalanceEnquiryValidation();

        } else if (requestCode == SdkConstants.REQUEST_FOR_ACTIVITY_CASH_WITHDRAWAL_CODE) {
            hideLoader();
            if (resultCode == RESULT_OK) {
                BankNameModel bankIINValue = (BankNameModel) data.getSerializableExtra(SdkConstants.IIN_KEY);
                bankspinner.setText(bankIINValue.getBankName());
                bankIINNumber = bankIINValue.getIin();
                SdkConstants.bankIIN = bankIINNumber;
                checkWithdrawalValidation();

            }
            checkWithdrawalValidation();
        } else if (requestCode == SdkConstants.REQUEST_CODE) {
            hideLoader();
            if (resultCode == RESULT_OK) {
                Intent respIntent = new Intent();
                respIntent.putExtra(SdkConstants.responseData, SdkConstants.transactionResponse);
                setResult(Activity.RESULT_OK, respIntent);
                finish();

            }
            checkWithdrawalValidation();
        } else if (requestCode == 1) {
            hideLoader();
            //Toast.makeText(this, "I am Here....", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void checkBalanceEnquiryStatus(String status, String message, BalanceEnquiryResponse balanceEnquiryResponse) {
        String aadhar = "";
        if (adharbool == true) {
            aadhar = aadharNumber.getText().toString().trim();
        } else if (virtualbool == true) {
            aadhar = aadharVirtualID.getText().toString().trim();
        }
//        releaseData();
        showLoader();
        finish();
        TransactionStatusModel transactionStatusModel = new TransactionStatusModel();
        if (balanceEnquiryResponse != null) {
            transactionStatusModel.setAadharCard(aadhar);
            transactionStatusModel.setBankName(balanceEnquiryResponse.getBankName());
            transactionStatusModel.setBalanceAmount(balanceEnquiryResponse.getBalance());
            transactionStatusModel.setReferenceNo(balanceEnquiryResponse.getReferenceNo());
            transactionStatusModel.setTransactionType("Balance Enquiry");
            transactionStatusModel.setStatus(balanceEnquiryResponse.getStatus());
            transactionStatusModel.setApiComment(balanceEnquiryResponse.getApiComment());
            transactionStatusModel.setStatusDesc(balanceEnquiryResponse.getStatusDesc());
            transactionStatusModel.setTxnID(balanceEnquiryResponse.getTxId());
            session.setFreshnessFactor(balanceEnquiryResponse.getNextFreshnessFactor());

            Gson g = new Gson();
            String jsonString = g.toJson(transactionStatusModel);
            SdkConstants.transactionResponse = jsonString;//transactionStatusModel.toString().replace("TransactionStatusModel","");
            if (SdkConstants.transactionStatus == true) {
                statusNotification("Success", "Balance Enquiry", TransactionStatusAeps2Activity.class, transactionStatusModel);
                Intent intent = new Intent(AEPS2HomeActivity.this, TransactionStatusAeps2Activity.class);
                intent.putExtra(SdkConstants.TRANSACTION_STATUS_KEY, transactionStatusModel);
                intent.putExtra("MOBILE_NUMBER", mobileNumber.getText().toString().trim());
                hideLoader();
                startActivity(intent);
            } else {
                finish();
            }
        } else {
            transactionStatusModel = null;
            session.setFreshnessFactor(null);
            session.clear();
            showAlert(message);
        }

    }


    @Override
    public void checkBalanceEnquiryAEPS2(String statussss, String message, AepsResponse balanceEnquiryRes) {
        String aadhar = "";
        if (adharbool == true) {
            aadhar = aadharNumber.getText().toString().trim();
        } else if (virtualbool == true) {
            aadhar = aadharVirtualID.getText().toString().trim();
        }
        showLoader();
        finish();
        if(statussss.equalsIgnoreCase("1")){
        TransactionStatusModel transactionStatusModel = new TransactionStatusModel();
                transactionStatusModel.setAadharCard(aadhar);
                try {
                    transactionStatusModel.setBankName(balanceEnquiryRes.getBankName());

                transactionStatusModel.setBalanceAmount(balanceEnquiryRes.getBalance());
                transactionStatusModel.setReferenceNo(balanceEnquiryRes.getRrn());
                transactionStatusModel.setTransactionType("Balance Enquiry");
                transactionStatusModel.setStatus(balanceEnquiryRes.getStatus());
                transactionStatusModel.setApiComment(balanceEnquiryRes.getApiComment());
                transactionStatusModel.setStatusDesc(balanceEnquiryRes.getTransactionStatus());
                transactionStatusModel.setTxnID(balanceEnquiryRes.getTxId());
                session.setFreshnessFactor("");
                 } catch (Exception e) {
                    e.printStackTrace();
                }

                Gson g = new Gson();
                String jsonString = g.toJson(transactionStatusModel);
                SdkConstants.transactionResponse = jsonString;//transactionStatusModel.toString().replace("TransactionStatusModel","");
                if (SdkConstants.transactionStatus == true) {
                    statusNotification("Success", "Balance Enquiry", TransactionStatusAeps2Activity.class, transactionStatusModel);
                    Intent intent = new Intent(AEPS2HomeActivity.this, TransactionStatusAeps2Activity.class);
                    intent.putExtra(SdkConstants.TRANSACTION_STATUS_KEY, transactionStatusModel);
                    intent.putExtra("MOBILE_NUMBER", mobileNumber.getText().toString().trim());
                    //Subhashree
                    hideLoader();
                    startActivity(intent);
                } else {
                    finish();
                }
        }else{


            TransactionStatusModel transactionStatusModel = new TransactionStatusModel();
            transactionStatusModel.setAadharCard(aadhar);
            try {
                transactionStatusModel.setBankName(balanceEnquiryRes.getBankName());

                transactionStatusModel.setBalanceAmount(balanceEnquiryRes.getBalance());
                transactionStatusModel.setReferenceNo(balanceEnquiryRes.getRrn());
                transactionStatusModel.setTransactionType("Balance Enquiry");
                transactionStatusModel.setStatus(balanceEnquiryRes.getStatus());
                transactionStatusModel.setApiComment(balanceEnquiryRes.getApiComment());
                transactionStatusModel.setStatusDesc(balanceEnquiryRes.getTransactionStatus());
                transactionStatusModel.setTxnID(balanceEnquiryRes.getTxId());
                session.setFreshnessFactor("");
            } catch (Exception e) {
                e.printStackTrace();
            }

            Gson g = new Gson();
            String jsonString = g.toJson(transactionStatusModel);
            SdkConstants.transactionResponse = jsonString;//transactionStatusModel.toString().replace("TransactionStatusModel","");
            if (SdkConstants.transactionStatus == true) {
                statusNotification("Success", "Balance Enquiry", TransactionStatusAeps2Activity.class, transactionStatusModel);
                Intent intent = new Intent(AEPS2HomeActivity.this, TransactionStatusAeps2Activity.class);
                intent.putExtra(SdkConstants.TRANSACTION_STATUS_KEY, transactionStatusModel);
                intent.putExtra("MOBILE_NUMBER", mobileNumber.getText().toString().trim());
                //Subhashree
                hideLoader();
                startActivity(intent);
            } else {
                finish();
            }

            //Error
        }

    }
    @Override
    public void checkStatementEnquiryAEPS2(String status, String message, JSONObject statementResponse) {
        String aadhar = "";
        if (adharbool == true) {
            aadhar = aadharNumber.getText().toString().trim();
        } else if (virtualbool == true) {
            aadhar = aadharVirtualID.getText().toString().trim();
        }
        showLoader();
        finish();
            if (statementResponse != null) {
                SdkConstants.transactionResponse = statementResponse.toString();//transactionStatusModel.toString().replace("TransactionStatusModel","");
                Intent intent = new Intent(AEPS2HomeActivity.this, StatementTransactionActivity.class);
                intent.putExtra(SdkConstants.TRANSACTION_STATUS_KEY, statementResponse.toString());
                hideLoader();
                startActivity(intent);
            } else {
                session.setFreshnessFactor(null);
                session.clear();
                showAlert("Internal Server error.");
            }
       // }
    }



    @Override
    public void checkCashWithdrawalStatus(String status, String message, CashWithdrawalResponse cashWithdrawalResponse) {
        String aadhar = "";
        if (adharbool == true) {
            aadhar = aadharNumber.getText().toString().trim();
        } else if (virtualbool == true) {
            aadhar = aadharVirtualID.getText().toString().trim();
        }
        String amount = amountEnter.getText().toString().trim();
//        releaseData();
        showLoader();
        finish();
        TransactionStatusModel transactionStatusModel = new TransactionStatusModel();
        if (cashWithdrawalResponse != null) {
            transactionStatusModel.setAadharCard(aadhar);
            transactionStatusModel.setBankName(cashWithdrawalResponse.getBankName());
            transactionStatusModel.setBalanceAmount(cashWithdrawalResponse.getBalance());
            transactionStatusModel.setReferenceNo(cashWithdrawalResponse.getReferenceNo());
            transactionStatusModel.setTransactionAmount(amount);
            transactionStatusModel.setTransactionType("Cash Withdrawal");
            transactionStatusModel.setStatus(cashWithdrawalResponse.getStatus());
            transactionStatusModel.setApiComment(cashWithdrawalResponse.getApiComment());
            transactionStatusModel.setStatusDesc(cashWithdrawalResponse.getStatusDesc());
            session.setFreshnessFactor(cashWithdrawalResponse.getNextFreshnessFactor());
            transactionStatusModel.setTxnID(cashWithdrawalResponse.getTxId());

            Gson g = new Gson();
            String jsonString = g.toJson(transactionStatusModel);
            SdkConstants.transactionResponse = jsonString;//transactionStatusModel.toString().replace("TransactionStatusModel","");
            if (SdkConstants.transactionStatus == true) {
                statusNotification("Success", "Transaction Amount " + cashWithdrawalResponse.getBalance(), TransactionStatusAeps2Activity.class, transactionStatusModel);
                Intent intent = new Intent(AEPS2HomeActivity.this, TransactionStatusAeps2Activity.class);
                intent.putExtra(SdkConstants.TRANSACTION_STATUS_KEY, transactionStatusModel);
                intent.putExtra("MOBILE_NUMBER", mobileNumber.getText().toString().trim());
                hideLoader();
                startActivity(intent);
            } else {
                finish();
            }
        } else {
            transactionStatusModel = null;
            session.setFreshnessFactor(null);
            session.clear();
            showAlert(message);
        }
    }

    @Override
    public void checkCashWithdrawalAEPS2(String status, String message, AepsResponse cashWithdrawalResponse) {

        try{


        // Toast.makeText(this, "here new", Toast.LENGTH_SHORT).show();
        String aadhar = "";
        if (adharbool == true) {
            aadhar = aadharNumber.getText().toString().trim();
        } else if (virtualbool == true) {
            aadhar = aadharVirtualID.getText().toString().trim();
        }
        showLoader();
        finish();
        String amount = amountEnter.getText().toString().trim();
        //releaseData();
        TransactionStatusModel transactionStatusModel = new TransactionStatusModel();
        if (cashWithdrawalResponse != null) {
            transactionStatusModel.setAadharCard(aadhar);
            transactionStatusModel.setBankName(cashWithdrawalResponse.getBankName());
            transactionStatusModel.setBalanceAmount(cashWithdrawalResponse.getBalance());
            transactionStatusModel.setReferenceNo(cashWithdrawalResponse.getRrn());
            transactionStatusModel.setTransactionAmount(amount);
            transactionStatusModel.setTransactionType("Cash Withdrawal");
            transactionStatusModel.setStatus(cashWithdrawalResponse.getStatus());
            transactionStatusModel.setApiComment(cashWithdrawalResponse.getApiComment());
            transactionStatusModel.setTxnID(cashWithdrawalResponse.getTxId());
            transactionStatusModel.setStatusDesc(message);
            session.setFreshnessFactor("");
        } else {
            transactionStatusModel = null;
            session.setFreshnessFactor(null);
        }
        if (SdkConstants.transactionStatus == true) {
            statusNotification("Success", "Transaction Amount "+cashWithdrawalResponse.getBalance(), TransactionStatusAeps2Activity.class, transactionStatusModel);
            Intent intent = new Intent(AEPS2HomeActivity.this, TransactionStatusAeps2Activity.class);
            intent.putExtra(Constants.TRANSACTION_STATUS_KEY, transactionStatusModel);
            intent.putExtra("MOBILE_NUMBER", mobileNumber.getText().toString().trim());
            //Subhashree
           hideLoader();
            startActivity(intent);
        } else {
            finish();
        }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public void checkEmptyFields() {
        Toast.makeText(AEPS2HomeActivity.this, "Kindly get Registered with AEPS to proceed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showLoader() {
        if (loadingView ==null){
            loadingView = new ProgressDialog(AEPS2HomeActivity.this);
            loadingView.setCancelable(false);
            loadingView.setMessage("Please Wait..");
        }
        loadingView.show();
    }

    @Override
    public void hideLoader() {
        if (loadingView!=null){
            loadingView.dismiss();
        }
    }


    private void checkBalanceEnquiryValidation() {
        // TODO Auto-generated method stub
        if (mobileNumber.getText() != null && !mobileNumber.getText().toString().trim().matches("")
                && Util.isValidMobile(mobileNumber.getText().toString().trim()) == true && bankspinner.getText() != null
                && !bankspinner.getText().toString().trim().matches("")) {

            boolean status = false;
            if (adharbool == true) {
                String aadharNo = aadharNumber.getText().toString();
                if (aadharNo.contains("-")) {
                    aadharNo = aadharNo.replaceAll("-", "").trim();
                    status = Util.validateAadharNumber(aadharNo);
                }
            } else if (virtualbool == true) {
                String aadharVid = aadharVirtualID.getText().toString();
                if (aadharVid.contains("-")) {
                    aadharVid = aadharVid.replaceAll("-", "").trim();
                    status = Util.validateAadharVID(aadharVid);
                }
            }
            if (status) {

            }
        } else {
            submitButton.setEnabled(false);
            submitButton.setBackground(getResources().getDrawable(R.drawable.button_submit));
        }

    }


    private void checkWithdrawalValidation() {
        // TODO Auto-generated method stub
        if (mobileNumber.getText() != null
                && !mobileNumber.getText().toString().trim().matches("")
                && Util.isValidMobile(mobileNumber.getText().toString().trim()) == true
                && mobileNumber.getText().toString().length() == 10
                && bankspinner.getText() != null
                && !bankspinner.getText().toString().trim().matches("")
                && amountEnter.getText() != null
                && !amountEnter.getText().toString().trim().matches("")) {

            boolean status = false;
            if (adharbool == true) {
                String aadharNo = aadharNumber.getText().toString();
                if (aadharNo.contains("-")) {
                    aadharNo = aadharNo.replaceAll("-", "").trim();
                    status = Util.validateAadharNumber(aadharNo);
                }
            } else if (virtualbool == true) {
                String aadharVid = aadharVirtualID.getText().toString();
                if (aadharVid.contains("-")) {
                    aadharVid = aadharVid.replaceAll("-", "").trim();
                    status = Util.validateAadharVID(aadharVid);
                }
            }
            if (status) {

            }
        } else {
            submitButton.setEnabled(false);
            submitButton.setBackground(getResources().getDrawable(R.drawable.button_submit));
        }

    }





    TextWatcher mWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            // TODO Auto-generated method stub
            checkBalanceEnquiryValidation();

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            // TODO Auto-generated method stub

        }

        @Override
        public void afterTextChanged(Editable s) {
            // TODO Auto-generated method stub
        }
    };
    TextWatcher cashWithdrawalWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            // TODO Auto-generated method stub
            checkWithdrawalValidation();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            // TODO Auto-generated method stub

        }

        @Override
        public void afterTextChanged(Editable s) {
            // TODO Auto-generated method stub

        }
    };


    //-----------RAJESH---------------
    @Override
    protected void onResume() {
        super.onResume();
        hideKeyboard();
        if(flagFromDriver){
            if(SdkConstants.RECEIVE_DRIVER_DATA.isEmpty() || SdkConstants.RECEIVE_DRIVER_DATA.equalsIgnoreCase("")){

               // fingerprint.setImageDrawable(getResources().getDrawable(R.drawable.ic_scanner));
                theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
                int color = typedValue.data;
                fingerprint.setColorFilter(color);
                fingerprint.setEnabled(true);
                submitButton.setBackgroundResource(R.drawable.button_submit);
                submitButton.setEnabled(false);
            } else if (balanaceInqueryAadharNo.equalsIgnoreCase("") || balanaceInqueryAadharNo.isEmpty()) {
                aadharNumber.setError("Enter Aadhar No.");
                fingerStrength();
            } else if (mobileNumber.getText().toString().isEmpty() || mobileNumber.getText().toString().equalsIgnoreCase("")) {
                mobileNumber.setError("Enter mobile no.");
                fingerStrength();
            } else if (bankspinner.getText().toString().isEmpty() || bankspinner.getText().toString().trim().equalsIgnoreCase("")) {
                bankspinner.setError("Choose your bank.");
                fingerStrength();
            } else {
                fingerStrength();
                //fingerprint.setImageDrawable(getResources().getDrawable(R.drawable.F));
                fingerprint.setColorFilter(R.color.colorGrey);
                fingerprint.setEnabled(false);
                submitButton.setEnabled(true);
                submitButton.setBackgroundResource(R.drawable.button_submit_blue);
            }

        }

    }

    public void hideKeyboard() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }



    //Finger Strength
    public void fingerStrength() {
        try {
            JSONObject respObj = new JSONObject(SdkConstants.RECEIVE_DRIVER_DATA);
            String scoreStr = respObj.getString("pidata_qscore");


            if (Float.parseFloat(scoreStr) <= 40) {
                depositBar.setVisibility(View.VISIBLE);
                depositBar.setProgress(Float.parseFloat(scoreStr));
                depositBar.setProgressTextMoved(true);
                depositBar.setEndColor(getResources().getColor(R.color.red));
                depositBar.setStartColor(getResources().getColor(R.color.red));
                depositNote.setVisibility(View.VISIBLE);
                fingerprintStrengthDeposit.setVisibility(View.VISIBLE);
            } /*else if (Float.parseFloat(scoreStr) >= 30 && Float.parseFloat(scoreStr) <= 60) {

                depositBar.setVisibility(View.VISIBLE);
                depositBar.setProgress(Float.parseFloat(scoreStr));
                depositBar.setProgressTextMoved(true);
                depositBar.setEndColor(getResources().getColor(R.color.yellow));
                depositBar.setStartColor(getResources().getColor(R.color.yellow));
                depositNote.setVisibility(View.VISIBLE);
                fingerprintStrengthDeposit.setVisibility(View.VISIBLE);
            }*/ else {

                depositBar.setVisibility(View.VISIBLE);
                depositBar.setProgress(Float.parseFloat(scoreStr));
                depositBar.setProgressTextMoved(true);
                depositBar.setEndColor(getResources().getColor(R.color.green));
                depositBar.setStartColor(getResources().getColor(R.color.green));
                depositNote.setVisibility(View.VISIBLE);
                fingerprintStrengthDeposit.setVisibility(View.VISIBLE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void releaseData() {
        amountEnter.setText(null);
        amountEnter.setError(null);
        aadharNumber.setText(null);
        aadharNumber.setError(null);
        mobileNumber.setText(null);
        mobileNumber.setError(null);
        bankspinner.setText(null);
        bankspinner.setError(null);
        bankIINNumber = "";
        balanceEnquiryaeps2RequestModel = null;
        cashWithdrawalaeps2RequestModel = null;
        depositBar.setVisibility(View.GONE);
        depositNote.setVisibility(View.GONE);
        fingerprintStrengthDeposit.setVisibility(View.GONE);
        fingerprintStrengthDeposit.setVisibility(View.GONE);
    }


    private void getUserAuthToken() {
        String url = SdkConstants.BASE_URL + "/api/getAuthenticateData";
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

                                if (status.equalsIgnoreCase("success")) {
                                    String userName = obj.getString("username");
                                    String userToken = obj.getString("usertoken");
                                    SdkConstants.userNameFromCoreApp = userName;
                                    session.setUsername(userName);
                                    session.setUserToken(userToken);
                                    hideLoader();

                                } else {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkVPNstatusForTransaction(final String transaction_type) {
        AndroidNetworking.get("https://vpn.iserveu.online/vpn/telnet_checkVpn")
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // stopProgressDialog();
                        try {
                            JSONObject obj = new JSONObject(response.toString());
                            String status = obj.getString("status");
                            String statusDesc = obj.getString("statusDesc");
                            if (status.equalsIgnoreCase("0")) {
                                if (transaction_type.equalsIgnoreCase("Cash")) {
                                    //check Velocity
                                    cashWithdrawalPresenter.performCashWithdrawalAEPS2(session.getUserToken(), cashWithdrawalaeps2RequestModel);
                                } else if (transaction_type.equalsIgnoreCase("Balance")) {
                                    balanceEnquiryPresenter.performBalanceEnquiryAEPS2(session.getUserToken(), balanceEnquiryaeps2RequestModel, balanceEnquiryExpandButton.getText().toString());

                                } else {
                                    showUserOnboardStatus("Sorry, something went wrong. Please try after sometimes.");
                                }
                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                            hideLoader();
                        }


                    }

                    @Override
                    public void onError(ANError anError) {
                        anError.getErrorBody();
                        hideLoader();
                    }
                });
    }
//check transaction

    private void checkVPNstatusForTransactionCashWithdraw(final String transaction_type, String checkCard) {
        // runProgressDialog();
        AndroidNetworking.get("https://vpn.iserveu.online/vpn/telnet_checkVpn")
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // stopProgressDialog();
                        try {
                            // {"statusDesc":"connected","status":"0"}
                            JSONObject obj = new JSONObject(response.toString());
                            String status = obj.getString("status");
                            String statusDesc = obj.getString("statusDesc");
                            if (status.equalsIgnoreCase("0")) {
                                if (transaction_type.equalsIgnoreCase("Cash")) {
                                    //check Velocity
                                    ValidateAeps2Transaction(checkCard);
                                } else if (transaction_type.equalsIgnoreCase("Balance")) {
                                    balanceEnquiryPresenter.performBalanceEnquiryAEPS2(session.getUserToken(), balanceEnquiryaeps2RequestModel, balanceEnquiryExpandButton.getText().toString());

                                } else {
                                    showUserOnboardStatus("Sorry, something went wrong. Please try after sometimes.");
                                }
                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                            hideLoader();
                        }


                    }

                    @Override
                    public void onError(ANError anError) {
                        anError.getErrorBody();
                        hideLoader();
                    }
                });
    }

    public void ValidateAeps2Transaction(String cardAadhar) {

        String aadhar_sha = Util.getSha256Hash(cardAadhar);
        Constants.AADHAR_CARD = cardAadhar;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("retailerId", session.getUserName());
            jsonObject.put("aadhaar_card", aadhar_sha);
            jsonObject.put("amount", amountEnter.getText().toString().trim());

            AndroidNetworking.post("https://us-central1-creditapp-29bf2.cloudfunctions.net/isuApi/aeps/validate_aeps")
                    .addJSONObjectBody(jsonObject)
                    .setPriority(Priority.HIGH)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONObject obj = new JSONObject(response.toString());
                                String statusString = obj.getString("status");
                                if (statusString.equalsIgnoreCase("0")) {
                                    String statusMsg = obj.getString("errorMessage");
                                    Util.showAlert(AEPS2HomeActivity.this, "Alert", statusMsg);
                                } else {
                                    //Allow Transaction
                                    cashWithdrawalPresenter.performCashWithdrawalAEPS2(session.getUserToken(), cashWithdrawalaeps2RequestModel);

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                hideLoader();
                                Util.showAlert(AEPS2HomeActivity.this, "Alert", "Velocity api is not working due to some technical issue ..");


                            }
                        }

                        @Override
                        public void onError(ANError anError) {
                            anError.getErrorBody();
                            Util.showAlert(AEPS2HomeActivity.this, "Alert", "Velocity api is not working due to some technical issue ..");

                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showUserOnboardStatus(final String statusDesc) {


        AlertDialog.Builder builder1 = new AlertDialog.Builder(AEPS2HomeActivity.this);
        builder1.setMessage(statusDesc);
        builder1.setTitle("Alert");
        builder1.setCancelable(false);
        builder1.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();

    }

    public void showAlert(String msg) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(AEPS2HomeActivity.this);
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

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String makePrettyString(String string) {
        String number = string.replaceAll("-", "");
        boolean isEndHyphen = string.endsWith("-") && (number.length() % 4 == 0);
        return number.replaceAll("(.{4}(?!$))", "$1-") + (isEndHyphen ? "-" : "");
    }

    private int getCursorPos(String oldString, String newString, int oldPos, boolean isDeleteHyphen) {
        int cursorPos = newString.length();
        if (oldPos != oldString.length()) {
            String stringWithMarker = oldString.substring(0, oldPos) + MARKER + oldString.substring(oldPos);

            cursorPos = (makePrettyString(stringWithMarker)).indexOf(MARKER);
            if (isDeleteHyphen)
                cursorPos -= 1;
        }
        return cursorPos;
    }

    public void statusNotification(String title, String body, Class intnetClass, TransactionStatusModel raw) {
        if (SdkConstants.showNotification) {
            NotificationCompat.Builder builder = notificationHelper.createTransactionStatusNotif(this,
                    title,
                    body,
                    intnetClass,
                    raw);

            if (builder != null) {
//                notificationHelper.create(0, builder);
            }
        }
    }


}

