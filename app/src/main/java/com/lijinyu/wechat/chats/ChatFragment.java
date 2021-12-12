package com.lijinyu.wechat.chats;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wechat.R;
import com.lijinyu.wechat.common.NodeNames;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

///**
// * A simple {@link Fragment} subclass.
// * Use the {@link ChatFragment#newInstance} factory method to
// * create an instance of this fragment.
// */
public class ChatFragment extends Fragment {

//    // TODO: Rename parameter arguments, choose names that match
//    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
//
//    // TODO: Rename and change types of parameters
//    private String mParam1;
//    private String mParam2;
    private RecyclerView rvChatList;
    private View progressBar;
    private TextView tvEmptyChatList;
    private ChatListAdapter chatListAdapter;
    private List<ChatListModel> chatListModelList;

    private DatabaseReference databaseReferenceChats, databaseReferenceUsers;
    private FirebaseUser currentUser;
    private ChildEventListener childEventListener;
    private Query query;

    public ChatFragment() {
        // Required empty public constructor
    }

//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
//     * @return A new instance of fragment ChatFragment.
//     */
    // TODO: Rename and change types and number of parameters
//    public static ChatFragment newInstance(String param1, String param2) {
//        ChatFragment fragment = new ChatFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
//        return fragment;
//    }

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
//        return super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChatList = view.findViewById(R.id.rvChats);
        tvEmptyChatList = view.findViewById(R.id.tvEmptyChatList);

        chatListModelList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(getActivity(), chatListModelList);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        rvChatList.setLayoutManager(linearLayoutManager);
        rvChatList.setAdapter(chatListAdapter);

        progressBar = view.findViewById(R.id.progressBar);

        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceChats = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS).child(currentUser.getUid());
        query = databaseReferenceChats.orderByChild(NodeNames.TIME_STAMP);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot,
                                     @Nullable String previousChildName) {
                updateList(snapshot, true, snapshot.getKey());
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

            }
        };

        query.addChildEventListener(childEventListener);


        progressBar.setVisibility(View.GONE);
        tvEmptyChatList.setVisibility(View.VISIBLE);
    }

    private void updateList(DataSnapshot snapshot, boolean isNew, String userId) {

        progressBar.setVisibility(View.GONE);
        tvEmptyChatList.setVisibility(View.GONE);

        String lastMessage, lastMessageTime, unreadCount;

        lastMessage = "";
        lastMessageTime = "";
        unreadCount = "";

        databaseReferenceUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String fullName = (snapshot.child(NodeNames.NAME).getValue()) != null ? snapshot.child(NodeNames.NAME).getValue().toString() : "";
                String photoName = (snapshot.child(NodeNames.PHOTO).getValue()) != null ? snapshot.child(NodeNames.PHOTO).getValue().toString() : "";

                ChatListModel chatListModel = new ChatListModel(userId, fullName, photoName, unreadCount, lastMessageTime);
                chatListModelList.add(chatListModel);
                chatListAdapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), getActivity().getString(R.string.failed_to_fetch_chat_list, error.getMessage()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        query.removeEventListener(childEventListener);
    }
}