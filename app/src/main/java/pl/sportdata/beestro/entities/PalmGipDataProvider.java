package pl.sportdata.beestro.entities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.sportdata.beestro.BeestroApplication;
import pl.sportdata.beestro.PalmGipJsonObjectRequest;
import pl.sportdata.beestro.R;
import pl.sportdata.beestro.entities.base.BaseItemContainer;
import pl.sportdata.beestro.entities.bills.Bill;
import pl.sportdata.beestro.entities.bills.BillUtils;
import pl.sportdata.beestro.entities.configuration.Configuration;
import pl.sportdata.beestro.entities.discounts.Discount;
import pl.sportdata.beestro.entities.entries.Entry;
import pl.sportdata.beestro.entities.groups.Group;
import pl.sportdata.beestro.entities.items.Item;
import pl.sportdata.beestro.entities.items.ItemUtils;
import pl.sportdata.beestro.entities.markups.Markup;
import pl.sportdata.beestro.entities.parameters.Parameters;
import pl.sportdata.beestro.entities.paymentTypes.PaymentType;
import pl.sportdata.beestro.entities.sync.SyncObject;
import pl.sportdata.beestro.entities.users.User;
import pl.sportdata.beestro.entities.users.UserUtils;
import pl.sportdata.beestro.modules.credentials.SettingsFragment;

public class PalmGipDataProvider implements DataProvider {

    private static final String LOGIN_URL = "/login";
    private static final String SYNC_URL = "/sync";
    private static final String REGISTER_URL = "/register";
    public static final String SYNC_OBJECT_STORAGE_KEY = "sync-object-storage-key";
    private final Context context;
    private SyncObject syncObject;
    private int challengeCode;
    private String gastroDay = "";
    private ParseResponseTask parseResponseTask;
    private String lastMessage;

    public PalmGipDataProvider(Context context) {
        this.context = context;
        Gson gson = new GsonBuilder().create();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        syncObject = gson.fromJson(sharedPref.getString(SYNC_OBJECT_STORAGE_KEY, null), SyncObject.class);
    }

    @Override
    public boolean hasData() {
        return syncObject != null;
    }

