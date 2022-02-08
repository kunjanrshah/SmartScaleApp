package com.krs.smart.mqtt;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.krs.smart.MainActivity;
import com.krs.smart.R;
import com.krs.smart.WeighingScaleApplication;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Date;

public class MessageService extends Service {

    // App alert event constants
    public final static String EVENT_ALERT = "EVENT_ALERT";
    public final static String EVENT_ALERT_MESSAGE = "org.jembi.rad.mqttdemo.MESSAGE";

    // App online/offline event constants
    public final static String EVENT_BROKER_CONNECTION = "EVENT_BROKER_CONNECTION";
    public final static String EVENT_BROKER_CONNECTION_STATUS = "EVENT_BROKER_CONNECTION_STATUS";

    // App alert event constants
    public final static String EVENT_MESSAGE = "EVENT_MESSAGE";
    public final static String EVENT_MESSAGE_CONTENT = "org.jembi.rad.mqttdemo.MESSAGE_CONTENT";

    // Notification Alert constants
    private static long[] mVibratePattern = { 0, 200, 200, 300 };
    public static final int ALERT_NOTIFICATION_ID = 1;

    // MQTT constants
    private static int qos = 1;
    private static boolean cleanSession = false;
    private static boolean automaticReconnect = true;

    private MqttAndroidClient mqttAndroidClient;
    private String serverUri;
    private String clientId;
    private String topic;
    private String TAG=MessageService.class.getName();
    public MessageService() {}

    @Override
    public void onCreate() {
        Log.i(TAG, "Creating service to retrieve MQTT messages");

        serverUri="tcp://broker.hivemq.com:1883";
        clientId="56b4eeca-b4dd-4aee-a996-1d5feb0b2b20";
        topic="kunjan/send";

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                updateBrokerConnectionStatus(true);

                if (reconnect) {
                    displayAlert("Reconnected to : " + serverURI);
                    if (cleanSession) {
                        subscribeToTopic();
                    }
                } else {
                    displayAlert("Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                displayAlert("The Connection was lost.");
                updateBrokerConnectionStatus(false);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // this callback is unnecessary because we will request a separate callback after subscription to a topic
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // we aren't sending messages, so no need to implement this callback method
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(automaticReconnect);
        mqttConnectOptions.setCleanSession(cleanSession);


        try {
            Log.i(TAG, "Connecting to " + serverUri);
            IMqttToken token = mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to connect", exception);
                    displayAlert("Failed to connect to: " + serverUri);
                }
            });


        } catch (MqttException ex) {
            Log.e(TAG, "Exception while connecting", ex);
        }

        Log.i(TAG, "Finished creating service to retrieve MQTT messages");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Checking status of service receiving MQTT messages "+startId);
        updateBrokerConnectionStatus(mqttAndroidClient.isConnected());
        return START_STICKY;
    }

    public void closeMqttConnection(){
        try {
            mqttAndroidClient.close();
            mqttAndroidClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "I am dead to you ...");
        closeMqttConnection();
    }

    private void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(topic, qos, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    displayAlert("Subscribed to: " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    displayAlert("Failed to subscribe to: " + topic);
                }
            });

            mqttAndroidClient.subscribe(topic, qos, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    String messageContent = new String(message.getPayload());
                    receiveMessage(new Message(new Date(), messageContent));
                }
            });
            MqttMessage message = new MqttMessage();
            message.setPayload("hello".getBytes());

            mqttAndroidClient.publish("kunjan/receive", message);


        } catch (MqttException ex){
            Log.e(TAG, "Exception while subscribing", ex);
        }
    }

    private void receiveMessage(Message message) {
        Log.i(TAG, "Received an MQTT message: " + message.getMessage());

        if (!WeighingScaleApplication.appInForeground) {
            // if the app is paused or running in the background, then display a notification
            Context context = this.getApplicationContext();

                // Define the Notification Intent
                Intent mainActivityIntent = new Intent(context, MainActivity.class);
                PendingIntent mainActivityPendingIntent = PendingIntent.getActivity(context, 0,
                        mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                // Build the notification
                Builder notificationBuilder = new Builder(context)
                        .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                        .setContentTitle(context.getString(R.string.alert_notification_title))
                        .setSubText(context.getString(R.string.alert_notification_subtitle, topic))
                        .setTicker(context.getString(R.string.alert_notification_ticker))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message.getMessage()))
                        .setNumber(1)
                        .setContentIntent(mainActivityPendingIntent)
                        .setAutoCancel(true);

                // set the ringtone
//                String ringtone = preferences.getString(SettingsActivity.NOTIFICATION_NEW_MESSAGE_RINGTONE, null);
//                if (ringtone != null) {
//                    notificationBuilder.setSound(Uri.parse(ringtone));
//                }

                // set the vibration
               // notificationBuilder.setVibrate(mVibratePattern);


                // Pass the Notification to the NotificationManager:
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(ALERT_NOTIFICATION_ID, notificationBuilder.build());

                Log.i(TAG, "Created a system notification: " + notificationBuilder.toString());

        }

        // send the message to the SubscribeActivity to display to the user
        displayMessage(message);
    }

    private void displayAlert(String message) {
        Log.i(TAG, "Alert: " + message);
        Intent it = new Intent(EVENT_ALERT);
        if (!TextUtils.isEmpty(message)) {
            it.putExtra(EVENT_ALERT_MESSAGE, message);
        }
        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(it);
    }

    private void displayMessage(Message message) {
        Log.i(TAG, "Message: " + message);
        Intent it = new Intent(EVENT_MESSAGE);
        it.putExtra(EVENT_MESSAGE_CONTENT, message);
        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(it);
    }

    private void updateBrokerConnectionStatus(Boolean status) {
        Log.i(TAG, "Broker connection status: " + status);
        Intent it = new Intent(EVENT_BROKER_CONNECTION);
        it.putExtra(EVENT_BROKER_CONNECTION_STATUS, status);
        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(it);
    }
}