package com.lijinyu.wechat.chats;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.wechat.R;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;
import com.lijinyu.wechat.common.Constants;
import com.lijinyu.wechat.common.Extras;
import com.lijinyu.wechat.common.NodeNames;
import com.lijinyu.wechat.common.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView ivSend, ivAttachment, ivUnfriend;
    private EditText etMessage;
    private DatabaseReference mRootRef;
    private FirebaseAuth firebaseAuth;
    private String currentUserId, chatUserId;
    private AlertDialog alert;
    private AlertDialog.Builder builder;
    private Context mContext;

    private RecyclerView rvMessages;
    private SwipeRefreshLayout srlMessages;
    private MessageAdapter messagesAdapter;
    private List<MessageModel> messageList;

    private int currentPage = 1;
    private static final int RECORD_PER_PAGE = 30;

    private DatabaseReference databaseReferenceMessages;
    private ChildEventListener childEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ivSend = findViewById(R.id.ivSend);
        ivAttachment = findViewById(R.id.ivAttachment);
        ivUnfriend = findViewById(R.id.ivUnfriend);
        etMessage = findViewById(R.id.etMessage);

        mContext = ChatActivity.this;

        ivSend.setOnClickListener(this::onClick);
        ivAttachment.setOnClickListener(this::onClick);
        ivUnfriend.setOnClickListener(this::onClick);
        firebaseAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        if (getIntent().hasExtra(Extras.USER_KEY)) {
            chatUserId = getIntent().getStringExtra(Extras.USER_KEY);
        }

        rvMessages = findViewById(R.id.rvMessage);
        srlMessages = findViewById(R.id.srlMessage);

        messageList = new ArrayList<>();
        messagesAdapter = new MessageAdapter(this, messageList);

        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messagesAdapter);

        loadMessages();
        rvMessages.scrollToPosition(messageList.size() - 1);
        srlMessages.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                currentPage++;
                loadMessages();
            }
        });

    }

    private void sendMessage(String msg, String msgType, String pushId) {
        try {
            if (!msg.equals("")) {
                HashMap messageMap = new HashMap();
                messageMap.put(NodeNames.MESSAGE_ID, pushId);
                messageMap.put(NodeNames.MESSAGE, msg);
                messageMap.put(NodeNames.MESSAGE_TYPE, msgType);
                messageMap.put(NodeNames.MESSAGE_FROM, currentUserId);
                messageMap.put(NodeNames.MESSAGE_TIME, ServerValue.TIMESTAMP);

                String currentUserRef = NodeNames.MESSAGES + "/" + currentUserId + "/" + chatUserId;
                String chatUserRef = NodeNames.MESSAGES + "/" + chatUserId + "/" + currentUserId;

                HashMap messageUserMap = new HashMap();
                messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
                messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

                etMessage.setText("");

                mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                        if (error != null) {
                            Toast.makeText(ChatActivity.this, getString(R.string.failed_to_send_message, error.getMessage()), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ChatActivity.this, R.string.message_sent_successfully, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (Exception exception) {
            Toast.makeText(ChatActivity.this, getString(R.string.failed_to_send_message, exception.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMessages() {
        messageList.clear();
        databaseReferenceMessages = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId);

        Query messageQuery = databaseReferenceMessages.limitToLast(currentPage * RECORD_PER_PAGE);

        if (childEventListener != null) {
            messageQuery.removeEventListener(childEventListener);
        }

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot,
                                     @Nullable String previousChildName) {
                MessageModel message = snapshot.getValue(MessageModel.class);
                messageList.add(message);
                messagesAdapter.notifyDataSetChanged();
                rvMessages.scrollToPosition(messageList.size() - 1);
                srlMessages.setRefreshing(false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot,
                                       @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                srlMessages.setRefreshing(false);
            }
        };
        messageQuery.addChildEventListener(childEventListener);
    }

    private void unfriend() {

        mRootRef.child(NodeNames.CHATS).child(chatUserId).child(currentUserId).removeValue();
        mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).removeValue();
        mRootRef.child(NodeNames.FRIEND_REQUESTS).child(chatUserId).child(currentUserId).removeValue();
        mRootRef.child(NodeNames.FRIEND_REQUESTS).child(currentUserId).child(chatUserId).removeValue();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivSend:
                if (Util.connectionAvailable(this)) {
                    DatabaseReference userMessagePush = mRootRef.child(NodeNames.MESSAGE).child(currentUserId).child(chatUserId).push();
                    String pushId = userMessagePush.getKey();
                    sendMessage(etMessage.getText().toString().trim(), Constants.MESSAGE_TYPE_TEXT, pushId);
                } else {
                    Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.ivAttachment:
                Toast.makeText(this, R.string.uncompleted, Toast.LENGTH_SHORT).show();
                break;
            case R.id.ivUnfriend:
                builder = new AlertDialog.Builder(mContext);
                alert = builder.setTitle(R.string.alert)
                        .setMessage(R.string.confirm_unfriend)
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                unfriend();
                                Toast.makeText(mContext, R.string.unfriended_successfully, Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }).create();
                alert.show();
        }
    }
}