    @Override
    public void cleanUp() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPref.edit().putString(SYNC_OBJECT_STORAGE_KEY, new Gson().toJson(syncObject)).apply();
    }

    @Override
    public void registerPattern(int userId, @NonNull String userPass, @NonNull String pattern, @NonNull DataProviderCredentialsListener listener) {
        String hostUrl = getRegisterHostUrl();
        if (TextUtils.isEmpty(hostUrl)) {
            listener.onLoginFail(context.getString(R.string.wrong_comm_settings));
        } else {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String salePointId = sharedPref.getString(SettingsFragment.SALE_POINT_ID_PREF, "");
            String deviceId = sharedPref.getString(SettingsFragment.DEVICE_ID_PREF, "");

            JSONObject syncJsonObject = null;
            try {
                syncJsonObject = new JSONObject();
                syncJsonObject.put("hash", pattern);
                syncJsonObject.put("id", userId);
                syncJsonObject.put("passwd", userPass);
                syncJsonObject.put("sale_point_id", Integer.valueOf(salePointId));
                syncJsonObject.put("device_id", Integer.valueOf(deviceId));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Volley.newRequestQueue(context)
                    .add(new PalmGipJsonObjectRequest(Request.Method.POST, hostUrl, syncJsonObject, getRegisterResponseListener(listener),
                            getRegisterErrorListener(listener)));
        }
    }

    @Override
    public void login(@NonNull String pattern, @NonNull DataProviderCredentialsListener listener) {
        String hostUrl = getLoginHostUrl();
        if (TextUtils.isEmpty(hostUrl)) {
            listener.onLoginFail(context.getString(R.string.wrong_comm_settings));
        } else {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String salePointId = sharedPref.getString(SettingsFragment.SALE_POINT_ID_PREF, "");
            String deviceId = sharedPref.getString(SettingsFragment.DEVICE_ID_PREF, "");

            JSONObject syncJsonObject = null;
            try {
                syncJsonObject = new JSONObject();
                syncJsonObject.put("hash", pattern);
                syncJsonObject.put("sale_point_id", Integer.valueOf(salePointId));
                syncJsonObject.put("device_id", Integer.valueOf(deviceId));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Volley.newRequestQueue(context).add(new PalmGipJsonObjectRequest(Request.Method.POST, hostUrl, syncJsonObject, getLoginResponseListener(listener),
                    getLoginErrorListener(listener)));
        }
    }

    @Override
    public void login(int userId, @NonNull String userPass, @NonNull DataProviderCredentialsListener listener) {
        String hostUrl = getLoginHostUrl();
        if (TextUtils.isEmpty(hostUrl)) {
            listener.onLoginFail(context.getString(R.string.wrong_comm_settings));
        } else {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String salePointId = sharedPref.getString(SettingsFragment.SALE_POINT_ID_PREF, "");
            String deviceId = sharedPref.getString(SettingsFragment.DEVICE_ID_PREF, "");

            JSONObject syncJsonObject = null;
            try {
                syncJsonObject = new JSONObject();
                syncJsonObject.put("id", userId);
                syncJsonObject.put("passwd", userPass);
                syncJsonObject.put("sale_point_id", Integer.valueOf(salePointId));
                syncJsonObject.put("device_id", Integer.valueOf(deviceId));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Volley.newRequestQueue(context).add(new PalmGipJsonObjectRequest(Request.Method.POST, hostUrl, syncJsonObject, getLoginResponseListener(listener),
                    getLoginErrorListener(listener)));
        }
    }

    private Response.ErrorListener getLoginErrorListener(final DataProviderCredentialsListener listener) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String errorMessage = null;
                if (error.networkResponse != null) {
                    if (error.networkResponse.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        listener.onUnauthorized();
                    } else if (error.networkResponse.data != null) {
                        errorMessage = new String(error.networkResponse.data);
                    }
                } else {
                    errorMessage = error.getMessage();
                }

                listener.onLoginFail(errorMessage);
            }
        };
    }

    private Response.Listener<JSONObject> getLoginResponseListener(final DataProviderCredentialsListener listener) {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Gson gson = new GsonBuilder().create();
                User user = gson.fromJson(response.toString(), User.class);
                if (user != null) {
                    listener.onLoginSuccess(user);
                } else {
                    listener.onLoginFail(context.getString(R.string.empty_response));
                }
            }
        };
    }

    private Response.ErrorListener getRegisterErrorListener(final DataProviderCredentialsListener listener) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String errorMessage = null;
                if (error.networkResponse != null) {
                    if (error.networkResponse.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        listener.onUnauthorized();
                    } else if (error.networkResponse.data != null) {
                        errorMessage = new String(error.networkResponse.data);
                    }
                } else {
                    errorMessage = error.getMessage();
                }

                listener.onRegisterFail(errorMessage);
            }
        };
    }

    private Response.Listener<JSONObject> getRegisterResponseListener(final DataProviderCredentialsListener listener) {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Gson gson = new GsonBuilder().create();
                User user = gson.fromJson(response.toString(), User.class);
                if (user != null) {
                    listener.onRegisterSuccess(user);
                } else {
                    listener.onRegisterFail(context.getString(R.string.empty_response));
                }
            }
        };
    }

    @Override
    public void sync(@NonNull final DataProviderSyncListener listener) {
        sync(getChanged(), listener);
    }

    @Override
    public void cleanSync(@NonNull DataProviderSyncListener listener) {
        sync(SyncObject.getEmpty(), listener);
    }

    @Override
    public void cleanSyncObject(@NonNull DataProviderSyncListener listener) {
        lastMessage = null;
        if (syncObject != null) {
            syncObject.bills = BillUtils.clearModifiedBills(syncObject.bills);
        }
    }

    private void sync(SyncObject syncObject, @NonNull DataProviderSyncListener listener) {
        lastMessage = null;
        String hostUrl = getSyncHostUrl();
        if (TextUtils.isEmpty(hostUrl)) {
            listener.onSyncFinished(context.getString(R.string.wrong_comm_settings));
        } else {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String salePointId = sharedPref.getString(SettingsFragment.SALE_POINT_ID_PREF, "");
            String deviceId = sharedPref.getString(SettingsFragment.DEVICE_ID_PREF, "");

            syncObject.parameters = new Parameters(gastroDay, Integer.parseInt(salePointId), Integer.parseInt(deviceId), challengeCode);

            Gson gson = new GsonBuilder().create();
            JSONObject syncJsonObject = null;
            try {
                syncJsonObject = new JSONObject(gson.toJson(syncObject));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Crashlytics.setString("lastPostSyncObject", syncJsonObject.toString());
            Volley.newRequestQueue(context).add(new PalmGipJsonObjectRequest(Request.Method.POST, hostUrl, syncJsonObject, getSyncResponseListener(listener),
                    getSyncErrorListener(listener, false)));
        }
    }

    @Override
    public void createBillsForSplit(@NonNull final List<Bill> bills, @NonNull final DataProviderSyncListener listener) {
        lastMessage = null;
        String hostUrl = getSyncHostUrl();
        if (TextUtils.isEmpty(hostUrl)) {
            listener.onSyncFinished(context.getString(R.string.wrong_comm_settings));
        } else {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String salePointId = sharedPref.getString(SettingsFragment.SALE_POINT_ID_PREF, "");
            String deviceId = sharedPref.getString(SettingsFragment.DEVICE_ID_PREF, "");

            SyncObject changedSyncObject = SyncObject.getEmpty();
            changedSyncObject.parameters = new Parameters(gastroDay, Integer.parseInt(salePointId), Integer.parseInt(deviceId), challengeCode);
            changedSyncObject.bills = new ArrayList<>();
            for (int i = 1, size = bills.size(); i < size; i++) {
                Bill bill = bills.get(i);
                if (bill.isNew()) {
                    Bill newBill = bill.copy();
                    newBill.setEntries(new ArrayList<Entry>());
                    Item dummyItem = getItems().get(0);
                    BillUtils.addBillEntry(newBill, dummyItem.id, 1, dummyItem.price, 1, 0);
                    changedSyncObject.bills.add(newBill);
                }
            }

            Gson gson = new GsonBuilder().create();
            JSONObject syncJsonObject = null;
            try {
                syncJsonObject = new JSONObject(gson.toJson(changedSyncObject));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Volley.newRequestQueue(context).add(new PalmGipJsonObjectRequest(Request.Method.POST, hostUrl, syncJsonObject, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Gson gson = new GsonBuilder().create();
                    SyncObject newSyncObject = gson.fromJson(response.toString(), SyncObject.class);
                    if (newSyncObject != null) {
                        challengeCode = newSyncObject.configuration.challengeCode;
                        gastroDay = newSyncObject.configuration.gastroDay;
                        if (newSyncObject.bills != null && !newSyncObject.bills.isEmpty()) {
                            if (TextUtils.isEmpty(newSyncObject.messages)) {
                                for (Bill newBill : newSyncObject.bills) {
                                    for (int i = 1, size = bills.size(); i < size; i++) {
                                        Bill bill = bills.get(i);
                                        if (bill.getTableNumber() == newBill.getTableNumber() && bill.getGuestNumber() == newBill.getGuestNumber()) {
                                            bill.setId(newBill.getId());
                                            bill.setNew(false);
                                            for (int j = 0, sizee = newBill.getEntries().size(); j < sizee; j++) {
                                                newBill.getEntries().get(j).setCancelled(true);
                                            }
                                        }
                                    }
                                }

                                stornoDummiesBillsForSplit(newSyncObject.bills, listener);
                            } else {
                                listener.onSyncFinished(newSyncObject.messages);
                            }

                        } else {
                            listener.onSyncFinished(TextUtils.isEmpty(newSyncObject.messages) ? context.getString(R.string.no_bills) : newSyncObject.messages);
                        }
                    } else {
                        listener.onSyncFinished(context.getString(R.string.empty_response));
                    }
                }
            }, getSyncErrorListener(listener, false)) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>(1);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            });
        }
    }

    @Override
    public void moveBillsForSplit(@NonNull final List<Bill> bills, final int moveTo, @NonNull final DataProviderSyncListener listener) {
        lastMessage = null;
        String hostUrl = getSyncHostUrl();
        if (TextUtils.isEmpty(hostUrl)) {
            listener.onSyncFinished(context.getString(R.string.wrong_comm_settings));
        } else {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String salePointId = sharedPref.getString(SettingsFragment.SALE_POINT_ID_PREF, "");
            String deviceId = sharedPref.getString(SettingsFragment.DEVICE_ID_PREF, "");

            SyncObject changedSyncObject = SyncObject.getEmpty();
            changedSyncObject.parameters = new Parameters(gastroDay, Integer.parseInt(salePointId), Integer.parseInt(deviceId), challengeCode);
            changedSyncObject.bills = new ArrayList<>();
            final Bill orgBill = bills.get(0);
            Bill toBill = bills.get(moveTo);
            orgBill.setEntries(new ArrayList<Entry>());
            orgBill.setMoveTo(toBill.getId());
            for (Entry toEntry : toBill.getEntries()) {
                toEntry.setMoved(true);
                toEntry.setNew(false);
                orgBill.getEntries().add(toEntry);
            }
            if (orgBill.getEntries().isEmpty()) {
                if (moveTo == bills.size() - 1) {
                    listener.onSyncFinished(null);
                } else {
                    moveBillsForSplit(bills, moveTo + 1, listener);
                }
            } else {
                changedSyncObject.bills.add(orgBill);

                Gson gson = new GsonBuilder().create();
                JSONObject syncJsonObject = null;
                try {
                    syncJsonObject = new JSONObject(gson.toJson(changedSyncObject));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Volley.newRequestQueue(context)
                        .add(new PalmGipJsonObjectRequest(Request.Method.POST, hostUrl, syncJsonObject, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Gson gson = new GsonBuilder().create();
                        SyncObject newSyncObject = gson.fromJson(response.toString(), SyncObject.class);
                        if (newSyncObject != null) {
                            challengeCode = newSyncObject.configuration.challengeCode;
                            gastroDay = newSyncObject.configuration.gastroDay;
                            if (newSyncObject.bills != null && !newSyncObject.bills.isEmpty()) {
                                if (TextUtils.isEmpty(newSyncObject.messages)) {
                                    //need to find original bill and update it's items ids, as it changes after moving to other bill
                                    boolean billFound = false;
                                    for (Bill gipBill : newSyncObject.bills) {
                                        if (gipBill.getId() == orgBill.getId()) {
                                            for (Entry gipEntry : gipBill.getEntries()) {
                                                for (Entry orgEntry : orgBill.getEntries()) {
                                                    if (gipEntry.getItemId() == orgEntry.getItemId()) {
                                                        orgEntry.setId(gipEntry.getId());
                                                    }
                                                }
                                            }
                                            billFound = true;
                                            break;
                                        }
                                    }

                                    if (billFound) {
                                        for (int i = 1, size = bills.size(); i < size; i++) {
                                            Bill toBill = bills.get(i);
                                            for (Entry toEntry : toBill.getEntries()) {
                                                for (Entry orgEntry : orgBill.getEntries()) {
                                                    if (toEntry.getItemId() == orgEntry.getItemId()) {
                                                        toEntry.setId(orgEntry.getId());
                                                    }
                                                }
                                            }
                                        }

                                        if (moveTo == bills.size() - 1) {
                                            listener.onSyncFinished(TextUtils.isEmpty(newSyncObject.messages) ? null : newSyncObject.messages);
                                        } else {
                                            moveBillsForSplit(bills, moveTo + 1, listener);
                                        }
                                    } else {
                                        listener.onSyncFinished(context.getString(R.string.wrong_response_after_split));
                                    }
                                } else {
                                    listener.onSyncFinished(newSyncObject.messages);
                                }
                            } else {
                                listener.onSyncFinished(
                                        TextUtils.isEmpty(newSyncObject.messages) ? context.getString(R.string.no_bills) : newSyncObject.messages);
                            }
                        } else {
                            listener.onSyncFinished(context.getString(R.string.empty_response));
                        }
                    }
                }, getSyncErrorListener(listener, false)) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>(1);
                        headers.put("Content-Type", "application/json");
                        return headers;
                    }
                });
            }
        }
    }

    @Override
    public void mergeBills(@NonNull Bill bill, @NonNull final DataProviderSyncListener listener) {
        lastMessage = null;
        String hostUrl = getSyncHostUrl();
        if (TextUtils.isEmpty(hostUrl)) {
            listener.onSyncFinished(context.getString(R.string.wrong_comm_settings));
        } else {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String salePointId = sharedPref.getString(SettingsFragment.SALE_POINT_ID_PREF, "");
            String deviceId = sharedPref.getString(SettingsFragment.DEVICE_ID_PREF, "");

            SyncObject changedSyncObject = SyncObject.getEmpty();
            changedSyncObject.parameters = new Parameters(gastroDay, Integer.parseInt(salePointId), Integer.parseInt(deviceId), challengeCode);
            changedSyncObject.bills = Collections.singletonList(bill);

            Gson gson = new GsonBuilder().create();
            JSONObject syncJsonObject = null;
            try {
                syncJsonObject = new JSONObject(gson.toJson(changedSyncObject));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Volley.newRequestQueue(context).add(new PalmGipJsonObjectRequest(Request.Method.POST, hostUrl, syncJsonObject, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Gson gson = new GsonBuilder().create();
                    SyncObject newSyncObject = gson.fromJson(response.toString(), SyncObject.class);
                    if (newSyncObject != null) {
                        challengeCode = newSyncObject.configuration.challengeCode;
                        gastroDay = newSyncObject.configuration.gastroDay;
                        if (newSyncObject.bills != null && !newSyncObject.bills.isEmpty()) {
                            if (TextUtils.isEmpty(newSyncObject.messages)) {
                                listener.onSyncFinished(null);
                            } else {
                                listener.onSyncFinished(newSyncObject.messages);
                            }
                        } else {
                            listener.onSyncFinished(TextUtils.isEmpty(newSyncObject.messages) ? context.getString(R.string.no_bills) : newSyncObject.messages);
                        }
                    } else {
                        listener.onSyncFinished(context.getString(R.string.empty_response));
                    }
                }
            }, getSyncErrorListener(listener, false)) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>(1);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            });
        }
    }

    private void stornoDummiesBillsForSplit(@NonNull final List<Bill> bills, @NonNull final DataProviderSyncListener listener) {
        lastMessage = null;
        String hostUrl = getSyncHostUrl();
        if (TextUtils.isEmpty(hostUrl)) {
            listener.onSyncFinished(context.getString(R.string.wrong_comm_settings));
        } else {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String salePointId = sharedPref.getString(SettingsFragment.SALE_POINT_ID_PREF, "");
            String deviceId = sharedPref.getString(SettingsFragment.DEVICE_ID_PREF, "");

            SyncObject changedSyncObject = SyncObject.getEmpty();
            changedSyncObject.parameters = new Parameters(gastroDay, Integer.parseInt(salePointId), Integer.parseInt(deviceId), challengeCode);
            changedSyncObject.bills = bills;

            Gson gson = new GsonBuilder().create();
            JSONObject syncJsonObject = null;
            try {
                syncJsonObject = new JSONObject(gson.toJson(changedSyncObject));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Volley.newRequestQueue(context).add(new PalmGipJsonObjectRequest(Request.Method.POST, hostUrl, syncJsonObject, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Gson gson = new GsonBuilder().create();
                    SyncObject newSyncObject = gson.fromJson(response.toString(), SyncObject.class);
                    if (newSyncObject != null) {
                        challengeCode = newSyncObject.configuration.challengeCode;
                        gastroDay = newSyncObject.configuration.gastroDay;
                        if (newSyncObject.bills != null && !newSyncObject.bills.isEmpty()) {
                            if (TextUtils.isEmpty(newSyncObject.messages)) {
                                listener.onSyncFinished(null);
                            } else {
                                listener.onSyncFinished(newSyncObject.messages);
                            }
                        } else {
                            listener.onSyncFinished(TextUtils.isEmpty(newSyncObject.messages) ? context.getString(R.string.no_bills) : newSyncObject.messages);
                        }
                    } else {
                        listener.onSyncFinished(context.getString(R.string.empty_response));
                    }
                }
            }, getSyncErrorListener(listener, false)) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>(1);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            });
        }
    }

    @Nullable
    private String getSyncHostUrl() {
        String hostUrl = getHostUrl();
        if (!TextUtils.isEmpty(hostUrl)) {
            hostUrl += SYNC_URL;
        }

        return hostUrl;
    }

    @Nullable
    private String getLoginHostUrl() {
        String hostUrl = getHostUrl();
        if (!TextUtils.isEmpty(hostUrl)) {
            hostUrl += LOGIN_URL;
        }

        return hostUrl;
    }

    @Nullable
    private String getRegisterHostUrl() {
        String hostUrl = getHostUrl();
        if (!TextUtils.isEmpty(hostUrl)) {
            hostUrl += REGISTER_URL;
        }

        return hostUrl;
    }

    private SyncObject getChanged() {
        SyncObject changedSyncObject = SyncObject.getEmpty();

        if (syncObject != null) {
            List<Bill> bills = syncObject.bills;
            if (bills != null && !bills.isEmpty()) {
                changedSyncObject.bills = new ArrayList<>();
                for (Bill bill : bills) {
                    Bill modifiedBill = BillUtils.getModified(bill, ItemUtils.findSeparatorItem(getItems()), ItemUtils.findDescriptorItem(getItems()));
                    if (modifiedBill != null) {
                        changedSyncObject.bills.add(modifiedBill);
                    }
                }
            }
        }

        return changedSyncObject;
    }

    private Response.ErrorListener getSyncErrorListener(final DataProviderSyncListener listener, final boolean clearData) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (clearData) {
                    syncObject = null;
                }
                lastMessage = null;

                if (error != null && error.networkResponse != null) {
                    if (error.networkResponse.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        listener.onUnauthorized();
                    } else if (error.networkResponse.data != null) {
                        listener.onSyncFinished(new String(error.networkResponse.data));
                    } else {
                        listener.onSyncFinished(String.format(context.getString(R.string.network_error), error.networkResponse.statusCode));
                    }
                } else if (error != null && !TextUtils.isEmpty(error.getMessage())) {
                    listener.onSyncFinished(error.getMessage());
                } else {
                    listener.onSyncFinished(context.getString(R.string.check_connection));
                }
            }
        };
    }

    private Response.Listener<JSONObject> getSyncResponseListener(final DataProviderSyncListener listener) {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (parseResponseTask != null && parseResponseTask.getStatus() != AsyncTask.Status.FINISHED) {
                    parseResponseTask.cancel(true);
                }
                parseResponseTask = new ParseResponseTask(listener);
                parseResponseTask.execute(response);
            }
        };
    }

    @Nullable
    private String getHostUrl() {
        String hostUrl = null;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String hostIp = sharedPref.getString(SettingsFragment.HOST_IP_PREF, "");
        String hostPort = sharedPref.getString(SettingsFragment.HOST_PORT_PREF, "");

        if (!TextUtils.isEmpty(hostIp)) {
            hostUrl = hostIp;
            if (!hostUrl.startsWith("http://")) {
                hostUrl = "http://" + hostUrl;
            }

            if (!TextUtils.isEmpty(hostPort)) {
                hostUrl += ":" + hostPort;
            }
        }

        return hostUrl;
    }

    @NonNull
    @Override
    public List<Group> getGroups() {
        return syncObject != null && syncObject.groups != null ? syncObject.groups : new ArrayList<Group>(0);
    }

    @NonNull
    @Override
    public List<Item> getItems() {
        List<Item> items = new ArrayList<>(0);
        if (syncObject != null && syncObject.groups != null) {
            for (Group group : syncObject.groups) {
                if (group.items != null) {
                    items.addAll(group.items);
                }
            }
        }

        return items;
    }

    @NonNull
    @Override
    public List<Item> getItems(int groupId) {
        List<Item> items = new ArrayList<>();
        if (syncObject != null && syncObject.groups != null) {
            for (Group group : syncObject.groups) {
                if (group.id == groupId) {
                    if (group.items != null) {
                        items.addAll(group.items);
                    }
                    break;
                }
            }
        }

        return items;
    }

    @NonNull
    @Override
    public List<Bill> getBills() {
        return syncObject != null && syncObject.bills != null ? syncObject.bills : new ArrayList<Bill>(0);
    }

    @NonNull
    @Override
    public List<User> getUsers() {
        return syncObject != null && syncObject.users != null ? syncObject.users : new ArrayList<User>(0);
    }

    @Nullable
    @Override
    public User getUser(int userId) {
        User user = null;
        if (syncObject != null && syncObject.users != null) {
            for (User user1 : syncObject.users) {
                if (user1.id == userId) {
                    user = user1;
                    break;
                }
            }
        }

        return user;
    }

    @NonNull
    @Override
    public BaseItemContainer<Markup> getMarkups() {
        return syncObject != null && syncObject.markups != null ? syncObject.markups : new BaseItemContainer<>(new ArrayList<Markup>(), 0, null);
    }

    @NonNull
    @Override
    public BaseItemContainer<PaymentType> getPaymentTypes() {
        return syncObject != null && syncObject.paymentTypes != null ? syncObject.paymentTypes : new BaseItemContainer<>(new ArrayList<PaymentType>(), 0, null);
    }

    @NonNull
    @Override
    public BaseItemContainer<Discount> getDiscounts() {
        return syncObject != null && syncObject.discounts != null ? syncObject.discounts : new BaseItemContainer<>(new ArrayList<Discount>(), 0, null);
    }

    @Nullable
    @Override
    public Item getItem(int id) {
        Item item = null;
        if (syncObject != null && syncObject.groups != null) {
            for (Group group : syncObject.groups) {
                for (Item item1 : group.items) {
                    if (item1.id == id) {
                        item = item1;
                        break;
                    }
                }
            }
        }

        return item;
    }

    @Nullable
    @Override
    public Discount getDiscount(int id) {
        Discount discount = null;
        if (syncObject != null && syncObject.discounts != null && syncObject.discounts.items != null) {
            for (Discount item : syncObject.discounts.items) {
                if (item.id == id) {
                    discount = item;
                    break;
                }
            }
        }

        return discount;
    }

    @Nullable
    @Override
    public PaymentType getPaymentType(int id) {
        PaymentType paymentType = null;
        if (syncObject != null && syncObject.paymentTypes != null && syncObject.paymentTypes.items != null) {
            for (PaymentType item : syncObject.paymentTypes.items) {
                if (item.id == id) {
                    paymentType = item;
                    break;
                }
            }
        }

        return paymentType;
    }

    @Nullable
    @Override
    public Markup getMarkup(int id) {
        Markup markup = null;
        if (syncObject != null && syncObject.markups != null && syncObject.markups.items != null) {
            for (Markup item : syncObject.markups.items) {
                if (item.id == id) {
                    markup = item;
                    break;
                }
            }
        }

        return markup;
    }

    @Nullable
    @Override
    public Bill getBillByTableId(int tableId) {
        Bill bill = null;
        if (syncObject != null && syncObject.bills != null) {
            for (Bill bill1 : syncObject.bills) {
                if (tableId == bill1.getTableNumber()) {
                    bill = bill1;
                }
            }
        }

        return bill;
    }

    @Nullable
    @Override
    public Bill getBillByTableIdGuestNumber(int tableId, int guestNumber) {
        Bill bill = null;
        if (syncObject != null && syncObject.bills != null) {
            for (Bill bill1 : syncObject.bills) {
                if (tableId == bill1.getTableNumber() && guestNumber == bill1.getGuestNumber()) {
                    bill = bill1;
                }
            }
        }

        return bill;
    }

    @Override
    public String getMessage() {
        String message = null;
        if (!TextUtils.isEmpty(lastMessage)) {
            message = lastMessage;
            lastMessage = null;
        }
        return message;
    }

    @Override
    public Bill getBillById(int billId) {
        if (syncObject != null && syncObject.bills != null) {
            for (Bill bill : syncObject.bills) {
                if (bill.getId() == billId) {
                    return bill;
                }
            }
        }

        return null;
    }

    class ParseResponseTask extends AsyncTask<JSONObject, Void, SyncObject> {

        private final DataProviderSyncListener listener;

        public ParseResponseTask(DataProviderSyncListener listener) {
            this.listener = listener;
        }

        @Override
        protected SyncObject doInBackground(JSONObject... params) {
            SyncObject newSyncObject = null;
            JSONObject response = params[0];
            if (response != null) {
                List<User> users = null;
                List<Bill> bills = null;
                List<Group> groups = null;
                BaseItemContainer<Markup> markups = null;
                BaseItemContainer<Discount> discounts = null;
                BaseItemContainer<PaymentType> paymentTypes = null;
                String messages;
                Configuration configuration = null;
                Parameters parameters = null;

                Gson gson = new GsonBuilder().create();
                try {
                    JSONArray jsonArray = response.optJSONArray("users");
                    if (jsonArray != null) {
                        users = new ArrayList<>();
                        for (int i = 0, size = jsonArray.length(); i < size; i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            users.add(gson.fromJson(jsonObject.toString(), User.class));
                        }
                    }

                    jsonArray = response.optJSONArray("bills");
                    if (jsonArray != null) {
                        bills = new ArrayList<>();
                        for (int i = 0, size = jsonArray.length(); i < size; i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            bills.add(gson.fromJson(jsonObject.toString(), Bill.class));
                        }
                    }

                    jsonArray = response.optJSONArray("groups");
                    if (jsonArray != null) {
                        groups = new ArrayList<>();
                        for (int i = 0, size = jsonArray.length(); i < size; i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            groups.add(gson.fromJson(jsonObject.toString(), Group.class));
                        }
                    }

                    JSONObject jsonObject = response.optJSONObject("markups");
                    if (jsonObject != null) {
                        Type type = new TypeToken<BaseItemContainer<Markup>>() {
                        }.getType();
                        markups = gson.fromJson(jsonObject.toString(), type);
                    }

                    jsonObject = response.optJSONObject("discounts");
                    if (jsonObject != null) {
                        Type type = new TypeToken<BaseItemContainer<Discount>>() {
                        }.getType();
                        discounts = gson.fromJson(jsonObject.toString(), type);
                    }

                    jsonObject = response.optJSONObject("payment_types");
                    if (jsonObject != null) {
                        Type type = new TypeToken<BaseItemContainer<PaymentType>>() {
                        }.getType();
                        paymentTypes = gson.fromJson(jsonObject.toString(), type);
                    }

                    messages = response.optString("messages");

                    jsonObject = response.optJSONObject("configuration");
                    if (jsonObject != null) {
                        configuration = gson.fromJson(jsonObject.toString(), Configuration.class);
                    }

                    jsonObject = response.optJSONObject("parameters");
                    if (jsonObject != null) {
                        parameters = gson.fromJson(jsonObject.toString(), Parameters.class);
                    }

                    newSyncObject = new SyncObject(users, markups, discounts, paymentTypes, groups, bills, messages, configuration, parameters);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (newSyncObject != null) {
                if (newSyncObject.bills != null) {
                    for (Bill newBill : newSyncObject.bills) {
                        List<Entry> newEntries = newBill.getEntries();
                        if (newEntries != null && !newEntries.isEmpty()) {
                            Bill oldBill = getBillById(newBill.getId());
                            if (oldBill == null) {
                                //search for created bill
                            }

                            if (oldBill != null) {
                                List<Entry> oldEntries = oldBill.getEntries();
                                for (Entry newEntry : newEntries) {
                                    for (Entry oldEntry : oldEntries) {
                                        if (newEntry.getId() == oldEntry.getId() && newEntry.getItemId() == oldEntry.getItemId()) {
                                            newEntry.setGuest(oldEntry.getGuest());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Item separator = null;
                if (newSyncObject.groups != null) {
                    for (Group group : newSyncObject.groups) {
                        if (separator == null) {
                            separator = ItemUtils.findSeparatorItem(group.items);
                        }

                        if (group.items != null) {
                            for (Item item : group.items) {
                                item.groupId = group.id;
                            }
                        }
                    }
                }

                if (newSyncObject.bills != null) {
                    for (int i = 0, size = newSyncObject.bills.size(); i < size; i++) {
                        Bill bill = newSyncObject.bills.get(i);
                        if (bill != null) {
                            if (separator != null) {
                                newSyncObject.bills.set(i, BillUtils.calculateBillGroups(bill, separator.id));
                            }
                            newSyncObject.bills.set(i, BillUtils.calculateBillDescriptions(bill));
                        }
                    }
                }
            }

            return newSyncObject;
        }

        @Override
        protected void onPostExecute(SyncObject newSyncObject) {
            super.onPostExecute(newSyncObject);
            if (!isCancelled()) {
                if (newSyncObject != null) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    User user = new Gson().fromJson(preferences.getString(BeestroApplication.PREF_KEY_USER, null), User.class);
                    User updatedUser = UserUtils.findUserWithId(user.id, newSyncObject.users);
                    if (updatedUser != null) {
                        preferences.edit().putString(BeestroApplication.PREF_KEY_USER, new Gson().toJson(updatedUser)).apply();
                    }
                    challengeCode = newSyncObject.configuration.challengeCode;
                    gastroDay = newSyncObject.configuration.gastroDay;
                    if (TextUtils.isEmpty(newSyncObject.messages)) {
                        lastMessage = null;
                    } else {
                        if (newSyncObject.bills == null) {
                            newSyncObject.bills = new ArrayList<>();
                        }

                        if (syncObject.bills != null) {
                            for (Bill localBill : syncObject.bills) {
                                if (localBill.isNew()) {
                                    newSyncObject.bills.add(localBill);
                                } else if (localBill.isModified()) {
                                    for (Bill gipBill : newSyncObject.bills) {
                                        if (gipBill.getId() == localBill.getId()) {
                                            if (gipBill.getEntries() == null) {
                                                gipBill.setEntries(new ArrayList<Entry>());
                                            }
                                            for (Entry localEntry : localBill.getEntries()) {
                                                if (localEntry.isNew()) {
                                                    gipBill.getEntries().add(localEntry);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        lastMessage = newSyncObject.messages;
                    }
                    syncObject = newSyncObject;
                }

                if (listener != null && !isCancelled()) {
                    listener.onSyncFinished(null);
                }
            }
        }
    }
}