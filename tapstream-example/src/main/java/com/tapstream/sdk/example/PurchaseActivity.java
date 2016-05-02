package com.tapstream.sdk.example;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PurchaseActivity extends AppCompatActivity {

    private static final String TAG = "ExamplePurchase";

    public static final String MOCK_SKU_DETAILS =
            "{\"price_amount_micros\": \"1000000\", \"title\": \"Test Purchase\", " +
            "\"price\": \"$1.00\", \"description\": \"Test Purchase\", \"type\": \"inapp\", " +
            "\"price_currency_code\": \"USD\", \"productId\": \"android.test.purchased\"}";

    public static final int BILLING_PURCHASE_REQUEST_CODE = 1001;

    public static final int BILLING_RESPONSE_RESULT_OK = 0;
    public static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;


    IInAppBillingService iabService;
    ArrayAdapter<CharSequence> ownedSkuListAdapter;
    Handler uiHandler;

    ServiceConnection iabServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            iabService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            Log.d(TAG, "IAB Service Bound");
            iabService = IInAppBillingService.Stub.asInterface(service);
            refreshPurchaseList();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);

        uiHandler = new Handler(Looper.getMainLooper());

        // Setup the purchased SKU list
        ListView ownedSkuList = (ListView)findViewById(R.id.listOwnedSKUs);
        ownedSkuListAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_list_item_1);
        ((ListView) findViewById(R.id.listOwnedSKUs)).setAdapter(ownedSkuListAdapter);

        // Bind the IAB service
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, iabServiceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (iabService != null) {
            unbindService(iabServiceConnection);
        }
    }


    private void refreshPurchaseList(){
        BackgroundWorkers.workers.submit(new Runnable() {
            @Override
            public void run() {

                final Bundle purchases;

                try {
                    purchases = iabService.getPurchases(3, getPackageName(), "inapp", null);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to get purchases", e);
                    return;
                }

                int response = purchases.getInt("RESPONSE_CODE");
                if (response != BILLING_RESPONSE_RESULT_OK) {
                    Log.e(TAG, "Failed to get list purchases");
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (String sku : purchases.getStringArrayList("INAPP_PURCHASE_ITEM_LIST")){
                            ownedSkuListAdapter.add(sku);
                        }
                    }
                });

            }
        });
    }

    /**
     * Follow the test purchase flow.
     * @param view
     */
    public void onClickPurchase(View view){
        BackgroundWorkers.workers.submit(new Runnable(){
            @Override
            public void run() {
                try {
                    Bundle buyIntentBundle = iabService.getBuyIntent(3, getPackageName(),
                            "android.test.purchased", "inapp", "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");

                    if (buyIntentBundle.getInt("RESPONSE_CODE") != BILLING_RESPONSE_RESULT_OK) {
                        Log.e("Example", "Buy intent request failed: " + buyIntentBundle.toString());
                        return;
                    }

                    PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");

                    startIntentSenderForResult(pendingIntent.getIntentSender(),
                            BILLING_PURCHASE_REQUEST_CODE, new Intent(), 0, 0, 0);

                } catch (Exception e){
                    Log.e(TAG, "Error during purchase", e);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BILLING_PURCHASE_REQUEST_CODE) {
            final String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            final String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                try {
                    final JSONObject jo = new JSONObject(purchaseData);
                    final String sku = jo.getString("productId");

                    Event event = new Event(
                            purchaseData,
                            MOCK_SKU_DETAILS,
                            dataSignature);

                    Tapstream.getInstance().fireEvent(event);

                    final Context ctx = this;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ownedSkuListAdapter.add(sku);
                            final String msg = "Purchased: " + sku;
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                            Log.i(TAG, msg);
                        }
                    });

                }
                catch (JSONException e) {
                    Log.e(TAG, "Failed to parse purchase data.", e);
                }
            }
        }
    }

    /**
     * Consumes all available purchases.
     * @param view
     */
    public void onClickConsume(View view){
        BackgroundWorkers.workers.submit(new Runnable() {
            @Override
            public void run() {
                Bundle purchases;

                try {
                    purchases = iabService.getPurchases(3, getPackageName(), "inapp", null);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to get purchases", e);
                    return;
                }
                int response = purchases.getInt("RESPONSE_CODE");
                if (response != BILLING_RESPONSE_RESULT_OK) {
                    Log.e(TAG, "Failed to get list purchases");
                    return;
                }

                ArrayList<String> ownedSkus =
                        purchases.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                ArrayList<String>  purchaseDataList =
                        purchases.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                ArrayList<String>  signatureList =
                        purchases.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");


                for (int i = 0; i < purchaseDataList.size(); ++i) {
                    String purchaseData = purchaseDataList.get(i);
                    String signature = signatureList.get(i);
                    String sku = ownedSkus.get(i);

                    try {
                        JSONObject purchaseDataJson = new JSONObject(purchaseData);
                        String token = purchaseDataJson.getString("purchaseToken");
                        consumePurchase(sku, token);
                    } catch (JSONException e){
                        Log.e(TAG, "Error deserializing purchase data", e);
                        return;
                    }

                }
            }
        });
    }

    private void consumePurchase(final String sku, final String token){
        Log.i(TAG, "Consuming " + sku);

        int consumeResponse;
        try {
             consumeResponse = iabService.consumePurchase(3, getPackageName(), token);
        } catch (RemoteException e){
            Log.e(TAG, "Failed to consume purchase", e);
            return;
        }

        if (consumeResponse == BILLING_RESPONSE_RESULT_OK){


            final Context ctx = this;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ownedSkuListAdapter.remove(sku);
                    String msg = "Consumed: " + sku;
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, msg);
                }
            });

            return;
        } else {
            Log.e(TAG, "Failed to consume purchase: " + consumeResponse);
            return;
        }
    }


}
