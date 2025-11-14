package com.example.signify;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.client.Manager;

public class ServerClient {
    public static String TAG = "ServerClientDebug";

    private static String EVENT_AUTHENTICATION = "onAuthentication";
    private static String EVENT_RESPONSE = "onResponse";

    private static String EVENT_TRANSCRIPT = "onTranscriptGenerated";
    private Socket mSocket = null;
    // --- START OF CHANGES ---

    // 1. Set a placeholder for your computer's IP. Find this using 'ipconfig' in
    // your PC's terminal.
    private String mServerIp = "10.194.160.24";

    // 2. Set the correct port to match your Python server.
    private int mServerPort = 8088;

    // --- END OF CHANGES ---

    private String mUsername = null;
    private String mPassword = null;

    // A single callback to the client aimed to be registered by the current
    // Activity
    private ServerResultCallback mSingleCallback = null;

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private static ServerClient mInstance = null;

    private ServerClient() {
        // Private constructor is part of singleton implementation
    }

    public synchronized static ServerClient getInstance() {
        if (mInstance == null) {
            mInstance = new ServerClient();
        }
        return mInstance;
    }

    public void init(String username, String password, String serverIp, int port) {
        mUsername = username;
        mPassword = password;
        // Note: The UI input from the app will overwrite the default IP and Port.
        // The defaults are set above as a fallback.
        mServerIp = serverIp;
        mServerPort = port;

        if (mSocket == null) {
            try { // Try to create the socket with the server
                IO.Options options = new IO.Options();
                options.forceNew = true;
                options.multiplex = true;

                // --- START OF CHANGES ---
                // 3. Set to false because the Python server is running on standard HTTP.
                options.secure = false;
                // --- END OF CHANGES ---

                options.reconnection = true;
                options.reconnectionDelay = 5000;
                options.reconnectionAttempts = 15;
                options.timeout = 20000;
                String serverAddress = "http://" + mServerIp + ":" + mServerPort;
                mSocket = IO.socket(serverAddress, options);
                Log.d(TAG, "ServerClient initialized successfully for address: " + serverAddress);
            } catch (URISyntaxException e) {
                // We failed to connect, consider to inform the user
                e.printStackTrace();
                Log.e(TAG, "URISyntaxException: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "ServerClient already initialized. Clearing sockets. Try again...");
            mSocket.disconnect();
            mSocket = null;
        }
    }

    public void registerCallback(ServerResultCallback callback) {
        mSingleCallback = callback;
    }

    public void unregisterCallback() {
        mSingleCallback = null;
    }

    /**
     * The connection to the server is explicitly issued by client activities.
     * <p>
     * Register the socket listeners just before trying to connect, so we can
     * receive feedback
     * from the connection state.
     */
    public void connect() {
        if (mSocket != null && !mSocket.connected() && mUsername != null) {
            unregisterSocketListeners();
            registerSocketListeners();
            Log.d(TAG, "Attempting to connect to: http://" + mServerIp + ":" + mServerPort);
            mSocket.connect();
        } else {
            if (mSocket == null) {
                Log.e(TAG, "Cannot connect: Socket is null. Call init() first.");
            } else if (mSocket.connected()) {
                Log.d(TAG, "Already connected to server.");
            } else if (mUsername == null) {
                Log.e(TAG, "Cannot connect: Username is not defined.");
            }
        }
    }

    /**
     * This is main method issued by the client activity to stream pictures to the
     * server.
     */
    public void sendImage(byte[] image) {
        if (mSocket != null && mSocket.connected()) {
            mSocket.emit("receiveImage", image, EVENT_RESPONSE);
            Log.d(TAG, "✓ Sent frame to server (" + image.length + " bytes)");
        } else {
            Log.d(TAG, "Cannot send message because socket is null or disconnected");
        }
    }

    public void sendVideoFrame(byte[] image, int processedFrameCount) {
        boolean sent = false;
        int attempts = 0;

        while (!sent && attempts < 5) {
            if (mSocket != null && mSocket.connected()) {
                mSocket.emit("receiveVideoStream", image, processedFrameCount);
                sent = true;
            } else {
                Log.d(TAG, "Waiting for client to reconnect...");
                attempts++;
            }
        }
    }

    public void startTranscriptProcessing() {
        Log.d(TAG, "from startTranscriptProcessing: emitting processVideo");
        if (mSocket != null && mSocket.connected()) {
            mSocket.emit("processVideo");
        } else {
            Log.d(TAG, "Cannot get transcript because socket is null or disconnected");
        }
    }

    public void checkTranscript() {
        Log.d(TAG, "from checkTranscript: emitting checkTranscript");
        if (mSocket != null && mSocket.connected()) {
            mSocket.emit("checkTranscript", EVENT_TRANSCRIPT);
        } else {
            Log.d(TAG, "Cannot check transcript because socket is null or disconnected");
        }
    }

    public void getPrediction() {
        if (mSocket != null && mSocket.connected()) {
            mSocket.emit("stopRecord", EVENT_RESPONSE);
        } else {
            Log.d(TAG, "Cannot get prediction because socket is null or disconnected");
        }
    }

    /**
     * Client activities might issue an explicit disconnect at anytime.
     * <p>
     * Unregister the socket listeners after issuing the disconnect to free
     * resources.
     */
    public void disconnect() {
        if (mSocket != null) {
            Log.d(TAG, "randomly disconnected :(");
            mSocket.disconnect();
            unregisterSocketListeners();
        } else {
            Log.d(TAG, "Cannot disconnect because socket is null.");
        }
    }

    private void registerSocketListeners() {
        if (mSocket != null) {
            mSocket.on(Socket.EVENT_CONNECT, onConnected);
            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectionError);
            mSocket.on(Manager.EVENT_RECONNECT, onReconnecting);
            mSocket.on(Manager.EVENT_RECONNECT_ERROR, onReconnecting);
            mSocket.on(Socket.EVENT_DISCONNECT, onDisconnected);
            mSocket.on(EVENT_AUTHENTICATION, onAuthentication);
            mSocket.on(EVENT_RESPONSE, onResponse);
            mSocket.on(EVENT_TRANSCRIPT, onTranscriptGenerated);
            mSocket.on(Manager.EVENT_ERROR, onTimeout);
            mSocket.on(Manager.EVENT_ERROR, onEventError);
        } else {
            Log.d(TAG, "Cannot register listeners because socket is null.");
        }
    }

    private void unregisterSocketListeners() {
        if (mSocket != null) {
            mSocket.off(Socket.EVENT_CONNECT, onConnected);
            mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectionError);
            mSocket.off(Manager.EVENT_RECONNECT, onReconnecting);
            mSocket.off(Manager.EVENT_RECONNECT_ERROR, onReconnecting);
            mSocket.off(Socket.EVENT_DISCONNECT, onDisconnected);
            mSocket.off(EVENT_AUTHENTICATION, onAuthentication);
            mSocket.off(EVENT_RESPONSE, onResponse);
            mSocket.off(EVENT_TRANSCRIPT, onTranscriptGenerated);
            mSocket.off(Manager.EVENT_ERROR, onTimeout);
            mSocket.off(Manager.EVENT_ERROR, onEventError);
        } else {
            Log.d(TAG, "Cannot unregister listeners because socket is null.");
        }
    }

