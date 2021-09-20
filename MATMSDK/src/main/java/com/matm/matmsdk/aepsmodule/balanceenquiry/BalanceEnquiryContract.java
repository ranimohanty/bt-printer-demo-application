package com.matm.matmsdk.aepsmodule.balanceenquiry;
import com.matm.matmsdk.aepsmodule.cashwithdrawal.AepsResponse;
import com.matm.matmsdk.aepsmodule.ministatement.StatementResponse;

import org.json.JSONObject;


public class BalanceEnquiryContract {


    public interface View {


        void checkBalanceEnquiryStatus(String status, String message, BalanceEnquiryResponse balanceEnquiryResponse);

        void checkBalanceEnquiryAEPS2(String status, String message, AepsResponse balanceEnquiryResponse);


        void checkStatementEnquiryAEPS2(String status, String message, JSONObject statementResponse);

        void checkEmptyFields();
        void showLoader();
        void hideLoader();


    }

    interface UserActionsListener {
        void performBalanceEnquiry(String retailer, String token, BalanceEnquiryRequestModel balanceEnquiryRequestModel);

        void performBalanceEnquiryAEPS2(String token, BalanceEnquiryAEPS2RequestModel balanceEnquiryRequestModel, String transaction_type);



    }


}

