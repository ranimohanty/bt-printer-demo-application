package com.matm.matmsdk.transaction_report;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import com.matm.matmsdk.Dashboard.MainActivity;

import isumatm.androidsdk.equitas.R;

import com.matm.matmsdk.FileUtils;
import com.matm.matmsdk.Service.BankResponse;
import com.matm.matmsdk.Utils.getToneGenerator;
import com.matm.matmsdk.Utils.PAXScreen;
import com.matm.matmsdk.Utils.SdkConstants;
import com.matm.matmsdk.aepsmodule.utils.GetPosConnectedPrinter;
import com.matm.matmsdk.aepsmodule.utils.Util;
import com.matm.matmsdk.btprinter.AEMPrinter;
import com.matm.matmsdk.btprinter.AEMScrybeDevice;
import com.matm.matmsdk.btprinter.CardReader;
import com.matm.matmsdk.btprinter.IAemCardScanner;
import com.matm.matmsdk.btprinter.IAemScrybe;
import com.matm.matmsdk.permission.PermissionsActivity;
import com.matm.matmsdk.permission.PermissionsChecker;
import com.matm.matmsdk.readfile.PreviewPDFActivity;
import com.paxsz.easylink.api.EasyLinkSdkManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.matm.matmsdk.permission.PermissionsActivity.PERMISSION_REQUEST_CODE;
import static com.matm.matmsdk.permission.PermissionsChecker.REQUIRED_PERMISSION;

public class TransactionStatusActivity extends AppCompatActivity implements IAemCardScanner, IAemScrybe {

    ImageView status_icon, sendButton;
    ImageButton backBtn;
    TextView balanceText, card_amount, bank_name, date_time, txnID;
    EditText editTextMobile;
    Button txndetails;
    CheckBox mobileCheckBox;
    Button printBtn, downloadBtn, closeBtn;
    LinearLayout mobileEditLayout, mobileTextLayout;
    ProgressDialog progressDialog;
    public EasyLinkSdkManager manager;
    BluetoothDevice bluetoothDevice;
    PermissionsChecker checker;
    Context mContext;
    BluetoothAdapter B;
    String statusTxt;
    private int STORAGE_PERMISSION_CODE = 1;
    String transaction_type;
    String mobile;
    String prefNum = "NA", MID = "NA", TID = "NA", CARD_TYPE = "NA", Card_NUM = "NA", balanceAmt = "NA", transactionAmt = "NA", TRANSACTION_ID = "NA";
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



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_status);
        manager = EasyLinkSdkManager.getInstance(this);

        //Runtime permission request required if Android permission >= Marshmallow
        checker = new PermissionsChecker(this);
        mContext = getApplicationContext();

        B = BluetoothAdapter.getDefaultAdapter();
        new getToneGenerator();
        status_icon = findViewById(R.id.status_icon);
        sendButton = findViewById(R.id.sendButton);
        balanceText = findViewById(R.id.balanceText);
        card_amount = findViewById(R.id.card_amount);
        mobileTextLayout = findViewById(R.id.mobileTextLayout);
        bank_name = findViewById(R.id.card_type);
        date_time = findViewById(R.id.date_time);
        txnID = findViewById(R.id.txnID);
        txndetails = findViewById(R.id.txndetailsBtn);
        mobileCheckBox = findViewById(R.id.mobileCheckBox);
        printBtn = findViewById(R.id.printBtn);
        downloadBtn = findViewById(R.id.downloadBtn);
        closeBtn = findViewById(R.id.closeBtn);
        backBtn = findViewById(R.id.backBtn);
        mobileEditLayout = findViewById(R.id.mobileEditLayout);
        editTextMobile = findViewById(R.id.editTextMobile);
        mobile = getIntent().getStringExtra("MOBILE_NUMBER");
        editTextMobile.setText(mobile);

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy  HH:mm:ss");
        String currentDateandTime = sdf.format(new Date());
        date_time.setText(currentDateandTime);
        transaction_type = getIntent().getStringExtra("TRANSACTION_TYPE");
        transactionAmt = getIntent().getStringExtra("TRANSACTION_AMOUNT");
