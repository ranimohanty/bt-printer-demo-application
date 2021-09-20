package com.matm.matmsdk.matm1;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.matm.matmsdk.Bluetooth.BluetoothConnectorActivity;
import com.matm.matmsdk.Bluetooth.BluetoothPrinter;
import com.matm.matmsdk.FileUtils;
import com.matm.matmsdk.Utils.getToneGenerator;
import com.matm.matmsdk.Utils.SdkConstants;
import com.matm.matmsdk.aepsmodule.transactionstatus.TransactionStatusAeps2Activity;
import com.matm.matmsdk.aepsmodule.utils.GetPosConnectedPrinter;
import com.matm.matmsdk.aepsmodule.utils.Util;
import com.matm.matmsdk.btprinter.AEMPrinter;
import com.matm.matmsdk.btprinter.AEMScrybeDevice;
import com.matm.matmsdk.btprinter.CardReader;
import com.matm.matmsdk.permission.PermissionsActivity;
import com.matm.matmsdk.permission.PermissionsChecker;
import com.matm.matmsdk.readfile.PreviewPDFActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import isumatm.androidsdk.equitas.R;

import static com.matm.matmsdk.permission.PermissionsActivity.PERMISSION_REQUEST_CODE;
import static com.matm.matmsdk.permission.PermissionsChecker.REQUIRED_PERMISSION;

public class Matm1TransactionStatusActivity extends AppCompatActivity {

    String EnquiryStatus, RRN, CardNumber, AvailableBalance, TransactionDatetime, AccountNo = "N/A", TerminalID, TxnStatus,TxnAmount;
    Button closeBtn,downloadBtn,printBtn;
    TextView card_amount,date_time,bank_name,balanceText;
    Button txndetails;
    PermissionsChecker checker;
    Context mContext;
    BluetoothAdapter B;
    ProgressDialog progressDialog;
    CheckBox mobileCheckBox;
    LinearLayout mobileEditLayout;
    ImageView sendButton;
    EditText editTextMobile;
     BluetoothDevice bluetoothDevice;
     String transactionTypeCheck;


