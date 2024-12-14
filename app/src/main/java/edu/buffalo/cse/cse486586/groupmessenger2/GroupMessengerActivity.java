package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";

    static final int SERVER_PORT = 10000;

    private ContentResolver mContentResolver;
    private ContentValues mContentValue;
    private Uri mUri;
    static int msgCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        mContentResolver = getContentResolver();
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        Log.v("myport", myPort);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText sendMessage = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = sendMessage.getText().toString() + "\n";
                sendMessage.setText(""); // This is one way to reset the input box.
                // tv.append("\t" + msg); // This is one way to display a string.
                Log.v("send msg: ", msg);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT0);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT1);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT2);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT3);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT4);
            }
        });

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {


        @Override
        protected Void doInBackground(String... msgs) {
            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(msgs[1]));

                String msgToSend = msgs[0];
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF(msgToSend);
                out.flush();
                // out.close();
                // socket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
            return null;
        }
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket socket = null;

            while (true) {
                try {
                    socket = serverSocket.accept();
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String inputMsg = in.readUTF();

                    if(inputMsg == null) {
                        publishProgress("");
                        return null;
                    }
                    publishProgress(inputMsg);

                    publishProgress(inputMsg);
                    String key = Integer.toString(msgCount);
                    mContentValue = new ContentValues();
                    mContentValue.put("key", Integer.toString(msgCount));
                    mContentValue.put("value", inputMsg);
                    mContentResolver.insert(mUri, mContentValue);
                    msgCount++;
                    Log.v("recieved msg: " + key, inputMsg);
                } catch (Exception e) {
                    Log.e(TAG, "ServerTask socket Exception");
                }
            }

        }

        @Override
        protected void onProgressUpdate(String...strings) {
            return ;
        }


    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
}
