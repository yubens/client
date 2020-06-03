package ar.com.idus.www.buyidusapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.gson.Gson;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import ar.com.idus.www.buyidusapp.models.Customer;
import ar.com.idus.www.buyidusapp.utilities.Constants;
import ar.com.idus.www.buyidusapp.utilities.ResponseObject;
import ar.com.idus.www.buyidusapp.utilities.SoftInputAssist;
import ar.com.idus.www.buyidusapp.utilities.Utilities;


public class MainActivity extends AppCompatActivity {
    Button btnEnter;
    TextView txtIdCustomer;
    EditText editIdCustomer, editPassCustomer;
    Customer customer;
    SharedPreferences sharedPreferences;
    boolean firstEntry = false;
    SoftInputAssist softInputAssist;


    public boolean isFirstEntry() {
        return this.firstEntry;
    }

    public void setFirstEntry(boolean firstEntry) {
        this.firstEntry = firstEntry;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        softInputAssist = new SoftInputAssist(this);

        ResponseObject responseToken;
        String idPhone, idCustomer;

        btnEnter = findViewById(R.id.btnEnter);
        txtIdCustomer = findViewById(R.id.txtIdCustomer);
        editIdCustomer = findViewById(R.id.editIdCustomer);
        editPassCustomer = findViewById(R.id.editPassCustomer);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    Constants.REQUEST_CODE_STATE);
        }

        if(Utilities.getData(sharedPreferences, "token").equals(Constants.NO_RESULT_STR)) {
            responseToken = Utilities.getNewToken(getApplicationContext(), sharedPreferences);

            if (responseToken == null) {
                showExit(getString(R.string.msgErrToken));
                return;
            }

            if (responseToken.getResponseCode() == Constants.SHOW_EXIT) {
                showExit(responseToken.getResponseData());
                return;
            }
        }

        idPhone = getIdPhone();

        idCustomer = Utilities.getData(sharedPreferences,"idCustomer");

        if (idCustomer.equals(Constants.NO_RESULT_STR))
            idCustomer = "";

        ResponseObject responsePhone = findPhone(idPhone);

        if (responsePhone == null) {
            showExit(getString(R.string.msgErrFind));
            return;
        }

        if (responsePhone.getResponseCode() == Constants.SHOW_EXIT) {
            showExit(responsePhone.getResponseData());
            return;
        }

        setFirstEntry(responsePhone.getResponseCode() == Constants.CREATED  || idCustomer.isEmpty());
        editIdCustomer.setText(idCustomer);

        if (!isFirstEntry())
            txtIdCustomer.setVisibility(View.GONE);
        else {
            editPassCustomer.setVisibility(View.GONE);
            System.out.println("primer response code findphone " + responsePhone.getResponseCode());
        }

        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id;
                String pass;
                boolean firstTime =  isFirstEntry();
                ResponseObject responseCustomer;

                id = editIdCustomer.getText().toString();
                pass = editPassCustomer.getText().toString();

                if (firstTime && id.isEmpty()) {
                    showMsg(getString(R.string.msgErrorEmptyId));
                } else if (!firstTime && (id.isEmpty() || pass.isEmpty())){
                    showMsg(getString(R.string.msgErrorEmptyIdPass));
                } else {
                    responseCustomer = getCustomer(id);

                    if (responseCustomer != null) {
                        switch (responseCustomer.getResponseCode()) {
                            case Constants.OK:
                                checkCustomer(responseCustomer.getResponseData(), id, pass, firstTime);
                                System.out.println(responseCustomer.getResponseData());
                                break;

                            case Constants.SHOW_ERROR:
                                showMsg(responseCustomer.getResponseData());
                                break;

                            case Constants.SHOW_EXIT:
                                showExit(responseCustomer.getResponseData());
                                break;
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        softInputAssist.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        softInputAssist.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        softInputAssist.onPause();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case Constants.REQUEST_CODE_STATE:
                if (grantResults.length == 0)
                    return;

                if (grantResults[0] != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_PHONE_STATE)) {

                        showMessageOKCancel(getString(R.string.msgErrorManualConfig),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    onBackPressed();
                                }
                            });

                        return;

                    }

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                            != PackageManager.PERMISSION_GRANTED) {
                        showMessageOKCancel(getString(R.string.msgErrorState),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.READ_PHONE_STATE},
                                            Constants.REQUEST_CODE_STATE);

                                }
                            });
                    }

                }

                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.btnAccept), okListener)
            .setCancelable(false)
            .create()
            .show();
    }

    private void checkCustomer (String data, String id, String pass, boolean firstTime) {
        Gson gson = new Gson();
        Customer[] customers;

        customers = gson.fromJson(data, Customer[].class);
        customer = customers[0];

        if (!customer.getHabilidado().equals(Constants.ENABLED) ) {
            showExit(getString(R.string.msgDisabledCustomer));
            return;
        }

        customer.setIdCliente(id);

        if (customer.getEmailOtorgado() == null || customer.getContrasena() == null || customer.getDireccionOtorgada() == null || customer.getTelefonoOtorgado() == null ||
                customer.getEmailOtorgado().isEmpty() || customer.getContrasena().isEmpty() || customer.getDireccionOtorgada().isEmpty() || customer.getTelefonoOtorgado().isEmpty() ||
                isFirstEntry()) {
            callRegister();
            return;
        }

        if (!isFirstEntry() && !customer.getContrasena().equals(pass)) {
            showMsg(getString(R.string.msgErrWrongPass));
            return;
        }

        callDistributor();
    }

    private void callDistributor() {
        Intent intent = new Intent(getApplicationContext(), DistributorActivity.class);
        intent.putExtra("customer", customer);
        startActivity(intent);
    }

    private void showExit(String msg) {
        Intent intent = new Intent(this, ErrorActivity.class);
        intent.putExtra("error", msg);
        startActivity(intent);
    }

    private void showMsg(String msg) {
        if (!MainActivity.this.isFinishing())
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        finishAndRemoveTask();
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    private ResponseObject findPhone(String idPhone) {
        int code;
        String url = "/findtelephone.php?token=" + Utilities.getData(sharedPreferences, "token") + "&idTelephone=" + idPhone;
        ResponseObject responseToken;

        ResponseObject responseObject = Utilities.getResponse(getApplicationContext(), url, 0);
        code = responseObject.getResponseCode();


        if (code == Constants.SERVER_ERROR || code == Constants.EXCEPTION || code == Constants.NO_DATA)
            responseObject = Utilities.getResponse(getApplicationContext(), url, 0);

        if (responseObject.getResponseCode() == Constants.INVALID_TOKEN) {
            responseToken = Utilities.getNewToken(getApplicationContext(), sharedPreferences);

            if (responseToken == null) {
                responseObject.setResponseCode(Constants.SHOW_EXIT);
                responseObject.setResponseData(getString(R.string.msgErrToken));

            } else if (responseToken.getResponseCode() == Constants.SHOW_EXIT) {
                responseObject.setResponseCode(Constants.SHOW_EXIT);
                responseObject.setResponseData(responseToken.getResponseData());
            } else {
                url = "/findtelephone.php?token=" + responseToken.getResponseData() + "&idTelephone=" + idPhone;
                responseObject = Utilities.getResponse(getApplicationContext(), url, 0);

                code = responseObject.getResponseCode();

                if (code == Constants.SERVER_ERROR || code == Constants.EXCEPTION || code == Constants.NO_DATA)
                    responseObject = Utilities.getResponse(getApplicationContext(), url, 0);
            }
        }

        switch (responseObject.getResponseCode()) {
            case Constants.OK:
                Utilities.saveData(sharedPreferences, "idPhone", idPhone);
                break;

            case Constants.NO_INTERNET:
                responseObject.setResponseCode(Constants.SHOW_EXIT);
                responseObject.setResponseData(getString(R.string.msgErrInternet));
                break;

            case Constants.SHOW_EXIT:
                break;

            case Constants.DISABLED:
                responseObject.setResponseCode(Constants.SHOW_EXIT);
                responseObject.setResponseData(getString(R.string.msgDisabledPhone));
                break;

            case Constants.CREATED: // primer ingreso
                Utilities.saveData(sharedPreferences, "idPhone", idPhone);
                System.out.println("primer ingreso");
                break;

            case Constants.NO_DATA:
                responseObject.setResponseCode(Constants.SHOW_EXIT);
                responseObject.setResponseData(getString(R.string.msgErrFind));
                break;

            case Constants.EXCEPTION:
                responseObject.setResponseCode(Constants.SHOW_EXIT);
                responseObject.setResponseData(getString(R.string.msgErrException) + " (" + responseObject.getResponseData() + ")");
                break;

            case Constants.SERVER_ERROR:
                responseObject.setResponseCode(Constants.SHOW_EXIT);
                responseObject.setResponseData(getString((R.string.msgErrServer)));
                break;

            case Constants.INVALID_TOKEN:
                responseObject.setResponseCode(Constants.SHOW_EXIT);
                responseObject.setResponseData(getString((R.string.msgErrToken)));
                break;
        }

        return responseObject;
    }

    private String getIdPhone() {
        String idPhone = Utilities.getData(sharedPreferences,"idPhone");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    100);
        }

        if (idPhone.equals(Constants.NO_RESULT_STR)) {
            idPhone = UUID.randomUUID().toString();

//            TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
//            idPhone = telephonyManager.getDeviceId();

//            int min = 10000;
//            int max = 10100;
//
//            int random_int = (int)(Math.random() * (max - min + 1) + min);
//            idPhone = String.valueOf(random_int);

            Utilities.saveData(sharedPreferences, "idPhone", idPhone);
        }

        return idPhone;
    }

    private void callRegister() {
        Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
        intent.putExtra("customer", customer);
        startActivity(intent);
    }

    private ResponseObject getCustomer(String id){
        String url = "/getCustomer.php?token=" + Utilities.getData(sharedPreferences, "token") + "&idCustomer=" + id;
        ResponseObject responseObject = Utilities.getResponse(getApplicationContext(), url, 5000);
        ResponseObject responseToken;

        int code = responseObject.getResponseCode();

        if (code == Constants.SERVER_ERROR || code == Constants.EXCEPTION || code == Constants.NO_DATA)
            responseObject = Utilities.getResponse(getApplicationContext(), url, 5000);

        if (responseObject.getResponseCode() == Constants.INVALID_TOKEN) {
            responseToken = Utilities.getNewToken(getApplicationContext(), sharedPreferences);

            if (responseToken == null) {
                responseObject.setResponseCode(Constants.SHOW_EXIT);
                responseObject.setResponseData(getString(R.string.msgErrToken));

            } else if (responseToken.getResponseCode() == Constants.SHOW_EXIT) {
                responseObject.setResponseCode(Constants.SHOW_EXIT);
                responseObject.setResponseData(responseToken.getResponseData());
            } else {
                url = "/getCustomer.php?token=" + responseToken.getResponseData() + "&idCustomer=" + id;
                responseObject = Utilities.getResponse(getApplicationContext(), url, 5000);

                code = responseObject.getResponseCode();

                if (code == Constants.SERVER_ERROR || code == Constants.EXCEPTION || code == Constants.NO_DATA)
                    responseObject = Utilities.getResponse(getApplicationContext(), url, 5000);
            }
        }

        switch (responseObject.getResponseCode()) {
            case Constants.NO_INTERNET:
                responseObject.setResponseCode(Constants.SHOW_ERROR);
                responseObject.setResponseData(getString(R.string.msgErrInternet));
                break;

            case Constants.SHOW_EXIT:
                break;

            case Constants.DISABLED:
                responseObject.setResponseCode(Constants.SHOW_EXIT);
                responseObject.setResponseData(getString(R.string.msgDisabledPhone));
                break;

            case Constants.NO_DATA:
                responseObject.setResponseCode(Constants.SHOW_ERROR);
                responseObject.setResponseData(getString(R.string.msgErrCustomerData));
                break;

            case Constants.EXCEPTION:
                responseObject.setResponseCode(Constants.SHOW_ERROR);
                responseObject.setResponseData(getString(R.string.msgErrException) + " (" + responseObject.getResponseData() + ")");
                break;

            case Constants.SERVER_ERROR:
                responseObject.setResponseCode(Constants.SHOW_ERROR);
                responseObject.setResponseData(getString((R.string.msgErrServer)));
                break;

            case Constants.INVALID_TOKEN:
                responseObject.setResponseCode(Constants.SHOW_ERROR);
                responseObject.setResponseData(getString((R.string.msgErrToken)));
                break;
        }

        return responseObject;
    }
}