    /**
     * Callback functions for the socket listeners
     */
    private Emitter.Listener onConnected = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // We connected to the server successfully
            Log.d(TAG, "✓ Connected to server successfully!");
            Log.d(TAG, "Starting authentication with username: " + mUsername);
            mSocket.emit("authenticate", mUsername, mPassword, EVENT_AUTHENTICATION);
        }
    };

    private Emitter.Listener onConnectionError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // We got an error while trying to connect
            // The socket will try to reconnect automatically as many times we set on the
            // options
            String reason = "no reason received.";
            if (args.length > 0) {
                reason = args[0].toString();
            }
            Log.e(TAG, "Error while trying to connect: " + reason);
            Log.e(TAG, "Server address: http://" + mServerIp + ":" + mServerPort);
            Log.e(TAG, "Please ensure:");
            Log.e(TAG, "1. Python server is running: python ImageServer.py");
            Log.e(TAG, "2. Server IP " + mServerIp + " is correct (check with ipconfig/ifconfig)");
            Log.e(TAG, "3. Device and server are on the same network");
            Log.e(TAG, "4. Firewall allows port " + mServerPort);
        }
    };

    private Emitter.Listener onReconnecting = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // Socket is trying to reconnect automatically
            Log.d(TAG, "Reconnecting to the server...");
        }
    };

    private Emitter.Listener onReconnectionError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // We fail to reconnect
            String reason = "no reason received.";
            if (args.length > 0) {
                reason = args[0].toString();
            }
            Log.e(TAG, "Reconnection failed: " + reason);
        }
    };

    private Emitter.Listener onDisconnected = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // We were disconnected from the server
            String reason = "no reason received.";
            if (args.length > 0) {
                reason = args[0].toString();
            }
            Log.d(TAG, "Disconnected from the server: " + reason);
        }
    };

    private Emitter.Listener onAuthentication = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            boolean result = (boolean) args[0];
            if (mSingleCallback != null) {
                mainHandler.post(() -> mSingleCallback.onConnected(result));
            }
            Log.d(TAG, "onAuthentication: " + result);
        }
    };

    private Emitter.Listener onResponse = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "✓ Received response from server, args length = " + args.length);
            JSONObject data = (JSONObject) args[0];
            String finalResult = "";
            boolean finalIsGloss = false;

            try {
                if (data.has("result")) {
                    finalResult = data.getString("result");
                    Log.d(TAG, "Result: " + finalResult);
                }
                if (data.has("isGloss")) {
                    finalIsGloss = data.getBoolean("isGloss");
                    Log.d(TAG, "IsGloss: " + finalIsGloss);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing response JSON: " + e.getMessage());
                e.printStackTrace();
            }

            // Use new variables that are effectively final for the lambda.
            final String resultForLambda = finalResult;
            final boolean isGlossForLambda = finalIsGloss;

            if (mSingleCallback != null) {
                Log.d(TAG, "Calling displayResponse callback with: " + resultForLambda);
                mainHandler.post(() -> mSingleCallback.displayResponse(resultForLambda, isGlossForLambda));
            } else {
                Log.e(TAG, "No callback registered to display response!");
            }

            Log.d(TAG, "onResponse completed: " + resultForLambda);
        }
    };

    private Emitter.Listener onTranscriptGenerated = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            String result = (String) args[0];
            if (mSingleCallback != null)
                mainHandler.post(() -> mSingleCallback.addNewTranscript(result));
            Log.d(TAG, "onTranscriptGenerated: " + result);
        }
    };

    private Emitter.Listener onTimeout = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            String reason = "no reason receiveddd";
            if (args.length > 0) {
                reason = args[0].toString();
            }
            Log.e(TAG, "Connection timed out! Reason: " + reason);
        }
    };

    private Emitter.Listener onEventError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // Something went wrong with an event
            String reason = "no reason received.";
            if (args.length > 0) {
                reason = args[0].toString();
            }
            Log.e(TAG, "Something went wrong with the last event: " + reason);
        }
    };
}
