package sayan.example.com.uberintegrationsample;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.concurrent.ExecutionException;

/**
 * Created by Admin on 01-09-2017.
 */

public class IonConnector {

    private static IonConnector instance = null;

    private IonConnector() {

    }

    public static IonConnector getInstance() {
        if(instance == null) {
            instance = new IonConnector();
        }
        return instance;
    }


    //create the progressDialog object
    public ProgressDialog createProgressDialog(Context context){
        final ProgressDialog dlg = new ProgressDialog(context);
        dlg.setTitle("Loading...");
        dlg.setIndeterminate(false);                            //indeterminate= circular progress
        dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        return dlg;
    }

    //get the JsonObject from the server using Ion Library
    public Future getJSONFuture(final Context context, String url, final ProgressDialog dialog, final boolean shouldDismissDialog) throws
            ExecutionException,
            InterruptedException {
        final Future<JsonObject> resultJSON = Ion.with(context)
                .load(url)
                .progressDialog(dialog)
                .setLogging("sayanJson", Log.DEBUG)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
//                        Toast.makeText(context, "JSON Loaded Successfully!", Toast.LENGTH_SHORT).show();
                        if (shouldDismissDialog) {
                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }
                        }
                    }
                });
        return resultJSON;
    }

    //get the JsonObject from the server using Ion Library
    public JsonObject getJson(final Context context, String url, final ProgressDialog dialog) throws ExecutionException, InterruptedException {
        dialog.show();
        Future<JsonObject> resultJSON = Ion.with(context)
                .load(url)
                .progressDialog(dialog)
                .setLogging("sayanJson", Log.DEBUG)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
//                        Toast.makeText(context, "JSON Loaded Successfully!", Toast.LENGTH_SHORT).show();
                        if (dialog.isShowing()){
                            dialog.dismiss();
                        }
                    }
                });
        return resultJSON.get();
    }
    //get the JsonObject from the server using Ion Library
    public JsonObject getJson(final Context context, String url) throws ExecutionException, InterruptedException {
        Future<JsonObject> resultJSON = Ion.with(context)
                .load(url)
                .setLogging("sayanJson", Log.DEBUG)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
//                        Toast.makeText(context, "JSON Loaded Successfully!", Toast.LENGTH_SHORT).show();
                    }
                });
        return resultJSON.get();
    }


    //get the JSONObject from the server using Ion Library
    public String getJSONAsString(final Context context, String url) throws ExecutionException, InterruptedException {
        Future<String> resultString = Ion.with(context)
                .load(url)
                .setLogging("sayanJson", Log.DEBUG)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
//                        Toast.makeText(context, "JSON Loaded Successfully!", Toast.LENGTH_SHORT).show();
                    }
                });
        return resultString.get();
    }

    // directly CartItemAdapterLoaderAsync image in the ImageView
    public void loadImageDirectly(String imageURL, ImageView imageView, int placeholder, int error) {
        Ion.with(imageView)
                .placeholder(placeholder)                //image when loading
                .error(error)                            //image if error/ no connection/ wrong url etc
                .smartSize(true)
                //uncomment this line and set imageURL with wrong URL and see error image animation
//                .animateLoad(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.spin_animation))
                //this defines animation of the successfully loaded image (animate it in)
//                .animateIn(AnimationUtils.loadAnimation(context, R.anim.fade_in_animation))
                .load(imageURL);        //the url for image
    }

    // directly CartItemAdapterLoaderAsync image in the ImageView
    public void loadImageDirectly(String imageURL, ImageView imageView, int placeholder) {
        Ion.with(imageView)
                .placeholder(placeholder)                //image when loading
                .smartSize(true)
//                .error(error)                            //image if error/ no connection/ wrong url etc
                //uncomment this line and set imageURL with wrong URL and see error image animation
//                .animateLoad(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.spin_animation))
                //this defines animation of the successfully loaded image (animate it in)
//                .animateIn(AnimationUtils.loadAnimation(context, R.anim.fade_in_animation))
                .load(imageURL);        //the url for image
    }

    // directly CartItemAdapterLoaderAsync image in the ImageView
    public void loadImageDirectly(String imageURL, ImageView imageView) {
        Ion.with(imageView)
                .smartSize(true)
//                .placeholder(placeholder)                //image when loading
//                .error(error)                            //image if error/ no connection/ wrong url etc
                //uncomment this line and set imageURL with wrong URL and see error image animation
//                .animateLoad(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.spin_animation))
                //this defines animation of the successfully loaded image (animate it in)
//                .animateIn(AnimationUtils.loadAnimation(context, R.anim.fade_in_animation))
                .load(imageURL);        //the url for image
    }

}