//        transactionAmt = getDecimalString(transactionAmt);
        TRANSACTION_ID = getIntent().getStringExtra("TRANSACTION_ID");


        String applName = getIntent().getStringExtra("APP_NAME");
        String aId = getIntent().getStringExtra("AID");
        prefNum = getIntent().getStringExtra("RRN_NO");
        MID = getIntent().getStringExtra("MID");
        TID = getIntent().getStringExtra("TID");
        String TXN_ID = getIntent().getStringExtra("TXN_ID");
        String INVOICE = getIntent().getStringExtra("INVOICE");

        CARD_TYPE = getIntent().getStringExtra("CARD_TYPE");
        String APPR_CODE = getIntent().getStringExtra("APPR_CODE");
        Card_NUM = getIntent().getStringExtra("CARD_NUMBER");
        balanceAmt = getIntent().getStringExtra("AMOUNT");
//        balanceAmt = getDecimalString(balanceAmt);
        String RESPONSE_CODE = getIntent().getStringExtra("RESPONSE_CODE");
        RESPONSE_CODE = RESPONSE_CODE.substring(2);


        m_AemScrybeDevice = new AEMScrybeDevice(TransactionStatusActivity.this);
        printerList = new ArrayList<String>();
        creditData = new String();


        if (balanceAmt.equalsIgnoreCase("0") || balanceAmt.equalsIgnoreCase("N/A") || balanceAmt.equalsIgnoreCase("NA")) {
            balanceAmt = "N/A";
        } else {
            balanceAmt = replaceWithZero(balanceAmt);
        }
        if (transactionAmt.equalsIgnoreCase("N/A")) {
            transactionAmt = "N/A";
        } else {
            transactionAmt = replaceWithZero(transactionAmt);
        }


        System.out.println(">>>----" + balanceAmt);

        String[] splitAmount = Card_NUM.split("D");
        Card_NUM = splitAmount[0];

        String firstnum = Card_NUM.substring(0, 2);
        String middlenum = Card_NUM.substring(2, Card_NUM.length() - 2);
        String lastNum = Card_NUM.replace(firstnum + middlenum, "");

        System.out.println(">>>---" + firstnum);
        System.out.println(">>>---" + middlenum);
        System.out.println(">>>---" + lastNum);

        if (transaction_type.equalsIgnoreCase("cash")) {
            transactionTypeCheck = "Cash Withdrawal";
            card_amount.setText("Txn Amt : Rs. " + transactionAmt);
        } else {
            transactionTypeCheck = "Balance Enquiry";
            card_amount.setText("Available Bal : Rs. " + balanceAmt);
        }


        // CARD_NUMBER =
        String flag = getIntent().getStringExtra("flag");
        if (flag.equalsIgnoreCase("failure")) {
            status_icon.setImageResource(R.drawable.hero_failure);
            statusTxt = "Failed";

            BankResponse.showStatusMessage(manager, RESPONSE_CODE, balanceText);
            PAXScreen.showFailure(manager);

            txnID.setText("Txn ID : " + TRANSACTION_ID);
            bank_name.setText(CARD_TYPE);
            balanceAmt = "N/A";
            card_amount.setText("");

            String transactionType = "";
            if (SdkConstants.transactionType.equalsIgnoreCase("0")) {
                transactionType = "BalanceEnquiry Failled!! ";
            } else {
                transactionType = "CashWithdraw Failled!! ";
            }

            String str = "";
            str = balanceText.getText().toString();

            String responseData = generateJsonData(transactionType, str, prefNum, Card_NUM, card_amount.getText().toString(), TID);
            SdkConstants.responseData = responseData;
        } else {
            //Show Success
            mobileTextLayout.setVisibility(View.VISIBLE);
            statusTxt = "Success";
            PAXScreen.showSuccess(manager);
            txnID.setText("Txn ID : " + TRANSACTION_ID);
            bank_name.setText(CARD_TYPE);
            if (transaction_type.equalsIgnoreCase("cash")) {
                transactionTypeCheck = "Cash Withdrawal";
                card_amount.setText("Txn Amt : Rs. " + transactionAmt);
            } else {
                transactionTypeCheck = "Balance Enquiry";
                card_amount.setText("Available Bal : Rs. " + balanceAmt);
            }

            String transactionType = "";
            if (SdkConstants.transactionType.equalsIgnoreCase("0")) {
                transactionType = "BalanceEnquiry Successful!! ";
            } else {
                transactionType = "CashWithdraw Successful!! ";
            }

            String str = "";
            str = statusTxt;

            String responseData = generateJsonData(transactionType, str, prefNum, Card_NUM, balanceAmt, TID);
            SdkConstants.responseData = responseData;


        }

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent mainActivity = new Intent(TransactionStatusActivity.this, MainActivity.class);
                mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            }
        });

        txndetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTransactionDetails(TransactionStatusActivity.this);
            }
        });

        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checker.lacksPermissions(REQUIRED_PERMISSION)) {
                    PermissionsActivity.startActivityForResult(TransactionStatusActivity.this, PERMISSION_REQUEST_CODE, REQUIRED_PERMISSION);
                } else {
                    Date date = new Date();
                    long timeMilli = date.getTime();
                    System.out.println("Time in milliseconds using Date class: " + String.valueOf(timeMilli));
                    createPdf(FileUtils.getAppPath(mContext) + String.valueOf(timeMilli) + "Order_Receipt.pdf");
                }
            }
        });

        printBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//
