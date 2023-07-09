package com.tapstream.sdk.example;

import static com.android.billingclient.api.BillingClient.BillingResponseCode;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsParams.Product;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.SkuDetails;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PurchaseActivity extends AppCompatActivity implements PurchasesUpdatedListener, BillingClientStateListener {

    private static final String TAG = "ExamplePurchase";

    /**
     * There's no more static SKU to test purchasing with. To test this feature, the app must be
     * set up in the Play console with the following product ID configured, with all the various
     * rigamarole (signing, uploading, releasing to test users) that entails.
     * <a href="https://developer.android.com/google/play/billing/test">More Information</a>
     */
    private static final String EXAMPLE_PRODUCT_ID = "tapstream_example.test";

    private ProductDetails exampleProductDetails;

    ArrayAdapter<CharSequence> ownedSkuListAdapter;
    Handler uiHandler;

    private BillingClient billingClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);

        uiHandler = new Handler(Looper.getMainLooper());

        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(this);

        // Setup the purchased SKU list
        ownedSkuListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ((ListView) findViewById(R.id.listOwnedSKUs)).setAdapter(ownedSkuListAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        Log.i(TAG, "Billing service disconnected");
    }

    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        Log.i(TAG, "Billing setup finished: " + billingResult);
        if (billingResult.getResponseCode() == BillingResponseCode.OK) {
            populateExampleProduct();
            refreshPurchaseList();
        }
    }

    private void populateExampleProduct() {
        Product product = Product.newBuilder()
                .setProductId(EXAMPLE_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        List<Product> productList = Collections.singletonList(product);

        QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                 .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(queryProductDetailsParams, (billingResult, list) -> {

            if (billingResult.getResponseCode() != BillingResponseCode.OK) {
                Log.e(TAG, "Failed to list products: " + billingResult.getDebugMessage());
                return;
            }

            if (list.isEmpty()) {
                Log.w(TAG, "Product not found: " + EXAMPLE_PRODUCT_ID);
            }

            list.forEach(item -> {
                if (EXAMPLE_PRODUCT_ID.equals(item.getProductId())) {
                    this.exampleProductDetails = item;
                } else {
                    Log.w(TAG, "Found unexpected product ID: " + item.getProductId());
                }
            });
        });
    }

    private void refreshPurchaseList() {

        if (!billingClient.isReady()) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready");
            return;
        }

        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(), (billingResult, purchases) -> {

            if (billingResult.getResponseCode() != BillingResponseCode.OK) {
                Log.e(TAG, "Failed to list purchases");
            }

            runOnUiThread(() -> {
                for (final Purchase p : purchases) {
                    for (String sku : p.getProducts()) {
                        ownedSkuListAdapter.add(sku);
                    }
                }
            });
        });
    }

    /**
     * Follow the test purchase flow.
     *
     * @param view
     */
    public void onClickPurchase(View view) throws Exception {
        if (this.exampleProductDetails == null) {
            Log.e(TAG, "No product loaded. Are you running a valid release from Google Play?");
            Toast.makeText(this, "No product found.", Toast.LENGTH_SHORT).show();
            return;
        }

        BillingFlowParams.ProductDetailsParams productDetailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(this.exampleProductDetails)
                        .build();

        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                Collections.singletonList(productDetailsParams);

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult billingResult = billingClient.launchBillingFlow(this, billingFlowParams);

        if (billingResult.getResponseCode() != BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow");
        }
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() != BillingResponseCode.OK || purchases == null) {
            Log.e(TAG, "Error during purchase workflow: " + billingResult.getDebugMessage());
            return;
        }

        for (Purchase purchase : purchases) {
            for (String productSku : purchase.getProducts()) {
                Event event = new Event(
                        purchase.getOrderId(),
                        productSku,
                        purchase.getQuantity(),
                        500,
                        "USD"
                );

                event.setReceipt(purchase.getOriginalJson(), purchase.getSignature());
                Tapstream.getInstance().fireEvent(event);
            }

            final Context ctx = this;
            runOnUiThread(() -> {
                for (String sku : purchase.getProducts()) {
                    ownedSkuListAdapter.add(sku);
                    final String msg = "Purchased: " + sku;
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, msg);
                }
            });

        }
    }


    /**
     * Consumes all available purchases.
     *
     * @param view
     */
    public void onClickConsume(View view) {

        if (!billingClient.isReady()) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready");
            return;
        }

        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(), (billingResult, purchases) -> {

            if (billingResult.getResponseCode() != BillingResponseCode.OK) {
                Log.e(TAG, "Failed to list purchases");
            }

            for (Purchase purchase : purchases) {
                consumePurchase(purchase);
            }
        });
    }

    private void consumePurchase(final Purchase purchase) {
        Log.i(TAG, "Consuming " + purchase.toString());

        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.consumeAsync(consumeParams, ((billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() != BillingResponseCode.OK) {
                Log.e(TAG, "Failed to consume purchase: " + billingResult.getDebugMessage());
            }

            runOnUiThread(() -> {
                for (String sku : purchase.getProducts()) {
                    ownedSkuListAdapter.remove(sku);
                    String msg = "Consumed: " + sku;
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, msg);
                }
            });
        }));
    }
}
