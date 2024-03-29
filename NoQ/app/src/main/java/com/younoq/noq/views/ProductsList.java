package com.younoq.noq.views;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.younoq.noq.R;
import com.younoq.noq.adapters.BottomSheetCategoryAdapter;
import com.younoq.noq.adapters.ProductListAdapter;
import com.younoq.noq.adapters.SearchSuggestionsAdapter;
import com.younoq.noq.classes.Category;
import com.younoq.noq.classes.Product;
import com.younoq.noq.classes.ProductSearchResult;
import com.younoq.noq.classes.ProductSearchResultLight;
import com.younoq.noq.classes.ResponseResult;
import com.younoq.noq.classes.SearchSuggestions;
import com.younoq.noq.models.AwsBackgroundWorker;
import com.younoq.noq.models.RxSearchObservable;
import com.younoq.noq.models.SaveInfoLocally;
import com.younoq.noq.networkhandler.NetworkHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Harsh Chaurasia(Phantom Boy).
 */

public class ProductsList extends AppCompatActivity {

    TextView tv_store_name, tv_category_name;
    String store_id, store_name, category_name, shoppingMethod, coming_from;
    Button btn_categories;
    private CoordinatorLayout coordinatorLayout;
    private String TAG ="ProductList";
    SaveInfoLocally saveInfoLocally;
    JSONArray jsonArray, jsonArray1, jsonArray2;
    JSONObject jobj;
    List<Product> productList;
    RecyclerView recyclerView, bs_recyclerview;
    ProductListAdapter productListAdapter;
    private LinearLayout layout_bottomsheet, ll_category;
    BottomSheetBehavior sheetBehavior;
    List<Category> categoriesList;
    BottomSheetCategoryAdapter categoryAdapter;
    private ImageView im_search_icon;
    private SearchView searchView;
    private Disposable disposable;
    private static String categoryName = "";
    private static int firstVisiblePosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_list);

        tv_store_name = findViewById(R.id.apl_store_name);
        tv_category_name = findViewById(R.id.apl_category_name);
        saveInfoLocally = new SaveInfoLocally(this);
        recyclerView = findViewById(R.id.apl_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        btn_categories = findViewById(R.id.apc_btn_categories);
        coordinatorLayout = findViewById(R.id.cpl_coordinator_layout);

        categoriesList = new ArrayList<>();
        ll_category = findViewById(R.id.cpl_category_linear_layout);
        im_search_icon = findViewById(R.id.cpl_search_icon);
        searchView = findViewById(R.id.cpl_search_view);

        bs_recyclerview = findViewById(R.id.bd_bottomsheet_recyclerview);
        bs_recyclerview.setHasFixedSize(true);
        bs_recyclerview.setLayoutManager(new GridLayoutManager(this, 3));
        layout_bottomsheet = findViewById(R.id.bd_bottomSheet);
        sheetBehavior = BottomSheetBehavior.from(layout_bottomsheet);

        Intent in= getIntent();
        category_name = in.getStringExtra("category_name");
        shoppingMethod = in.getStringExtra("shoppingMethod");
        coming_from = in.getStringExtra("coming_from");

        retrieve_categories();

        final String queryHint = "Search in \"" + category_name + "\"";
        searchView.setQueryHint(queryHint);

        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED: {
                        Log.d(TAG, "BottomSheet Expanded");
                    }
                    break;
                    case BottomSheetBehavior.STATE_COLLAPSED: {
                        Log.d(TAG, "BottomSheet Collapsed");
                    }
                    break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        btn_categories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//                    Log.d(TAG, "Expanding BottomSheet");
                } else {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
//                    Log.d(TAG, "Collapsing BottomSheet");
                }
            }
        });

        im_search_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setVisibility(View.VISIBLE);
                ll_category.setVisibility(View.GONE);
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Log.d(TAG, "Close Button Clicked");
//                im_search_icon.setVisibility(View.VISIBLE);
//                searchView.setVisibility(View.GONE);
//                productList.clear();
//                productListAdapter.notifyDataSetChanged();
//                retrieve_products_list();
                return true;
            }
        });

        productList = new ArrayList<>();

        store_id = saveInfoLocally.get_store_id();
        store_name = saveInfoLocally.getStoreName() +", "+ saveInfoLocally.getStoreAddress();
        tv_store_name.setText(store_name);

        tv_category_name.setText(category_name);

        retrieve_products_list();

    }

    @Override
    protected void onStart() {
        super.onStart();
        observeSearchView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        firstVisiblePosition = ((LinearLayoutManager)
                Objects.requireNonNull(recyclerView.getLayoutManager())).findFirstVisibleItemPosition();
        categoryName = category_name;
        Log.d(TAG, "in onPause : firstVisiblePosition : "+firstVisiblePosition);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!categoryName.equals(category_name)) {
            Log.d(TAG, "Previous Category : "+categoryName+", Current Category : "+category_name);
            firstVisiblePosition = 0;
        }
        Log.d(TAG, "in onResume : firstVisiblePosition : "+firstVisiblePosition);
        recyclerView.scrollToPosition(firstVisiblePosition);
    }

    void retrieve_categories() {

        final String store_id = saveInfoLocally.get_store_id();
        final String type= "retrieve_categories";
        try {
            final String res = new AwsBackgroundWorker(this).execute(type, store_id).get();
            Log.d(TAG, "Result in ProductList"+res);

            jsonArray = new JSONArray(res);

            for(int i = 0; i < jsonArray.length(); i++){

                jsonArray1 = jsonArray.getJSONArray(i);
//                Log.d(TAG, "Item - "+i+" "+jsonArray1.getString(0));
                final int times_purchased = Integer.parseInt(jsonArray1.getString(2));
                categoriesList.add(
                        new Category(
                                jsonArray1.getString(0),
                                jsonArray1.getString(1),
                                jsonArray1.getString(3),
                                times_purchased
                        )
                );

                categoryAdapter = new BottomSheetCategoryAdapter(this, categoriesList, shoppingMethod);
                bs_recyclerview.setAdapter(categoryAdapter);

            }


        } catch (ExecutionException | JSONException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    void retrieve_products_list() {

        final String type = "retrieve_products_list";
        try {
            final String res = new AwsBackgroundWorker(this).execute(type, store_id, category_name).get();
            Log.d(TAG, "Products list : "+res);

            jsonArray2 = new JSONArray(res);

            for(int i = 0; i < jsonArray2.length(); i++){

                jobj = jsonArray2.getJSONObject(i);
//                Log.d(TAG, "Item - "+i+" "+jsonArray1.getString(0));
                productList.add(
                        new Product(
                                0,
                                jobj.getString("store_id"),
                                jobj.getString("barcode"),
                                jobj.getString("product_name"),
                                jobj.getString("mrp"),
                                jobj.getString("retailer_price"),
                                "0",
                                jobj.getString("our_price"),
                                jobj.getString("total_discount"),
                                "0",
                                jobj.getString("has_image"),
                                jobj.getString("quantity"),
                                jobj.getString("category"),
                                shoppingMethod
                        )
                );

            }

            productListAdapter = new ProductListAdapter(this, productList, shoppingMethod, coordinatorLayout);
            recyclerView.setAdapter(productListAdapter);


        } catch (ExecutionException | JSONException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void retrieve_search_results(String queryString) {
//        search_string = queryString;
        RequestBody body = RequestBody.create(queryString, MediaType.parse("text/plain"));
        RequestBody storeId = RequestBody.create(store_id, MediaType.parse("text/plain"));
        RequestBody catName = RequestBody.create(category_name, MediaType.parse("text/plain"));
        Call<ProductSearchResult> call =  NetworkHandler.getNetworkHandler(getApplicationContext()).getNetworkApi().getSearchResultsByCategory(body, storeId, catName);

        call.enqueue(new Callback<ProductSearchResult>() {


            @Override
            public void onResponse(Call<ProductSearchResult> call, Response<ProductSearchResult> response) {
                Log.d(TAG, "onResponse: call" + response.isSuccessful() );
                ResponseResult result = response.body().getResponseResult();
                productList.clear();
                productListAdapter.notifyDataSetChanged();
                List<List<String>> searchResults = response.body().getProductList();
                if (searchResults != null) {
                    ListIterator<List<String>> iter = searchResults.listIterator();
                    if (result.getResponseCode().equalsIgnoreCase("200")) {
                        while (iter.hasNext()) {
                            List<String> pList = iter.next();
                            productList.add(
                                    new Product(
                                            0,
                                            pList.get(1),
                                            pList.get(0),
                                            pList.get(3),
                                            pList.get(5),
                                            pList.get(7),
                                            "0",
                                            pList.get(10),
                                            pList.get(11),
                                            "0",
                                            pList.get(15),
                                            pList.get(16),
                                            pList.get(17),
                                            shoppingMethod
                                    )
                            );
                            productListAdapter.notifyDataSetChanged();
                        }

                    }
                }
            }

            @Override
            public void onFailure(Call<ProductSearchResult> call, Throwable t) {
                Log.e(TAG, "onFailure: " + call.request() );

                Log.e(TAG, "onFailure: " + t );

                t.printStackTrace();

                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void observeSearchView() {

        disposable = RxSearchObservable.fromView(searchView)
                .debounce(300, TimeUnit.MILLISECONDS)
                .filter(text -> !text.isEmpty() && text.length() >= 3)
                .map(text -> text.toLowerCase().trim())
                .distinctUntilChanged()
                .switchMap(s -> Observable.just(s))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    Log.d(TAG, "Searching for "+s);
                    retrieve_search_results(s);
                });
    }

    public void Go_to_Basket(View view) {
        Intent in = new Intent(this, CartActivity.class);
        in.putExtra("category_name", category_name);
        in.putExtra("comingFrom", "ProductList");
        in.putExtra("shoppingMethod", shoppingMethod);
        startActivity(in);
    }

    public void Go_to_Profile(View view) {
        final String phone = saveInfoLocally.getPhone();
        Intent in = new Intent(this, MyProfile.class);
        in.putExtra("Phone", phone);
        startActivity(in);
    }

    @Override
    public void onBackPressed() {
        Intent in = new Intent(this, ProductsCategory.class);
        in.putExtra("shoppingMethod", shoppingMethod);
        startActivity(in);
//        if(coming_from.equals("Cart")){
//            Intent in = new Intent(this, ProductsCategory.class);
//            in.putExtra("shoppingMethod", shoppingMethod);
//            startActivity(in);
//        } else {
//            super.onBackPressed();
//        }
    }
}
