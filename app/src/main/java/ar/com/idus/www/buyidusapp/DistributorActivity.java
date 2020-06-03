package ar.com.idus.www.buyidusapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import ar.com.idus.www.buyidusapp.models.Company;
import ar.com.idus.www.buyidusapp.models.Customer;
import ar.com.idus.www.buyidusapp.utilities.Constants;
import ar.com.idus.www.buyidusapp.utilities.ResponseObject;
import ar.com.idus.www.buyidusapp.utilities.Utilities;

public class DistributorActivity extends AppCompatActivity {
    Customer customer;
    SharedPreferences sharedPreferences;
    Company company;
    TextView txtDistName, txtDistEmail, txtDistTitle, txtEmailTitle;
    Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distributor);
        Bundle bundle = getIntent().getExtras();
        ResponseObject responseCompany;

        btnContinue = findViewById(R.id.btnContinue);
        txtDistEmail = findViewById(R.id.txtDistEmail);
        txtDistName = findViewById(R.id.txtDistName);
        txtDistTitle = findViewById(R.id.txtDistTitle);
        txtEmailTitle = findViewById(R.id.txtEmailTitle);

        if (bundle == null) {
            showExit(getString(R.string.msgErrDistribData));
            return;
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        customer = (Customer) bundle.getSerializable("customer");

        if (customer == null) {
            showExit(getString(R.string.msgErrCustomerData));
            return;
        }

        company = (Company) bundle.getSerializable("company");

        if (company == null) {
            responseCompany = getCompany(customer.getEmpresaId());

            if (responseCompany != null) {
                switch (responseCompany.getResponseCode()) {
                    case Constants.OK:
                        checkCompany(responseCompany.getResponseData());
                        break;

                    case Constants.SHOW_ERROR:
                        showMsg(responseCompany.getResponseData());
                        break;

                    case Constants.SHOW_EXIT:
                        showMsg(responseCompany.getResponseData());
                        showExit(responseCompany.getResponseData());
                        break;
                }
            }
        }

        txtDistName.setText(company.getNombre() != null ? company.getNombre() : getString(R.string.notAvailableData));
        txtDistEmail.setText(company.getCorreo() != null ? company.getCorreo() : getString(R.string.notAvailableData));

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callOrder();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    private void showMsg(String msg) {
        if (!DistributorActivity.this.isFinishing())
            Toast.makeText(DistributorActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    private void callOrder() {
        Intent intent = new Intent(getApplicationContext(), OrderActivity.class);
        intent.putExtra("customer", customer);
        intent.putExtra("company", company);
        startActivity(intent);
    }

    private void showExit(String data) {
        Intent intent = new Intent(this, ErrorActivity.class);
        intent.putExtra("error", data);
        startActivity(intent);
    }

    private void checkCompany(String data) {
        Gson gson = new Gson();
        Company[] companies;

        companies = gson.fromJson(data, Company[].class);
        company = companies[0];
    }

    private ResponseObject getCompany (String id) {
        String url = "/getCompany.php?token=" + Utilities.getData(sharedPreferences, "token") + "&idCompany=" + id;
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
                url = "/getCompany.php?token=" + responseToken.getResponseData() + "&idCustomer=" + id;
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