    AEMScrybeDevice m_AemScrybeDevice;
    AEMPrinter m_AemPrinter = null;
    CardReader m_cardReader = null;
    CardReader.CARD_TRACK cardTrackType;
    String creditData,tempdata,replacedData,data;
    ArrayList<String> printerList;
    String responseString,response;
    int numChars ;
    String[] responseArray = new String[1];
    char[] printerStatus=new char[]{0x1B,0x7E,0x42,0x50,0x7C,0x47,0x45,0x54,0x7C,0x50,0x52,0x4E,0x5F,0x53,0x54,0x5E};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matm1_transaction_status);
        checker = new PermissionsChecker(this);
        mContext = getApplicationContext();
        B=BluetoothAdapter.getDefaultAdapter();

        Bundle bundle = getIntent().getExtras();

        new getToneGenerator();

        if (bundle != null) {
            if(getIntent().hasExtra("BalanceEnquiryStatus")){
                EnquiryStatus = bundle.getString("BalanceEnquiryStatus");
                AccountNo = bundle.getString("AccountNo");
                if(AccountNo.equalsIgnoreCase("") || AccountNo.equalsIgnoreCase(null) || AccountNo.equalsIgnoreCase("null")){
                    AccountNo = "N/A";
                }
                transactionTypeCheck = "Balance Enquiry";
            }else {
                TxnStatus = bundle.getString("TxnStatus");
                TxnAmount = bundle.getString("TxnAmount");
                TxnAmount = getDecimalString(TxnAmount);
                transactionTypeCheck = "Cash Withdrawal";
            }
            RRN = bundle.getString("RRN");
            CardNumber = bundle.getString("CardNumber");
            AvailableBalance = bundle.getString("AvailableBalance");
            AvailableBalance = getDecimalString(AvailableBalance);
            TransactionDatetime = bundle.getString("TransactionDatetime");
            TerminalID = bundle.getString("TerminalID");

        }

        card_amount = findViewById(R.id.card_amount);
        date_time = findViewById(R.id.date_time);
        bank_name = findViewById(R.id.bank_name);
        balanceText = findViewById(R.id.balanceText);
        txndetails = findViewById(R.id.txndetailsBtn);
        mobileCheckBox = findViewById(R.id.mobileCheckBox);
        mobileEditLayout = findViewById(R.id.mobileEditLayout);
        editTextMobile = findViewById(R.id.editTextMobile);
        sendButton = findViewById(R.id.sendButton);

        closeBtn = findViewById(R.id.closeBtn);
        downloadBtn = findViewById(R.id.downloadBtn);
        printBtn = findViewById(R.id.printBtn);

        if(getIntent().hasExtra("BalanceEnquiryStatus")){
            balanceText.setText(EnquiryStatus);
            card_amount.setText("Available Bal : Rs. " + AvailableBalance);
        }else {
            balanceText.setText(TxnStatus);
            card_amount.setText("Txn Amt : Rs. " + TxnAmount);

        }
        bank_name.setText(RRN);
        date_time.setText(TransactionDatetime);

        closeBtn.setOnClickListener(v -> finish());

        txndetails.setOnClickListener(v -> showTransactionDetails(Matm1TransactionStatusActivity.this));

        downloadBtn.setOnClickListener(v -> {
            if (checker.lacksPermissions(REQUIRED_PERMISSION)) {
                PermissionsActivity.startActivityForResult(Matm1TransactionStatusActivity.this, PERMISSION_REQUEST_CODE, REQUIRED_PERMISSION);
            } else {
                Date date = new Date();
                long timeMilli = date.getTime();
                System.out.println("Time in milliseconds using Date class: " + String.valueOf(timeMilli));
                createPdf(FileUtils.getAppPath(mContext) + String.valueOf(timeMilli)+"Order_Receipt.pdf");
            }
        });

        printBtn.setOnClickListener(v -> {
                    registerForContextMenu(printBtn);
                    if (GetPosConnectedPrinter.aemPrinter == null) {
                        printerList = m_AemScrybeDevice.getPairedPrinters();

                        if (printerList.size() > 0) {
                            openContextMenu(v);
                        } else {
                            showAlert("No Paired Printers found");
                        }
                    } else {
                        m_AemPrinter = GetPosConnectedPrinter.aemPrinter;
                        callBluetoothFunction(TxnStatus, CardNumber, TxnAmount,RRN,AvailableBalance,TransactionDatetime, TerminalID);
                    }
//            if (SdkConstants.Bluetoothname.equalsIgnoreCase("ESIAF3996")) {
//                SdkConstants.bluetoothDevice = null;
//                SdkConstants.Bluetoothname = "null";
//            }else if(SdkConstants.Bluetoothname.contains("BPFS")){
//                SdkConstants.bluetoothDevice = null;
//                SdkConstants.Bluetoothname = "null";
//            }
//            else {
//                bluetoothDevice = SdkConstants.bluetoothDevice;
//            }
//
//            if (bluetoothDevice != null) {
//
//                if (!B.isEnabled()) {
//
//                    Toast.makeText(getApplicationContext(), "Your Bluetooth is OFF .",Toast.LENGTH_LONG).show();
//                } else {
//                    if(getIntent().hasExtra("BalanceEnquiryStatus")){
//                        callBluetoothFunction(EnquiryStatus, CardNumber, AccountNo,RRN,
//                                AvailableBalance,TransactionDatetime, TerminalID,bluetoothDevice);
//                    }else {
//                        callBluetoothFunction(TxnStatus, CardNumber, TxnAmount,RRN,
//                                AvailableBalance,TransactionDatetime, TerminalID,bluetoothDevice);
//                    }
//
//                }
//
//            } else {
//                Intent in = new Intent(getApplicationContext(), BluetoothConnectorActivity.class);
//                startActivity(in);
////                Toast.makeText(getApplicationContext(), "Please connect the printer",Toast.LENGTH_LONG).show();
//            }


        });

        mobileCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mobileEditLayout.setVisibility(View.VISIBLE);
                } else {
                    mobileEditLayout.setVisibility(View.GONE);
                }
            }
        });

        editTextMobile.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() < 10) {
                    editTextMobile.setError(getResources().getString(R.string.mobileerror));
                }
                if (s.length() > 0) {
                    editTextMobile.setError(null);
                    String x = s.toString();
                    if (x.startsWith("0") || !Util.isValidMobile(editTextMobile.getText().toString().trim())) {
                        editTextMobile.setError(getResources().getString(R.string.mobilevaliderror));
                    }
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextMobile.getText() == null || editTextMobile.getText().toString().trim().matches("") ||
                        !Util.isValidMobile(editTextMobile.getText().toString().trim())) {

                    editTextMobile.setError(getResources().getString(R.string.mobileerror));
                } else {
                    showLoader();
                    mobileNumberSMS();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void createPdf(String dest) {

        if (new File(dest).exists()) {
            new File(dest).delete();
        }

        try {
            /**
             * Creating Document
             */
            Document document = new Document();

            // Location to save
            PdfWriter.getInstance(document, new FileOutputStream(dest));

            // Open to write
            document.open();

            // Document Settings
            document.setPageSize(PageSize.A3);
            document.addCreationDate();
            document.addAuthor("");
            document.addCreator("");
            Rectangle rect = new Rectangle(577, 825, 18, 15);
            rect.enableBorderSide(1);
            rect.enableBorderSide(2);
            rect.enableBorderSide(4);
            rect.enableBorderSide(8);
            rect.setBorder(Rectangle.BOX);
            rect.setBorderWidth(2);
            rect.setBorderColor(BaseColor.BLACK);
            document.add(rect);

            BaseColor mColorAccent = new BaseColor(0, 153, 204, 255);
            float mHeadingFontSize = 24.0f;
            float mValueFontSize = 26.0f;

            /**
             * How to USE FONT....
             */
            BaseFont urName = BaseFont.createFont("assets/fonts/brandon_medium.otf", "UTF-8", BaseFont.EMBEDDED);

            // LINE SEPARATOR
            LineSeparator lineSeparator = new LineSeparator();
            lineSeparator.setLineColor(new BaseColor(0, 0, 0, 68));

            BaseFont bf = BaseFont.createFont(
                    BaseFont.TIMES_ROMAN,
                    BaseFont.CP1252,
                    BaseFont.EMBEDDED);
            Font font = new Font(bf, 30);
            Font font2 = new Font(bf, 26);


            Font mOrderDetailsTitleFont = new Font(urName, 36.0f, Font.NORMAL, BaseColor.BLACK);
            Chunk mOrderDetailsTitleChunk = new Chunk(SdkConstants.SHOP_NAME, mOrderDetailsTitleFont);
            Paragraph mOrderDetailsTitleParagraph = new Paragraph(mOrderDetailsTitleChunk);
            mOrderDetailsTitleParagraph.setAlignment(Element.ALIGN_CENTER);
            document.add(mOrderDetailsTitleParagraph);
            document.add(new Paragraph("\n"));

            Font mOrderShopTitleFont = new Font(urName, 25.0f, Font.NORMAL, BaseColor.BLACK);
            Chunk mOrderShopTitleChunk = new Chunk("Receipt", mOrderShopTitleFont);
            Paragraph mOrderShopTitleParagraph = new Paragraph(mOrderShopTitleChunk);
            mOrderShopTitleParagraph.setAlignment(Element.ALIGN_CENTER);
            document.add(mOrderShopTitleParagraph);
            document.add(new Paragraph("\n"));

            Font mOrderDetailsTitleFont11;

                mOrderDetailsTitleFont11 = new Font(urName, 40.0f, Font.NORMAL, BaseColor.GREEN);


            Chunk mOrderDetailsTitleChunk1 = new Chunk("Success", mOrderDetailsTitleFont11);
            Paragraph mOrderDetailsTitleParagraph1 = new Paragraph(mOrderDetailsTitleChunk1);
            mOrderDetailsTitleParagraph1.setAlignment(Element.ALIGN_CENTER);
            document.add(mOrderDetailsTitleParagraph1);
            document.add(new Paragraph("\n"));


            Font mOrderDateFont = new Font(urName, mHeadingFontSize, Font.NORMAL, mColorAccent);
            Font mOrderDateValueFont = new Font(urName, mValueFontSize, Font.NORMAL, BaseColor.BLACK);

            Paragraph p = new Paragraph();
            p.add(new Chunk("Date/Time : ", mOrderDateFont));
            p.add(new Chunk(date_time.getText().toString().trim(),mOrderDateValueFont));
            document.add(p);
            document.add(new Paragraph("\n"));

            Paragraph p1 = new Paragraph();
            p1.add(new Chunk("Operation Performed : ", mOrderDateFont));
            p1.add(new Chunk("mATM 1",mOrderDateValueFont));
            document.add(p1);

            document.add(new Paragraph("\n"));

            Font mOrderDetailsFont = new Font(urName, 30.0f, Font.BOLD,mColorAccent );
            Chunk mOrderDetailsChunk = new Chunk("Transaction Details", mOrderDetailsFont);
            Paragraph mOrderDetailsParagraph = new Paragraph(mOrderDetailsChunk);
            mOrderDetailsParagraph.setAlignment(Element.ALIGN_CENTER);
            document.add(mOrderDetailsParagraph);
            document.add(new Paragraph("\n"));
            // document.add(new Chunk(lineSeparator));
            Font mOrderIdFont = new Font(urName, mValueFontSize, Font.NORMAL, BaseColor.BLACK);
            // Fields of Order Details...
            // Adding Chunks for Title and value
            if(getIntent().hasExtra("BalanceEnquiryStatus")){

                Chunk mOrderIdChunk = new Chunk("Account No: " + AccountNo, mOrderIdFont);
                Paragraph mOrderTxnParagraph = new Paragraph(mOrderIdChunk);
                document.add(mOrderTxnParagraph);
            }else {

                Chunk mOrdertxnAmtChunk = new Chunk("Transaction Amount: " + TxnAmount, mOrderIdFont);
                Paragraph mOrdertxnAmtParagraph = new Paragraph(mOrdertxnAmtChunk);
                document.add(mOrdertxnAmtParagraph);
            }

            Chunk mBankNameChunk = new Chunk("Terminal ID:  " + TerminalID, mOrderIdFont);
            Paragraph mBankNameParagraph = new Paragraph(mBankNameChunk);
            document.add(mBankNameParagraph);
            Chunk mOrderrrnChunk = new Chunk("RRN No.: " + RRN, mOrderIdFont);
            Paragraph mOrderrnParagraph = new Paragraph(mOrderrrnChunk);
            document.add(mOrderrnParagraph);
            Chunk mOrdertxnTypeChunk = new Chunk("Card No.: " + CardNumber, mOrderIdFont);
            Paragraph mOrdertxnTypeParagraph = new Paragraph(mOrdertxnTypeChunk);
            document.add(mOrdertxnTypeParagraph);
            Chunk mOrdertxnType1Chunk = new Chunk("Transaction Type: " + transactionTypeCheck, mOrderIdFont);
            Paragraph mOrdertxnType1Paragraph = new Paragraph(mOrdertxnType1Chunk);
            document.add(mOrdertxnType1Paragraph);
            Chunk mOrderbalanceChunk = new Chunk("Available Balance: " + AvailableBalance, mOrderIdFont);
            Paragraph mOrderbalanceParagraph = new Paragraph(mOrderbalanceChunk);
            document.add(mOrderbalanceParagraph);



            Font mOrderAcNameFont = new Font(urName, mHeadingFontSize, Font.NORMAL, mColorAccent);
            Chunk mOrderAcNameChunk = new Chunk("Thank You", mOrderAcNameFont);
            Paragraph mOrderAcNameParagraph = new Paragraph(mOrderAcNameChunk);
            mOrderAcNameParagraph.setAlignment(Element.ALIGN_RIGHT);
            document.add(mOrderAcNameParagraph);
            Font mOrderAcNameValueFont = new Font(urName, mValueFontSize, Font.NORMAL, BaseColor.BLACK);
            Chunk mOrderAcNameValueChunk = new Chunk(SdkConstants.BRAND_NAME, mOrderAcNameValueFont);
            Paragraph mOrderAcNameValueParagraph = new Paragraph(mOrderAcNameValueChunk);
            mOrderAcNameValueParagraph.setAlignment(Element.ALIGN_RIGHT);
            document.add(mOrderAcNameValueParagraph);


            document.close();

            Toast.makeText(mContext, "PDF saved in the internal storage", Toast.LENGTH_SHORT).show();

            //  FileUtils.openFile(mContext, new File(dest));
            Intent intent = new Intent(Matm1TransactionStatusActivity.this, PreviewPDFActivity.class);
            intent.putExtra("filePath",dest);
            startActivity(intent);

        } catch (IOException | DocumentException ie) {
            Log.e("createPdf: Error ","" + ie.getLocalizedMessage());
        } catch (ActivityNotFoundException ae) {
            Toast.makeText(mContext, "No application found to open this file.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == PermissionsActivity.PERMISSIONS_GRANTED) {
            Toast.makeText(mContext, "Permission Granted to Save", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext, "Permission not granted, Try again!", Toast.LENGTH_SHORT).show();
        }
    }


    private void callBluetoothFunction(final String BalanceEnquiryStatus, final String CardNumber, final String AccountNo,final String RRN,final String AvailableBalance,
                                       final String TransactionDatetime,final String TerminalID) {

        try {
            m_AemPrinter.setFontType(AEMPrinter.DOUBLE_HEIGHT);
            m_AemPrinter.setFontType(AEMPrinter.TEXT_ALIGNMENT_CENTER);
            m_AemPrinter.print("-----Transaction Report-----\n");
            m_AemPrinter.setFontType(AEMPrinter.TEXT_ALIGNMENT_CENTER);

            if(getIntent().hasExtra("BalanceEnquiryStatus")){
                m_AemPrinter.print("Success\n\n");
                m_AemPrinter.print("Account No : " + AccountNo);
            }else {
                m_AemPrinter.print("Success\n\n");
                m_AemPrinter.print("Txn Amount : " + TxnAmount);
            }
            m_AemPrinter.print("Card No.: " + CardNumber);
            m_AemPrinter.print("Ref No.: " + RRN);
            m_AemPrinter.print("Available Balance : " + AvailableBalance);
            m_AemPrinter.print("Transaction Type : " + transactionTypeCheck);
            m_AemPrinter.print("Date/Time : " + TransactionDatetime);
            m_AemPrinter.print("Terminal ID : " + TerminalID);

            m_AemPrinter.setFontType(AEMPrinter.TEXT_ALIGNMENT_CENTER);
            m_AemPrinter.print("Thank You\n");
            m_AemPrinter.setFontType(AEMPrinter.TEXT_ALIGNMENT_RIGHT);
            m_AemPrinter.print(SdkConstants.BRAND_NAME);
            m_AemPrinter.print("-----------------------------------");


        } catch (IOException e) {
            e.printStackTrace();
        }

//        final BluetoothPrinter mPrinter = new BluetoothPrinter(bluetoothDevice);
//        mPrinter.connectPrinter(new BluetoothPrinter.PrinterConnectListener() {
//
//            @Override
//            public void onConnected() {
//                mPrinter.addNewLine();
//                mPrinter.setAlign(BluetoothPrinter.ALIGN_CENTER);
//                mPrinter.setBold(true);
//                mPrinter.addNewLine();
//                mPrinter.setAlign(BluetoothPrinter.ALIGN_CENTER);
//                mPrinter.printText("-----Transaction Report-----");
//                mPrinter.addNewLine();
//                mPrinter.setAlign(BluetoothPrinter.ALIGN_CENTER);
//                mPrinter.setBold(true);
//
//                if(getIntent().hasExtra("BalanceEnquiryStatus")){
//                    mPrinter.printText("Success");
//                    mPrinter.addNewLine();
//                    mPrinter.addNewLine();
//                    mPrinter.printText("Account No : " + AccountNo);
//                    mPrinter.addNewLine();
//                }else {
//                    mPrinter.printText("Success");
//                    mPrinter.addNewLine();
//                    mPrinter.addNewLine();
//                    mPrinter.printText("Txn Amount : " + TxnAmount);
//                    mPrinter.addNewLine();
//                }
//                mPrinter.printText("Card No.: " + CardNumber);
//                mPrinter.addNewLine();
//                mPrinter.printText("Ref No.: " + RRN);
//                mPrinter.addNewLine();
//                mPrinter.printText("Available Balance : " + AvailableBalance);
//                mPrinter.addNewLine();
//                mPrinter.printText("Transaction Type : " + transactionTypeCheck);
//                mPrinter.addNewLine();
//                mPrinter.printText("Date/Time : " + TransactionDatetime);
//                mPrinter.addNewLine();
//                mPrinter.printText("Terminal ID : " + TerminalID);
//                mPrinter.addNewLine();
//                mPrinter.addNewLine();
//                mPrinter.setAlign(BluetoothPrinter.ALIGN_RIGHT);
//                mPrinter.printText("Thank You");
//                mPrinter.addNewLine();
//                mPrinter.setAlign(BluetoothPrinter.ALIGN_RIGHT);
//                mPrinter.printText(SdkConstants.BRAND_NAME);
//                mPrinter.addNewLine();
//                mPrinter.addNewLine();
//                mPrinter.addNewLine();
//                mPrinter.printText("-----------------------------------");
//                mPrinter.addNewLine();
//                mPrinter.addNewLine();
//                mPrinter.finish();
//            }
//
//            @Override
//            public void onFailed() {
//                Log.d("BluetoothPrinter", "Connection failed");
//                // finish();
//                Toast.makeText(Matm1TransactionStatusActivity.this, "Please switch on bluetooth printer", Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    public void showTransactionDetails(Activity activity) {
        try {
            final Dialog dialog = new Dialog(activity);
            Window window = dialog.getWindow();
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.transaction_matm1_details_layout);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

            TextView tvStatus = dialog.findViewById(R.id.tvStatus);
            TextView tvCardNo = dialog.findViewById(R.id.tvCardNo);
            TextView tvAccountNo = dialog.findViewById(R.id.tvAccountNo);
            TextView tvRef = dialog.findViewById(R.id.tvRef);
            TextView availableBalance = dialog.findViewById(R.id.availableBalance);
            TextView tvBalance = dialog.findViewById(R.id.tvBalance);
            TextView txnType = dialog.findViewById(R.id.txnType);
            TextView tvTerminalId = dialog.findViewById(R.id.tvTerminalId);
            TextView tvAcc = dialog.findViewById(R.id.tvAcc);
            Button dialogBtn_close = dialog.findViewById(R.id.close_Btn);

            if(getIntent().hasExtra("BalanceEnquiryStatus")){
                tvAcc.setText("ACCOUNT NO.");
                tvAccountNo.setText(AccountNo);
                tvStatus.setText(EnquiryStatus);
                txnType.setText("Balance Enquiry");
                availableBalance.setText("Transaction Amount");
                tvBalance.setText("N/A");
                transactionTypeCheck = txnType.getText().toString();
            }else {
                tvAcc.setText("TXN AMOUNT");
                tvAccountNo.setText(TxnAmount);
                tvStatus.setText(TxnStatus);
                txnType.setText("Cash Withdrawal");
                tvBalance.setText(AvailableBalance);
                transactionTypeCheck = txnType.getText().toString();
            }
            tvCardNo.setText(CardNumber);
            tvRef.setText(RRN);

            tvTerminalId.setText(TerminalID);

            dialogBtn_close.setOnClickListener(v -> dialog.cancel());
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mobileNumberSMS() {
        String msgValue = "Thanks for visiting " + SdkConstants.SHOP_NAME + ". Current balance for " + CardNumber +
              " is Rs " + AvailableBalance + ". Dated " + date_time.getText().toString() + ".";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_name", SdkConstants.userNameFromCoreApp);
            jsonObject.put("MobileNumber", editTextMobile.getText().toString());
            jsonObject.put("smsFor", "transaction");
            jsonObject.put("message", msgValue);
            AndroidNetworking.post("https://wallet-deduct-sms-vn3k2k7q7q-uc.a.run.app/")
                    .addJSONObjectBody(jsonObject)
                    .setPriority(Priority.HIGH)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONObject obj = new JSONObject(response.toString());
                                String status = obj.getString("status");
                                String msg = obj.optString("message");
                                if (status.equalsIgnoreCase("0")) {
                                    JSONObject results = obj.getJSONObject("results");
                                    String statusMsg = results.getString("status");
                                    String message = results.getString("message");
                                    hideLoader();
                                    Toast.makeText(Matm1TransactionStatusActivity.this, "Message Sent Successfully.", Toast.LENGTH_SHORT).show();
                                } else {
                                    hideLoader();
                                    Toast.makeText(Matm1TransactionStatusActivity.this, msg, Toast.LENGTH_SHORT).show();
                                }

                            } catch (JSONException e) {
                                hideLoader();
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onError(ANError anError) {
                            anError.getErrorBody();
                            hideLoader();
                            Toast.makeText(Matm1TransactionStatusActivity.this, "Wallet balance not available", Toast.LENGTH_SHORT).show();

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showLoader() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(Matm1TransactionStatusActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Please Wait..");
        }
        progressDialog.show();
    }
    public void hideLoader() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    public String getDecimalString(String s) {
        String d = "N/A";
        if (s != null && !s.contains(" ") && !s.equals("")) {
            if (s.contains(".")) {
                int index = s.lastIndexOf(".");
                int size = s.length() - 1;
                if (index == size) {
                    d = s + "00";
                } else if (index == size - 1) {
                    d = s + "0";
                } else {
                    d = s;
                }
            } else {
                d = s + ".00";
            }
        }
        return d;
    }

    public void showAlert(String alertMsg)
    {
        android.app.AlertDialog.Builder alertBox = new android.app.AlertDialog.Builder(Matm1TransactionStatusActivity.this);
        alertBox.setMessage(alertMsg).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                return;
            }
        });

        android.app.AlertDialog alert = alertBox.create();
        alert.show();
    }

}