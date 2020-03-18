package io.agora.vlive.ui.live;

import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.agora.vlive.R;
import io.agora.vlive.proxy.model.SeatInfo;
import io.agora.vlive.proxy.model.UserProfile;
import io.agora.vlive.proxy.struts.response.AudienceListResponse;
import io.agora.vlive.proxy.struts.response.EnterRoomResponse;
import io.agora.vlive.ui.actionsheets.InviteUserActionSheet;
import io.agora.vlive.ui.components.LiveHostInSeatAdapter;
import io.agora.vlive.ui.components.LiveMessageEditLayout;
import io.agora.vlive.ui.components.LiveRoomMessageList;
import io.agora.vlive.ui.components.SeatItemDialog;

public class HostInLiveActivity extends LiveRoomActivity implements View.OnClickListener,
        LiveHostInSeatAdapter.LiveHostInSeatOnClickedListener,
        InviteUserActionSheet.InviteUserActionSheetListener,
        SeatItemDialog.OnSeatDialogItemClickedListener {
    private static final String TAG = HostInLiveActivity.class.getSimpleName();

    private static final int ROOM_NAME_HINT_COLOR = Color.rgb(101, 101, 101);
    private static final int ROOM_NAME_COLOR = Color.rgb(235, 235, 235);

    private boolean mInitCalled;
    private int mRadius;

    private RecyclerView mSeatRecyclerView;
    private LiveHostInSeatAdapter mSeatAdapter;
    private AppCompatImageView mOwnerIcon;
    private RelativeLayout mVideoContainer;
    private TextureView mOwnerTextureView;

    private InviteUserActionSheet mInviteUserListActionSheet;

    // Generated by back end server according to room id
    private List<SeatInfo> mSeatInfoList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_in);
        hideStatusBar(false);
        mRadius = getResources().getDimensionPixelSize(R.dimen.live_host_in_owner_video_radius);
        initUI();
    }

    @Override
    protected void onPermissionGranted() {
        initUI();
    }

    private void initUI() {
        if (!mInitCalled) {
            mInitCalled = true;
            return;
        }

        setRoomNameText();

        participants = findViewById(R.id.host_in_participant);
        participants.setUserLayoutListener(this);
        participants.setIconResource("fake_icon_2.jpeg");
        participants.setIconResource("fake_icon_3.jpeg");
        participants.setIconResource("fake_icon_2.jpeg");
        participants.setIconResource("fake_icon_3.jpeg");

        messageList = findViewById(R.id.message_list);
        messageList.addMessage(LiveRoomMessageList.MSG_TYPE_CHAT, "康康有毒", "他说会因为那一分钟而永远记住我，那时候我觉得很动听。但现在我看着时钟，我就告诉我自己，我要从这一分钟开始忘掉");
        messageList.addMessage(LiveRoomMessageList.MSG_TYPE_CHAT, "康康有毒", "他说会因为那一分钟而永远记住我，那时候我觉得很动听。但现在我看着时钟，我就告诉我自己，我要从这一分钟开始忘掉");
        messageList.addMessage(LiveRoomMessageList.MSG_TYPE_CHAT, "起司甜甜", "何必在乎其它人");
        messageList.addMessage(LiveRoomMessageList.MSG_TYPE_CHAT, "康康有毒", "他说会因为那一分钟而永远记住我，那时候我觉得很动听。但现在我看着时钟，我就告诉我自己，我要从这一分钟开始忘掉");
        messageList.addMessage(LiveRoomMessageList.MSG_TYPE_CHAT, "起司甜甜", "何必在乎其它人");
        messageList.addMessage(LiveRoomMessageList.MSG_TYPE_CHAT, "起司甜甜", "何必在乎其它人");
        messageList.addMessage(LiveRoomMessageList.MSG_TYPE_CHAT, "起司甜甜", "何必在乎其它人");
        messageList.notifyDataSetChanged();
        messageList.scrollToPosition(messageList.getAdapter().getItemCount() - 1);

        mSeatRecyclerView = findViewById(R.id.live_host_in_seat_recycler);
        GridLayoutManager layoutManager = new GridLayoutManager(this,
                3, RecyclerView.VERTICAL, false);
        mSeatRecyclerView.setLayoutManager(layoutManager);
        mSeatAdapter = new LiveHostInSeatAdapter(this);
        mSeatAdapter.setIsRoomOwner(isHost);
        mSeatAdapter.setSeatListener(this);
        mSeatRecyclerView.setAdapter(mSeatAdapter);

        bottomButtons = findViewById(R.id.host_in_bottom_layout);
        bottomButtons.setLiveBottomButtonListener(this);
        bottomButtons.setHost(isHost);
        if (isOwner) bottomButtons.setBeautyEnabled(config().isBeautyEnabled());

        findViewById(R.id.live_bottom_btn_close).setOnClickListener(this);
        findViewById(R.id.live_bottom_btn_more).setOnClickListener(this);
        findViewById(R.id.live_bottom_btn_fun1).setOnClickListener(this);
        findViewById(R.id.live_bottom_btn_fun2).setOnClickListener(this);

        messageEditLayout = findViewById(R.id.message_edit_layout);
        mMessageEditText = messageEditLayout.findViewById(LiveMessageEditLayout.EDIT_TEXT_ID);

        mVideoContainer = findViewById(R.id.host_video_layout);
        initOwner();
    }

    private void initOwner() {
        mOwnerIcon = findViewById(R.id.host_in_owner_default_image);
        mOwnerIcon.setVisibility(View.VISIBLE);
        mOwnerIcon.setOutlineProvider(new RoomOwnerOutline());
        mOwnerIcon.setClipToOutline(true);
        mOwnerIcon.setImageResource(R.drawable.default_portrait_gray);

        if (isOwner && !config().isVideoMuted()) {
            mOwnerIcon.setVisibility(View.GONE);
            mOwnerTextureView = new TextureView(this);
            mOwnerTextureView.setClipToOutline(true);
            mOwnerTextureView.setOutlineProvider(new RoomOwnerOutline());
            mVideoContainer.addView(mOwnerTextureView);
            cameraProxy().setRenderView(mOwnerTextureView);
        }
    }

    private void setRoomNameText() {
        String nameHint = getResources().getString(R.string.live_host_in_room_name_hint);
        SpannableString name = new SpannableString(nameHint + roomName);
        name.setSpan(new ForegroundColorSpan(ROOM_NAME_HINT_COLOR),
                0, nameHint.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        name.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.text_size_medium)),
                0, nameHint.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        name.setSpan(new ForegroundColorSpan(ROOM_NAME_COLOR),
                nameHint.length(), name.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        name.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.text_size_normal)),
                nameHint.length(), name.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

        ((AppCompatTextView) findViewById(R.id.host_in_room_name)).setText(name);
    }

    @Override
    protected void onGlobalLayoutCompleted() {
        View topLayout = findViewById(R.id.host_in_top_participant_layout);
        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) topLayout.getLayoutParams();
        params.topMargin += systemBarHeight;
        topLayout.setLayoutParams(params);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.live_bottom_btn_close:
                curDialog = showDialog(R.string.finish_broadcast_title,
                        R.string.finish_broadcast_message, this);
                break;
            case R.id.live_bottom_btn_more:
                showActionSheetDialog(ACTION_SHEET_TOOL, isHost, true, this);
                break;
            case R.id.live_bottom_btn_fun1:
                if (isHost) {
                    showActionSheetDialog(ACTION_SHEET_BG_MUSIC, true, true, this);
                } else {
                    showActionSheetDialog(ACTION_SHEET_GIFT, false, true, this);
                }
                break;
            case R.id.live_bottom_btn_fun2:
                if (isHost) {
                    // this button is hidden when current user is not host.
                    showActionSheetDialog(ACTION_SHEET_BEAUTY, true, true, this);
                }
                break;
            case R.id.dialog_positive_button:
                closeDialog();
                finish();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        curDialog = showDialog(R.string.finish_broadcast_title,
                R.string.finish_broadcast_message,
                view -> {
                    closeDialog();
                    finish();
                });
    }

    @Override
    public void finish() {
        super.finish();
        bottomButtons.clearStates(application());
    }

    @Override
    public void onEnterRoomResponse(EnterRoomResponse response) {
        super.onEnterRoomResponse(response);
        mSeatInfoList = response.data.room.coVideoSeats;
        runOnUiThread(() -> mSeatAdapter.resetSeatStates(mSeatInfoList));
    }

    @Override
    public void onSeatAdapterHostInviteClicked(int position, View view) {
        Log.i(TAG, "onSeatAdapterHostInviteClicked:" + position);
        mInviteUserListActionSheet = (InviteUserActionSheet) showActionSheetDialog(
                ACTION_SHEET_INVITE_AUDIENCE, isHost, true, this);
        requestAudienceList();
    }

    private void requestAudienceList() {

    }

    @Override
    public void onAudienceListResponse(AudienceListResponse response) {

    }

    @Override
    public void onSeatAdapterAudienceApplyClicked(int position, View view) {
        Log.i(TAG, "onSeatAdapterAudienceApplyClicked:" + position);
        curDialog = showDialog(R.string.live_room_host_in_audience_apply_title,
                R.string.live_room_host_in_audience_apply_message,
                v -> {
                    audienceApplyForSeat();
                    closeDialog();
                });
    }

    @Override
    public void onSeatAdapterMoreClicked(int position, View view, LiveHostInSeatAdapter.SeatState state) {
        Log.i(TAG, "onSeatAdapterMoreClicked");
        int mode = isOwner ? SeatItemDialog.MODE_OWNER : SeatItemDialog.MODE_HOST;
        SeatItemDialog dialog = new SeatItemDialog(this, state,
                mode, view, position, this);
        dialog.show();
    }

    @Override
    public void onSeatDialogItemClicked(int position, SeatItemDialog.Operation operation) {
        Log.i(TAG, "onSeatDialogItemClicked: position=" + position + " operation:" + operation.toString());
    }

    private void audienceApplyForSeat() {
        Log.i(TAG, "audience apply for a seat:");
    }

    @Override
    public void onSeatAdapterPositionClosed(int position, View view) {
        Log.i(TAG, "onSeatAdapterAudienceApplyClicked:" + position);
    }

    @Override
    public void onActionSheetAudienceInvited(UserProfile user) {
        Log.i(TAG, "onActionSheetAudienceInvited");
    }

    @Override
    public void onRtmHostStateChanged(String uid, int index, int operate) {

    }

    private class RoomOwnerOutline extends ViewOutlineProvider {
        @Override
        public void getOutline(View view, Outline outline) {
            Rect rect = new Rect();
            view.getGlobalVisibleRect(rect);
            int leftMargin = 0;
            int topMargin = 0;
            Rect selfRect = new Rect(leftMargin, topMargin,
                    rect.right - rect.left - leftMargin,
                    rect.bottom - rect.top - topMargin);
            outline.setRoundRect(selfRect, mRadius);
        }
    }
}