//                if (SdkConstants.Bluetoothname.equalsIgnoreCase("ESIAF3996")) {
//                    SdkConstants.bluetoothDevice = null;
//                    SdkConstants.Bluetoothname = "null";
//                } else if (SdkConstants.Bluetoothname.contains("BPFS")) {
//                    SdkConstants.bluetoothDevice = null;
//                    SdkConstants.Bluetoothname = "null";
//                } else {
//                    bluetoothDevice = SdkConstants.bluetoothDevice;
//                }

//                if (bluetoothDevice != null) {
//
//                    if (!B.isEnabled()) {
//
//                        finish();
//                        Toast.makeText(getApplicationContext(), "Your Bluetooth is OFF .", Toast.LENGTH_LONG).show();
//                    } else {
//                        callBluetoothFunction(TRANSACTION_ID, date_time.getText().toString(), prefNum, MID, TID, CARD_TYPE, Card_NUM, transactionAmt, bluetoothDevice);
//                    }
//
//                } else {
//                    Intent in = new Intent(TransactionStatusActivity.this, BluetoothConnectorActivity.class);
//                    startActivity(in);
//                }
                registerForContextMenu(printBtn);
                if (GetPosConnectedPrinter.aemPrinter ==null)
                {
                    printerList = m_AemScrybeDevice.getPairedPrinters();

                    if (printerList.size() > 0) {
                        openContextMenu(v);
                    }
                    else {
                        showAlert("No Paired Printers found");
                    }
                }else {
                    m_AemPrinter = GetPosConnectedPrinter.aemPrinter;
                    callBluetoothFunction(TRANSACTION_ID, date_time.getText().toString(), prefNum, MID, TID, CARD_TYPE, Card_NUM, transactionAmt);
                }


            }
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
                    if (x.startsWith("0") || Util.isValidMobile(editTextMobile.getText().toString().trim()) == false) {
                        editTextMobile.setError(getResources().getString(R.string.mobilevaliderror));
                    }
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextMobile.getText() == null || editTextMobile.getText().toString().trim().matches("") || Util.isValidMobile(editTextMobile.getText().toString().trim()) == false) {
                    editTextMobile.setError(getResources().getString(R.string.mobileerror));
                } else {
                    showLoader();
                    mobileNumberSMS(Card_NUM);
                }

            }
        });
    }

    public String replaceWithZero(String s) {
        float amount = Integer.valueOf(s) / 100F;
        DecimalFormat formatter = new DecimalFormat("##,##,##,##0.00");
        return formatter.format(Double.parseDouble(String.valueOf(amount)));
    }

    public String generateJsonData(String status, String statusDesc, String rrn, String cardno, String bal, String terminalId) {
        String jdata = "";
        JSONObject obj = new JSONObject();
        try {
            obj.put("TransactionStatus", status);
            obj.put("StatusDescription", statusDesc);
            obj.put("RRN", rrn);
            obj.put("CardNumber", cardno);
            obj.put("Balance", bal);
            obj.put("TerminalID", terminalId);
            jdata = obj.toString();


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jdata;
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
            document.setMargins(0, 0, 50, 50);
            Rectangle rect = new Rectangle(577, 825, 18, 15);
            rect.enableBorderSide(1);
            rect.enableBorderSide(2);
            rect.enableBorderSide(4);
            rect.enableBorderSide(8);
            rect.setBorder(Rectangle.BOX);
            rect.setBorderWidth(2);
            rect.setBorderColor(BaseColor.BLACK);
            document.add(rect);


            /*commit git test*/

            BaseColor mColorAccent = new BaseColor(0, 153, 204, 255);
            float mHeadingFontSize = 24.0f;
            float mValueFontSize = 26.0f;

            BaseFont urName = BaseFont.createFont("assets/fonts/brandon_medium.otf", "UTF-8", BaseFont.EMBEDDED);

            // LINE SEPARATOR
            LineSeparator lineSeparator = new LineSeparator();
            lineSeparator.setLineColor(new BaseColor(0, 0, 0, 68));

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
            Font mOrderDetailsTitleFont11;
            if (statusTxt.equalsIgnoreCase("FAILED")) {
                mOrderDetailsTitleFont11 = new Font(urName, 40.0f, Font.NORMAL, BaseColor.RED);

            } else {
                mOrderDetailsTitleFont11 = new Font(urName, 40.0f, Font.NORMAL, BaseColor.GREEN);
            }

            Chunk mOrderDetailsTitleChunk1 = new Chunk(statusTxt, mOrderDetailsTitleFont11);
            Paragraph mOrderDetailsTitleParagraph1 = new Paragraph(mOrderDetailsTitleChunk1);
            mOrderDetailsTitleParagraph1.setAlignment(Element.ALIGN_CENTER);
            document.add(mOrderDetailsTitleParagraph1);
            document.add(new Paragraph("\n"));

            Font mOrderDateFont = new Font(urName, mHeadingFontSize, Font.NORMAL, mColorAccent);
            Font mOrderDateValueFont = new Font(urName, mValueFontSize, Font.NORMAL, BaseColor.BLACK);

            Paragraph p = new Paragraph();
            p.add(new Chunk("Date/Time : ", mOrderDateFont));
            p.add(new Chunk(date_time.getText().toString().trim(), mOrderDateValueFont));
            document.add(p);
            document.add(new Paragraph("\n\n"));
            Paragraph p1 = new Paragraph();
            p1.add(new Chunk("Operation Performed : ", mOrderDateFont));
            p1.add(new Chunk("mATM 2", mOrderDateValueFont));
            document.add(p1);

            document.add(new Paragraph("\n\n"));


            Font mOrderDetailsFont = new Font(urName, 30.0f, Font.BOLD, mColorAccent);
            Chunk mOrderDetailsChunk = new Chunk("Transaction Details", mOrderDetailsFont);
            Paragraph mOrderDetailsParagraph = new Paragraph(mOrderDetailsChunk);
            mOrderDetailsParagraph.setAlignment(Element.ALIGN_CENTER);
            document.add(mOrderDetailsParagraph);
            document.add(new Paragraph("\n"));
            Font mOrderIdFont = new Font(urName, mValueFontSize, Font.NORMAL, BaseColor.BLACK);
            Chunk mOrderIdChunk = new Chunk("Transaction ID: " + TRANSACTION_ID, mOrderIdFont);
            Paragraph mOrderTxnParagraph = new Paragraph(mOrderIdChunk);
            document.add(mOrderTxnParagraph);
            Chunk mOrderIdValueChunk = new Chunk("MID: " + MID, mOrderIdFont);
            Paragraph mOrderaadharParagraph = new Paragraph(mOrderIdValueChunk);
            document.add(mOrderaadharParagraph);
            Chunk mBankNameChunk = new Chunk("Terminal ID:  " + TID, mOrderIdFont);
            Paragraph mBankNameParagraph = new Paragraph(mBankNameChunk);
            document.add(mBankNameParagraph);
            Chunk mOrderrrnChunk = new Chunk("RRN No.: " + prefNum, mOrderIdFont);
            Paragraph mOrderrnParagraph = new Paragraph(mOrderrrnChunk);
            document.add(mOrderrnParagraph);
            Chunk mOrdertxnTypeChunk = new Chunk("Card No.: " + Card_NUM, mOrderIdFont);
            Paragraph mOrdertxnTypeParagraph = new Paragraph(mOrdertxnTypeChunk);
            document.add(mOrdertxnTypeParagraph);
            Chunk mOrderbalanceChunk = new Chunk("Balance Amount: " + balanceAmt, mOrderIdFont);
            Paragraph mOrderbalanceParagraph = new Paragraph(mOrderbalanceChunk);
            document.add(mOrderbalanceParagraph);
            Chunk mOrderbalanceChunk1 = new Chunk("Transaction Type: " + transactionTypeCheck, mOrderIdFont);
            Paragraph mOrderbalanceParagraph1 = new Paragraph(mOrderbalanceChunk1);
            document.add(mOrderbalanceParagraph1);
            Chunk mOrdertxnAmtChunk = new Chunk("Transaction Amount: " + transactionAmt, mOrderIdFont);
            Paragraph mOrdertxnAmtParagraph = new Paragraph(mOrdertxnAmtChunk);
            document.add(mOrdertxnAmtParagraph);

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

            Intent intent = new Intent(TransactionStatusActivity.this, PreviewPDFActivity.class);
            intent.putExtra("filePath", dest);
            startActivity(intent);

        } catch (IOException | DocumentException ie) {
            Log.e("createPdf: Error ", "" + ie.getLocalizedMessage());
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

    private void showBrandSetAlert() {
        try {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(TransactionStatusActivity.this);
            builder1.setMessage("Unable to download/print the receipt. Please contact admin.");
            builder1.setTitle("Warning!!!");
            builder1.setCancelable(false);
            builder1.setPositiveButton(
                    "GOT IT",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert11 = builder1.create();
            alert11.show();
        } catch (Exception e) {

        }
    }

    private void callBluetoothFunction(final String txnId, final String date, final String reffNo, final String mid, final String terminalId, final String type, final String cardNumber, final String transactionAmt) {

        try {
            m_AemPrinter.setFontType(AEMPrinter.DOUBLE_HEIGHT);
            m_AemPrinter.setFontType(AEMPrinter.TEXT_ALIGNMENT_CENTER);
            m_AemPrinter.print(SdkConstants.SHOP_NAME);
            m_AemPrinter.print("\n\n");
            m_AemPrinter.setFontType(AEMPrinter.TEXT_ALIGNMENT_CENTER);
            m_AemPrinter.print("-----Transaction Report-----");
            m_AemPrinter.print("\n");
            m_AemPrinter.setFontType(AEMPrinter.TEXT_ALIGNMENT_CENTER);
            m_AemPrinter.setFontType(AEMPrinter.DOUBLE_HEIGHT);
            m_AemPrinter.print(statusTxt);
            m_AemPrinter.print("\n\n");
            m_AemPrinter.print("TXNId: " + txnId);
            m_AemPrinter.print("\n");
            m_AemPrinter.print("Date/Time: " + date);
            m_AemPrinter.print("\n");
            m_AemPrinter.print("RRN No.: " + reffNo);
            m_AemPrinter.print("\n");
            m_AemPrinter.print("Mid : " + mid);
            m_AemPrinter.print("\n");
            m_AemPrinter.print("TerminalID: " + terminalId);
            m_AemPrinter.print("\n");
            m_AemPrinter.print("Card No.: " + cardNumber);
            m_AemPrinter.print("\n");
            m_AemPrinter.print("BalanceAmount : " + balanceAmt);
            m_AemPrinter.print("\n");
            m_AemPrinter.print("Transaction Type : " + transactionTypeCheck);
            m_AemPrinter.print("\n");
            m_AemPrinter.print("TransactionAmount : " + transactionAmt);
            m_AemPrinter.print("\n\n");
            m_AemPrinter.setFontType(AEMPrinter.TEXT_ALIGNMENT_RIGHT);
            m_AemPrinter.print(SdkConstants.BRAND_NAME);
            m_AemPrinter.print("\n\n");
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
//                mPrinter.printText(SdkConstants.SHOP_NAME);
//                mPrinter.addNewLine();
//                mPrinter.addNewLine();
//                mPrinter.setAlign(BluetoothPrinter.ALIGN_CENTER);
//                mPrinter.printText("-----Transaction Report-----");
//                mPrinter.addNewLine();
//                mPrinter.setAlign(BluetoothPrinter.ALIGN_CENTER);
//                mPrinter.setBold(true);
//                mPrinter.printText(statusTxt);
//                mPrinter.addNewLine();
//                mPrinter.addNewLine();
//                mPrinter.printText("TXNId: " + txnId);
//                mPrinter.addNewLine();
//                mPrinter.printText("Date/Time: " + date);
//                mPrinter.addNewLine();
//                mPrinter.printText("RRN No.: " + reffNo);
//                mPrinter.addNewLine();
//                mPrinter.printText("Mid : " + mid);
//                mPrinter.addNewLine();
//                mPrinter.printText("TerminalID: " + terminalId);
//                mPrinter.addNewLine();
//                mPrinter.printText("Card No.: " + cardNumber);
//                mPrinter.addNewLine();
//                mPrinter.printText("BalanceAmount : " + balanceAmt);
//                mPrinter.addNewLine();
//                mPrinter.printText("Transaction Type : " + transactionTypeCheck);
//                mPrinter.addNewLine();
//                mPrinter.printText("TransactionAmount : " + transactionAmt);
//                mPrinter.addNewLine();
//                mPrinter.addNewLine();
//                mPrinter.setBold(true);
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
//                Log.d("BluetoothPrinter", "Conection failed");
//                Toast.makeText(TransactionStatusActivity.this, "Please switch on bluetooth printer", Toast.LENGTH_SHORT).show();
//            }
//        });

    }

    public void mobileNumberSMS(String cardNumber) {

        String msgValue = "Thanks for visiting " + SdkConstants.SHOP_NAME + ". Current balance for " + cardNumber + " account seeded with balance  is Rs " + balanceAmt + ". Dated " + date_time.getText().toString() + ".";

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

                                    Toast.makeText(TransactionStatusActivity.this, "Message Sent Successfully . ", Toast.LENGTH_SHORT).show();

                                } else {

                                    hideLoader();
                                    Toast.makeText(TransactionStatusActivity.this, msg, Toast.LENGTH_SHORT).show();

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
                            Toast.makeText(TransactionStatusActivity.this, "Message Error", Toast.LENGTH_SHORT).show();


                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showLoader() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(TransactionStatusActivity.this);
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

    public void showTransactionDetails(Activity activity) {
        try {
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.transaction_matm_details_layout);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

            TextView aadhar_number = (TextView) dialog.findViewById(R.id.aadhar_number);
            TextView rref_num = (TextView) dialog.findViewById(R.id.rref_num);
            TextView card_transaction_type = (TextView) dialog.findViewById(R.id.card_transaction_type);
            TextView txnType = (TextView) dialog.findViewById(R.id.txnType);
            TextView card_number = (TextView) dialog.findViewById(R.id.card_number);
            TextView card_transaction_amount = (TextView) dialog.findViewById(R.id.card_transaction_amount);
            aadhar_number.setText(MID);
            rref_num.setText(prefNum);
            card_transaction_type.setText(TID);
            card_number.setText(Card_NUM);

            if (transaction_type.equalsIgnoreCase("cash")) {
                card_transaction_amount.setText((balanceAmt));
                balanceAmt = card_transaction_amount.getText().toString();
                txnType.setText("Cash Withdrawal");
                transactionTypeCheck = txnType.getText().toString();
            } else {
                txnType.setText("Balance Enquiry");
                transactionAmt = "N/A";
                card_transaction_amount.setText(transactionAmt);
                transactionTypeCheck = txnType.getText().toString();
            }

            Button dialogBtn_close = (Button) dialog.findViewById(R.id.close_Btn);
            dialogBtn_close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.cancel();

                }
            });

            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
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
        AlertDialog.Builder alertBox = new AlertDialog.Builder(TransactionStatusActivity.this);
        alertBox.setMessage(alertMsg).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                return;
            }
        });

        AlertDialog alert = alertBox.create();
        alert.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Select Printer to connect");

        for (int i = 0; i < printerList.size(); i++)
        {
            menu.add(0, v.getId(), 0, printerList.get(i));
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        super.onContextItemSelected(item);
        String printerName = item.getTitle().toString();
        try
        {
            m_AemScrybeDevice.connectToPrinter(printerName);
            m_cardReader = m_AemScrybeDevice.getCardReader(this);
            m_AemPrinter = m_AemScrybeDevice.getAemPrinter();
            GetPosConnectedPrinter.aemPrinter = m_AemPrinter;
            Toast.makeText(TransactionStatusActivity.this,"Connected with " + printerName,Toast.LENGTH_SHORT ).show();
//            String data=new String(batteryStatusCommand);
//            m_AemPrinter.print(data);
            //  m_cardReader.readMSR();
        }
        catch (IOException e)
        {
            if (e.getMessage().contains("Service discovery failed"))
            {
                Toast.makeText(TransactionStatusActivity.this,"Not Connected\n"+ printerName + " is unreachable or off otherwise it is connected with other device",Toast.LENGTH_SHORT ).show();
            }
            else if (e.getMessage().contains("Device or resource busy"))
            {
                Toast.makeText(TransactionStatusActivity.this,"the device is already connected",Toast.LENGTH_SHORT ).show();
            }
            else
            {
                Toast.makeText(TransactionStatusActivity.this,"Unable to connect",Toast.LENGTH_SHORT ).show();
            }
        }
        return true;
    }

    CardReader.MSRCardData creditDetails;
    public void onScanMSR(final String buffer, CardReader.CARD_TRACK cardTrack)
    {
        cardTrackType = cardTrack;
        creditData = buffer;
        TransactionStatusActivity.this.runOnUiThread(new Runnable()
        {
            public void run()
            {
//                editText.setText(buffer.toString());
            }
        });
    }
    public void onScanDLCard(final String buffer)
    {
        CardReader.DLCardData dlCardData = m_cardReader.decodeDLData(buffer);
        String name = "NAME:" + dlCardData.NAME + "\n";
        String SWD = "SWD Of: " + dlCardData.SWD_OF + "\n";
        String dob = "DOB: " + dlCardData.DOB + "\n";
        String dlNum = "DLNUM: " + dlCardData.DL_NUM + "\n";
        String issAuth = "ISS AUTH: " + dlCardData.ISS_AUTH + "\n";
        String doi = "DOI: " + dlCardData.DOI + "\n";
        String tp = "VALID TP: " + dlCardData.VALID_TP + "\n";
        String ntp = "VALID NTP: " + dlCardData.VALID_NTP + "\n";

        final String data = name + SWD + dob + dlNum + issAuth + doi + tp + ntp;

        runOnUiThread(new Runnable()
        {
            public void run()
            {
//                editText.setText(data);
            }
        });
    }

    public void onScanRCCard(final String buffer)
    {
        CardReader.RCCardData rcCardData = m_cardReader.decodeRCData(buffer);
        String regNum = "REG NUM: " + rcCardData.REG_NUM + "\n";
        String regName = "REG NAME: " + rcCardData.REG_NAME + "\n";
        String regUpto = "REG UPTO: " + rcCardData.REG_UPTO + "\n";

        final String data = regNum + regName + regUpto;

        runOnUiThread(new Runnable()
        {
            public void run()
            {
//                editText.setText(data);
            }
        });
    }
    @Override
    public void onScanRFD(final String buffer)
    {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(buffer);
        String temp = "";
        try
        {
            temp = stringBuffer.deleteCharAt(8).toString();
        }
        catch (Exception e)
        {
            // TODO: handle exception
        }
        final String data = temp;

        TransactionStatusActivity.this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                //rfText.setText("RF ID:   " + data);
//                editText.setText("ID " + data);
                try {
                    m_AemPrinter.print(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }
    public void onDiscoveryComplete(ArrayList<String> aemPrinterList)
    {
        printerList = aemPrinterList;
        for(int i=0;i<aemPrinterList.size();i++)
        {
            String Device_Name=aemPrinterList.get(i);
            String status = m_AemScrybeDevice.pairPrinter(Device_Name);
            Log.e("STATUS", status);
        }
    }
    @Override
    public void onScanPacket(String buffer) {
        if (buffer.equals("PRINTEROK")){
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(buffer);
            String temp = "";
            try
            {
                temp = stringBuffer.toString();
            }
            catch (Exception e) {
                // TODO: handle exception
            }
            tempdata = temp;
            final String strData=tempdata.replace("|","&");
            //Log.e("BufferData",data);
            final String[][] formattedData = {strData.split("&", 3)};
            // Log.e("Response Data",formattedData[2]);
            responseString = formattedData[0][2];
            responseArray[0]=responseString.replace("^","");
            Log.e("Response Array",responseArray[0]);
            TransactionStatusActivity.this.runOnUiThread(new Runnable() {
                public void run()
                {
                    replacedData=tempdata.replace("|","&");
                    formattedData[0] =replacedData.split("&",3);
                    response= formattedData[0][2];
                    if(response.contains("BAT")){
//                        txtBatteryStatus.setText(response.replace("^","").replace("BAT","")+"%");
                    }
//                    editText.setText(response.replace("^",""));
                }
            });

        }else {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(buffer);
            String temp = "";
            try
            {
                temp = stringBuffer.toString();
            }
            catch (Exception e)
            {
                // TODO: handle exception
            }
            tempdata = temp;
            final String strData=tempdata.replace("|","&");
            //Log.e("BufferData",data);
            final String[][] formattedData = {strData.split("&", 3)};
            // Log.e("Response Data",formattedData[2]);
            responseString = formattedData[0][2];
            responseArray[0]=responseString.replace("^","");
            Log.e("Response Array",responseArray[0]);
            TransactionStatusActivity.this.runOnUiThread(new Runnable() {
                public void run()
                {
                    replacedData=tempdata.replace("|","&");
                    formattedData[0] =replacedData.split("&",3);
                    response= formattedData[0][2];
                    if(response.contains("BAT")){
//                        txtBatteryStatus.setText(response.replace("^","").replace("BAT","")+"%");
                    }
//                    editText.setText(response.replace("^",""));
                }
            });
        }
    }

}
