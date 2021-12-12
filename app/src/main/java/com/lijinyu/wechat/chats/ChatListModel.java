package com.lijinyu.wechat.chats;

public class ChatListModel {
    private String userId;
    private String userName;
    private String photoName;
    private String unreadCount;
    private String lastMessageTime;

    public ChatListModel(String userId, String userName, String photoName, String unreadCount,
                         String lastMessageTime) {
        this.userId = userId;
        this.userName = userName;
        this.photoName = photoName;
        this.unreadCount = unreadCount;
        this.lastMessageTime = lastMessageTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhotoName() {
        return photoName;
    }

    public void setPhotoName(String photoName) {
        this.photoName = photoName;
    }

    public String getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(String unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
}
